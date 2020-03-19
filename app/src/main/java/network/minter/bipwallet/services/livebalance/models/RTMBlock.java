package network.minter.bipwallet.services.livebalance.models;

import org.joda.time.DateTime;
import org.parceler.Parcel;

import java.math.BigDecimal;

@Parcel
public class RTMBlock {
    public Long height;
    public Long size;
    public Long txCount;
    public BigDecimal blockTime;
    public DateTime timestamp;
    public BigDecimal reward;
    public String hash;
}
