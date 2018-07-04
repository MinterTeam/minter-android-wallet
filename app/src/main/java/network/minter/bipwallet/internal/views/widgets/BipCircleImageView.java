package network.minter.bipwallet.internal.views.widgets;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import de.hdodenhof.circleimageview.CircleImageView;
import network.minter.bipwallet.internal.common.annotations.Dp;


/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class BipCircleImageView extends CircleImageView implements RemoteImageView {
    private final RemoteImageViewDelegate mRemoteDelegate;

    public BipCircleImageView(Context context) {
        super(context);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    public BipCircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    public BipCircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mRemoteDelegate = new RemoteImageViewDelegate(this);
    }

    @Override
    public void setImageUrl(String url, @Dp float size) {
        mRemoteDelegate.setImageUrl(url, size);
    }

    @Override
    public void setImageUrl(Uri uri, int resId) {
        mRemoteDelegate.setImageUrl(uri, resId);
    }

    @Override
    public void setImageUrl(String url, int resId) {
        mRemoteDelegate.setImageUrl(url, resId);
    }

    @Override
    public void setImageUrl(Uri uri, float size) {
        mRemoteDelegate.setImageUrl(uri, size);
    }

    @Override
    public void setImageUrl(Uri uri) {
        mRemoteDelegate.setImageUrl(uri);
    }

    @Override
    public void setImageUrl(String url) {
        mRemoteDelegate.setImageUrl(url);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer) {
        mRemoteDelegate.setImageUrl(imageUrlContainer);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, float size) {
        mRemoteDelegate.setImageUrl(imageUrlContainer, size);
    }

    @Override
    public void setImageUrl(RemoteImageContainer imageUrlContainer, int resId) {
        mRemoteDelegate.setImageUrl(imageUrlContainer, resId);
    }
}
