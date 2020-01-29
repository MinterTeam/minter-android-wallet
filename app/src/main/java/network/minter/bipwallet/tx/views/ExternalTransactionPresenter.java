package network.minter.bipwallet.tx.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.annimon.stream.Optional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.CoinAccount;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CacheTxRepository;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction;
import network.minter.bipwallet.internal.helpers.DeepLinkHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.tx.contract.ExternalTransactionView;
import network.minter.bipwallet.tx.contract.TxInitData;
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.CheckTransaction;
import network.minter.blockchain.models.operational.ExternalTransaction;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.models.operational.TxCoinBuy;
import network.minter.blockchain.models.operational.TxCoinSell;
import network.minter.blockchain.models.operational.TxCoinSellAll;
import network.minter.blockchain.models.operational.TxCreateCoin;
import network.minter.blockchain.models.operational.TxDeclareCandidacy;
import network.minter.blockchain.models.operational.TxDelegate;
import network.minter.blockchain.models.operational.TxEditCandidate;
import network.minter.blockchain.models.operational.TxMultisend;
import network.minter.blockchain.models.operational.TxRedeemCheck;
import network.minter.blockchain.models.operational.TxSendCoin;
import network.minter.blockchain.models.operational.TxSetCandidateOffline;
import network.minter.blockchain.models.operational.TxSetCandidateOnline;
import network.minter.blockchain.models.operational.TxUnbound;
import network.minter.core.MinterSDK;
import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.util.RLPBoxed;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.HistoryTransaction;
import network.minter.explorer.repo.GateEstimateRepository;
import network.minter.explorer.repo.GateGasRepository;
import network.minter.explorer.repo.GateTransactionRepository;
import timber.log.Timber;

import static network.minter.bipwallet.apis.reactive.ReactiveGate.rxGate;
import static network.minter.bipwallet.apis.reactive.ReactiveGate.toGateError;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.clamp;
import static network.minter.core.internal.helpers.StringHelper.hexStringToChars;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@InjectViewState
public class ExternalTransactionPresenter extends MvpBasePresenter<ExternalTransactionView> {
    @Inject SecretStorage secretStorage;
    @Inject GateEstimateRepository estimateRepo;
    @Inject GateGasRepository gasRepo;
    @Inject GateTransactionRepository gateTxRepo;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    @Inject
    CachedRepository<List<HistoryTransaction>, CacheTxRepository> cachedTxRepo;
    private ExternalTransaction mExtTx;
    private MinterAddress mFrom;
    private BytesData mPayload;
    private String mCheckPassword = null;

    @Inject
    public ExternalTransactionPresenter() {

    }

    @Override
    public void attachView(ExternalTransactionView view) {
        super.attachView(view);
        accountStorage.update();
        mFrom = secretStorage.getAddresses().get(0);
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        mFrom = secretStorage.getAddresses().get(0);

        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            Bundle params = intent.getExtras();
            if (params == null) {
                getViewState().finishCancel();
                return;
            }

            String rawPass;
            try {
                rawPass = params.getString("p", null);
                if (rawPass == null) {
                    mCheckPassword = null;
                } else {
                    mCheckPassword = RLPBoxed.decodeString(hexStringToChars(rawPass));
                }
            } catch (Throwable t) {
                Timber.w(t, "Unable to decode check password");
                mCheckPassword = null;
            }

            final String hash = params.getString("d", null);
            try {
                mExtTx = DeepLinkHelper.parseRawTransaction(hash);
                if (!validateTx()) {
                    return;
                }
            } catch (StringIndexOutOfBoundsException t) {
                getViewState().disableAll();
                Timber.w(t, "Unable to parse remote transaction: %s", hash);
                showTxErrorDialog("Invalid transaction data: non-hex string passed");
                return;
            } catch (Throwable t) {
                getViewState().disableAll();
                Timber.w(t, "Unable to parse remote transaction: %s", hash);
                showTxErrorDialog("Invalid transaction data: %s", t.getMessage());
                return;
            }
        } else {
            String rawTx = intent.getStringExtra(ExternalTransactionActivity.EXTRA_RAW_DATA);
            try {
                if (rawTx == null) {
                    throw new InvalidExternalTransaction("Empty transaction data", InvalidExternalTransaction.CODE_INVALID_TX);
                }
                Intent out = DeepLinkHelper.rawTxToIntent(rawTx);
                handleExtras(out);
                return;
            } catch (Throwable t) {
                getViewState().disableAll();
                Timber.w(t, "Unable to parse remote transaction: %s", rawTx);
                showTxErrorDialog("Invalid transaction data: %s", t.getMessage());
                return;
            }
        }

