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

package network.minter.bipwallet.tx

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.apis.gate.TxInitDataRepository
import network.minter.bipwallet.apis.reactive.ReactiveGate.*
import network.minter.bipwallet.apis.reactive.rxGate
import network.minter.bipwallet.apis.reactive.toObservable
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.crypto.MinterAddress
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.GateResult.copyError
import network.minter.explorer.models.PushResult
import network.minter.explorer.repo.GateTransactionRepository
import java.math.BigInteger

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

typealias Callback = () -> Unit
typealias ErrorCallback = (GateResult<*>) -> Unit
typealias FailCallback = (Throwable) -> Unit
typealias SuccessCallback = (GateResult<PushResult>) -> Unit
typealias TxCreator = (initData: TxInitData) -> TransactionSign
typealias TxCreatorObservable = (initData: TxInitData) -> Observable<TransactionSign>

class TransactionSender(
        private val address: MinterAddress,
        private val initDataRepo: TxInitDataRepository,
        private val gateRepo: GateTransactionRepository
) {
    var txCreator: TxCreator? = null
    var txCreatorObservable: TxCreatorObservable? = null
    var startListener: Callback? = null
    var progressListener: Callback? = null
    var errorCallback: ErrorCallback? = null
    var successCallback: SuccessCallback? = null


    fun start() {
        startListener?.invoke()
    }

    fun next() {
        progressListener?.invoke()

        initDataRepo.load(address)
                .subscribeOn(Schedulers.io())
                .switchMap { initData ->
                    // if error occurred upper, notify error
                    if (!initData.isSuccess) {
                        return@switchMap copyError<TransactionSign>(initData.errorResult).toObservable()
                    }
                    initData.nonce = initData.nonce!! + BigInteger.ONE

                    if (txCreator == null && txCreatorObservable == null) {
                        return@switchMap createGateErrorPlain<TransactionSign>(
                                IllegalStateException("One of tx creator must be set: txCreator or txCreatorObservable")
                        ).toObservable()
                    }

                    if (txCreator != null) {
                        return@switchMap createDummy(txCreator!!.invoke(initData)).toObservable()
                    }

                    txCreatorObservable!!.invoke(initData).map { createDummy(it) }
                }
                .switchMap { sign ->
                    if (!sign.isOk) {
                        return@switchMap copyError<PushResult>(sign).toObservable()
                    }
                    gateRepo.sendTransaction(sign.result).rxGate()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(toGateError())
                .subscribe(
                        { result: GateResult<PushResult> ->
                            onSuccessExecuteTransaction(result)
                        },
                        { t: Throwable ->
                            // failed, because it's not a success, as success can contains failed transaction result, human-readable
                            // any other exceptions are the fail, just convert message to result error
                            onFailedExecuteTransaction(createGateErrorPlain<Any>(t))
                        }
                )
    }

    private fun onSuccessExecuteTransaction(result: GateResult<PushResult>) {
        if (!result.isOk) {
            onErrorExecuteTransaction(result)
            return
        }
        successCallback?.invoke(result)
    }

    private fun onFailedExecuteTransaction(errorResult: GateResult<*>) {
        errorCallback?.invoke(errorResult)
    }

    private fun onErrorExecuteTransaction(errorResult: GateResult<*>) {
        errorCallback?.invoke(errorResult)
    }
}