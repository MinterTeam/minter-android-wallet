package network.minter.bipwallet.receiving.contract;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.core.crypto.MinterAddress;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface ReceiveTabView extends MvpView, ErrorViewWithRetry {
    void setQRCode(Bitmap bmp);
    void setAddress(MinterAddress address);
    void setOnActionShareQR(View.OnClickListener listener);
    void setOnActionQR(View.OnClickListener listener);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startQRPreview(View shared, String filePath);
    void showQRProgress();
    void hideQRProgress();
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startShare(Intent intent);
    void setOnClickAddress(View.OnClickListener listener);
}
