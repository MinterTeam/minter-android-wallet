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

package network.minter.bipwallet.internal.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.internal.helpers.DisplayHelper;
import network.minter.bipwallet.internal.helpers.FingerprintHelper;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.NetworkHelper;
import network.minter.bipwallet.internal.helpers.SoundManager;
import network.minter.bipwallet.internal.settings.SettingsManager;

import static network.minter.bipwallet.internal.settings.SettingsManagerKt.EnableSounds;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class HelpersModule {

    @Provides
    @WalletApp
    public DisplayHelper provideDisplayHelper(Context context) {
        return new DisplayHelper(context);
    }

    @Provides
    @WalletApp
    public NetworkHelper provideNetworkHelper(Context context) {
        return new NetworkHelper(context);
    }

    @Provides
    @WalletApp
    public ImageHelper provideImageHelper(Context context, DisplayHelper displayHelper) {
        return new ImageHelper(context, displayHelper);
    }

    @Provides
    @WalletApp
    public SoundManager provideSoundManager(Context context, SettingsManager settings) {
        return new SoundManager(() -> settings.get(EnableSounds), context);
    }

    @Provides
    @WalletApp
    public FingerprintHelper provideFingerprintHelper(Context context) {
        return new FingerprintHelper(context);
    }


}
