package network.minter.bipwallet.internal.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import network.minter.bipwallet.BuildConfig;


/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class UrlHelper {

//    public static String createFrontUrl(String path) {
//        return String.format("%s%s", BuildConfig.BASE_FRONT_URL, path);
//    }

    public static String encodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String encodeUTF8(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    encodeUTF8(entry.getKey().toString()),
                    encodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }
}
