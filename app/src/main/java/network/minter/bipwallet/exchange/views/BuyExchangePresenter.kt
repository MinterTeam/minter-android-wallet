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
package network.minter.bipwallet.exchange.views

import moxy.InjectViewState
import network.minter.bipwallet.R
import network.minter.bipwallet.apis.explorer.RepoTransactions
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.coins.RepoCoins
import network.minter.bipwallet.exchange.contract.BuyExchangeView
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.exceptions.ErrorManager
import network.minter.bipwallet.internal.helpers.MathHelper.bdHuman
import network.minter.bipwallet.internal.storage.RepoAccounts
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.blockchain.models.operational.OperationType
import network.minter.core.MinterSDK
import network.minter.explorer.repo.ExplorerPoolsRepository
import network.minter.explorer.repo.GateEstimateRepository
import network.minter.explorer.repo.GateGasRepository
import network.minter.explorer.repo.GateTransactionRepository
import java.util.*
import javax.inject.Inject

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@InjectViewState
open class BuyExchangePresenter @Inject constructor(
        session: AuthSession,
        secretStorage: SecretStorage,
        accountStorage: RepoAccounts,
        txRepo: RepoTransactions,
        explorerCoinsRepository: RepoCoins,
        gasRepo: GateGasRepository,
        estimateRepository: GateEstimateRepository,
        poolsRepository: ExplorerPoolsRepository,
        gateTransactionRepository: GateTransactionRepository,
        txInitDataRepo: TxInitDataRepository,
        errorManager: ErrorManager) :
        ExchangePresenter<BuyExchangeView>(
                session,
                secretStorage,
                accountStorage,
                txRepo,
                explorerCoinsRepository,
                gasRepo,
                estimateRepository,
                poolsRepository,
                gateTransactionRepository,
                txInitDataRepo,
                errorManager
        ) {
    override val isBuying: Boolean
        get() = true

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState!!.setFee(String.format("%s %s", bdHuman(OperationType.BuyCoin.fee), MinterSDK.DEFAULT_COIN.uppercase(Locale.getDefault())))
        viewState.setCalculationTitle(R.string.label_you_will_pay_approximately)
    }
}