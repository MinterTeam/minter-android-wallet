package network.minter.bipwallet.services.livebalance.models;

import org.joda.time.DateTime;
import org.parceler.Parcel;

import java.math.BigDecimal;

@Parcel
public class RTMBlock {
    /*
    {"height":23427,
    "size":791,
    "txCount":0,
    "blockTime":4.218547318,
    "timestamp":"2020-03-11T15:30:39Z",
    "reward":"333.000000000000000000",
    "hash":"Mh1fea7554f806bd7265fcfd21ed8bafa87df9c1d399008b8657ff9ca4c428cebb",
    "validators":[]}
     */

    public Long height;
    public Long size;
    public Long txCount;
    public BigDecimal blockTime;
    public DateTime timestamp;
    public BigDecimal reward;
    public String hash;
}
