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

package network.minter.bipwallet.internal.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import network.minter.bipwallet.addresses.AddressManageModule;
import network.minter.bipwallet.addresses.ui.AddressItemActivity;
import network.minter.bipwallet.addresses.ui.AddressListActivity;
import network.minter.bipwallet.advanced.AdvancedModeModule;
import network.minter.bipwallet.advanced.ui.AdvancedGenerateActivity;
import network.minter.bipwallet.advanced.ui.AdvancedMainActivity;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.auth.ui.AuthFragment;
import network.minter.bipwallet.auth.ui.RegisterActivity;
import network.minter.bipwallet.auth.ui.SigninActivity;
import network.minter.bipwallet.auth.ui.SplashFragment;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.internal.di.annotations.ActivityScope;
import network.minter.bipwallet.internal.di.annotations.FragmentScope;
import network.minter.bipwallet.sending.ui.DialogTxSendActivity;
import network.minter.bipwallet.settings.ui.PasswordChangeMigrationActivity;
import network.minter.bipwallet.tx.ui.TransactionListActivity;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module(includes = AndroidSupportInjectionModule.class)
public interface InjectorsModule {

    @ContributesAndroidInjector
    @ActivityScope
    AuthActivity authActivityInjector();

    @ContributesAndroidInjector
    @FragmentScope
    SplashFragment splashFragmentInjector();

    @ContributesAndroidInjector
    @FragmentScope
    AuthFragment authFragmentInjector();

    @ContributesAndroidInjector
    @ActivityScope
    SigninActivity signinActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    RegisterActivity registerActivityInjector();

    @ContributesAndroidInjector(modules = AdvancedModeModule.class)
    @ActivityScope
    AdvancedMainActivity advancedMainActivity();

    @ContributesAndroidInjector
    @ActivityScope
    AdvancedGenerateActivity advancedGenerateActivity();

    @ContributesAndroidInjector(modules = AddressManageModule.class)
    @ActivityScope
    AddressListActivity addressListActivityInjector();

    @ContributesAndroidInjector(modules = AddressManageModule.class)
    @ActivityScope
    AddressItemActivity addressItemActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    TransactionListActivity transactionListActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    DialogTxSendActivity dialogTxSendActivityInjector();

    @ContributesAndroidInjector(modules = ExchangeModule.class)
    @ActivityScope
    ConvertCoinActivity convertCoinActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    PasswordChangeMigrationActivity passwordChangeMigrationActivityInjector();
}
