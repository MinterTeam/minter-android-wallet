/*
 * Copyright (C) by MinterTeam. 2021
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.databinding.ItemListDelegatedStakeBinding
import network.minter.bipwallet.databinding.ItemListDelegatedValidatorBinding
import network.minter.bipwallet.internal.adapter.LoadState
import network.minter.bipwallet.internal.helpers.ContextHelper
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcher
import network.minter.bipwallet.internal.views.list.diff.DiffUtilDispatcherDelegate
import network.minter.bipwallet.tx.adapters.vh.TxProgressViewHolder
import network.minter.core.MinterSDK

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
typealias OnDelegatedClickListener = (DelegatedValidator) -> Unit
typealias OnUnbondItemClickListener = (DelegatedStake) -> Unit

class DelegationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DiffUtilDispatcherDelegate<DelegatedItem> {
    companion object {
        private const val ITEM_PROGRESS = R.layout.item_list_transaction_progress
    }

    private var data: MutableList<DelegatedItem> = ArrayList()
    private var inflater: LayoutInflater? = null
    private var loadState: MutableLiveData<LoadState>? = null
    private var onDelegatedClickListener: OnDelegatedClickListener? = null
    private var onUnbondItemClickListener: OnUnbondItemClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.context)
        }
        val view = inflater!!.inflate(viewType, parent, false)
        if (viewType == ITEM_PROGRESS) {
            return TxProgressViewHolder(view)
        } else if (viewType == DelegatedItem.ITEM_STAKE) {
            return StakeViewHolder(ItemListDelegatedStakeBinding.inflate(inflater!!, parent, false))
        }
        return ValidatorViewHolder(ItemListDelegatedValidatorBinding.inflate(inflater!!, parent, false))
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (getItemViewType(i) == DelegatedItem.ITEM_VALIDATOR) {
            val vh = viewHolder as ValidatorViewHolder
            val item = getItem(i) as DelegatedValidator
            vh.b.fakeHeader.visible = true

            if (item.name.isNullOrEmpty()) {
                vh.b.itemTitle.text = item.publicKey.toShortString()
                vh.b.itemPublicKey.text = item.publicKey.toShortString()
            } else {
                vh.b.itemTitle.text = item.name
                vh.b.itemPublicKey.text = item.publicKey.toShortString()
            }

            if (item.description != null) {
                ViewCompat.setTooltipText(vh.b.itemPublicKey, item.description)
            }
            vh.b.itemFees.text = vh.b.root.context.getString(R.string.stake_validator_fees,
                    item.commission,
                    item.minStake.humanize(),
                    MinterSDK.DEFAULT_COIN
            )


            vh.b.itemAvatar.setImageUrlFallback(item.publicKey.avatar, R.drawable.img_avatar_delegate)
            vh.b.actionCopy.setOnClickListener {
                ContextHelper.copyToClipboard(it.context, item.publicKey.toString())
            }
            vh.b.actionDelegate.setOnClickListener {
                if (onDelegatedClickListener != null) {
                    onDelegatedClickListener!!.invoke(getItem(viewHolder.bindingAdapterPosition) as DelegatedValidator)
                }
            }
        } else if (getItemViewType(i) == DelegatedItem.ITEM_STAKE) {
            val vh = viewHolder as StakeViewHolder
            val item = getItem(i) as DelegatedStake?
            val ctx = vh.b.root.context

            if (item!!.isKicked) {
                vh.b.itemCoin.setTextColor(ContextCompat.getColor(ctx, R.color.grey))
                vh.b.itemAvatar.setImageResource(R.drawable.ic_warning_yellow)
            } else {
                vh.b.itemCoin.setTextColor(ContextCompat.getColor(ctx, R.color.textColorPrimary))
                vh.b.itemAvatar.setImageUrl(item)
            }

            vh.b.itemCoin.text = item.coin.symbol
            vh.b.itemAmount.text = bdHuman(item.amount)
            if (item.coin.id == MinterSDK.DEFAULT_COIN_ID) {
                vh.b.itemSubamount.visibility = View.GONE
                vh.b.itemSubamount.text = null
            } else {
                vh.b.itemSubamount.text = String.format("%s %s", bdHuman(item.amountBIP), MinterSDK.DEFAULT_COIN)
                vh.b.itemSubamount.visibility = View.VISIBLE
            }
            vh.b.actionUnbond.setOnClickListener {
                if (onUnbondItemClickListener != null) {
                    onUnbondItemClickListener!!.invoke(getItem(viewHolder.bindingAdapterPosition) as DelegatedStake)
                }
            }
        }
    }

    fun getItem(pos: Int): DelegatedItem {
        return data[pos]
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasProgressRow() && position == itemCount - 1) {
            ITEM_PROGRESS
        } else getItem(position).viewType
    }

    fun setLoadState(loadState: MutableLiveData<LoadState>?) {
        this.loadState = loadState
    }

    fun setOnDelegatedClickListener(listener: OnDelegatedClickListener) {
        onDelegatedClickListener = listener
    }

    fun setOnUnbondItemClickListener(listener: OnUnbondItemClickListener) {
        onUnbondItemClickListener = listener
    }

    private fun hasProgressRow(): Boolean {
        return loadState != null && loadState!!.value != LoadState.Loaded
    }

    class StakeViewHolder(val b: ItemListDelegatedStakeBinding) : RecyclerView.ViewHolder(b.root)
    class ValidatorViewHolder(val b: ItemListDelegatedValidatorBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemCount(): Int {
        return data.size
    }

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>?, items: MutableList<DelegatedItem>, detectMoves: Boolean) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items, detectMoves)
    }

    override fun <T : DiffUtil.Callback?> dispatchChanges(diffUtilCallbackCls: Class<T>?, items: MutableList<DelegatedItem>) {
        DiffUtilDispatcher.dispatchChanges(this, diffUtilCallbackCls, items)
    }

    override fun getItems(): MutableList<DelegatedItem> {
        return data
    }

    override fun setItems(items: MutableList<DelegatedItem>) {
        data = items
    }

    override fun clear() {
        data.clear()
    }
}