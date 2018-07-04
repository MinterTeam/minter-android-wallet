package network.minter.bipwallet.internal.helpers.forms.validators;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class NumberValidator extends BaseValidator {

    private int mMin = 0;
    private long mMax = Integer.MAX_VALUE;
    private CharSequence mError;

    public NumberValidator(CharSequence errorMessage, int min, int max) {
        super(errorMessage);
        mError = errorMessage;
        mMin = min;
        mMax = max;
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        if(!isRequired() && (value == null || value.length() == 0)) {
            return true;
        }

        if (value == null) {
            setErrorMessage("Введите число");
            return false;
        }

        Long v;
        try {
            v = Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            setErrorMessage("Введите число");
            return false;
        }

        if (v < mMin || v > mMax) {
            setErrorMessage(mError);
            return false;
        }

        return true;
    }
}
