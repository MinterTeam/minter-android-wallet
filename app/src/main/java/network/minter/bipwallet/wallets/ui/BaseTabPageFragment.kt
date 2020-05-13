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
package network.minter.bipwallet.wallets.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import dagger.android.support.AndroidSupportInjection
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.FragmentPageWalletsBinding
import network.minter.bipwallet.internal.BaseFragment
import network.minter.bipwallet.internal.views.list.ViewElevationOnScroll
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView
import network.minter.bipwallet.wallets.contract.BaseWalletsPageView.ViewStatus

abstract class BaseTabPageFragment : BaseFragment(), BaseWalletsPageView {
    private lateinit var binding: FragmentPageWalletsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPageWalletsBinding.inflate(inflater, container, false)

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addOnScrollListener(ViewElevationOnScroll(binding.top))
//        binding.list.isNestedScrollingEnabled = false
        return binding.root
    }

    override fun setViewStatus(status: ViewStatus) {
        setViewStatus(status, null)
    }

    override fun setViewStatus(status: ViewStatus, error: CharSequence?) {
        when (status) {
            ViewStatus.Progress -> {
                binding.list.visibility = View.GONE
                binding.action.visibility = View.GONE
                binding.emptyTitle.visibility = View.GONE
                binding.progress.visibility = View.VISIBLE
            }
            ViewStatus.Empty -> {
                binding.list.visibility = View.GONE
                binding.action.visibility = View.GONE
                binding.emptyTitle.visibility = View.VISIBLE
                binding.progress.visibility = View.GONE
            }
            ViewStatus.Error -> {
                binding.list.visibility = View.GONE
                binding.action.visibility = View.GONE
                binding.emptyTitle.visibility = View.VISIBLE
                binding.emptyTitle.text = error ?: "Unexpected error"
                binding.progress.visibility = View.GONE
            }
            ViewStatus.Normal -> {
                binding.list.visibility = View.VISIBLE
                binding.action.visibility = View.VISIBLE
                binding.emptyTitle.visibility = View.GONE
                binding.progress.visibility = View.GONE
            }
        }
    }

    override fun setListTitle(title: CharSequence) {
        binding.title.text = title
    }

    override fun setListTitle(title: Int) {
        binding.title.setText(title)
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        binding.list.adapter = adapter
    }

    override fun setActionTitle(title: CharSequence) {
        binding.action.text = title
    }

    override fun setActionTitle(title: Int) {
        binding.action.setText(title)
    }

    override fun setOnActionClickListener(listener: View.OnClickListener) {
        binding.action.setOnClickListener(listener)
    }

    override fun showProgress(show: Boolean) {
        binding.progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun setEmptyTitle(title: CharSequence) {
        binding.emptyTitle.text = title
    }

    override fun setEmptyTitle(title: Int) {
        binding.emptyTitle.setText(title)
    }

    override fun showEmpty(show: Boolean) {
        binding.emptyTitle.visibility = if (show) View.VISIBLE else View.GONE
    }

    class ItemViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        @JvmField @BindView(R.id.item_avatar)
        var avatar: BipCircleImageView? = null

        @JvmField @BindView(R.id.item_title)
        var title: TextView? = null

        @JvmField @BindView(R.id.item_amount)
        var amount: TextView? = null

        @JvmField @BindView(R.id.item_subamount)
        var subname: TextView? = null

        init {
            ButterKnife.bind(this, itemView!!)
        }
    }
}