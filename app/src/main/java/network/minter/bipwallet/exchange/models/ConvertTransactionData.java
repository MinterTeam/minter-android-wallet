/*
 * Copyright (C) by MinterTeam. 2018
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

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class ConvertTransactionData {
    private final Type mType;
    private final String mGasCoin;
    private final String mSellCoin;
    private final String mBuyCoin;
    private final BigDecimal mAmount;
    private final BigDecimal mEstimate;

    public enum Type {
        Sell,
        SellAll,
        Buy,
    }

    public ConvertTransactionData(Type type, String gasCoin, String sellCoin, String buyCoin, BigDecimal amount, BigDecimal estimate) {
        mType = type;
        mGasCoin = gasCoin;
        mSellCoin = sellCoin;
        mBuyCoin = buyCoin;
        mAmount = amount;
        mEstimate = estimate;
    }

    public Transaction build(BigInteger nonce) throws OperationInvalidDataException {
        final Transaction tx;

        if (mType == Type.Sell) {
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .sellCoin()
                    .setCoinToSell(mSellCoin)
                    .setValueToSell(mAmount)
                    .setCoinToBuy(mBuyCoin)
                    .setMinValueToBuy(getEstimate().multiply(new BigDecimal(0.9d)))
                    .build();
        } else if (mType == Type.Buy) {
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .buyCoin()
                    .setCoinToSell(mSellCoin)
                    .setValueToBuy(mAmount)
                    .setCoinToBuy(mBuyCoin)
                    .setMaxValueToSell(getEstimate().multiply(new BigDecimal(1.1d)))
                    .build();
        } else {
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .sellAllCoins()
                    .setCoinToSell(mSellCoin)
                    .setCoinToBuy(mBuyCoin)
                    .setMinValueToBuy(getEstimate().multiply(new BigDecimal(0.9d)))
                    .build();
        }

        return tx;
    }

    private BigDecimal getEstimate() {
        return firstNonNull(mEstimate, new BigDecimal(0));
    }
}
