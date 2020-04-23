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

package network.minter.bipwallet.settings.contract;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.jvm.functions.Function1;
import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.security.SecurityModule;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@StateStrategyType(AddToEndSingleStrategy.class)
public interface SettingsTabView extends MvpView {
    void setOnOurChannelClickListener(View.OnClickListener listener);
    void setOnSupportChatClickListener(View.OnClickListener listener);

    @StateStrategyType(OneExecutionStateStrategy.class)
    void startLogin();
    void setMainAdapter(RecyclerView.Adapter<?> mainAdapter);
    void setAdditionalAdapter(RecyclerView.Adapter<?> additionalAdapter);
    void setSecurityAdapter(RecyclerView.Adapter<?> securityAdapter);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAvatarChooser();
    void showMessage(CharSequence message);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startDialog(Function1<Context, WalletDialog> executor);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startPinCodeManager(int requestCode, SecurityModule.PinMode mode);
    void startBiometricPrompt(BiometricPrompt.AuthenticationCallback callback);
    void startFingerprintEnrollment();
    void startIntent(Intent intent);
}
