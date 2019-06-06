package network.minter.bipwallet.delegation.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;

import network.minter.core.crypto.MinterPublicKey;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationItem {

    public MinterPublicKey pubKey;
    public ArrayList<DelegatedCoin> coins = new ArrayList<>();

    public static class DelegatedCoin{
        public String coin;
        public BigDecimal amount;

        DelegatedCoin(String coin, BigDecimal amount) {
            this.coin = coin;
            this.amount = amount;
        }
    }
}
