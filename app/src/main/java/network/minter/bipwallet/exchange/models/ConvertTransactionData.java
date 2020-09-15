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

package network.minter.bipwallet.exchange.models;

import java.math.BigDecimal;
import java.math.BigInteger;

import network.minter.blockchain.models.operational.OperationInvalidDataException;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.explorer.models.CoinItemBase;

import static com.google.common.base.MoreObjects.firstNonNull;
import static network.minter.core.MinterSDK.DEFAULT_COIN_ID;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class ConvertTransactionData {
    private final Type mType;
    private final BigInteger mGasCoin;
    private final CoinItemBase mSellCoin;
    private final CoinItemBase mBuyCoin;
    private final BigDecimal mAmount;
    private final BigDecimal mEstimate;

    public enum Type {
        Sell,
        SellAll,
        Buy,
    }

    public ConvertTransactionData(Type type, BigInteger gasCoin, CoinItemBase sellCoin, CoinItemBase buyCoin, BigDecimal amount, BigDecimal estimate) {
        mType = type;
        mGasCoin = gasCoin;
        mSellCoin = sellCoin;
        mBuyCoin = buyCoin;
        mAmount = amount;
        mEstimate = estimate;
    }

    public Transaction build(BigInteger nonce, BigInteger gasPrice, BigDecimal balance) throws OperationInvalidDataException {
        final Transaction tx;

        // if sellAll AND selling coin is a custom coin AND calculator says enough MNT to use as gas coin
        boolean customToCustom = mType == Type.SellAll && !mSellCoin.id.equals(DEFAULT_COIN_ID) && mGasCoin.equals(DEFAULT_COIN_ID);

        if (mType == Type.Sell || customToCustom) {
            // SELL
            tx = new Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)
                    .sellCoin()
                    .setCoinIdToSell(mSellCoin.id)
                    .setValueToSell(mAmount)
                    .setCoinIdToBuy(mBuyCoin.id)
                    .setMinValueToBuy("0")
                    .build();
        } else if (mType == Type.Buy) {
            // BUY
            tx = new Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)
                    .buyCoin()
                    .setCoinIdToSell(mSellCoin.id)
                    .setValueToBuy(mAmount)
                    .setCoinIdToBuy(mBuyCoin.id)
                    .setMaxValueToSell(getEstimate().multiply(new BigDecimal("1.1")))
                    .build();
        } else {
            // this case used ONLY: when not enough mnt to pay fee with mnt
            // SELL ALL
            tx = new Transaction.Builder(nonce)
                    .setGasCoinId(mGasCoin)
                    .setGasPrice(gasPrice)
                    .sellAllCoins()
                    .setCoinIdToSell(mSellCoin.id)
                    .setCoinIdToBuy(mBuyCoin.id)
                    .setMinValueToBuy("0")
                    .build();
        }

        return tx;
    }

    private BigDecimal getEstimate() {
        return firstNonNull(mEstimate, new BigDecimal(0));
    }
}
