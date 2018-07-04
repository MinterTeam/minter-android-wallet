/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.auth;

import android.net.Uri;
import android.view.View;

import com.arellomobile.mvp.MvpView;

import java.util.List;
import java.util.Map;

import dagger.Module;
import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class AuthModule {

    public interface AuthView extends MvpView {
        void setOnCreateWallet(View.OnClickListener listener);
        void setOnAdvancedMode(View.OnClickListener listener);
        void setOnSignin(View.OnClickListener listener);
        void setOnHelp(View.OnClickListener listener);
        void startAdvancedMode();
        void startRegister();
        void startSignIn();
        void startHelp();
    }

    public interface RegisterView extends MvpView, ProgressView, ErrorViewWithRetry {
        void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
        void setOnSubmit(View.OnClickListener listener);
        void setOnFormValidateListener(InputGroup.OnFormValidateListener listener);
        void setEnableSubmit(boolean enable);
        void startHome();
        void validate(boolean withError);
        void setInputError(String fieldName, String message);
        void setResultError(CharSequence error);
        void clearErrors();
        void setInputErrors(Map<String, List<String>> data);
        void startConfirmation(Uri endpoint);
        void hideKeyboard();
    }

    public interface SigninView extends MvpView, ProgressView, ErrorViewWithRetry {
        void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
        void setOnSubmit(View.OnClickListener listener);
        void setOnFormValidateListener(InputGroup.OnFormValidateListener listener);
        void setEnableSubmit(boolean enable);
        void startHome();
        void setResultError(CharSequence error);
        void setInputError(String fieldName, String message);
        void clearErrors();
        void setInputErrors(Map<String, List<String>> fieldsErrors);
        void hideKeyboard();
    }
}
