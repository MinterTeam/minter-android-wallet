package network.minter.bipwallet.internal.helpers;

import network.minter.bipwallet.internal.exceptions.InvalidExternalTransaction;
import network.minter.blockchain.models.operational.ExternalTransaction;
import okhttp3.HttpUrl;

public final class DeepLinkHelper {

    public static ExternalTransaction parseTransaction(String result) throws InvalidExternalTransaction {
        if (result.startsWith("minter://")) {
            String tx = result.replace("minter://tx?d=", "");
            return parseRawTransaction(tx);
        } else if (result.startsWith("http")) {
            HttpUrl url = HttpUrl.parse(result);
            if (url == null) {
                throw new InvalidExternalTransaction("Unable to parse transaction url", InvalidExternalTransaction.CODE_INVALID_LINK);
            }
            String tx = url.queryParameter("d");
            if (tx == null) {
                throw new InvalidExternalTransaction("No transaction data in passed URL", InvalidExternalTransaction.CODE_INVALID_LINK);
            }
            return parseRawTransaction(tx);
        } else {
            return parseRawTransaction(result);
        }
    }

    private static ExternalTransaction parseRawTransaction(String rawTx) throws InvalidExternalTransaction {
        ExternalTransaction tx;
        try {
            tx = ExternalTransaction.fromEncoded(rawTx);
        } catch (Throwable t) {
            throw new InvalidExternalTransaction("Unable to parse transaction", InvalidExternalTransaction.CODE_INVALID_TX, t);
        }

        if (tx == null) {
            throw new InvalidExternalTransaction("Unable to parse transaction: unrecognized data encoded", InvalidExternalTransaction.CODE_INVALID_TX);
        }

        return tx;
    }
}
