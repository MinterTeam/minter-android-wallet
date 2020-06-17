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

package network.minter.bipwallet.delegation.views

import android.content.Intent
import moxy.InjectViewState
import network.minter.bipwallet.apis.explorer.RepoValidators
import network.minter.bipwallet.delegation.adapter.ValidatorSelectorAdapter
import network.minter.bipwallet.delegation.contract.ValidatorSelectorView
import network.minter.bipwallet.delegation.ui.ValidatorSelectorActivity
import network.minter.bipwallet.delegation.ui.ValidatorSelectorActivity.Filter
import network.minter.bipwallet.internal.mvp.MvpBasePresenter
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.explorer.models.ValidatorItem
import timber.log.Timber
import javax.inject.Inject

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
class ValidatorSelectorPresenter @Inject constructor() : MvpBasePresenter<ValidatorSelectorView>() {
    @Inject lateinit var repoValidators: RepoValidators
    @Inject lateinit var accountStorage: RepoAccounts

    private val adapter = ValidatorSelectorAdapter()
    private var filter: Filter = Filter.Online

    override fun handleExtras(intent: Intent?) {
        super.handleExtras(intent)
        if (intent?.hasExtra(ValidatorSelectorActivity.EXTRA_FILTER) == true) {
            filter = intent.getSerializableExtra(ValidatorSelectorActivity.EXTRA_FILTER) as Filter
        }

        repoValidators.observe()
                .retryWhen(errorResolver)
                .joinToUi()
                .subscribe(
                        { res ->
                            val validators = when (filter) {
                                Filter.None -> {
                                    res
                                }
                                Filter.Online -> {
                                    res.toMutableList().filterOnline()
                                }
                                Filter.Delegated -> {
                                    res.toMutableList().filterDelegated()
                                }
                            }

                            viewState.createItemSeparator(repoValidators.entity.lastUsed.size > 0)
                            adapter.setItems(repoValidators.entity.lastUsed, validators)
                            adapter.notifyDataSetChanged()
                            viewState.hideProgress()
                        },
                        { t ->
                            Timber.e(t, "Unable to load validators")
                            viewState.onError(t)
                            viewState.hideProgress()
                        }
                )
                .disposeOnDestroy()

        repoValidators.update()
    }

    private fun MutableList<ValidatorItem>.filterOnline(): List<ValidatorItem> {
        return filter { it.status == ValidatorItem.STATUS_ONLINE }
    }

    private fun MutableList<ValidatorItem>.filterDelegated(): List<ValidatorItem> {
        return filter {
            accountStorage.entity.mainWallet.hasDelegated(it.pubKey)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        adapter.setOnItemClickListener {
            viewState.finishSuccess(it)
        }
        viewState.setAdapter(adapter)
    }
}