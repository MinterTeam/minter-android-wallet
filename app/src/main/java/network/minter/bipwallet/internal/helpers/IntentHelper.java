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

package network.minter.bipwallet.internal.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import network.minter.bipwallet.internal.common.Acceptor;


/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class IntentHelper {

    public static <T> T getParcelExtraOrError(Intent intent, @NonNull String name) {
        return getParcelExtraOrError(intent, name, String.format("Intent does not have extra: %s", name));
    }

    public static <T> T getParcelExtraOrError(Intent intent, @NonNull String name, String errorMessage) {
        T result = getParcelExtra(intent, name);
        if (result == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return result;
    }

    public static <T> T getParcelExtra(Intent intent, @NonNull String name) {
        return getParcelExtra(intent, name, null);
    }

    public static <T> T getParcelExtraOrError(Bundle bundle, @NonNull String name, String errorMessage) {
        T result = getParcelExtra(bundle, name, null);
        if (result == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return result;
    }

    public static <T> T getParcelExtra(Bundle bundle, @NonNull String name) {
        return getParcelExtra(bundle, name, null);
    }

    public static <T> T getParcelExtra(Bundle bundle, @NonNull String name, @Nullable Acceptor<T> callback) {
        if (bundle == null || !bundle.containsKey(name)) {
            return null;
        }

        final T data = Parcels.unwrap(bundle.getParcelable(name));
        if (callback != null) {
            callback.accept(data);
        }

        return data;
    }

    public static <T> T getParcelExtra(Intent intent, @NonNull String name, @Nullable Acceptor<T> callback) {
        if (intent == null || !intent.hasExtra(name)) {
            return null;
        }

        final T data = Parcels.unwrap(intent.getParcelableExtra(name));
        if (callback != null) {
            callback.accept(data);
        }

        return data;

    }

    public static Intent newUrl(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public static Intent newDial(String phoneNum) {
        if (phoneNum.length() < 4) {
            throw new IllegalArgumentException("Phone number can't be null");
        }

        if (!phoneNum.substring(0, 4).equals("tel:")) {
            phoneNum = "tel:" + phoneNum;
        }

        return new Intent(Intent.ACTION_DIAL, Uri.parse(phoneNum));
    }
}