        mPayload = mExtTx.getPayload();
        calculateFee(mExtTx);
        try {
            fillData(mExtTx);
        } catch (Throwable t) {
            showTxErrorDialog("Invalid transaction data: %s", t.getMessage());
        }
    }

    private void showTxErrorDialog(String message, Object... args) {
        getViewState().startDialog(false, ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to scan transaction")
                .setText(message, args)
                .setPositiveAction(R.string.btn_close, (d, w) -> {
                    d.dismiss();
                    getViewState().finishCancel();
                })
                .create());
    }

    private boolean validateTx() {
        if (mExtTx.getType() == OperationType.RedeemCheck) {
            TxRedeemCheck d = mExtTx.getData();
            if (d.getProof().size() == 0 && mCheckPassword == null) {
                getViewState().disableAll();

                getViewState().startDialog(false, ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to scan transaction")
                        .setText("This check given without proof and password. One of parameters is required.")
                        .setPositiveAction(R.string.btn_close, (_d, _w) -> {
                            _d.dismiss();
                            getViewState().finishCancel();
                        })
                        .create());
                return false;
            } else if (d.getProof().size() == 0 && mCheckPassword != null) {
                d.setProof(CheckTransaction.makeProof(mFrom, mCheckPassword));
            }
        }

        return true;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        getViewState().setPayloadTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mPayload = new BytesData(s.toString().getBytes());
                calculateFee(mExtTx);
            }
        });
    }

    protected Observable<TxInitData> getTxInitData(MinterAddress address) {
        return Observable.combineLatest(
                rxGate(estimateRepo.getTransactionCount(address)),
                rxGate(gasRepo.getMinGas()),
                (txCountGateResult, gasValueGateResult) -> {

                    // if some request failed, returning error result
                    if (!txCountGateResult.isOk()) {
                        return new TxInitData(GateResult.copyError(txCountGateResult));
                    } else if (!gasValueGateResult.isOk()) {
                        return new TxInitData(GateResult.copyError(gasValueGateResult));
                    }

                    return new TxInitData(
                            txCountGateResult.result.count.add(BigInteger.ONE),
                            gasValueGateResult.result.gas

                    );
                }
        );

    }

    private void calculateFee(ExternalTransaction tx) {
        long bytesLen = firstNonNull(mPayload, tx.getPayload(), new BytesData(new char[0])).size();
        BigDecimal baseFee = tx.getType().getFee();
        if (tx.getType().equals(OperationType.Multisend)) {
            TxMultisend txData = tx.getData(TxMultisend.class);
            if (txData.getItems().size() == 0) {
                onFailedExecuteTransaction(new Exception("Multisend transaction must contains at least 1 target address"));
                return;
            }

            //10+(n-1)*5 units
            baseFee = OperationType.SendCoin.getFee();
            baseFee = baseFee.add(
                    new BigDecimal(clamp(txData.getItems().size() - 1, 0)).multiply(OperationType.FEE_BASE.multiply(new BigDecimal("5")))
            );
        } else if (tx.getType().equals(OperationType.CreateCoin)) {
            // https://docs.minter.network/#section/Commissions
            final TxCreateCoin txData = tx.getData(TxCreateCoin.class);
            baseFee = TxCreateCoin.calculateCreatingCost(txData.getSymbol());
        }

        BigDecimal fee = baseFee.add(new BigDecimal(bytesLen).multiply(new BigDecimal("0.002")));

        getViewState().setCommission(String.format("%s %s", fee, MinterSDK.DEFAULT_COIN));
    }

    private void fillData(ExternalTransaction tx) {
        getViewState().setPayload(tx.getPayloadString());
        calculateFee(tx);
        getViewState().setSecondVisible(View.VISIBLE);

        getViewState().setOnConfirmListener(this::onSubmit);
        getViewState().setOnCancelListener(this::onCancel);

        switch (tx.getType()) {
            case SendCoin: {
                final TxSendCoin data = tx.getData(TxSendCoin.class);
                getViewState().setFirstLabel("You're sending");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getValue()), data.getCoin()));
                getViewState().setSecondLabel("To");
                getViewState().setSecondValue(data.getTo().toString());
            }
            break;

            case SellCoin: {
                final TxCoinSell data = tx.getData(TxCoinSell.class);
                getViewState().setFirstLabel("You're want selling");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getValueToSell()), data.getCoinToSell()));
                getViewState().setSecondLabel("To");
                getViewState().setSecondValue(data.getCoinToBuy());
            }
            break;

            case SellAllCoins: {
                final TxCoinSellAll data = tx.getData(TxCoinSellAll.class);
                getViewState().setFirstLabel("You're selling");
                Optional<CoinAccount> acc = accountStorage.getData().findAccountByCoin(data.getCoinToSell());
                if (acc.isPresent()) {
                    getViewState().setFirstValue(String.format("%s %s", bdHuman(acc.get().getBalance()), data.getCoinToSell()));
                }

                getViewState().setSecondLabel("For");
                getViewState().setSecondValue(data.getCoinToBuy());
            }
            break;

            case BuyCoin: {
                final TxCoinBuy data = tx.getData(TxCoinBuy.class);
                getViewState().setFirstLabel("You're buying");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getValueToBuy()), data.getCoinToBuy()));
                getViewState().setSecondLabel("For");
                getViewState().setSecondValue(data.getCoinToSell());
            }
            break;

            case CreateCoin: {
                final TxCreateCoin data = tx.getData(TxCreateCoin.class);
                getViewState().setFirstLabel("You're creating coin");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getInitialAmount()), data.getSymbol()));
                getViewState().setSecondLabel("Info");
                getViewState().setSecondValue(String.format("Name: %s\nCRR: %d\nReserve: %s",
                        data.getName(),
                        data.getConstantReserveRatio(),
                        bdHuman(data.getInitialReserve())
                ));
            }
            break;

            case DeclareCandidacy: {
                final TxDeclareCandidacy data = tx.getData(TxDeclareCandidacy.class);
                getViewState().setFirstLabel("You're declaring candidacy");
                getViewState().setFirstValue(data.getPublicKey().toString());
                getViewState().setSecondLabel("Info");
                getViewState().setSecondValue(String.format("Address: %s\nCoin: %s\nCommission: %d",
                        data.getAddress(),
                        data.getCoin(),
                        data.getCommission()
                ));
            }
            break;
            case Delegate: {
                final TxDelegate data = tx.getData(TxDelegate.class);
                getViewState().setFirstLabel("You're delegating");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getStake()), data.getCoin()));
                getViewState().setSecondLabel("To");
                getViewState().setSecondValue(data.getPublicKey().toString());
            }
            break;
            case Unbound: {
                final TxUnbound data = tx.getData(TxUnbound.class);
                getViewState().setFirstLabel("You're unbonding");
                getViewState().setFirstValue(String.format("%s %s", bdHuman(data.getValue()), data.getCoin()));
                getViewState().setSecondLabel("From");
                getViewState().setSecondValue(data.getPublicKey().toString());
            }
            break;
            case RedeemCheck: {
                final TxRedeemCheck data = tx.getData(TxRedeemCheck.class);
                getViewState().setFirstLabel("You're using check");
                CheckTransaction check = data.getDecodedCheck();
                getViewState().setFirstValue(String.format("%s %s", check.getCoin(), bdHuman(check.getValue())));
                getViewState().setSecondVisible(View.GONE);
            }
            break;
            case SetCandidateOnline: {
                final TxSetCandidateOnline data = tx.getData(TxSetCandidateOnline.class);
                getViewState().setFirstLabel("You're switching on candidate");
                getViewState().setFirstValue(data.getPublicKey().toString());
                getViewState().setSecondVisible(View.GONE);
            }
            break;
            case SetCandidateOffline: {
                final TxSetCandidateOffline data = tx.getData(TxSetCandidateOffline.class);
                getViewState().setFirstLabel("You're switching off candidate");
                getViewState().setFirstValue(data.getPublicKey().toString());
                getViewState().setSecondVisible(View.GONE);
            }
            break;

            case Multisend: {
                final TxMultisend data = tx.getData(TxMultisend.class);
                getViewState().setFirstLabel("You're multi-sending");
                StringBuilder sb = new StringBuilder();
                for (TxSendCoin item : data.getItems()) {
                    sb.append(item.getTo().toShortString()).append(" <- ").append(bdHuman(item.getValue())).append('\n');
                }
                getViewState().setFirstValue(sb.toString());
                getViewState().setSecondVisible(View.GONE);
            }
            break;

            case EditCandidate: {
                final TxEditCandidate data = tx.getData(TxEditCandidate.class);
                getViewState().setFirstLabel("You're editing candidate");
                getViewState().setFirstValue(data.getPubKey().toString());
                getViewState().setSecondLabel("Info");

                getViewState().setSecondValue(String.format("Owner address: %s\nReward address: %s",
                        data.getOwnerAddress().toShortString(),
                        data.getRewardAddress().toShortString()
                ));
            }
            break;

            default: {
                getViewState().startDialog(false, ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send")
                        .setText("Wallet doesn't support this type of transaction: %s", tx.getType().name())
                        .setPositiveAction(R.string.btn_close)
                        .create());
            }

        }
    }

    private void startExecuteTransaction() {
        getViewState().startDialog(false, ctx -> {
            final WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, R.string.please_wait)
                    .setText(R.string.tx_send_in_progress)
                    .create();

            dialog.setCancelable(false);

            Observable<TxInitData> initData;

            if (mExtTx.getNonce() != null && !mExtTx.getNonce().equals(BigInteger.ZERO)) {
                TxInitData d = new TxInitData(mExtTx.getNonce(), mExtTx.getGasPrice());
                initData = Observable.just(d);
            } else {
                initData = getTxInitData(mFrom);
            }

            Disposable d = initData
                    .switchMap((Function<TxInitData, ObservableSource<GateResult<TransactionSendResult>>>) cntRes -> {
                        // if in previous request we've got error, returning it
                        if (!cntRes.isSuccess()) {
                            return Observable.just(GateResult.copyError(cntRes.errorResult));
                        }

                        Transaction tx = new Transaction.Builder(cntRes.nonce, mExtTx)
                                .setGasPrice(cntRes.gas)
                                .setPayload(mPayload)
                                .buildFromExternal();

                        final SecretData data = secretStorage.getSecret(mFrom);
                        final TransactionSign sign = tx.signSingle(data.getPrivateKey());

                        return safeSubscribeIoToUi(
                                rxGate(gateTxRepo.sendTransaction(sign))
                                        .onErrorResumeNext(toGateError())
                        );
                    })
                    .doFinally(this::onExecuteComplete)
                    .subscribe(this::onSuccessExecuteTransaction, this::onFailedExecuteTransaction);
            unsubscribeOnDestroy(d);

            return dialog;
        });
    }

    private void onErrorExecuteTransaction(GateResult<?> errorResult) {
        Timber.e(errorResult.getMessage(), "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText((errorResult.getMessage()))
                .setPositiveAction("Close")
                .create());
    }

    private void onFailedExecuteTransaction(Throwable throwable) {
        Timber.w(throwable, "Uncaught tx error");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(throwable.getMessage())
                .setPositiveAction("Close")
                .create());
    }

    private void onSuccessExecuteTransaction(GateResult<TransactionSendResult> result) {
        if (!result.isOk()) {
            onErrorExecuteTransaction(result);
            return;
        }

        accountStorage.update(true);
        cachedTxRepo.update(true);
        getViewState().startDialog(false, ctx -> {
            WalletConfirmDialog.Builder builder = new WalletConfirmDialog.Builder(ctx, "Success!")
                    .setText("Transaction successfully sent")
                    .setPositiveAction("View transaction", (d, v) -> {
                        Wallet.app().sounds().play(R.raw.click_pop_zap);
                        getViewState().startExplorer(result.result.txHash.toString());
                        d.dismiss();
                        getViewState().finishSuccess();
                        //@TODO
//                        getAnalytics().send(AppEvent.SentCoinPopupViewTransactionButton);
                    })
                    .setNegativeAction("Close", (d, w) -> {
                        d.dismiss();
                        //@TODO
//                        getAnalytics().send(AppEvent.SentCoinPopupCloseButton);
                        getViewState().finishSuccess();
                    });
            return builder.create();
        });
    }

    private void onExecuteComplete() {
    }

    private void onSubmit(View view) {
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Sign transaction")
                .setText("Press \"Confirm and send\" to sign and send transaction to the network, or Cancel to dismiss")
                .setPositiveAction(R.string.btn_confirm_send, (d, w) -> startExecuteTransaction())
                .setNegativeAction(R.string.btn_cancel)
                .create());
    }

    private void onCancel(View view) {
        getViewState().finishCancel();
    }

}
