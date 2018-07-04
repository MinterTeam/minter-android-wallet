package network.minter.bipwallet.internal.helpers.forms.validators;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class LengthValidator extends BaseValidator {
    private int mMin = 0;
    private int mMax = Integer.MAX_VALUE;

    public LengthValidator(CharSequence errorMessage, int min) {
        super(errorMessage);
        mMin = min;
    }

    public LengthValidator(CharSequence errorMessage, int min, int max) {
        super(errorMessage);
        mMin = min;
        mMax = max;
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        if (mMin == 0 && (value == null || value.length() == 0)) {
            return true;
        }

        return value != null && value.length() >= mMin && value.length() <= mMax;
    }
}
