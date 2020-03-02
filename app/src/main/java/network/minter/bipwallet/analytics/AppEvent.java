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

package network.minter.bipwallet.analytics;

import org.parceler.Parcel;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public enum AppEvent {
    //Screens
    WalletsSreen,
    SendScreen,
    ReceiveScreen,
    SettingsScreen,
    TransactionsScreen,
    AddressesScreen,

    //Coins
    CoinsUsernameButton,

    //Events
    //Transaction list
    TransactionDetailsButton,
    TransactionExplorerButton,

    //Send Coin
    SendCoinsChooseCoinButton,
    SendCoinsUseMaxButton,
    SendCoinsSendButton,
    SendCoinsQRButton,
    SendCoinPopupScreen,
    SentCoinPopupScreen,

    //SendCoinPopup
    SendCoinPopupSendButton,
    SendCoinPopupCancelButton,

    //SentCoinPopupScreen
    SentCoinPopupViewTransactionButton,
    SentCoinPopupCloseButton,

    //ReceiveScreen
    ReceiveShareButton,

    //Settings
    SettingsChangeUserpicButton,
    SettingsLogoutButton,
    UsernameEditScreen,
    EmailEditScreen,
    PasswordEditScreen,

    //Addresses
    AddressesCopyButton,

    //ConvertSpend
    ConvertSpendUseMaxButton,
    ConvertSpendExchangeButton,
    ConvertSpendScreen,

    //ConvertGet
    ConvertGetExchangeButton,
    ConvertGetScreen;
    private String value;

    AppEvent() {
    }

    AppEvent(final String v) {
        value = v;
    }

    @Override
    public String toString() {
        return getValue();
    }

    public String getValue() {
        if (value == null) {
            return name();
        }

        return value;
    }
}
