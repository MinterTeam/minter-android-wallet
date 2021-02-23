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

package network.minter.bipwallet.delegation.ui

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import network.minter.bipwallet.R
import network.minter.bipwallet.databinding.ActivityValidatorSelectorBinding
import network.minter.bipwallet.delegation.contract.ValidatorSelectorView
import network.minter.bipwallet.delegation.views.ValidatorSelectorPresenter
import network.minter.bipwallet.internal.BaseMvpInjectActivity
import network.minter.bipwallet.internal.helpers.ErrorViewHelper
import network.minter.bipwallet.internal.helpers.IntentHelper.toParcel
import network.minter.bipwallet.internal.helpers.ViewExtensions.visible
import network.minter.bipwallet.internal.system.ActivityBuilder
import network.minter.bipwallet.internal.views.list.BorderedItemSeparator
import network.minter.explorer.models.ValidatorItem
import org.parceler.Parcels
import javax.inject.Inject
import javax.inject.Provider

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class ValidatorSelectorActivity : BaseMvpInjectActivity(), ValidatorSelectorView {
    @Inject lateinit var presenterProvider: Provider<ValidatorSelectorPresenter>
    @InjectPresenter lateinit var presenter: ValidatorSelectorPresenter
    private lateinit var binding: ActivityValidatorSelectorBinding
    private var itemSeparator: BorderedItemSeparator? = null


    @ProvidePresenter
    fun providePresenter(): ValidatorSelectorPresenter {
        return presenterProvider.get()
    }

    enum class Filter {
        None,
        Online,
        Delegated
    }

    companion object {
        const val RESULT = "RESULT_VALIDATOR"
        const val EXTRA_FILTER = "EXTRA_FILTER"

        fun getResult(intent: Intent): ValidatorItem? {
            if (!intent.hasExtra(RESULT)) {
                return null
            }

            return Parcels.unwrap<ValidatorItem>(
                    intent.getParcelableExtra(RESULT)
            )
        }
    }

    override fun createItemSeparator(haveLastUsedHeader: Boolean) {
        if (itemSeparator != null) {
            return
        }

        itemSeparator = BorderedItemSeparator(this, R.drawable.shape_bottom_separator, false, true)
        if (haveLastUsedHeader) {
            itemSeparator!!.setSkipElements(listOf(0, 1, 3))
        }
        binding.list.addItemDecoration(itemSeparator!!)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityValidatorSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar)

        presenter.handleExtras(intent)

        binding.list.layoutManager = LinearLayoutManager(this)
    }

    class Builder : ActivityBuilder {
        private var filter: Filter = Filter.None

        constructor(from: Activity) : super(from)
        constructor(from: Fragment) : super(from)
        constructor(from: Service) : super(from)

        fun setFilter(filter: Filter): Builder {
            this.filter = filter
            return this
        }

        override fun onBeforeStart(intent: Intent) {
            super.onBeforeStart(intent)
            intent.putExtra(EXTRA_FILTER, filter)
        }

        override fun getActivityClass(): Class<*> {
            return ValidatorSelectorActivity::class.java
        }
    }

    override fun showEmpty(show: Boolean) {
        runOnUiThread {
            binding.emptyText.visible = show
        }
    }

    override fun onError(t: Throwable?) {
        ErrorViewHelper(binding.errorView).onError(t)
    }

    override fun onError(err: String?) {
        ErrorViewHelper(binding.errorView).onError(err)
    }

    override fun onErrorWithRetry(errorMessage: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, errorResolver)
    }

    override fun onErrorWithRetry(errorMessage: String?, actionName: String?, errorResolver: View.OnClickListener?) {
        ErrorViewHelper(binding.errorView).onErrorWithRetry(errorMessage, actionName, errorResolver)
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        binding.list.adapter = adapter
    }

    override fun finishCancel() {
        finish()
    }

    override fun finishSuccess(validator: ValidatorItem) {
        val res = Intent()
        res.putExtra(RESULT, validator.toParcel())
        setResult(Activity.RESULT_OK, res)
        finish()
    }

    override fun showProgress() {
        binding.progress.visible = true
    }

    override fun hideProgress() {
        binding.progress.visible = false
    }
}
