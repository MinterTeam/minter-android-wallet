package network.minter.bipwallet.auth.views;

import android.view.View;
import android.widget.CompoundButton;

import java.security.SecureRandom;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.contract.CreateWalletView;
import network.minter.bipwallet.internal.di.annotations.FragmentScope;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.bip39.NativeBip39;

import static network.minter.bipwallet.internal.helpers.ContextHelper.copyToClipboardNoAlert;

@FragmentScope
@InjectViewState
public class CreateWalletPresenter extends MvpBasePresenter<CreateWalletView> {

    private SecureRandom mRandom = new SecureRandom();
    private MnemonicResult mMnemonicResult;

    @Inject
    public CreateWalletPresenter() {

    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        mMnemonicResult = NativeBip39.encodeBytes(mRandom.generateSeed(16));
    }

    @Override
    public void attachView(CreateWalletView view) {
        super.attachView(view);
        getViewState().setTitle(R.string.btn_create_wallet);
        getViewState().setDescription(R.string.hint_save_seed);
        getViewState().setSeed(mMnemonicResult.getMnemonic());
        getViewState().setOnSeedClickListener(this::onCopySeed);
        getViewState().setOnSavedClickListener(this::onSavedSeed);

    }

    private void onSavedSeed(CompoundButton compoundButton, boolean checked) {
        getViewState().setSubmitEnabled(checked);
    }

    private void onCopySeed(View view) {
        getViewState().showCopiedAlert();
        copyToClipboardNoAlert(view.getContext(), mMnemonicResult.getMnemonic());
    }
}
