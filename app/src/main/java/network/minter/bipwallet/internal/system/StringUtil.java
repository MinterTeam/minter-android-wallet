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

package network.minter.bipwallet.internal.system;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class StringUtil {

    public static boolean safeCompare(View context, CharSequence a, @StringRes int b) {
        return safeCompare(a, context.getResources().getString(b));
    }

    public static boolean safeCompare(View context, @StringRes int a, CharSequence b) {
        return safeCompare(context.getResources().getString(a), b);
    }

    public static boolean safeCompare(@NonNull Context context, @StringRes int a, CharSequence b) {
        return safeCompare(context.getResources().getString(a), b);
    }

    public static boolean safeCompare(@Nullable CharSequence a, @Nullable CharSequence b) {
        if (a == null && b == null) {
            return true;
        } else if (a != null && b == null) {
            return false;
        } else if (a == null && b != null) {
            return false;
        } else if (a.length() != b.length()) {
            return false;
        }

        return a.toString().equals(b.toString());
    }
}
