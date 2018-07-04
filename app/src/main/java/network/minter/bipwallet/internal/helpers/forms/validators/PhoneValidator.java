package network.minter.bipwallet.internal.helpers.forms.validators;

import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Stars. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class PhoneValidator extends RegexValidator {
    private final static Pattern PHONE_PATTERN = Patterns.PHONE;

    public PhoneValidator() {
        super(PHONE_PATTERN.pattern(), "Invalid phone number");
    }

    public PhoneValidator(boolean required) {
        super(PHONE_PATTERN.pattern(), "Invalid phone number", required);
    }

    public PhoneValidator(CharSequence errorMessage) {
        super(PHONE_PATTERN, errorMessage);
    }

    public PhoneValidator(CharSequence errorMessage, boolean required) {
        super(PHONE_PATTERN, errorMessage, required);
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        return super.getCondition(value);
    }
}
