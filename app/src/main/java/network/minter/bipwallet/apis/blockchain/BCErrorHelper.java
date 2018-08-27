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

package network.minter.bipwallet.apis.blockchain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import network.minter.blockchain.models.operational.Transaction;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BCErrorHelper {

    public static String normalizeBlockChainInsufficientFundsMessage(String input) {
        final Pattern pattern = Pattern.compile("(.*Wanted\\s+)(\\d+)(.*)");
        if (!input.matches(pattern.pattern())) {
            return input;
        }

        final Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            return input;
        }
        BigInteger amountBI = new BigInteger(matcher.group(2));
        BigDecimal amount = new BigDecimal(amountBI).setScale(18, BigDecimal.ROUND_DOWN).divide(Transaction.VALUE_MUL_DEC, BigDecimal.ROUND_DOWN);

        return input.replaceAll(pattern.pattern(), String.format("$1%s$3", bdHuman(amount)));

    }
}
