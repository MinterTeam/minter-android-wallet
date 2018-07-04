package network.minter.bipwallet.internal.helpers;

import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;

import org.xml.sax.XMLReader;

/**
 * Atlas_Android. 2017
 *
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
