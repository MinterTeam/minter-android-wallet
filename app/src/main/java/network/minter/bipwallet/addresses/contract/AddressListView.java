package network.minter.bipwallet.addresses.contract;

import androidx.recyclerview.widget.RecyclerView;
import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.addresses.models.AddressItem;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface AddressListView extends MvpView, ProgressView {
    void setAdapter(RecyclerView.Adapter<?> adapter);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startAddressItem(int requestCode, String name, AddressItem address);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startCreateAddress(int requestCode);
    void scrollToPosition(int position);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startTransactionsList();
}
