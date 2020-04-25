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

package network.minter.bipwallet.home;

import java.util.List;

import dagger.Component;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.di.WalletComponent;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.settings.ui.SettingsTabFragment;
import network.minter.bipwallet.wallets.ui.WalletsTabFragment;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Component(dependencies = WalletComponent.class, modules = {
        HomeModule.class
})
@HomeScope
public interface HomeComponent {

    void inject(HomeActivity activity);
    void inject(WalletsTabFragment fragment);
    void inject(SendTabFragment fragment);
    void inject(SettingsTabFragment fragment);

    @HomeTabsClasses
    List<Class<? extends HomeTabFragment>> tabsClasses();
    HomeActivity homeActivity();
}
