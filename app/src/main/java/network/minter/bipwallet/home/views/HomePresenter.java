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

package network.minter.bipwallet.home.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.airbnb.deeplinkdispatch.DeepLink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.analytics.AppEvent;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.HomeTabsClasses;
import network.minter.bipwallet.home.contract.HomeView;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.bipwallet.services.livebalance.RTMService;
import network.minter.bipwallet.services.livebalance.ServiceConnector;
import network.minter.bipwallet.services.livebalance.broadcast.RTMBlockReceiver;
import timber.log.Timber;

import static network.minter.bipwallet.internal.Wallet.app;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class HomePresenter extends MvpBasePresenter<HomeView> {

    @Inject KVStorage storage;

    private final HashMap<Integer, Integer> mBottomIdPositionMap = new HashMap<Integer, Integer>() {{
        put(R.id.bottom_coins, 0);
        put(R.id.bottom_send, 1);
        put(R.id.bottom_receive, 2);
        put(R.id.bottom_settings, 3);
    }};

    @Inject @HomeTabsClasses
    List<Class<? extends network.minter.bipwallet.home.HomeTabFragment>> tabsClasses;
    @Inject CachedRepository<UserAccount, AccountStorage> accountStorage;
    private int mLastPosition = 0;

    @Inject
    public HomePresenter() {
    }

    @Override
    public void attachView(HomeView view) {
        super.attachView(view);
        getViewState().setCurrentPage(mLastPosition);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceConnector.release(app().context());
        Timber.d("Disconnecting service");
    }

    public void onPageSelected(int position) {
        mLastPosition = position;
        switch (position) {
            case R.id.bottom_coins:
                getAnalytics().send(AppEvent.CoinsScreen);
                break;
            case R.id.bottom_receive:
                getAnalytics().send(AppEvent.ReceiveScreen);
                break;
            case R.id.bottom_send:
                getAnalytics().send(AppEvent.SendScreen);
                break;
            case R.id.bottom_settings:
                getAnalytics().send(AppEvent.SettingsScreen);
                break;
        }
    }

    @Override
    public void handleExtras(Intent intent) {
        super.handleExtras(intent);
        if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
            Bundle params = intent.getExtras();
            if (params == null) {
                return;
            }

            final String uri = params.getString(DeepLink.URI, null);
            if (uri == null) return;

            if (uri.startsWith("minter://tx")) {
                getViewState().setCurrentPage(mBottomIdPositionMap.get(R.id.bottom_send));
                final String hash = params.getString("d", null);

                Timber.d("Deeplink URI: %s", uri);
                Timber.d("Deeplink TX: %s", hash);

                getViewState().startRemoteTransaction(hash);
            }
        }
    }

    public int getBottomIdByPosition(int position) {
        for (Map.Entry<Integer, Integer> item : mBottomIdPositionMap.entrySet()) {
            if (item.getValue() == position) {
                return item.getKey();
            }
        }

        return 0;
    }

    public int getTabPosition(String name) {
        int position = 0;
        for (Class<? extends HomeTabFragment> cls : tabsClasses) {
            if (cls.getName().equals(name)) {
                break;
            }
            position++;
        }

        return position;
    }

    public void onNavigationBottomItemSelected(MenuItem item) {

    }

    public int getBottomPositionById(int itemId) {
        return mBottomIdPositionMap.get(itemId);
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        ServiceConnector.bind(app().context());
        ServiceConnector.onConnected()
                .subscribe(res -> res.setOnMessageListener((message, channel, address) -> {
                    if (channel.equals(RTMService.CHANNEL_BLOCKS)) {
                        RTMBlockReceiver.send(Wallet.app().context(), message);
                    } else {
                        accountStorage.update(true, account -> app().balanceNotifications().showBalanceUpdate(message, address));
                        app().explorerTransactionsRepoCache().update(true);
                        Timber.d("WS ON MESSAGE[%s]: %s", channel, message);
                    }
                }));
    }
}
