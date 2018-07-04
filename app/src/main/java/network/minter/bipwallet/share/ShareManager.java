/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.share;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;

import network.minter.bipwallet.internal.helpers.ViewHelper;
import okhttp3.HttpUrl;
import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;


/**
 * BipWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ShareManager {
    private final static ShareManager INSTANCE = new ShareManager();

    public enum Target {
        VKONTAKTE("vkontakte"),
        FACEBOOK("facebook"),
        TWITTER("twitter"),
        WHATSAPP("whatsapp"),
        TELEGRAM("telegram"),
        OTHER("other");

        final String tag;

        Target(final String tagName) {
            tag = tagName;
        }

        public static Target findByTag(final View view) {
            if (view.getTag() instanceof Target) {
                return findByTag(((Target) view.getTag()).tag);
            }

            return findByTag((String) view.getTag());
        }

        public static Target findByTag(final String tagName) {
            for (Target item : Target.values()) {
                if (item.tag.equals(tagName)) {
                    return item;
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    public static ShareManager getInstance() {
        return INSTANCE;
    }

    /**
     * @param context
     * @param views   Группа должна быть исключительно одноуровненой, то есть дочерние элементы должны быть вьюхами с тэгами
     * @see Target
     */

    public void bindAction(@NonNull final Context context, ViewGroup views, SharingText shareText) {
        bindAction(context, ViewHelper.getChildren(views), shareText, null);
    }


    public void bindAction(@NonNull final Context context, final ViewGroup buttons, SharingText shareText,
                           @Nullable ShareClickListener shareClickListener) {
        bindAction(context, ViewHelper.getChildren(buttons), shareText, shareClickListener);
    }


    public void bindAction(@NonNull final Context context, final View[] buttons,
                           SharingText sharingText,
                           @Nullable ShareClickListener shareClickListener) {
        if (buttons == null) return;

        checkNotNull(context, "Context required");

        for (View button : buttons) {
            final Target target = Target.findByTag(button);
            if (target == null) {
                Timber.w("Share button with tag %s is not configured!", button.getTag());
                continue;
            }

            final Intent intent = createTargetIntent(target, sharingText);

            button.setOnClickListener(v -> {
                if (shareClickListener != null) shareClickListener.onShareClick(v);
                context.startActivity(intent);
            });
        }
    }

    @NonNull
    public Intent createTargetIntent(@NonNull Target target, @NonNull SharingText sharingText) {
        checkNotNull(target, "Target can't be null");
        checkNotNull(sharingText, "Sharing text can't be null");
        final Intent intent;
        switch (target) {
            case VKONTAKTE:
                intent = createVkontakteIntent(sharingText);
                break;
            case FACEBOOK:
                intent = createFacebookIntent(sharingText);
                break;
            case TWITTER:
                intent = createTwitterIntent(sharingText);
                break;
            case WHATSAPP:
                intent = createWhatsappIntent(sharingText);
                break;
            case TELEGRAM:
                intent = createTelegramIntent(sharingText);
                break;
            case OTHER:
                intent = createCommonIntent(sharingText);
                break;

            default:
                intent = createCommonIntent(sharingText);

        }
        return intent;
    }

    @NonNull
    public Intent createCommonIntent(@NonNull SharingText sharingText,
                                     @Nullable String chooserText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sharingText.toString());
        return Intent.createChooser(intent, firstNonNull(chooserText, "Рассказать друзьям"));
    }

    @NonNull
    public Intent createCommonIntent(@NonNull SharingText sharingText) {
        return createCommonIntent(sharingText, null);
    }

    @NonNull
    public Intent createWhatsappIntent(@NonNull final SharingText sharingText,
                                       @Nullable String choserText) {
        Intent waIntent = new Intent(Intent.ACTION_SEND);
        waIntent.setType("text/plain");
        waIntent.setPackage("com.whatsapp");
        waIntent.putExtra(Intent.EXTRA_TEXT, sharingText.toString());
        return Intent.createChooser(waIntent, firstNonNull(choserText, "Рассказать в Whatsapp"));
    }

    @NonNull
    public Intent createWhatsappIntent(@NonNull final SharingText sharingText) {
        return createWhatsappIntent(sharingText, null);
    }

    @NonNull
    public Intent createTelegramIntent(@NonNull final SharingText sharingText) {
        final HttpUrl.Builder builder = HttpUrl.parse("https://telegram.me/share/url").newBuilder();
        builder.addQueryParameter("url", sharingText.getUrl());
        builder.addQueryParameter("text",
                sharingText.getTitle() + "\n" + sharingText.getDescription());

        return intentFromUrl(builder.toString());
    }

    @NonNull
    public Intent createTwitterIntent(@NonNull final SharingText sharingText) {
        final HttpUrl.Builder builder = HttpUrl.parse(
                "https://twitter.com/intent/tweet").newBuilder();
        builder.addQueryParameter("url", sharingText.getUrl());
        builder.addQueryParameter("text",
                sharingText.getTitle() + "\n" + sharingText.getDescription());

        return intentFromUrl(builder.toString());
    }

    @NonNull
    public Intent createFacebookIntent(@NonNull final SharingText sharingText) {
        final HttpUrl.Builder builder = HttpUrl.parse(
                "https://www.facebook.com/sharer.php").newBuilder();
        builder.addQueryParameter("u", sharingText.getUrl());
        builder.addQueryParameter("t",
                sharingText.getTitle() + "\n" + sharingText.getDescription());

        return intentFromUrl(builder.toString());
    }

    @NonNull
    public Intent createVkontakteIntent(@NonNull final SharingText sharingText) {
        final HttpUrl.Builder builder = HttpUrl.parse("http://vk.com/share.php").newBuilder();
        builder.addQueryParameter("url", firstNonNull(sharingText.url, ""));
        builder.addQueryParameter("title", firstNonNull(sharingText.title, ""));
        builder.addQueryParameter("description", firstNonNull(sharingText.descriptionText, ""));
        builder.addQueryParameter("image", firstNonNull(sharingText.imageUrl, ""));

        return intentFromUrl(builder.toString());
    }

    private Intent intentFromUrl(@NonNull final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(url));

        return intent;
    }

    public interface ShareClickListener {
        void onShareClick(View view);
    }

    public final static class IntentBuilder {
        private final Intent intent = new Intent(Intent.ACTION_SEND);
        private String mChooserTitle;

        public IntentBuilder() {
            intent.setType("text/plain");
        }

        public IntentBuilder setChooserTitle(String title) {
            mChooserTitle = title;
            return this;
        }

        public IntentBuilder setContentType(String contentType) {
            intent.setType(contentType);
            return this;
        }

        public IntentBuilder setText(Spanned text) {
            intent.putExtra(Intent.EXTRA_TEXT, text);
            return this;
        }

        public IntentBuilder setText(String text) {
            intent.putExtra(Intent.EXTRA_TEXT, text);
            return this;
        }

        public IntentBuilder setHtml(String html) {
            intent.putExtra(Intent.EXTRA_HTML_TEXT, html);
            return this;
        }

        public IntentBuilder setStream(Uri uri) {
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            return this;
        }

        public IntentBuilder setSubject(String subject) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }

        public IntentBuilder setTargetPackage(String targetPackage) {
            intent.setPackage(targetPackage);
            return this;
        }

        public IntentBuilder setImageRemote(String url) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
            return this;
        }

        public Intent build() {
            return Intent.createChooser(intent, firstNonNull(mChooserTitle, "Пригласить друзей"));
        }


    }
}
