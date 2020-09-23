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

package network.minter.bipwallet.internal;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import dagger.android.support.AndroidSupportInjection;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public abstract class BaseInjectFragment extends BaseFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (enableAutoInjection()) {
            AndroidSupportInjection.inject(this);
        }

        super.onCreate(savedInstanceState);
        prepareIdlingResources();
    }

    @Override
    public void onAttach(Context context) {
        if (enableAutoInjection()) {
            AndroidSupportInjection.inject(this);
        }
        super.onAttach(context);
        prepareIdlingResources();
    }

    @VisibleForTesting
    public void prepareIdlingResources() {
    }

//    @Override
//    public void onError(Throwable t) {
//        if (getActivity() instanceof ErrorView) {
//            ((ErrorView) getActivity()).onError(t);
//        }
//    }
//
//    @Override
//    public void onError(String err) {
//        if (getActivity() instanceof ErrorView) {
//            ((ErrorView) getActivity()).onError(err);
//        }
//    }
//
//    @Override
//    public void onErrorWithRetry(String errorMessage, View.OnClickListener errorResolver) {
//        onErrorWithRetry(errorMessage, getResources().getString(R.string.btn_retry), errorResolver);
//    }
//
//    @Override
//    public void onErrorWithRetry(String errorMessage, String actionName,
//                                 View.OnClickListener errorResolver) {
////        new SnackbarBuilder(this)
////                .setMessage(errorMessage)
////                .setAction(actionName, errorResolver)
////                .setDurationIndefinite()
////                .show();
//    }

    protected boolean enableAutoInjection() {
        return true;
    }
}
