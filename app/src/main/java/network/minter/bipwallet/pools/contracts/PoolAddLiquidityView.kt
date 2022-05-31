/*
 * Copyright (C) by MinterTeam. 2022
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

package network.minter.bipwallet.pools.contracts

import android.view.View
import com.edwardstock.inputfield.form.InputWrapper
import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.OneExecutionStateStrategy
import moxy.viewstate.strategy.StateStrategyType
import network.minter.bipwallet.internal.dialogs.DialogExecutor
import network.minter.core.crypto.MinterHash
import java.math.BigDecimal

/**
 * minter-android-wallet. 2021
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
@StateStrategyType(AddToEndSingleStrategy::class)
interface PoolAddLiquidityView: MvpView {
    fun setCoin0(coin0Amount: BigDecimal?, coin0: String)
    fun setCoin1(coin1Amount: BigDecimal?, coin1: String)
    fun setOnTextChangedListener(listener: (InputWrapper, Boolean, Boolean) -> Unit)
    fun setOnSwapCoins(listener: (View) -> Unit)
    fun setEnableSubmit(enable: Boolean)
    fun setOnClickUseMax0(listener: (View) -> Unit)
    fun setOnClickUseMax1(listener: (View) -> Unit)
    fun setCoin0EnableUseMax(enable: Boolean)
    fun setCoin1EnableUseMax(enable: Boolean)
    fun setOnSubmit(listener: (View) -> Unit)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startDialog(executor: DialogExecutor)

    fun setFee(feeText: CharSequence)

    fun setCoin0Error(error: CharSequence?)
    fun setCoin1Error(error: CharSequence?)

    fun finishSuccess()
    fun finishCancel()

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun startExplorer(txHash: MinterHash)
}