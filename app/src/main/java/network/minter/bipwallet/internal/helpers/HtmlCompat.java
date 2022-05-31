/*
 * Copyright (C) by MinterTeam. 2022
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

import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;

import org.xml.sax.XMLReader;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class HtmlCompat {
    private final static Html.TagHandler sSupportTagHandler;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            sSupportTagHandler = new SupportTagHandler();
        } else {
            sSupportTagHandler = null;
        }
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String htmlString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(htmlString, 0);
        }

        return Html.fromHtml(htmlString, null, sSupportTagHandler);
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String htmlString, Object... args) {
        final String out = String.format(htmlString, args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(out, 0);
        }

        return Html.fromHtml(out, null, sSupportTagHandler);
    }

    static final class SupportTagHandler implements Html.TagHandler {
        boolean first = true;
        boolean firstNextLine = true;
        String parent = null;
        int index = 1;

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equals("ul")) {
                parent = "ul";
            } else if (tag.equals("ol")) {
                parent = "ol";
            }

            if (tag.equals("li")) {
                if (parent.equals("ul")) {
                    if (first) {
                        output.append(firstNextLine ? "\t•  " : "\n\n\t•  ");
                        firstNextLine = false;
                        first = false;
                    } else {
                        first = true;
                    }
                } else {
                    if (first) {
                        output.append(firstNextLine ? "\t" + index + ".  " : "\n\n\t" + index + ".  ");
                        firstNextLine = false;
                        first = false;
                        index++;
                    } else {
                        first = true;
                    }
                }
            }
        }
    }
}
