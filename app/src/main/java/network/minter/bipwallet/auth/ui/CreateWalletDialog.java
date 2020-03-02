package network.minter.bipwallet.auth.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import network.minter.bipwallet.R;
import network.minter.bipwallet.auth.contract.CreateWalletView;
import network.minter.bipwallet.auth.views.CreateWalletPresenter;
import network.minter.bipwallet.internal.BaseMvpBottomSheetDialogFragment;

public class CreateWalletDialog extends BaseMvpBottomSheetDialogFragment implements CreateWalletView {

    @Inject Provider<CreateWalletPresenter> presenterProvider;
    @InjectPresenter CreateWalletPresenter presenter;

    @BindView(R.id.dialog_title) TextView title;
    @BindView(R.id.dialog_description) TextView description;
    @BindView(R.id.seed) TextView seed;
    @BindView(R.id.layout_seed) View seedLayout;
    @BindView(R.id.layout_seed_alert) View alertCopied;
    @BindView(R.id.action_saved_seed) Switch actionSavedSeed;
    @BindView(R.id.submit) View submit;


    @ProvidePresenter
    public CreateWalletPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_wallet, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void setTitle(int resId) {
        title.setText(resId);
    }

    @Override
    public void setDescription(int resId) {
        description.setText(resId);
    }

    @Override
    public void setSeed(CharSequence seedPhrase) {
        seed.setText(seedPhrase);
    }

    @Override
    public void setOnSeedClickListener(View.OnClickListener listener) {
        seedLayout.setOnClickListener(listener);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        submit.setOnClickListener(listener);
    }

    @Override
    public void showCopiedAlert() {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in_out);
        set.setTarget(alertCopied);
        set.start();
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        submit.setEnabled(enabled);
    }

    @Override
    public void setOnSavedClickListener(Switch.OnCheckedChangeListener checkedChangeListener) {
        actionSavedSeed.setOnCheckedChangeListener(checkedChangeListener);
    }
}
