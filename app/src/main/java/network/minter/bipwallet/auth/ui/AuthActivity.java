package network.minter.bipwallet.auth.ui;

import android.os.Bundle;
import android.support.transition.ChangeBounds;
import android.support.transition.ChangeClipBounds;
import android.support.transition.Slide;
import android.support.transition.TransitionSet;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;

public class AuthActivity extends BaseMvpInjectActivity implements SplashFragment.AuthSwitchActivity {
    private SplashFragment mSplashFragment;
    private AuthFragment mAuthFragment;

    @Override
    public void showAuth(View sharedView) {
        TransitionSet sharedSet = new TransitionSet();
        sharedSet.addTransition(new ChangeBounds());
        sharedSet.addTransition(new ChangeClipBounds());
        sharedSet.addTarget(R.id.logo);

        TransitionSet commonSet = new TransitionSet();
        commonSet.addTransition(new Slide(Gravity.BOTTOM));
        commonSet.setDuration(sharedSet.getDuration() * 2);
        commonSet.addTarget(R.id.actionCreateWallet);
        commonSet.addTarget(R.id.actionAdvancedMode);
        commonSet.addTarget(R.id.actionSignin);
        commonSet.addTarget(R.id.actionHelp);

        mAuthFragment.setEnterTransition(commonSet);
        mAuthFragment.setSharedElementEnterTransition(sharedSet);

        getSupportFragmentManager().beginTransaction()
                .addSharedElement(sharedView, ViewCompat.getTransitionName(sharedView))
                .replace(R.id.authContainer, mAuthFragment)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        mSplashFragment = new SplashFragment();
        mAuthFragment = new AuthFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.authContainer, mSplashFragment)
                .commit();
    }

}
