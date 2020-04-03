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

package network.minter.bipwallet.wallets.views;

import android.view.View;

import javax.inject.Inject;

import moxy.InjectViewState;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AddressListBalancesTotal;
import network.minter.bipwallet.advanced.models.CoinBalanceDiffUtilImpl;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.internal.views.list.SimpleRecyclerAdapter;
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView;
import network.minter.bipwallet.wallets.contract.CoinsTabPageView;
import network.minter.bipwallet.wallets.ui.BaseTabPageFragment;
import network.minter.explorer.models.CoinBalance;
import network.minter.profile.MinterProfileApi;
import timber.log.Timber;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

@InjectViewState
public class CoinsTabPagePresenter extends MvpBasePresenter<CoinsTabPageView> {
    @Inject CachedRepository<AddressListBalancesTotal, AccountStorage> accountStorage;
    @Inject SecretStorage secretStorage;
    private SimpleRecyclerAdapter<CoinBalance, BaseTabPageFragment.ItemViewHolder> mCoinsAdapter;

    @Inject
    public CoinsTabPagePresenter() {

    }

    @Override
    public void attachView(CoinsTabPageView view) {
        super.attachView(view);
        accountStorage.update();
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        getViewState().setListTitle("My Coins");
        getViewState().setActionTitle(R.string.btn_exchange);
        getViewState().setOnActionClickListener(this::onClickConvert);
        getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Progress);

        mCoinsAdapter = new SimpleRecyclerAdapter.Builder<CoinBalance, BaseTabPageFragment.ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, BaseTabPageFragment.ItemViewHolder.class)
                .setBinder((itemViewHolder, item, position) -> {
                    itemViewHolder.title.setText(item.getCoin());
                    itemViewHolder.amount.setText(bdHuman(item.getAmount()));
                    itemViewHolder.avatar.setImageUrlFallback(MinterProfileApi.getCoinAvatarUrl(item.getCoin()), R.drawable.img_avatar_default);
                    itemViewHolder.subname.setVisibility(View.GONE);
                }).build();
        getViewState().setAdapter(mCoinsAdapter);

        safeSubscribeIoToUi(accountStorage.observe())
                .subscribe(res -> {
                    Timber.d("Update coins list");
                    mCoinsAdapter.dispatchChanges(CoinBalanceDiffUtilImpl.class, res.getBalance(secretStorage.getMainWallet()).getCoinsList());
                    getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Normal);
                }, t -> {
                    Timber.e(t);
                    getViewState().setViewStatus(BaseWalletsPageView.ViewStatus.Error, t.getMessage());
                });


    }

    private void onClickConvert(View view) {

    }
}
