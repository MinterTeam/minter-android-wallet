/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.settings.views;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import network.minter.bipwallet.auth.ui.InputGroup;
import network.minter.bipwallet.home.HomeScope;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.bipwallet.settings.SettingsTabModule;
import network.minter.bipwallet.settings.ui.SettingsFieldType;
import network.minter.bipwallet.settings.ui.SettingsUpdateFieldDialog;
import network.minter.my.repo.MyProfileRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallMy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@HomeScope
@InjectViewState
public class SettingsUpdateFieldPresenter extends MvpBasePresenter<SettingsTabModule.SettingsUpdateFieldView> {

    @Inject MyProfileRepository profileRepo;
    private CharSequence mLabel;
    private String mField;
    private String mValue;
    private SettingsFieldType mType;


    @Inject
    public SettingsUpdateFieldPresenter() {
    }

    private boolean mValid = false;

    @Override
    public void handleExtras(Bundle bundle) {
        super.handleExtras(bundle);

        mLabel = bundle.getCharSequence(SettingsUpdateFieldDialog.ARG_LABEL);
        mField = bundle.getString(SettingsUpdateFieldDialog.ARG_FIELD_NAME);
        mValue = bundle.getString(SettingsUpdateFieldDialog.ARG_VALUE, null);
        mType = SettingsFieldType.values()[bundle.getInt(SettingsUpdateFieldDialog.ARG_TYPE)];

        getViewState().setLabel(mLabel);
        getViewState().setValue(mValue);
        getViewState().configureInput(mType, new InputGroup.OnTextChangedListener() {
            @Override
            public void onTextChanged(EditText editText, boolean valid) {
                getViewState().setEnableSubmit(valid);
                mValid = valid;
                if (valid) {
                    mValue = editText.getText().toString();
                }
            }
        });

        getViewState().setOnSubmit(this::onSubmit);
    }

    private void onSubmit(View view) {
        if (!mValid) {
            return;
        }

        safeSubscribeIoToUi(rxCallMy(profileRepo.updateField(mField, mValue)))
                .subscribe(res -> {
                    getViewState().callOnSaveListener();
                    getViewState().dismiss();
                });
    }
}
