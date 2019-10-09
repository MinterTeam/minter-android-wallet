package network.minter.bipwallet.internal.exceptions;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

public class InvalidExternalTransaction extends Exception {

    public final static int CODE_INVALID_DEEPLINK = 0x01;
    public final static int CODE_INVALID_LINK = 0x02;
    public final static int CODE_INVALID_TX = 0x03;

    private int mCode = CODE_INVALID_TX;

    public InvalidExternalTransaction(String message, @ExtTxCode int code) {
        super(message);
        mCode = code;
    }

    public InvalidExternalTransaction(String message, @ExtTxCode int code, Throwable cause) {
        super(message, cause);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }

    @Nullable
    @Override
    public String getMessage() {
        if (getCause() != null) {
            return String.format("%s: %s", super.getMessage(), getCause().getMessage());
        }

        return super.getMessage();
    }

    @IntDef({
            CODE_INVALID_DEEPLINK,
            CODE_INVALID_LINK,
            CODE_INVALID_TX
    })
    public @interface ExtTxCode {
    }
}
