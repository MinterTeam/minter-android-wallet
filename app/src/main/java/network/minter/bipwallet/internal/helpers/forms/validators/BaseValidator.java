package network.minter.bipwallet.internal.helpers.forms.validators;

import com.annimon.stream.Stream;

import java.util.ArrayList;

/**
 * Atlas_Android. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseValidator {
    protected CharSequence mErrorMessage;
    private boolean mRequired = true;
    private ArrayList<ValidationErrorListener> mErrorListeners = new ArrayList<>(0);


    public BaseValidator(boolean required) {
        mRequired = required;
        mErrorMessage = "Incorrect data";
    }

    public BaseValidator() {
        mErrorMessage = "Incorrect data";
    }

    public BaseValidator(CharSequence errorMessage, boolean required) {
        mErrorMessage = errorMessage;
        mRequired = required;
    }

    public BaseValidator(CharSequence errorMessage) {
        mErrorMessage = errorMessage;
    }

    public boolean validate(CharSequence value) {
        final boolean valid = getCondition(value);
        if (!valid) {
            Stream.of(mErrorListeners)
                    .forEach(item -> item.onError(BaseValidator.this));
        }

        return valid;
    }

    public CharSequence getErrorMessage() {
        return mErrorMessage;
    }

    public void setErrorMessage(final CharSequence errorMessage) {
        mErrorMessage = errorMessage;
    }

    public BaseValidator addErrorListener(ValidationErrorListener listener) {
        mErrorListeners.add(listener);
        return this;
    }

    public boolean isRequired() {
        return mRequired;
    }

    protected abstract boolean getCondition(CharSequence value);

    public interface ValidationErrorListener {
        void onError(BaseValidator validator);
    }


}
