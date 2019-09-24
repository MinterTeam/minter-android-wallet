package network.minter.bipwallet.tx.ui;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import butterknife.ButterKnife;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.tx.contract.RemoteTransactionView;
import network.minter.bipwallet.tx.views.RemoteTransactionPresenter;
import network.minter.blockchain.models.operational.ExternalTransaction;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class RemoteTransactionActivity extends BaseMvpInjectActivity implements RemoteTransactionView {

    public final static String EXTRA_EXTERNAL_TX = "EXTRA_EXTERNAL_TX";

    @Inject Provider<RemoteTransactionPresenter> presenterProvider;
    @InjectPresenter RemoteTransactionPresenter presenter;

    @ProvidePresenter
    RemoteTransactionPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_transaction);
        ButterKnife.bind(this);

        presenter.handleExtras(getIntent());
    }

    public static final class Builder extends ActivityBuilder {
        private final ExternalTransaction mTx;

        public Builder(@NonNull Activity from, String txHash) {
            super(from);
            mTx = ExternalTransaction.fromEncoded(txHash);
        }

        public Builder(@NonNull Fragment from, String txHash) {
            super(from);
            mTx = ExternalTransaction.fromEncoded(txHash);
        }

        public Builder(@NonNull Service from, String txHash) {
            super(from);
            mTx = ExternalTransaction.fromEncoded(txHash);
        }

        @Override
        protected Class<?> getActivityClass() {
            return RemoteTransactionActivity.class;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_EXTERNAL_TX, mTx);
        }
    }
}
