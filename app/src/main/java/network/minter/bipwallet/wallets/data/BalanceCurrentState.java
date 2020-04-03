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

package network.minter.bipwallet.wallets.data;

import android.os.Bundle;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.settings.SettingsManager;
import network.minter.bipwallet.wallets.contract.WalletsTabView;

import static network.minter.bipwallet.internal.helpers.Plurals.bips;

@Parcel
public final class BalanceCurrentState {
    private final static String sStateBalance = "BalanceCurrentState::CURRENT";
    private final static int[] sTitles = new int[]{
            R.string.tab_coins_title,
            R.string.tab_coins_title_total,
            R.string.tab_coins_title_total,
    };
    int cursor = 0;
    List<BalanceState> items = new ArrayList<>(3);

    public BalanceCurrentState() {
        cursor = Wallet.app().settings().getInt(SettingsManager.CurrentBalanceCursor);
        items.add(new BalanceState("0", "0000", bips(0L)));
        items.add(new BalanceState("0", "0000", bips(0L)));
        items.add(new BalanceState("$0", "00", ""));
    }

    public void setAvailableBIP(String intPart, String fractPart, String amount) {
        items.set(0, new BalanceState(intPart, fractPart, amount));
    }

    public void setTotalBIP(String intPart, String fractPart, String amount) {
        items.set(1, new BalanceState(intPart, fractPart, amount));
    }

    public void setTotalUSD(String intPart, String fractPart) {
        items.set(2, new BalanceState(intPart, fractPart, ""));
    }

    public void applyTo(WalletsTabView view) {
        BalanceState state = items.get(cursor);
        view.setBalanceTitle(sTitles[cursor]);
        view.setBalance(state.mIntPart, state.mFractPart, state.mAmount);


        view.setBalanceClickListener(v -> {
            cursor += 1;
            cursor %= items.size();
            applyTo(view);

            Wallet.app().settings().putInt(SettingsManager.CurrentBalanceCursor, cursor);
        });
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(sStateBalance, Parcels.wrap(this));
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(sStateBalance)) {
            final BalanceCurrentState saved = Parcels.unwrap(savedInstanceState.getParcelable(sStateBalance));
            if (saved != null) {
                cursor = saved.cursor;
                items = saved.items;
            }
        }
    }
}
