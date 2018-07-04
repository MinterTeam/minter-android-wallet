package network.minter.bipwallet.internal.helpers.forms.validators;

/**
 * Atlas_Android. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class EmptyValidator extends BaseValidator {

    public EmptyValidator() {
        super();
    }

    public EmptyValidator(CharSequence value) {
        super(value);
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        return value != null && value.length() > 0;
    }
}
