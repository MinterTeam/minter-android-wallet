package network.minter.bipwallet.auth.views;

import android.view.View;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.internal.di.annotations.FragmentScope;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
@FragmentScope
public class AuthPresenter extends MvpBasePresenter<AuthModule.AuthView> {

    @Inject
    public AuthPresenter() {
    }

    @Override
    public void attachView(AuthModule.AuthView view) {
        super.attachView(view);

        getViewState().setOnAdvancedMode(this::onClickAdvancedMode);
        getViewState().setOnCreateWallet(this::onClickCreateWallet);
        getViewState().setOnSignin(this::onClickSignIn);
        getViewState().setOnHelp(this::onClickHelp);
    }

    private void onClickHelp(View view) {
        getViewState().startHelp();
    }

    private void onClickSignIn(View view) {
        getViewState().startSignIn();
    }

    private void onClickCreateWallet(View view) {
        getViewState().startRegister();
    }

    private void onClickAdvancedMode(View view) {
        getViewState().startAdvancedMode();
    }


}
