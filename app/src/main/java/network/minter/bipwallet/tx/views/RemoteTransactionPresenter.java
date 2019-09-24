package network.minter.bipwallet.tx.views;

import android.content.Intent;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.helpers.IntentHelper;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.tx.contract.RemoteTransactionView;
import network.minter.bipwallet.tx.ui.RemoteTransactionActivity;
import network.minter.blockchain.models.operational.ExternalTransaction;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@InjectViewState
public class RemoteTransactionPresenter extends MvpBasePresenter<RemoteTransactionView> {
    @Inject SecretStorage secret;

    private ExternalTransaction mTx;

    @Inject
    public RemoteTransactionPresenter() {

    }

    @Override
    public void attachView(RemoteTransactionView view) {
        super.attachView(view);
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        mTx = IntentHelper.getParcelExtraOrError(intent, RemoteTransactionActivity.EXTRA_EXTERNAL_TX, "Empty transaction hash passed");
    }
}
