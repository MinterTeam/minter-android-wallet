package network.minter.bipwallet.auth.contract;

import android.net.Uri;
import android.view.View;

import java.util.List;
import java.util.Map;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.mvp.ErrorViewWithRetry;
import network.minter.bipwallet.internal.mvp.ProgressView;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public interface RegisterView extends MvpView, ProgressView, ErrorViewWithRetry {
    void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
    void setOnSubmit(View.OnClickListener listener);
    void setOnFormValidateListener(InputGroup.OnFormValidateListener listener);
    void setEnableSubmit(boolean enable);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHome();
    void validate(boolean withError);
    void setInputError(String fieldName, String message);
    void setResultError(CharSequence error);
    void clearErrors();
    void setInputErrors(Map<String, List<String>> data);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startConfirmation(Uri endpoint);
    void hideKeyboard();
}
