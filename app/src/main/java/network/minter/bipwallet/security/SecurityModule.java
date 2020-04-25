/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
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

package network.minter.bipwallet.security;

import dagger.Module;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@Module
public class SecurityModule {
    public final static int MAX_TRIES_UNTIL_LOCK = 5;
    public final static int LOCK_INTERVAL_S = 60;

    public final static String EXTRA_MODE = "EXTRA_MODE";
    public final static String EXTRA_PIN = "EXTRA_PIN";
    public final static String EXTRA_SUCCESS_INTENT = "EXTRA_SUCCESS_INTENT";

    public enum PinMode {
        Creation,
        Confirmation,
        Validation,
        Deletion,
        Change,
        EnableFingerprint,
        DisableFingerprint;

        public static PinMode fromInt(int ordinal) {
            for (PinMode m : PinMode.values()) {
                if (m.ordinal() == ordinal) {
                    return m;
                }
            }

            return Creation;
        }
    }

    public interface KeypadListener {

    }

}
