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
package network.minter.bipwallet.delegation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.ContextHelper
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder
import network.minter.core.MinterSDK

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
typealias OnDelegatedClickListener = (DelegatedValidator) -> Unit
typealias OnUnbondItemClickListener = (DelegatedStake) -> Unit

class DelegationListAdapter : PagedListAdapter<DelegatedItem, RecyclerView.ViewHolder> {
    companion object {
        private val sDiffCallback: DiffUtil.ItemCallback<DelegatedItem> = object : DiffUtil.ItemCallback<DelegatedItem>() {
            override fun areItemsTheSame(oldItem: DelegatedItem, newItem: DelegatedItem): Boolean {
                return oldItem.isSameOf(newItem)
            }

            override fun areContentsTheSame(oldItem: DelegatedItem, newItem: DelegatedItem): Boolean {
                return oldItem == newItem
            }
        }
        private const val ITEM_PROGRESS = R.layout.item_list_transaction_progress
    }

    private var mInflater: LayoutInflater? = null
    private var mLoadState: MutableLiveData<LoadState>? = null
    private var mOnDelegatedClickListener: OnDelegatedClickListener? = null
    private var mOnUnbondItemClickListener: OnUnbondItemClickListener? = null

    constructor() : super(sDiffCallback)
    constructor(config: AsyncDifferConfig<DelegatedItem?>) : super(config)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.context)
        }
        val view = mInflater!!.inflate(viewType, parent, false)
        if (viewType == ITEM_PROGRESS) {
            return TxProgressViewHolder(view)
        } else if (viewType == DelegatedItem.ITEM_STAKE) {
            return StakeViewHolder(view)
        }
        return ValidatorViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (getItemViewType(i) == DelegatedItem.ITEM_VALIDATOR) {
            val vh = viewHolder as ValidatorViewHolder
            val item = getItem(i) as DelegatedValidator
            vh.fakeHeader.visible = true

            if (item.name.isNullOrEmpty()) {
                vh.title.text = item.publicKey.toShortString()
                vh.publicKey.text = item.publicKey.toShortString()
            } else {
                vh.title.text = item.name
                vh.publicKey.text = item.publicKey.toShortString()
            }

            if (item.description != null) {
                ViewCompat.setTooltipText(vh.publicKey, item.description)
            }
            vh.icon.setImageUrlFallback(item.publicKey.avatar, R.drawable.img_avatar_delegate)
            vh.actionCopy.setOnClickListener {
                ContextHelper.copyToClipboard(it.context, item.publicKey.toString())
            }
            vh.actionDelegate.setOnClickListener {
                if (mOnDelegatedClickListener != null) {
                    mOnDelegatedClickListener!!.invoke(getItem(viewHolder.bindingAdapterPosition) as DelegatedValidator)
                }
            }
        } else if (getItemViewType(i) == DelegatedItem.ITEM_STAKE) {
            val vh = viewHolder as StakeViewHolder
            val item = getItem(i) as DelegatedStake?

            vh.avatar!!.setImageUrl(item)
            vh.coin!!.text = item!!.coin!!.symbol
            vh.amount!!.text = bdHuman(item.amount)
            if (item.coin!!.id == MinterSDK.DEFAULT_COIN_ID) {
                vh.subamount!!.visibility = View.GONE
                vh.subamount!!.text = null
            } else {
                vh.subamount!!.text = String.format("%s %s", bdHuman(item.amountBIP), MinterSDK.DEFAULT_COIN)
                vh.subamount!!.visibility = View.VISIBLE
            }
            vh.actionUnbond!!.setOnClickListener {
                if (mOnUnbondItemClickListener != null) {
                    mOnUnbondItemClickListener!!.invoke(getItem(viewHolder.bindingAdapterPosition) as DelegatedStake)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasProgressRow() && position == itemCount - 1) {
            ITEM_PROGRESS
        } else getItem(position)!!.viewType
    }

    fun setLoadState(loadState: MutableLiveData<LoadState>?) {
        mLoadState = loadState
    }

    fun setOnDelegatedClickListener(listener: OnDelegatedClickListener) {
        mOnDelegatedClickListener = listener
    }

    fun setOnUnbondItemClickListener(listener: OnUnbondItemClickListener) {
        mOnUnbondItemClickListener = listener
    }

    private fun hasProgressRow(): Boolean {
        return mLoadState != null && mLoadState!!.value != LoadState.Loaded
    }

    class StakeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @JvmField @BindView(R.id.item_avatar)
        var avatar: BipCircleImageView? = null

        @JvmField @BindView(R.id.item_coin)
        var coin: TextView? = null

        @JvmField @BindView(R.id.item_amount)
        var amount: TextView? = null

        @JvmField @BindView(R.id.item_subamount)
        var subamount: TextView? = null

        @JvmField @BindView(R.id.action_unbond)
        var actionUnbond: View? = null

        @JvmField @BindView(R.id.main)
        var main: View? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class ValidatorViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.fake_header) lateinit var fakeHeader: View
        @BindView(R.id.item_title) lateinit var title: TextView
        @BindView(R.id.item_public_key) lateinit var publicKey: TextView
        @BindView(R.id.action_delegate) lateinit var actionDelegate: View
        @BindView(R.id.action_copy) lateinit var actionCopy: View
        @BindView(R.id.item_avatar) lateinit var icon: BipCircleImageView

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}