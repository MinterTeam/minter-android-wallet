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

package network.minter.bipwallet.apis.gate

import io.reactivex.Observable
import io.reactivex.Observable.combineLatest
import io.reactivex.schedulers.Schedulers
import network.minter.bipwallet.internal.helpers.MathHelper.humanizeDecimal
import network.minter.bipwallet.tx.contract.TxInitData
import network.minter.blockchain.models.TransactionCommissionValue
import network.minter.blockchain.models.operational.OperationType
import network.minter.blockchain.models.operational.Transaction
import network.minter.blockchain.models.operational.TransactionSign
import network.minter.core.MinterSDK
import network.minter.core.crypto.MinterAddress
import network.minter.core.crypto.MinterPublicKey
import network.minter.core.crypto.PrivateKey
import network.minter.explorer.models.GasValue
import network.minter.explorer.models.GateResult
import network.minter.explorer.models.PriceCommissions
import network.minter.explorer.models.TxCount
import network.minter.explorer.repo.GateEstimateRepository
import network.minter.explorer.repo.GateGasRepository
import java.math.BigDecimal
import java.math.BigInteger

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */
class TxInitDataRepository(
        val estimateRepo: GateEstimateRepository,
        val gasRepo: GateGasRepository
) {

    private fun makeDummyTx(gasPrice: BigInteger = BigInteger.ONE, payload: ByteArray? = null): Transaction.Builder {
        val b = Transaction.Builder(BigInteger.ONE)
                .setGasPrice(gasPrice)
                .setGasCoinId(MinterSDK.DEFAULT_COIN_ID)

        if (payload != null) {
            b.setPayload(payload)
        }
        return b
    }

    fun estimateBaseFee(opType: OperationType, payload: ByteArray? = null): Observable<GateResult<TransactionCommissionValue>> {
        val pk = PrivateKey("1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC")
        val addr = pk.publicKey.toMinter()
        val pubKey = MinterPublicKey("Mp1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC")
        val sign: TransactionSign? = when (opType) {
            OperationType.SendCoin -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .sendCoin()
                        .setCoinId(0)
                        .setValue("0")
                        .setTo(addr)
                        .build()
                        .signSingle(pk)
            }
            OperationType.SellCoin -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .sellCoin()
                        .setCoinIdToBuy(0)
                        .setCoinIdToSell(0)
                        .setValueToSell("10")
                        .setMinValueToBuy("0")
                        .build().signSingle(pk)
            }
            OperationType.SellAllCoins -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .sellAllCoins()
                        .setCoinIdToBuy(0)
                        .setCoinIdToSell(0)
                        .setMinValueToBuy("0")
                        .build().signSingle(pk)
            }
            OperationType.BuyCoin -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .buyCoin()
                        .setCoinIdToBuy(0)
                        .setCoinIdToSell(0)
                        .setValueToBuy("0")
                        .setMaxValueToSell("1000000")
                        .build().signSingle(pk)
            }
            OperationType.CreateCoin -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .createCoin()
                        .setConstantReserveRatio(10)
                        .setInitialAmount("100")
                        .setInitialReserve("100")
                        .setName("AAA")
                        .setSymbol("AAA")
                        .build().signSingle(pk)
            }
            OperationType.DeclareCandidacy -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .declareCandidacy()
                        .setAddress(addr)
                        .setCoinId(0)
                        .setCommission(10)
                        .setPublicKey(pubKey)
                        .setStake("100")
                        .build().signSingle(pk)
            }
            OperationType.Delegate -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .delegate()
                        .setCoinId(0)
                        .setPublicKey(pubKey)
                        .setStake("100")
                        .build().signSingle(pk)
            }
            OperationType.Unbound -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .unbound()
                        .setCoinId(0)
                        .setPublicKey(pubKey)
                        .setValue("100")
                        .build().signSingle(pk)
            }
            OperationType.RedeemCheck -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .redeemCheck()
                        .setProof(ByteArray(65))
                        .setRawCheck(ByteArray(64))
                        .build().signSingle(pk)
            }
            OperationType.SetCandidateOnline -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .setCandidateOnline()
                        .setPublicKey(pubKey)
                        .build()
                        .signSingle(pk)
            }
            OperationType.SetCandidateOffline -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .setCandidateOffline()
                        .setPublicKey(pubKey)
                        .build()
                        .signSingle(pk)
            }
            OperationType.CreateMultisigAddress -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .createMultisigAddress()
                        .addAddress(addr)
                        .addWeight(0)
                        .setThreshold(1)
                        .build()
                        .signSingle(pk)
            }
            OperationType.Multisend -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .multiSend()
                        .addItem(0, addr, "100")
                        .build()
                        .signSingle(pk)
            }
            OperationType.EditCandidate -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .editCandidate()
                        .setControlAddress(addr)
                        .setRewardAddress(addr)
                        .setPublicKey(pubKey)
                        .setOwnerAddress(addr)
                        .build()
                        .signSingle(pk)
            }
            OperationType.SetHaltBlock -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .setHaltBlock()
                        .setHeight(100)
                        .setPublicKey(pubKey)
                        .build()
                        .signSingle(pk)
            }
            OperationType.RecreateCoin -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .recreateCoin()
                        .setConstantReserveRatio(10)
                        .setInitialAmount("100")
                        .setInitialReserve("100")
                        .setName("AAA")
                        .setSymbol("AAA")
                        .build().signSingle(pk)
            }
            OperationType.EditCoinOwner -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .editCoinOwner()
                        .setNewOwner(addr)
                        .setSymbol("AAA")
                        .build()
                        .signSingle(pk)
            }
            OperationType.EditMultisig -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .editMultisig()
                        .addAddress(addr)
                        .setThreshold(1)
                        .addWeight(1)
                        .build()
                        .signSingle(pk)
            }
            OperationType.EditCandidatePublicKey -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .editCandidatePublicKey()
                        .setNewPublicKey(pubKey)
                        .setPublicKey(pubKey)
                        .build()
                        .signSingle(pk)
            }
            OperationType.AddLiquidity -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .addLiquidity()
                        .setCoin0(0)
                        .setCoin1(1)
                        .setVolume("100")
                        .setMaximumVolume("100000")
                        .build()
                        .signSingle(pk)
            }
            OperationType.RemoveLiquidity -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .removeLiquidity()
                        .setCoin0(0)
                        .setCoin1(1)
                        .setLiquidity("100")
                        .setMinVolume0("1")
                        .setMinVolume1("1")
                        .build()
                        .signSingle(pk)
            }
            OperationType.SellSwapPool -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .sellSwapPool()
                        .addCoinId(0)
                        .addCoinId(0)
                        .setValueToSell("10")
                        .build()
                        .signSingle(pk)
            }
            OperationType.SellAllSwapPool -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .sellAllSwapPool()
                        .addCoinId(0)
                        .addCoinId(1)
                        .setMinValueToBuy("1")
                        .build()
                        .signSingle(pk)
            }
            OperationType.BuySwapPool -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .buySwapPool()
                        .addCoinId(0)
                        .addCoinId(0)
                        .setValueToBuy("10")
                        .build()
                        .signSingle(pk)
            }
            OperationType.EditCandidateCommission -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .editCandidateCommission()
                        .setPublicKey(pubKey)
                        .setCommission(10)
                        .build()
                        .signSingle(pk)
            }
            OperationType.MintToken -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .mintToken()
                        .setCoinId(0)
                        .setValue("100")
                        .build()
                        .signSingle(pk)
            }
            OperationType.BurnToken -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .burnToken()
                        .setCoinId(0)
                        .setValue("100")
                        .build()
                        .signSingle(pk)
            }
            OperationType.CreateToken -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .createToken()
                        .setInitialAmount("100")
                        .setName("AAA")
                        .setSymbol("AAA")
                        .build().signSingle(pk)
            }
            OperationType.RecreateToken -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .recreateToken()
                        .setInitialAmount("100")
                        .setName("AAA")
                        .setSymbol("AAA")
                        .build().signSingle(pk)
            }
            OperationType.VoteCommission -> {
                val data = makeDummyTx(BigInteger.ONE, payload).voteCommission()
                data.height = BigInteger("999999")
                data.pubKey = pubKey
                data.coinId = MinterSDK.DEFAULT_COIN_ID

                data.payloadByte = BigInteger("2")
                data.send = BigInteger("10")
                data.sellBancor = BigInteger("100")
                data.sellAllBancor = BigInteger("100")
                data.buyBancor = BigInteger("100")
                data.sellPoolBase = BigInteger("100")
                data.sellPoolDelta = BigInteger("100")
                data.sellAllPoolBase = BigInteger("100")
                data.sellAllPoolDelta = BigInteger("100")
                data.buyPoolBase = BigInteger("100")
                data.buyPoolDelta = BigInteger("100")
                data.createCoin = BigInteger("0")
                data.createToken = BigInteger("0")
                data.recreateCoin = BigInteger("10000000")
                data.recreateToken = BigInteger("10000000")
                data.createTicker3 = BigInteger("1000000000")
                data.createTicker4 = BigInteger("100000000")
                data.createTicker5 = BigInteger("10000000")
                data.createTicker6 = BigInteger("1000000")
                data.createTicker7to10 = BigInteger("100000")
                data.declareCandidacy = BigInteger("100000")
                data.delegate = BigInteger("200")
                data.unbond = BigInteger("200")
                data.redeemCheck = BigInteger("100")
                data.setCandidateOn = BigInteger("1000")
                data.setCandidateOff = BigInteger("1000")
                data.createMultisig = BigInteger("1000")
                data.multisendBase = BigInteger("2")
                data.multisendDelta = BigInteger("20")
                data.editCandidate = BigInteger("100000")
                data.setHaltBlock = BigInteger("100000")
                data.editTickerOwner = BigInteger("10000")
                data.editMultisig = BigInteger("10000")
                data.editCandidatePubKey = BigInteger("10000000")
                data.createSwapPool = BigInteger("10000")
                data.addLiquidity = BigInteger("200")
                data.removeLiquidity = BigInteger("200")
                data.editCandidateCommission = BigInteger("10000000")
                data.mintToken = BigInteger("1000")
                data.burnToken = BigInteger("1000")
                data.voteCommission = BigInteger("10")
                data.voteUpdate = BigInteger("10")
                data.build().signSingle(pk)
            }
            OperationType.VoteUpdate -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .voteUpdate()
                        .setHeight(BigInteger("999"))
                        .setPubKey(pubKey)
                        .setVersion("1.0.0")
                        .build()
                        .signSingle(pk)
            }
            OperationType.CreateSwapPool -> {
                makeDummyTx(BigInteger.ONE, payload)
                        .createSwapPool()
                        .setCoin0(0)
                        .setCoin1(1)
                        .setVolume0("100")
                        .setVolume1("100")
                        .build()
                        .signSingle(pk)
            }
            else -> null
        }
        return estimateRepo.getTransactionCommission(sign!!)
    }

    fun loadFee(): Observable<TxInitData> {
        return combineLatest(
                gasRepo.minGas,
                gasRepo.commissions,
                { t1: GateResult<GasValue>, t2: GateResult<PriceCommissions> ->
                    TxInitData(t1, t2)
                })
                .switchMap { initData ->
                    if (initData.gasRepresentingCoin.id == MinterSDK.DEFAULT_COIN_ID) {
                        return@switchMap Observable.just(initData)
                    }

                    estimateRepo.getCoinExchangeCurrencyToSell(
                            initData.gasRepresentingCoin.id,
                            BigDecimal.ONE,
                            MinterSDK.DEFAULT_COIN_ID

                    ).map {
                        if (!it.isOk) {
                            return@map initData
                        }

                        initData.gasBaseCoinRate = it.result.amount
                        initData
                    }
                }
                .subscribeOn(Schedulers.io())
    }

    fun load(address: MinterAddress): Observable<TxInitData> {
        return combineLatest(
                estimateRepo.getTransactionCount(address),
                gasRepo.minGas,
                gasRepo.commissions,
                { t1: GateResult<TxCount>, t2: GateResult<GasValue>, t3: GateResult<PriceCommissions> ->
                    TxInitData(t1, t2, t3)
                })
                .switchMap { initData ->
                    if (initData.gasRepresentingCoin.id == MinterSDK.DEFAULT_COIN_ID) {
                        return@switchMap Observable.just(initData)
                    }

                    estimateRepo.getCoinExchangeCurrencyToSell(
                            initData.gasRepresentingCoin.id,
                            BigDecimal.ONE,
                            MinterSDK.DEFAULT_COIN_ID

                    ).map {
                        if (!it.isOk) {
                            return@map initData
                        }

                        initData.gasBaseCoinRate = it.result.amount
                        initData
                    }
                }
                .subscribeOn(Schedulers.io())
    }

    fun loadFeeWithTx(payload: ByteArray? = null): Observable<TxInitData> {
        return combineLatest(
                gasRepo.minGas,
                gasRepo.commissions,
                { t1: GateResult<GasValue>, t2: GateResult<PriceCommissions> ->
                    TxInitData(t1, t2)
                })
                .switchMap { initData ->
                    if (initData.gasRepresentingCoin.id == MinterSDK.DEFAULT_COIN_ID) {
                        return@switchMap Observable.just(initData)
                    }

                    estimateBaseFee(OperationType.SendCoin, payload).map {
                        if (!it.isOk) {
                            return@map initData
                        }

                        initData.gasBaseCoinRate = it.result.getValue() / initData.priceCommissions.getByType(OperationType.SendCoin).humanizeDecimal()
                        initData.payloadFee = initData.gasBaseCoinRate * initData.priceCommissions.payloadByte.humanizeDecimal()
                        initData
                    }
                }
                .subscribeOn(Schedulers.io())
    }
}