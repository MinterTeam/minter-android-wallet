package network.minter.bipwallet.internal.helpers.forms;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.helpers.forms.validators.BaseValidator;

import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;


/**
 * Atlas_Android. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class FormField {
    private CharSequence mFieldName;
    private CharSequence mLabel;
    private CharSequence mValue;
    private CharSequence mNewValue;
    private Value mNewValueGetter;
    private boolean mIsRequired = false;
    private List<BaseValidator> mValidators = new ArrayList<>(0);
    private ArrayList<String> mErrors = new ArrayList<>();


    public FormField(CharSequence fieldName, CharSequence label, Value value) {
        mFieldName = fieldName;
        mLabel = label;
        mValue = value.get();
        mNewValueGetter = value;
    }

    public FormField(CharSequence fieldName, String label, String value) {
        this.mFieldName = fieldName;
        this.mLabel = label;
        this.mValue = value;
    }

    public boolean hasValidators() {
        return mValidators.size() > 0;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public CharSequence getFieldName() {
        return mFieldName;
    }

    public void setFieldName(CharSequence fieldName) {
        mFieldName = fieldName;
    }

    public CharSequence getValue() {
        if (mNewValueGetter != null) {
            return firstNonNull(mNewValueGetter.get(), mValue, "");
        }

        return firstNonNull(mNewValue, mValue, "");
    }

    public FormField setValue(Value value) {
        mNewValueGetter = value;
        return this;
    }

    public FormField setValue(CharSequence newValue) {
        mNewValue = newValue;
        return this;
    }

    public boolean isModified() {
        if (mNewValueGetter != null) {
            return mValue == null && mNewValueGetter.get() != null || mNewValueGetter.get() != null && !mNewValueGetter.get().equals(mValue);
        }

        return mValue == null && mNewValue != null
                || mNewValue != null && !mNewValue.equals(mValue);

    }

    public boolean isRequired() {
        return mIsRequired;
    }

    public void setIsRequired(boolean isRequired) {
        mIsRequired = isRequired;
    }

    public FormField addValidator(BaseValidator validator) {
        mValidators.add(validator);
        return this;
    }

    public void removeValidators() {
        mValidators.clear();
    }

    public boolean validate() {
        mErrors.clear();
//        if (!isRequired() && isEmptyValue()) {
//            return true;
//        } else if (isRequired() && isEmptyValue()) {
//            addError("%s обязательно к заполнению", getLabel());
//            return false;
//        }

        int errors = 0;
        for (BaseValidator validator : mValidators) {
            if (!validator.validate(getValue())) {
                errors++;
                addError(validator.getErrorMessage());
            }
        }

        return errors == 0;
    }

    public FormField addError(@NonNull CharSequence format, Object... args) {
        getErrors().add(String.format(Wallet.LC_EN, format.toString(), args));
        return this;
    }

    public boolean hasErrors() {
        return getErrors().size() > 0;
    }

    public ArrayList<String> getErrors() {
        return mErrors;
    }

    public String getLastError() {
        if (mErrors.size() == 0) return null;

        return mErrors.get(mErrors.size() - 1);
    }

    @NonNull
    public String getErrorsString(boolean labelPrefix) {
        final StringBuilder sb = new StringBuilder("");
        for (String err : getErrors()) {
            if (labelPrefix && getLabel() != null) {
                sb.append(getLabel());
                sb.append(": ");
            }
            sb.append(err).append("\n");
        }

        return sb.toString();
    }

    @NonNull
    public String getErrorsString() {
        return getErrorsString(false);
    }

    @Override
    public String toString() {
        return firstNonNull(getValue(), "").toString();
    }

    private boolean isEmptyValue() {
        return getValue() == null || getValue().length() == 0;
    }

    public interface Value {
        CharSequence get();
    }
}
