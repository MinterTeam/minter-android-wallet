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

package network.minter.bipwallet.settings.views.migration;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public final class MigrationException extends Exception {
    public final static int STEP_1_GET_REMOTE_ADDRESS_LIST = 0;
    public final static int STEP_2_RE_ENCRYPT_REMOTE_DATA = 1;
    public final static int STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE = 3;

    private int mStep;

    public MigrationException(@MigrationStep int step, Throwable other) {
        super(other);
        mStep = step;
    }

    @MigrationStep
    public int getStep() {
        return mStep;
    }

    @IntDef({
            STEP_1_GET_REMOTE_ADDRESS_LIST,
            STEP_2_RE_ENCRYPT_REMOTE_DATA,
            STEP_3_UPDATE_ENCRYPTED_DATA_REMOTE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MigrationStep {
    }
}
