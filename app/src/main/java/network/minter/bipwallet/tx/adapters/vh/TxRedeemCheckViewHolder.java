package network.minter.bipwallet.tx.adapters.vh;

import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.tx.adapters.TxItem;
import network.minter.explorer.models.HistoryTransaction;

import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 21-May-19
 */
public class TxRedeemCheckViewHolder extends ExpandableTxViewHolder {
    public @BindView(R.id.item_title_type)
    TextView itemTitleType;
    public @BindView(R.id.detail_from_value)
    TextView fromValue;
    public @BindView(R.id.detail_coin_value)
    TextView coinValue;
    public @BindView(R.id.detail_amount_value)
    TextView amountValue;

    public TxRedeemCheckViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void bind(TxItem item) {
        super.bind(item);
        avatar.setImageResource(R.drawable.img_avatar_redeem);
        final HistoryTransaction.TxRedeemCheckResult data = item.getTx().getData();
        fromValue.setText(data.getCheck().getSender().toString());
        title.setText(item.getTx().hash.toShortString());
        amount.setText(String.format("+ %s", bdHuman(data.getCheck().getValue())));
        amountValue.setText(bdHuman(data.getCheck().getValue()));
        subamount.setText(data.getCheck().getCoin());
        coinValue.setText(data.getCheck().getCoin());
    }
}
