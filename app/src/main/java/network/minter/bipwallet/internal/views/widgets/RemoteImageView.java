package network.minter.bipwallet.internal.views.widgets;

import android.net.Uri;
import android.support.annotation.DimenRes;

import network.minter.bipwallet.internal.common.annotations.Dp;


/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface RemoteImageView {
    void setImageUrl(String url, float size);
    void setImageUrl(Uri uri, @DimenRes int resId);
    void setImageUrl(String url, @DimenRes int resId);
    void setImageUrl(Uri uri, float size);
    void setImageUrl(Uri uri);
    void setImageUrl(String url);
    void setImageUrl(RemoteImageContainer imageUrlContainer);
    void setImageUrl(RemoteImageContainer imageUrlContainer, @Dp float size);
    void setImageUrl(RemoteImageContainer imageUrlContainer, @DimenRes int resId);
}
