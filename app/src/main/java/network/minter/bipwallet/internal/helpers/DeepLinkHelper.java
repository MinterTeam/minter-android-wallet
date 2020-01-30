package network.minter.bipwallet.internal.helpers;

import android.content.Intent;

import com.airbnb.deeplinkdispatch.DeepLink;

import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction;
import network.minter.blockchain.models.operational.CheckTransaction;
import network.minter.blockchain.models.operational.ExternalTransaction;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.blockchain.models.operational.TxRedeemCheck;
import network.minter.core.crypto.MinterAddress;
import okhttp3.HttpUrl;

public final class DeepLinkHelper {

    public static boolean isDeeplinkUrl(String result) {
        return result.startsWith("minter://") || result.startsWith("http");
    }

    public static Intent rawTxToIntent(String result) throws InvalidExternalTransaction {
        if (!isDeeplinkUrl(result)) {
            throw new InvalidExternalTransaction("Unable to parse transaction url", InvalidExternalTransaction.CODE_INVALID_LINK);
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
                throw new InvalidExternalTransaction("Unable to parse transaction url", InvalidExternalTransaction.CODE_INVALID_LINK);
            }

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
        }

        return intent;
    }

    public static ExternalTransaction parseTransaction(MinterAddress from, String result) throws InvalidExternalTransaction {
        if (result.startsWith("minter://")) {
            // workaround, HttpUrl does not support custom protocols, so make it happy
            result = result.replace("minter://", "https://bip.to/");
        }

        if (result.startsWith("http")) {
            HttpUrl url = HttpUrl.parse(result);
            if (url == null) {
                throw new InvalidExternalTransaction("Unable to parse transaction url", InvalidExternalTransaction.CODE_INVALID_LINK);
            }
            String tx = url.queryParameter("d");
            if (tx == null) {
                throw new InvalidExternalTransaction("No transaction data in passed URL", InvalidExternalTransaction.CODE_INVALID_LINK);
            }
            String p = url.queryParameter("p");
            if (p == null) {
                return parseRawTransaction(tx);
            }

            ExternalTransaction ext = parseRawTransaction(tx);
            if (ext.getType() == OperationType.RedeemCheck) {
                ext.getData(TxRedeemCheck.class).setProof(CheckTransaction.makeProof(from, p));
            }

            return ext;
        } else {
            return parseRawTransaction(result);
        }
    }

    public static ExternalTransaction parseRawTransaction(String rawTx) throws InvalidExternalTransaction {
        if (rawTx == null) {
            throw new InvalidExternalTransaction("No transaction data passed", InvalidExternalTransaction.CODE_INVALID_LINK);
        }

        ExternalTransaction tx;
        tx = ExternalTransaction.fromEncoded(rawTx);

        return tx;
    }
}
