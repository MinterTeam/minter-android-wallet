package network.minter.bipwallet.exchange.contract;

import moxy.MvpView;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface ConvertCoinView extends MvpView, ErrorViewWithRetry {
    void setupTabs();
    void setCurrentPage(int page);
}
