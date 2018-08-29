/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.home;

import android.view.Menu;
import android.view.View;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.BaseFragment;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.views.SnackbarBuilder;

/**
 * Dogsy. 2017
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class HomeTabFragment extends BaseFragment implements ErrorView, ErrorViewWithRetry {


    public void createToolbarMenuOptions(Menu menu) {

    }

    public void onTabSelected() {

    }

    @Override
    public void onError(Throwable t) {
        if (getActivity() instanceof ErrorView) {
            ((ErrorView) getActivity()).onError(t);
        }
    }

    @Override
    public void onError(String err) {
        if (getActivity() instanceof ErrorView) {
            ((ErrorView) getActivity()).onError(err);
        }
    }

    @Override
    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {
        onErrorWithRetry(errorMessage, getResources().getString(R.string.btn_retry), errorResolver);
    }

    @Override
    public void onErrorWithRetry(String errorMessage, String actionName,
                                 View.OnClickListener errorResolver) {
        new SnackbarBuilder(this)
                .setMessage(errorMessage)
                .setAction(actionName, errorResolver)
                .setDurationIndefinite()
                .show();
    }

    public CharSequence getTitle() {
        return null;
    }

    public void onTrimMemory(int level) {

    }
}
