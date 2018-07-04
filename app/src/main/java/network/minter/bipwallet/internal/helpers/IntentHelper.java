package network.minter.bipwallet.internal.helpers;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.parceler.Parcels;

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
