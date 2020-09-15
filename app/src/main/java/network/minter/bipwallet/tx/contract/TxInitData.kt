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

package network.minter.bipwallet.tx.contract

import network.minter.bipwallet.apis.reactive.castErrorResultTo
import network.minter.blockchain.models.TransactionCommissionValue
import network.minter.explorer.models.GasValue
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.TxCount
import java.math.BigDecimal
import java.math.BigInteger

class TxInitData {
    var nonce: BigInteger? = null
    var gas: BigInteger? = null
    var commission: BigDecimal? = null
    var errorResult: GateResult<*>? = null

    constructor(vararg values: GateResult<*>) {
        for (item in values) {
            if (!item.isOk) {
                errorResult = item.castErrorResultTo<Any>()
                return
            }
        }
        setValues(*values)
    }

    constructor(nonce: BigInteger?, gas: BigInteger?) {
        this.nonce = nonce
        this.gas = gas
    }

    constructor(err: GateResult<*>?) {
        errorResult = err
    }

    val isSuccess: Boolean
        get() {
            val tmp = errorResult
            return tmp?.error == null || tmp.isOk
        }

    private fun setValues(vararg values: GateResult<*>) {
        for (item in values) {
            setValue(item)
        }
    }

    private fun setValue(src: GateResult<*>) {
        when (src.result) {
            is GasValue -> {
                gas = (src.result as GasValue).gas
            }
            is TransactionCommissionValue -> {
                commission = (src.result as TransactionCommissionValue).getValue()
            }
            is TxCount -> {
                nonce = (src.result as TxCount).count
            }
        }
    }
}