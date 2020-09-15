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
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.setTextFormat
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.internal.storage.models.AddressListBalancesTotal
import network.minter.bipwallet.internal.views.list.SimpleRecyclerAdapter
import network.minter.bipwallet.internal.views.list.multirow.MultiRowAdapter
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView
import network.minter.bipwallet.wallets.contract.CoinsTabPageView
import network.minter.bipwallet.wallets.data.CoinBalanceDiffUtilImpl
import network.minter.bipwallet.wallets.ui.BaseTabPageFragment
import network.minter.bipwallet.wallets.views.rows.RowWalletsButton
import network.minter.bipwallet.wallets.views.rows.RowWalletsHeader
import network.minter.bipwallet.wallets.views.rows.RowWalletsList
import network.minter.core.MinterSDK
import network.minter.explorer.models.CoinBalance
import timber.log.Timber
import javax.inject.Inject

@InjectViewState
class CoinsTabPagePresenter @Inject constructor() : MvpBasePresenter<CoinsTabPageView>() {
    @Inject lateinit var accountStorage: RepoAccounts
    @Inject lateinit var secretStorage: SecretStorage

    private var globalAdapter: MultiRowAdapter = MultiRowAdapter()
    private var coinsAdapter: SimpleRecyclerAdapter<CoinBalance, BaseTabPageFragment.ItemViewHolder>? = null
    private var rowButton: RowWalletsButton? = null
    private var rowHeader: RowWalletsHeader? = null
    private var rowList: RowWalletsList? = null

    @Deprecated("Use new variable binding", ReplaceWith("coin.avatar"))
    private fun CoinBalance.getImageUrl(): String {
        return coin.avatar
    }

    init {
        coinsAdapter = SimpleRecyclerAdapter.Builder<CoinBalance, BaseTabPageFragment.ItemViewHolder>()
                .setCreator(R.layout.item_list_with_image, BaseTabPageFragment.ItemViewHolder::class.java)
                .setBinder { vh: BaseTabPageFragment.ItemViewHolder, item: CoinBalance, _: Int ->
                    vh.title!!.text = item.coin.symbol
                    vh.separator!!.visible = true
                    vh.amount!!.text = item.amount.humanize()
                    vh.avatar!!.setImageUrlFallback(item.coin.avatar, R.drawable.img_avatar_default)
                    if (item.coin.id != MinterSDK.DEFAULT_COIN_ID) {
                        vh.subname!!.setTextFormat(R.string.fmt_decimal_and_coin, item.bipValue.humanize(), MinterSDK.DEFAULT_COIN)
                        vh.subname!!.visible = true
                    } else {
                        vh.subname!!.visible = false
                    }

                }.build()

        rowButton = RowWalletsButton(R.string.btn_exchange, false, ::onClickConvert)
        rowHeader = RowWalletsHeader(R.string.title_my_coins)
        rowList = RowWalletsList(coinsAdapter!!)

        globalAdapter.addRow(rowButton!!)
        globalAdapter.addRow(rowHeader!!)
        globalAdapter.addRow(rowList!!)
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        Timber.d("WALLETS coins first attach")
        viewState.setViewStatus(BaseWalletsPageView.ViewStatus.Progress)

        accountStorage
                .retryWhen(errorResolver)
                .observe()
                .joinToUi()
                .subscribe(
                        { res: AddressListBalancesTotal ->
                            Timber.d("Update coins list")
                            coinsAdapter!!.dispatchChanges(CoinBalanceDiffUtilImpl::class.java, res.getBalance(secretStorage.mainWallet).coinsList)
                            viewState!!.setViewStatus(BaseWalletsPageView.ViewStatus.Normal)
                        },
                        { t ->
                            Timber.e(t, "Unable to load coin list")
                            viewState!!.setViewStatus(BaseWalletsPageView.ViewStatus.Error, t.message)
                        }
                )
                .disposeOnDestroy()


    }

    override fun attachView(view: CoinsTabPageView) {
        super.attachView(view)
        Timber.d("WALLETS coins attach")
        viewState.setAdapter(globalAdapter)
        accountStorage.update()
    }

    override fun detachView(view: CoinsTabPageView) {
        super.detachView(view)
        Timber.d("WALLETS coins detach")
    }

    override fun destroyView(view: CoinsTabPageView?) {
        super.destroyView(view)
        Timber.d("WALLETS coins destroy view")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("WALLETS onDestroy")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickConvert(view: View) {
        viewState.startConvert()
    }
}