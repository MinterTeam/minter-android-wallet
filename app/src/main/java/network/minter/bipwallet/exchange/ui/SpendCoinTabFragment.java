/*
 * Copyright (C) by MinterTeam. 2019
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
package network.minter.bipwallet.exchange.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import network.minter.bipwallet.R;
import network.minter.bipwallet.exchange.ExchangeModule.SpendCoinTabView;
import network.minter.bipwallet.exchange.views.SpendCoinTabPresenter;
import network.minter.bipwallet.internal.system.testing.IdlingManager;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SpendCoinTabFragment extends BaseCoinTabFragment implements SpendCoinTabView {

    public static final String IDLE_SPEND_COIN_CONFIRM_DIALOG = "IDLE_SPEND_COIN_CONFIRM_DIALOG";
    public static final String IDLE_SPEND_COIN_COMPLETE_DIALOG = "IDLE_SPEND_COIN_COMPLETE_DIALOG";

    @Inject Provider<SpendCoinTabPresenter> presenterProvider;
    @InjectPresenter SpendCoinTabPresenter presenter;
    @Inject IdlingManager idlingManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @VisibleForTesting
    @Override
    public void prepareIdlingResources() {
        super.prepareIdlingResources();
        idlingManager.add(IDLE_SPEND_COIN_CONFIRM_DIALOG,
                IDLE_SPEND_COIN_COMPLETE_DIALOG,
                IDLE_WAIT_ESTIMATE);
    }

    @ProvidePresenter
    SpendCoinTabPresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_coin_exchange_spend;
    }
}
