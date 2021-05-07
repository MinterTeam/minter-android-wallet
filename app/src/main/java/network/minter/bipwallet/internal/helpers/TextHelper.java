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

package network.minter.bipwallet.internal.helpers;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.List;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;

import static network.minter.bipwallet.internal.helpers.ViewExtensions.tr;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public final class TextHelper {

    public static String booleanToWord(boolean input) {
        return input ? tr(R.string.yes) : tr(R.string.no);
    }

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean textContains(String source, String comparable) {
        if (source == null || comparable == null) {
            return false;
        }
        return source.toLowerCase(Wallet.LC_EN).contains(comparable.toLowerCase(Wallet.LC_EN));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean textContains(CharSequence source, CharSequence comparable) {
        if (source == null || comparable == null) {
            return false;
        }

        return textContains(source.toString(), comparable.toString());
    }

    public static String firstUppercase(String input) {
        if (input == null) {
            return null;
        }

        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String glue(List<String> input, String glue) {
        if (input.size() < 1) {
            return null;
        } else if (input.size() == 1) {
            return input.get(0);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.size(); i++) {
            sb.append(input.get(i));

            if (i + 1 < input.size()) {
                sb.append(glue);
            }
        }

        return sb.toString();
    }

    public static String humanReadableBytes(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Wallet.LC_EN, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
