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

package network.minter.bipwallet.internal.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.widget.Toast;

import timber.log.Timber;

public class ContextHelper {

    public static Activity parseActivity(Context context) {
        if (context instanceof Activity)
            return (Activity) context;
        else if (context instanceof ContextWrapper)
            return parseActivity(((ContextWrapper) context).getBaseContext());

        return null;
    }

    public static void copyToClipboard(Context ctx, Uri uri) {
        copyToClipboard(ctx, uri, "");
    }

    public static void copyToClipboard(Context ctx, Uri uri, CharSequence label) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Timber.e("Clipboard is null. Is this requires some access?");
            return;
        }

        ClipData data = ClipData.newUri(ctx.getContentResolver(), label, uri);
        clipboard.setPrimaryClip(data);
        Toast.makeText(ctx, "Copied", Toast.LENGTH_LONG).show();
    }

    public static void copyToClipboard(Context ctx, String text) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Timber.e("Clipboard is null. Is this requires some access?");
            return;
        }
        ClipData data = ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(data);
        Toast.makeText(ctx, "Copied", Toast.LENGTH_LONG).show();
    }
}
