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
package network.minter.bipwallet.wallets.views

import android.view.View
import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.internal.data.CachedRepository
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.AccountStorage
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.internal.views.list.SimpleRecyclerAdapter
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView
import network.minter.bipwallet.wallets.contract.CoinsTabPageView
import network.minter.bipwallet.wallets.data.CoinBalanceDiffUtilImpl
import network.minter.bipwallet.wallets.ui.BaseTabPageFragment
import network.minter.explorer.models.CoinBalance
import network.minter.profile.MinterProfileApi
import timber.log.Timber
import javax.inject.Inject

@InjectViewState
class CoinsTabPagePresenter @Inject constructor() : MvpBasePresenter<CoinsTabPageView>() {
    @Inject
    lateinit var accountStorage: CachedRepository<AddressListBalancesTotal, AccountStorage>

    @Inject
    lateinit var secretStorage: SecretStorage

    private var mCoinsAdapter: SimpleRecyclerAdapter<CoinBalance, BaseTabPageFragment.ItemViewHolder>? = null

    override fun attachView(view: CoinsTabPageView) {
        super.attachView(view)
        accountStorage.update()
    }

    internal fun CoinBalance.getImageUrl(): String {
        return MinterProfileApi.getCoinAvatarUrl(coin!!)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.setListTitle("My Coins")
        viewState.setActionTitle(R.string.btn_exchange)
        viewState.setOnActionClickListener(View.OnClickListener { v -> onClickConvert(v) })
        viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Progress)
        mCoinsAdapter = SimpleRecyclerAdapter.Builder<CoinBalance, BaseTabPageFragment.ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, BaseTabPageFragment.ItemViewHolder::class.java)
                .setBinder { vh: BaseTabPageFragment.ItemViewHolder, item: CoinBalance, _: Int ->
                    vh.title!!.text = item.coin
                    vh.amount!!.text = item.amount.humanize()
                    vh.avatar!!.setImageUrlFallback(item.getImageUrl(), R.drawable.img_avatar_default)
                    vh.subname!!.visibility = View.GONE
                }.build()

        viewState.setAdapter(mCoinsAdapter!!)

        accountStorage.observe().joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            Timber.d("Update coins list")
                            mCoinsAdapter!!.dispatchChanges(CoinBalanceDiffUtilImpl::class.java, res.getBalance(secretStorage.mainWallet).coinsList)
                            viewState!!.setViewStatus(BaseWalletsPageView.ViewStatus.Normal)
                        },
                        { t: Throwable ->
                            Timber.e(t)
                            viewState!!.setViewStatus(BaseWalletsPageView.ViewStatus.Error, t.message)
                        }
                )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickConvert(view: View) {
        viewState.startConvert()
    }
}