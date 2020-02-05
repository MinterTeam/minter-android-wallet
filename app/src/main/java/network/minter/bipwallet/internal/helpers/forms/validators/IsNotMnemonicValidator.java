package network.minter.bipwallet.internal.helpers.forms.validators;

import network.minter.core.bip39.NativeBip39;

public class IsNotMnemonicValidator extends BaseValidator {

    public IsNotMnemonicValidator(CharSequence errorMessage, boolean required) {
        super(errorMessage, required);
    }

    public IsNotMnemonicValidator(CharSequence errorMessage) {
        super(errorMessage);
    }

    @Override
    protected boolean getCondition(CharSequence value) {
        if(value == null || value.length() == 0) {
            return true;
        }

        String val = value.toString();
        return !NativeBip39.validateMnemonic(val, NativeBip39.LANG_DEFAULT);
    }
}
