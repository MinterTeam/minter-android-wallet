/*
 * Copyright (C) by MinterTeam. 2021
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

import android.content.Intent;

import com.airbnb.deeplinkdispatch.DeepLink;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction;
import network.minter.blockchain.models.operational.ExternalTransaction;
import okhttp3.HttpUrl;

import static network.minter.bipwallet.internal.helpers.ViewExtensions.tr;

public final class DeepLinkHelper {

    public static boolean isDeeplinkUrl(String result) {
        return result.startsWith("minter://") || result.startsWith("http");
    }

    public static Intent rawTxToIntent(String result) throws InvalidExternalTransaction {
        if (!isDeeplinkUrl(result)) {
            result = "https://" + result;
        }

        Intent intent = new Intent();
        intent.putExtra(DeepLink.IS_DEEP_LINK, true);

        if (result.startsWith("minter://")) {
            // workaround, HttpUrl does not support custom protocols, so make it happy
            result = result.replace("minter://", "https://bip.to/");
        }

        if (result.startsWith("http")) {
            HttpUrl url = HttpUrl.parse(result);
            if (url == null) {
                throw new InvalidExternalTransaction(tr(R.string.deeplink_err_unable_to_parse_transaction), InvalidExternalTransaction.CODE_INVALID_LINK);
            }

            // old-style deeplinks
            if (url.pathSegments().size() == 1 && url.pathSegments().get(0).equals("tx")) {
                boolean hasParam = false;

                for (int i = 0; i < url.querySize(); i++) {
                    final String param = url.queryParameterName(i);
                    if (param.equals("d")) {
                        hasParam = true;
                    }
                    intent.putExtra(url.queryParameterName(i), url.queryParameterValue(i));
                }

                if (!hasParam) {
                    throw new InvalidExternalTransaction("No ?d parameter passed to deeplink", InvalidExternalTransaction.CODE_INVALID_DEEPLINK);
                }
            } else if (url.pathSegments().size() == 2 && url.pathSegments().get(0).equals("tx")) {
                for (int i = 0; i < url.querySize(); i++) {
                    intent.putExtra(url.queryParameterName(i), url.queryParameterValue(i));
                }
                intent.putExtra("data", url.pathSegments().get(1));
            } else {
                throw new InvalidExternalTransaction(tr(R.string.deeplink_err_unknown_deeplink_format), InvalidExternalTransaction.CODE_INVALID_DEEPLINK);
            }
        }

        return intent;
    }

    public static ExternalTransaction parseRawTransaction(String rawTx) throws InvalidExternalTransaction {
        if (rawTx == null) {
            throw new InvalidExternalTransaction(tr(R.string.deeplink_err_no_tx_data_passed), InvalidExternalTransaction.CODE_INVALID_LINK);
        }

        ExternalTransaction tx;
        tx = ExternalTransaction.fromEncoded(rawTx);

        return tx;
    }
}
