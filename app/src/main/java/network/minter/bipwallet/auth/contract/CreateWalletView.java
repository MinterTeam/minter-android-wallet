package network.minter.bipwallet.auth.contract;

import android.view.View;
import android.widget.Switch;

import androidx.annotation.StringRes;
import moxy.MvpView;

public interface CreateWalletView extends MvpView {

    void setTitle(@StringRes int resId);
    void setDescription(@StringRes int resId);
    void setSeed(CharSequence seedPhrase);
    void setOnSeedClickListener(View.OnClickListener listener);
    void setOnSavedClickListener(Switch.OnCheckedChangeListener checkedChangeListener);
    void setOnSubmit(View.OnClickListener listener);
    void showCopiedAlert();
    void setSubmitEnabled(boolean enabled);
}
