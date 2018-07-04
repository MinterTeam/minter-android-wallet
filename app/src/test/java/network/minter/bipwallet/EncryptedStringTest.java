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

package network.minter.bipwallet;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import network.minter.mintercore.crypto.EncryptedString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class EncryptedStringTest {

    @Test
    public void testEncrypting() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String mnemonic = "globe arrange forget twice potato nurse ice dwarf arctic piano scorpion tube";
        String rawPassword = "123456";
        String IV = "Minter seed";
        EncryptedString enc = new EncryptedString(mnemonic, rawPassword, IV);
        assertEquals(
                "e28513acd2336aa048b68cf382a45ec0bc7bed1e7d35f2b7bf0b6c1406e6f3c57fc91c08ba972f7ed82050e54867e1624b2e2f145aa8d0a40d51ad4eb258faa7e2a9ccaed555d15d7830df188897c054",
                enc.getEncrypted()
        );
    }

    @Test
    public void testDecrypting() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String mnemonic = "globe arrange forget twice potato nurse ice dwarf arctic piano scorpion tube";
        String rawPassword = "123456";
        String IV = "Minter seed";

        EncryptedString enc = new EncryptedString(mnemonic, rawPassword, IV);
        String encrypted = "e28513acd2336aa048b68cf382a45ec0bc7bed1e7d35f2b7bf0b6c1406e6f3c57fc91c08ba972f7ed82050e54867e1624b2e2f145aa8d0a40d51ad4eb258faa7e2a9ccaed555d15d7830df188897c054";
        assertEquals(
                encrypted,
                enc.getEncrypted()
        );


        String decrypted = enc.decrypt(rawPassword, IV);
        assertNotNull(decrypted);
        assertEquals(mnemonic, decrypted);
    }
}
