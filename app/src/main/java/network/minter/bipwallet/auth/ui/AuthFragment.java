package network.minter.bipwallet.auth.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.auth.views.AuthPresenter;
import network.minter.bipwallet.internal.BaseInjectFragment;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AuthFragment extends BaseInjectFragment implements AuthModule.AuthView {
    @Inject Provider<AuthPresenter> authPresenterProvider;
    @InjectPresenter AuthPresenter presenter;
    @BindView(R.id.actionCreateWallet) Button actionCreateWallet;
    @BindView(R.id.actionAdvancedMode) Button actionAdvancedMode;
    @BindView(R.id.actionSignin) Button actionSignin;
    @BindView(R.id.actionHelp) Button actionHelp;

    @Override
    public void setOnCreateWallet(View.OnClickListener listener) {
        actionCreateWallet.setOnClickListener(listener);
    }

    @Override
    public void setOnAdvancedMode(View.OnClickListener listener) {
        actionAdvancedMode.setOnClickListener(listener);
    }

    @Override
    public void setOnSignin(View.OnClickListener listener) {
        actionSignin.setOnClickListener(listener);
    }

    @Override
    public void setOnHelp(View.OnClickListener listener) {
        actionHelp.setOnClickListener(listener);
    }

    @Override
    public void startAdvancedMode() {
        getActivity().startActivity(new Intent(getActivity(), AdvancedMainActivity.class));
    }

    @Override
    public void startRegister() {
        getActivity().startActivity(new Intent(getActivity(), RegisterActivity.class));
    }

    @Override
    public void startSignIn() {
        getActivity().startActivity(new Intent(getActivity(), SigninActivity.class));
    }

    @Override
    public void startHelp() {

    }

    @ProvidePresenter
    AuthPresenter providePresenter() {
        return authPresenterProvider.get();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}
