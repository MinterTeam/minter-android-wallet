/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeClipBounds;
import androidx.transition.Slide;
import androidx.transition.TransitionSet;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseMvpInjectActivity;

public class AuthActivity extends BaseMvpInjectActivity implements SplashFragment.AuthSwitchActivity {
    private SplashFragment mSplashFragment;
    private AuthFragment mAuthFragment;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void showAuth(View sharedView) {
        if (getSupportFragmentManager() == null) {
            return;
        }

        TransitionSet sharedSet = new TransitionSet();
        sharedSet.addTransition(new ChangeBounds());
        sharedSet.addTransition(new ChangeClipBounds());
        sharedSet.addTarget(R.id.logo);
        sharedSet.addTarget(ViewCompat.getTransitionName(sharedView));

        TransitionSet commonSet = new TransitionSet();
        commonSet.addTransition(new Slide(Gravity.BOTTOM));
        commonSet.setDuration(sharedSet.getDuration() * 2);
        commonSet.addTarget(R.id.action_signin);
        commonSet.addTarget(R.id.action_create_wallet);
        commonSet.addTarget(R.id.action_help);

        mAuthFragment.setEnterTransition(commonSet);
        mAuthFragment.setSharedElementEnterTransition(sharedSet);

        getSupportFragmentManager().beginTransaction()
                .addSharedElement(sharedView, ViewCompat.getTransitionName(sharedView))
                .replace(R.id.container_auth, mAuthFragment)
                .commit();
    }

    @VisibleForTesting
    public final AuthFragment getAuthFragment() {
        return mAuthFragment;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mSplashFragment != null) {
            mSplashFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        mSplashFragment = new SplashFragment();
        mAuthFragment = new AuthFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container_auth, mSplashFragment)
                .commit();
    }

}
