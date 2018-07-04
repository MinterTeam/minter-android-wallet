package network.minter.bipwallet.internal.helpers;

import java.util.List;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.Wallet;


public class StringsHelper {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String boolToString(Boolean b) {
        if (b == null || !b) {
            return Wallet.app().res().getString(R.string.no);
        }

        return Wallet.app().res().getString(R.string.yes);
    }

    public static String glue(List<String> input, String glue) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.size(); i++) {
            sb.append(input.get(i));
            if (i + 1 < input.size()) {
                sb.append(glue);
            }
        }

        return sb.toString();
    }
}
