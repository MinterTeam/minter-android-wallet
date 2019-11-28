package network.minter.bipwallet.tx.contract;

import java.math.BigDecimal;
import java.math.BigInteger;

import network.minter.blockchain.models.TransactionCommissionValue;
import network.minter.explorer.models.GasValue;
import network.minter.explorer.models.GateResult;
import network.minter.explorer.models.TxCount;

public class TxInitData {
    public BigInteger nonce;
    public BigInteger gas;
    public BigDecimal commission;
    public GateResult<?> errorResult;


    public TxInitData(GateResult<?>... values) {
        for (GateResult<?> item : values) {
            if (!item.isOk()) {
                errorResult = GateResult.copyError(item);
                return;
            }
        }

        setValues(values);
    }

    public TxInitData(BigInteger nonce, BigInteger gas) {
        this.nonce = nonce;
        this.gas = gas;
    }

    public TxInitData(GateResult<?> err) {
        errorResult = err;
    }

    public boolean isSuccess() {
        return errorResult == null || errorResult.isOk();
    }

    private void setValues(GateResult<?>... values) {
        for (GateResult<?> item : values) {
            setValue(item);
        }
    }

    private void setValue(GateResult<?> src) {
        if (src.result instanceof GasValue) {
            gas = ((GasValue) src.result).gas;
        } else if (src.result instanceof TransactionCommissionValue) {
            commission = ((TransactionCommissionValue) src.result).getValue();
        } else if (src.result instanceof TxCount) {
            nonce = ((TxCount) src.result).count;
        }
    }
}
