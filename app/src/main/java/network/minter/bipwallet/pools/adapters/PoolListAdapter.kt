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

package network.minter.bipwallet.pools.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import network.minter.bipwallet.apis.reactive.avatar
import network.minter.bipwallet.apis.reactive.bipToUsd
import network.minter.bipwallet.databinding.ItemListPoolBinding
import network.minter.bipwallet.internal.helpers.DateHelper.formatDateLong
import network.minter.bipwallet.internal.helpers.MathHelper.asCurrency
import network.minter.bipwallet.internal.helpers.MathHelper.humanize
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.pools.models.PoolCombined
import network.minter.bipwallet.pools.models.PoolsFilter

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class PoolListAdapter : PagingDataAdapter<PoolCombined, PoolListAdapter.ViewHolder>(sDiffUtilCallback) {

    companion object {
        private val sDiffUtilCallback = object: DiffUtil.ItemCallback<PoolCombined> () {
            override fun areItemsTheSame(oldItem: PoolCombined, newItem: PoolCombined): Boolean {
                return oldItem.pool.coin0.symbol == newItem.pool.coin0.symbol && oldItem.pool.coin1.symbol == newItem.pool.coin1.symbol
            }

            override fun areContentsTheSame(oldItem: PoolCombined, newItem: PoolCombined): Boolean {
                return ObjectsCompat.equals(oldItem, newItem)
            }

        }
    }

    private var onAddLiquidityListener: ((PoolCombined)->Unit)? = null
    private var onRemoveLiquidityListener: ((PoolCombined)->Unit)? = null

//    private var _myAddress: (() -> MinterAddress)? = null
    private var _inflater: LayoutInflater? = null

    fun setOnAddLiquidityListener(cb: (PoolCombined)->Unit) {
        onAddLiquidityListener = cb
    }

    fun setOnRemoveLiquidityListener(cb: (PoolCombined)->Unit) {
        onRemoveLiquidityListener = cb
    }


    class ViewHolder(val b: ItemListPoolBinding): RecyclerView.ViewHolder(b.root)


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pool = getItem(position)!!
        holder.b.itemAvatarFirst.setImageUrl(pool.pool.coin0.avatar)
        holder.b.itemAvatarSecond.setImageUrl(pool.pool.coin1.avatar)

        holder.b.itemLpTitle.text = pool.pool.token.symbol
        @SuppressLint("SetTextI18n")
        holder.b.itemPair.text = "${pool.pool.coin0.symbol} / ${pool.pool.coin1.symbol}"

        if(pool.stake != null) {
            holder.b.actionAdd.visible = true
            holder.b.actionRemove.visible = true
        } else {
            holder.b.actionAdd.visible = true
            holder.b.actionRemove.visible = false
        }

        // as farming response does not include real pools information, we should remove basic pool information
        if(pool.filter == PoolsFilter.Farming) {
            holder.b.labelApy.visible = false
            holder.b.valueApy.visible = false
            holder.b.labelVolume1d.visible = false
            holder.b.valueVolume1d.visible = false

            holder.b.labelFarmingApr.visible = true
            holder.b.valueFarmingApr.visible = true
            holder.b.valueFarmingApr.text = pool.getFarmingPercent()

            holder.b.labelEndDate.visible = true
            holder.b.valueEndDate.visible = true
            holder.b.valueEndDate.text = pool.farm!!.finishAt?.formatDateLong()

            if(pool.stake != null) {
                holder.b.layoutStake.visible = true
                holder.b.valueStakedAmountFirst.text = "${pool.stake.amount0.humanize()} ${pool.stake.coin0.symbol}"
                holder.b.valueStakedAmountSecond.text = "${pool.stake.amount1.humanize()} ${pool.stake.coin1.symbol}"
                holder.b.valueYourShare.text = "${pool.stake.liquidityShare.asCurrency()}%"
                holder.b.valueYourLiquidity.text = pool.stake.liquidityBip.bipToUsd()
            } else {
                holder.b.layoutStake.visible = false
            }
        } else {
            if(pool.pool.volumeBip1d != null) {
                holder.b.labelApy.visible = true
                holder.b.valueApy.visible = true
                holder.b.valueApy.text = pool.getApy()
                holder.b.labelVolume1d.visible = true
                holder.b.valueVolume1d.visible = true
                holder.b.valueVolume1d.text = pool.volume1dUsd
            } else {
                if(pool.filter == PoolsFilter.Staked) {
                    holder.b.labelApy.visible = false
                    holder.b.valueApy.visible = false
                    holder.b.labelVolume1d.visible = false
                    holder.b.valueVolume1d.visible = false
                }
            }

            if(pool.stake != null) {
                holder.b.layoutStake.visible = true
                holder.b.valueStakedAmountFirst.text = "${pool.stake.amount0.humanize()} ${pool.stake.coin0.symbol}"
                holder.b.valueStakedAmountSecond.text = "${pool.stake.amount1.humanize()} ${pool.stake.coin1.symbol}"
                holder.b.valueYourShare.text = "${pool.stake.liquidityShare.asCurrency()}%"
                holder.b.valueYourLiquidity.text = pool.stake.liquidityBip.bipToUsd()
            } else {
                holder.b.layoutStake.visible = false
            }

            if(pool.farm != null) {
                holder.b.labelFarmingApr.visible = true
                holder.b.valueFarmingApr.visible = true
                holder.b.valueFarmingApr.text = pool.getFarmingPercent()
                holder.b.labelEndDate.visible = true
                holder.b.valueEndDate.visible = true
                holder.b.valueEndDate.text = pool.farm.finishAt?.formatDateLong()
            } else {
                holder.b.labelFarmingApr.visible = false
                holder.b.valueFarmingApr.visible = false
                holder.b.labelEndDate.visible = false
                holder.b.valueEndDate.visible = false
            }
        }

        holder.b.actionAdd.setOnClickListener {
            onAddLiquidityListener?.invoke(getItem(holder.bindingAdapterPosition)!!)
        }
        holder.b.actionRemove.setOnClickListener {
            onRemoveLiquidityListener?.invoke(getItem(holder.bindingAdapterPosition)!!)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if(_inflater == null) {
            _inflater = LayoutInflater.from(parent.context)
        }

        return ViewHolder(
                ItemListPoolBinding.inflate(_inflater!!, parent, false)
        )
    }
}