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

package network.minter.bipwallet.internal.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import androidx.annotation.NonNull;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public final class MathHelper {

    public static float clamp(float input, float min, float max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    public static int clamp(int input, int min) {
        if (input < min) {
            return min;
        }

        return input;
    }

    public static long clamp(long input, long min) {
        if (input < min) {
            return min;
        }

        return input;
    }

    public static int clamp(int input, int min, int max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    public static long clamp(long input, long min, long max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    // BigDecimal
    public static boolean bdGT(BigDecimal from, double to) {
        return bdGT(from, new BigDecimal(to));
    }

    public static boolean bdGT(BigDecimal from, BigDecimal to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(to) > 0;
    }

    public static boolean bdGTE(BigDecimal from, double to) {
        return bdGTE(from, new BigDecimal(to));
    }

    public static boolean bdGTE(BigDecimal from, BigDecimal to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(to) >= 0;
    }

    public static boolean bdLT(BigDecimal from, double to) {
        return bdLT(from, new BigDecimal(to));
    }

    public static boolean bdLT(BigDecimal from, BigDecimal to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(to) < 0;
    }

    public static boolean bdLTE(BigDecimal from, double to) {
        return bdLTE(from, new BigDecimal(to));
    }

    public static boolean bdLTE(BigDecimal from, BigDecimal to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(to) <= 0;
    }

    public static String bdHuman(double source) {
        return bdHuman(new BigDecimal(source));
    }

    public static String bdHuman(BigDecimal source) {
        BigDecimal num = firstNonNull(source, new BigDecimal(0));

        if (bdNull(num)) {
            return formatDecimalCurrency(num, 4, true);
        }

        if (bdLT(num, new BigDecimal(1))) {
            if (num.stripTrailingZeros().scale() <= 4) {
                return formatDecimalCurrency(num.setScale(4, BigDecimal.ROUND_DOWN), 4, true);
            }

            return formatDecimalCurrency(num.setScale(8, BigDecimal.ROUND_UP), 8, false);
        }

        final BigDecimal out = num.setScale(4, RoundingMode.DOWN);
        return formatDecimalCurrency(out, 4, true);
    }

    public static boolean bdEQ(double a, BigDecimal b) {
        return bdEQ(new BigDecimal(a), b);
    }

    public static boolean bdEQ(double a, double b) {
        return bdEQ(new BigDecimal(a), new BigDecimal(b));
    }

    public static boolean bdEQ(BigDecimal a, double b) {
        return bdEQ(a, new BigDecimal(b));
    }

    public static boolean bdEQ(BigDecimal a, BigDecimal b) {
        if (a == null) return false;
        return a.compareTo(b) == 0;
    }

    public static boolean bdNull(BigDecimal source) {
        BigDecimal test;
        if (source.scale() > 18) {
            test = source.setScale(18, BigDecimal.ROUND_UP);
        } else {
            test = source;
        }
        return test.setScale(18).equals(new BigDecimal("0e-18"));
    }

    // BigInteger
    public static boolean biGT(BigInteger from, long to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigInteger(String.valueOf(to))) > 0;
    }

    public static boolean biGTE(BigInteger from, long to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigInteger(String.valueOf(to))) >= 0;
    }

    public static boolean biLT(BigInteger from, long to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigInteger(String.valueOf(to))) < 0;
    }

    public static boolean biLTE(BigInteger from, long to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigInteger(String.valueOf(to))) <= 0;
    }

    @NonNull
    public static BigDecimal bigDecimalFromString(CharSequence text) {
        if (text == null) {
            return BigDecimal.ZERO;
        }
        String amountText = text
                .toString()
                .replaceAll("\\s+", "")
                .replaceAll("[,]+", "")
                .replace(",", ".");


        if (amountText.isEmpty()) {
            amountText = "0";
        }
        if (amountText.equals(".")) {
            amountText = "0";
        } else if (amountText.substring(0, 1).equals(".")) {
            amountText = "0" + amountText;
        }
        if (amountText.substring(amountText.length() - 1).equals(".")) {
            amountText = amountText + "0";
        }

        BigDecimal out;
        try {
            out = new BigDecimal(amountText);
        } catch (NumberFormatException e) {
            out = BigDecimal.ZERO;
        }

        return out;
    }

    private static String formatDecimalCurrency(BigDecimal in, int fractions, boolean exactFractions) {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        if (exactFractions) {
            fmt.setMinimumFractionDigits(fractions);
            fmt.setMaximumFractionDigits(fractions);
        } else {
            fmt.setMaximumFractionDigits(fractions);
            fmt.setMinimumFractionDigits(0);
        }

        fmt.setDecimalFormatSymbols(symbols);

        return fmt.format(in);
    }
}
