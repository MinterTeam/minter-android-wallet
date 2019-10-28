package network.minter.bipwallet.delegation.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import network.minter.bipwallet.internal.views.widgets.RemoteImageContainer;
import network.minter.core.crypto.MinterPublicKey;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 06-Jun-19
 */
public class DelegationItem implements RemoteImageContainer {

    public MinterPublicKey pubKey;
    public String name;
    public String icon;
    public String description;
    public ArrayList<DelegatedCoin> coins = new ArrayList<>();
    public BigDecimal delegatedBips = BigDecimal.ZERO;

    @Override
    public String getImageUrl() {
        return icon;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s: %s", firstNonNull(name, pubKey.toShortString()), bdHuman(delegatedBips));
    }

    public static class DelegatedCoin{
        public String coin;
        public BigDecimal amount;

        DelegatedCoin(String coin, BigDecimal amount) {
            this.coin = coin;
            this.amount = amount;
        }
    }
}
