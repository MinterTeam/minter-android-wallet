package network.minter.bipwallet.auth.contract;

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
public interface SigninView extends MvpView, ProgressView, ErrorViewWithRetry {
    void setOnTextChangedListener(InputGroup.OnTextChangedListener listener);
    void setOnSubmit(View.OnClickListener listener);
    void setOnFormValidateListener(InputGroup.OnFormValidateListener listener);
    void setEnableSubmit(boolean enable);
    @StateStrategyType(OneExecutionStateStrategy.class)
    void startHome();
    void setResultError(CharSequence error);
    void setInputError(String fieldName, String message);
    void clearErrors();
    void setInputErrors(Map<String, List<String>> fieldsErrors);
    void hideKeyboard();
}
