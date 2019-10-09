package network.minter.bipwallet.tx.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import org.parceler.Parcels;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.external.ui.AppDeepLink;
import network.minter.bipwallet.external.ui.WebDeepLink;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.views.widgets.WalletButton;
import network.minter.bipwallet.tx.contract.ExternalTransactionView;
import network.minter.bipwallet.tx.views.ExternalTransactionPresenter;
import network.minter.blockchain.models.operational.ExternalTransaction;
import network.minter.explorer.MinterExplorerApi;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@AppDeepLink({"tx"})
@WebDeepLink({"tx"})
public class ExternalTransactionActivity extends BaseMvpInjectActivity implements ExternalTransactionView {

    public final static String EXTRA_EXTERNAL_TX = "EXTRA_EXTERNAL_TX";

    @Inject Provider<ExternalTransactionPresenter> presenterProvider;
    @InjectPresenter ExternalTransactionPresenter presenter;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.input_first) AppCompatEditText inputFirst;
    @BindView(R.id.layout_input_first) TextInputLayout layoutInputFirst;
    @BindView(R.id.input_second) AppCompatEditText inputSecond;
    @BindView(R.id.layout_input_second) TextInputLayout layoutInputSecond;
    @BindView(R.id.input_payload) AppCompatEditText inputPayload;
    @BindView(R.id.fee_value) TextView feeValue;
    @BindView(R.id.text_error) TextView textError;
    @BindView(R.id.action) WalletButton action;
    @BindView(R.id.cancel_action) WalletButton cancelAction;

    private WalletDialog mCurrentDialog = null;

    @Override
    public void setFirstLabel(CharSequence label) {
        layoutInputFirst.setHint(label);
    }

    @Override
    public void setFirstValue(CharSequence value) {
        inputFirst.setText(value);
    }

    @Override
    public void setSecondLabel(CharSequence label) {
        layoutInputSecond.setHint(label);
    }

    @Override
    public void setSecondValue(CharSequence value) {
        inputSecond.setText(value);
    }

    @Override
    public void setPayload(CharSequence payloadString) {
        inputPayload.setText(payloadString);
    }

    @Override
    public void setCommission(CharSequence fee) {
        feeValue.setText(fee);
    }

    @Override
    public void setSecondVisible(int visibility) {
        layoutInputSecond.setVisibility(visibility);
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void setPayloadTextChangedListener(TextWatcher textWatcher) {
        inputPayload.addTextChangedListener(textWatcher);
    }

    @Override
    public void setOnConfirmListener(View.OnClickListener listener) {
        action.setOnClickListener(listener);
    }

    @Override
    public void setOnCancelListener(View.OnClickListener listener) {
        cancelAction.setOnClickListener(listener);
    }

    @Override
    public void hideProgress() {
        WalletDialog.releaseDialog(mCurrentDialog);
        mCurrentDialog = null;
    }

    @Override
    public void showProgress() {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, ctx -> new WalletProgressDialog.Builder(ctx, "Please, wait")
                .setText("We're loading required account information")
                .create());
    }

    @Override
    public void finishSuccess() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finishCancel() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void startExplorer(String txHash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + txHash)));
    }

    @ProvidePresenter
    ExternalTransactionPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onStop() {
        super.onStop();
        WalletDialog.releaseDialog(mCurrentDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_transaction);
        ButterKnife.bind(this);

        setupToolbar(toolbar);

        presenter.handleExtras(getIntent());
    }

    public static final class Builder extends ActivityBuilder {
        private final ExternalTransaction mTx;

        public Builder(@NonNull Activity from, ExternalTransaction tx) {
            super(from);
            mTx = tx;
        }

        public Builder(@NonNull Fragment from, ExternalTransaction tx) {
            super(from);
            mTx = tx;
        }

        public Builder(@NonNull Service from, ExternalTransaction tx) {
            super(from);
            mTx = tx;
        }

        @Override
        protected Class<?> getActivityClass() {
            return ExternalTransactionActivity.class;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_EXTERNAL_TX, Parcels.wrap(mTx));
        }
    }
}
