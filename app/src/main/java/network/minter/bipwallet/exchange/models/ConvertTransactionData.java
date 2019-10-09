/*
 * Copyright (C) by MinterTeam. 2019
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
import network.minter.core.MinterSDK;

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
    private final BigInteger mGasPrice;

    public enum Type {
        Sell,
        SellAll,
        Buy,
    }

    public ConvertTransactionData(Type type, String gasCoin, String sellCoin, String buyCoin, BigDecimal amount, BigDecimal estimate, BigInteger gasPrice) {
        mType = type;
        mGasCoin = gasCoin;
        mSellCoin = sellCoin;
        mBuyCoin = buyCoin;
        mAmount = amount;
        mEstimate = estimate;
        mGasPrice = gasPrice;
    }

    public Transaction build(BigInteger nonce, BigInteger gasPrice, BigDecimal balance) throws OperationInvalidDataException {
        final Transaction tx;

        // if sellAll AND selling coin is a custom coin AND calculator says enough MNT to use as gas coin
        boolean customToCustom = mType == Type.SellAll && !mSellCoin.equals(MinterSDK.DEFAULT_COIN) && mGasCoin.equals(MinterSDK.DEFAULT_COIN);

        if (mType == Type.Sell || customToCustom) {
            // SELL
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .setGasPrice(gasPrice)
                    .sellCoin()
                    .setCoinToSell(mSellCoin)
                    .setValueToSell(mAmount)
                    .setCoinToBuy(mBuyCoin)
//                    .setMinValueToBuy(getEstimate().multiply(new BigDecimal(0.9d)))
                    .setMinValueToBuy("0")
                    .build();
        } else if (mType == Type.Buy) {
            // BUY
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .setGasPrice(gasPrice)
                    .buyCoin()
                    .setCoinToSell(mSellCoin)
                    .setValueToBuy(mAmount)
                    .setCoinToBuy(mBuyCoin)
                    .setMaxValueToSell(getEstimate().multiply(new BigDecimal(1.1d)))
//                    .setMaxValueToSell(balance)
                    .build();
        } else {
            // this case used ONLY: when not enough mnt to pay fee with mnt
            // SELL ALL
            tx = new Transaction.Builder(nonce)
                    .setGasCoin(mGasCoin)
                    .setGasPrice(gasPrice)
                    .sellAllCoins()
                    .setCoinToSell(mSellCoin)
                    .setCoinToBuy(mBuyCoin)
//                    .setMinValueToBuy(getEstimate().multiply(new BigDecimal(0.9d)))
                    .setMinValueToBuy("0")
                    .build();
        }

        return tx;
    }

//    public Transaction build(BigInteger nonce) throws OperationInvalidDataException {
//        return build(nonce, mGasPrice);
//    }

    private BigDecimal getEstimate() {
        return firstNonNull(mEstimate, new BigDecimal(0));
    }
}
