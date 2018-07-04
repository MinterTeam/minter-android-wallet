package network.minter.bipwallet.internal.helpers;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.List;

import network.minter.bipwallet.internal.Wallet;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public final class TextHelper {

    public static String booleanToWord(boolean input) {
        return input ? "да" : "нет";
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
