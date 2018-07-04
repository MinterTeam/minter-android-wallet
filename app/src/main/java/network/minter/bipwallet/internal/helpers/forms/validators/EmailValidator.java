package network.minter.bipwallet.internal.helpers.forms.validators;

import android.util.Patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atlas_Android. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class EmailValidator extends BaseValidator {

    private final static Pattern VALID_EMAIL_ADDRESS_REGEX = Patterns.EMAIL_ADDRESS;

    public EmailValidator(boolean required) {
        super("Invalid E-Mail address", required);
    }

    public EmailValidator() {
        super("Invalid E-Mail address");
    }

    public EmailValidator(CharSequence errorMessage) {
        super(errorMessage);
    }

    public EmailValidator(CharSequence errorMessage, boolean required) {
        super(errorMessage, required);
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        if(!isRequired() && (value == null || value.length() == 0)) {
            return true;
        }

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(value);
        return matcher.find();
    }
}
