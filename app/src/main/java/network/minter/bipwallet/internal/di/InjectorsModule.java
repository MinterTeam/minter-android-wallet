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

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import network.minter.bipwallet.addressbook.ui.AddressBookActivity;
import network.minter.bipwallet.addressbook.ui.AddressContactEditDialog;
import network.minter.bipwallet.auth.ui.AuthActivity;
import network.minter.bipwallet.auth.ui.AuthFragment;
import network.minter.bipwallet.auth.ui.SplashFragment;
import network.minter.bipwallet.delegation.ui.DelegateUnbondActivity;
import network.minter.bipwallet.delegation.ui.DelegatedListActivity;
import network.minter.bipwallet.delegation.ui.ValidatorSelectorActivity;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.ui.BuyExchangeFragment;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.exchange.ui.SellExchangeFragment;
import network.minter.bipwallet.external.ui.ExternalActivity;
import network.minter.bipwallet.internal.di.annotations.ActivityScope;
import network.minter.bipwallet.internal.di.annotations.FragmentScope;
import network.minter.bipwallet.internal.di.annotations.ServiceScope;
import network.minter.bipwallet.security.SecurityModule;
import network.minter.bipwallet.security.ui.PinEnterActivity;
import network.minter.bipwallet.security.ui.PinValidationDialog;
import network.minter.bipwallet.services.livebalance.RTMService;
import network.minter.bipwallet.share.ShareDialog;
import network.minter.bipwallet.tx.TransactionsModule;
import network.minter.bipwallet.tx.ui.ExternalTransactionActivity;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.bipwallet.tx.ui.TransactionViewDialog;
import network.minter.bipwallet.wallets.dialogs.ui.AddWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.CreateWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.EditWalletDialog;
import network.minter.bipwallet.wallets.dialogs.ui.SignInMnemonicDialog;
import network.minter.bipwallet.wallets.ui.CoinsTabPageFragment;
import network.minter.bipwallet.wallets.ui.TxsTabPageFragment;

/**
 * minter-android-wallet. 2018
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
    CreateWalletDialog createWalletDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    SignInMnemonicDialog signInWalletDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    EditWalletDialog walletEditDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    AddWalletDialog walletAddDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    AuthFragment authFragmentInjector();

    @ContributesAndroidInjector(modules = TransactionsModule.class)
    @ActivityScope
    TransactionListActivity transactionListActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    DelegatedListActivity delegationListActivityInjector();

    @ContributesAndroidInjector(modules = ExchangeModule.class)
    @ActivityScope
    ConvertCoinActivity convertCoinActivityInjector();

    @ContributesAndroidInjector
    @FragmentScope
    BuyExchangeFragment getCoinTabFragmentInjector();

    @ContributesAndroidInjector
    @FragmentScope
    SellExchangeFragment spendCoinTabFragmentInjector();

    @ContributesAndroidInjector
    @ServiceScope
    RTMService balanceUpdateService();

    @ContributesAndroidInjector
    @ActivityScope
    ExternalActivity externalActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    PinEnterActivity pinCodeActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    ExternalTransactionActivity externalTransactionActivityInjector();

    @ContributesAndroidInjector(modules = {SecurityModule.class})
    @FragmentScope
    PinValidationDialog pinValidationDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    CoinsTabPageFragment coinsTabPageFragmentInjector();

    @ContributesAndroidInjector
    @FragmentScope
    TxsTabPageFragment txsTabPageFragmentInjector();

    @ContributesAndroidInjector
    @ActivityScope
    AddressBookActivity addressBookActivityInjector();

    @ContributesAndroidInjector
    @FragmentScope
    AddressContactEditDialog addressContactEditDialogInjector();

    @ContributesAndroidInjector
    @FragmentScope
    TransactionViewDialog transactionViewDialogInjector();

    @ContributesAndroidInjector
    @ActivityScope
    DelegateUnbondActivity delegateUnbondActivityInjector();

    @ContributesAndroidInjector
    @ActivityScope
    ValidatorSelectorActivity validatorSelectorActivityInjector();

    @ContributesAndroidInjector
    @FragmentScope
    ShareDialog shareDialogInjector();


}
