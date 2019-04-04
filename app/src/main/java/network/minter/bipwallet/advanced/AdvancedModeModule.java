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

package network.minter.bipwallet.advanced;

import android.text.TextWatcher;
import android.view.View;
import android.widget.Switch;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;

import dagger.Module;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.internal.dialogs.WalletInputDialog;
import network.minter.bipwallet.internal.mvp.ErrorView;
import network.minter.bipwallet.internal.mvp.ProgressTextView;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class AdvancedModeModule {

    public interface GenerateView extends MvpView, ProgressTextView, ErrorView {
        void setOnCopy(View.OnClickListener listener);
        void setOnSwitchedConfirm(Switch.OnCheckedChangeListener listener);
        void setOnActionClick(View.OnClickListener listener);
        void setOnSecuredByClickListener(View.OnClickListener listener);
        void setMnemonic(CharSequence phrase);
        void setEnableLaunch(boolean enable);
        void setEnableCopy(boolean enable);
        @StateStrategyType(OneExecutionStateStrategy.class)
        void startHome();
        void setEnableSecureVariants(boolean enable, AdvancedMainActivity.OnSelectSecureVariant onSelect);
        void askPassword(WalletInputDialog.OnSubmitListener submitListener);
        void finishSuccess();
        void setActionTitle(CharSequence title);
    }

    public interface MainView extends MvpView, ErrorView, ProgressTextView {
        void setMnemonicTextChangedListener(TextWatcher textWatcher);
        void setOnActivateMnemonic(View.OnClickListener listener);
        @StateStrategyType(OneExecutionStateStrategy.class)
        void startGenerate();
        @StateStrategyType(OneExecutionStateStrategy.class)
        void startGenerate(int requestCode);
        void setError(CharSequence errorMessage);
        void setTitle(CharSequence title);
        void askPassword(WalletInputDialog.OnSubmitListener submitListener);
        void finishSuccess();
        @StateStrategyType(OneExecutionStateStrategy.class)
        void startHome();
    }
}
