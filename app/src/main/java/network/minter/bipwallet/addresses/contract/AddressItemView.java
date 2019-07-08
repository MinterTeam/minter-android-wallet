package network.minter.bipwallet.addresses.contract;

import android.app.Dialog;
import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface AddressItemView extends MvpView {
    void setAddress(String addressName);
    void setSecuredBy(String securedByVal);
    void setOnClickDelete(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startRemoveDialog(CharSequence attention, CharSequence description, String yes, String no, Dialog.OnClickListener onYesListener);
    void finishWithResult(int resultCode);
    void showProgress(CharSequence text);
    void hideProgress();
    void setOnCopy(View.OnClickListener listener);
    void hideActions();
    void setDescription(CharSequence description);
    void setName(String name);
}
