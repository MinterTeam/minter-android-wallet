package network.minter.bipwallet.internal.helpers.forms.validators;

import java.util.regex.Pattern;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class RegexValidator extends BaseValidator {
    private final String mPattern;

    public RegexValidator(String pattern) {
        super("Field is invalid");
        mPattern = pattern;
    }

    public RegexValidator(String pattern, boolean required) {
        super("Field is invalid", required);
        mPattern = pattern;
    }

    public RegexValidator(Pattern pattern, CharSequence errorMessage) {
        super(errorMessage);
        mPattern = pattern.pattern();
    }

    public RegexValidator(Pattern pattern, CharSequence errorMessage, boolean required) {
        super(errorMessage, required);
        mPattern = pattern.pattern();
    }

    public RegexValidator(String pattern, CharSequence errorMessage) {
        super(errorMessage);
        mPattern = pattern;
    }

    public RegexValidator(String pattern, CharSequence errorMessage, boolean required) {
        super(errorMessage, required);
        mPattern = pattern;
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        if(!isRequired() && (value == null || value.length() == 0)) {
            return true;
        }

        String val = value == null ? "" : value.toString();
        return val.matches(mPattern);
    }
}
