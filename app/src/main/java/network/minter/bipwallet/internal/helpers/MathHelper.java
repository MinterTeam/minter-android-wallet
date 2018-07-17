/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;

/**
 * Dogsy. 2017
 *
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
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigDecimal(to)) > 0;
    }

    public static boolean bdGTE(BigDecimal from, double to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigDecimal(to)) >= 0;
    }

    public static boolean bdLT(BigDecimal from, double to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigDecimal(to)) < 0;
    }

    public static boolean bdLTE(BigDecimal from, double to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(new BigDecimal(to)) <= 0;
    }

    public static boolean bdLTE(BigDecimal from, BigDecimal to) {
        if (from == null) {
            return false;
        }
        return from.compareTo(to) <= 0;
    }

    public static String bdHuman(BigDecimal source) {
        return firstNonNull(source, new BigDecimal(0)).stripTrailingZeros().toPlainString();
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
}
