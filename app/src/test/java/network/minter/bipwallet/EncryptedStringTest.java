/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
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

import network.minter.core.crypto.EncryptedString;
import network.minter.core.crypto.HashUtil;

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
        EncryptedString enc = new EncryptedString(mnemonic, HashUtil.sha256Hex(rawPassword), IV);
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

        EncryptedString enc = new EncryptedString(mnemonic, HashUtil.sha256Hex(rawPassword), IV);
        String encrypted = "e28513acd2336aa048b68cf382a45ec0bc7bed1e7d35f2b7bf0b6c1406e6f3c57fc91c08ba972f7ed82050e54867e1624b2e2f145aa8d0a40d51ad4eb258faa7e2a9ccaed555d15d7830df188897c054";
        assertEquals(
                encrypted,
                enc.getEncrypted()
        );


        String decrypted = enc.decrypt(HashUtil.sha256Hex(rawPassword), IV);
        assertNotNull(decrypted);
        assertEquals(mnemonic, decrypted);
    }

    /*
    {"data":[{"id":116,"address":"Mx0004ae43810ac75200a0c681487d1748a4f1e0b3","isMain":true,"isServerSecured":true,"encrypted":"9e3ecba46255bd5a1fbcfa71dafa5de57c34e1fea56bca0c1d04118839b0c2d8304edba9c1ac32ee1ba0fef36b71729d9e53ecca40fd8210ef70727b46d8ae7d61515969ea39e42f33005792cb0c896d"}],"links":{"prev":null,"next":null,"first":"http:\/\/my.beta.minter.network\/api\/v1\/addresses\/encrypted?page=1","last":"http:\/\/my.beta.minter.network\/api\/v1\/addresses\/encrypted?page=1"},"meta":{"total":1,"count":1,"perPage":50,"currentPage":1,"firstPage":1,"lastPage":1}}
     */
    @Test
    public void testDecryptingNIKITA() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String mnemonic = "dawn absorb depart army describe blame barely erase battle scrub plate garment";
        String rawPassword = "123123";
        String IV = "Minter seed";

        EncryptedString enc = new EncryptedString(mnemonic, HashUtil.sha256Hex(rawPassword), IV);
        String encrypted = "9e3ecba46255bd5a1fbcfa71dafa5de57c34e1fea56bca0c1d04118839b0c2d8304edba9c1ac32ee1ba0fef36b71729d9e53ecca40fd8210ef70727b46d8ae7d61515969ea39e42f33005792cb0c896d";
        assertEquals(
                encrypted,
                enc.getEncrypted()
        );


        String decrypted = enc.decrypt(HashUtil.sha256Hex(rawPassword), IV);
        assertNotNull(decrypted);
        assertEquals(mnemonic, decrypted);
    }

    @Test
    public void testEncryptingNIKITA() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String mnemonic = "dawn absorb depart army describe blame barely erase battle scrub plate garment";
        String rawPassword = "123123";
        String IV = "Minter seed";

        EncryptedString enc = new EncryptedString(mnemonic, HashUtil.sha256Hex(rawPassword), IV);
        String encrypted = "9e3ecba46255bd5a1fbcfa71dafa5de57c34e1fea56bca0c1d04118839b0c2d8304edba9c1ac32ee1ba0fef36b71729d9e53ecca40fd8210ef70727b46d8ae7d61515969ea39e42f33005792cb0c896d";

        assertEquals(
                encrypted,
                enc.getEncrypted()
        );
    }

    // {"data":[{"id":73,"address":"Mx228e5a68b847d169da439ec15f727f08233a7ca6","isMain":true,"isServerSecured":true,"encrypted":"f1c985db8723f33de9c976ee5061b31afdc919243dbea2e92cd45b12d77c45dbcb122dfb4d1becb205b8fafdea8ff53a17978e5821603e9160817b6033153fe0b2454bfc6599e9a3f0eaa57d557d8c71"}],"links":{"prev":null,"next":null,"first":"http:\/\/my.beta.minter.network\/api\/v1\/addresses\/encrypted?page=1","last":"http:\/\/my.beta.minter.network\/api\/v1\/addresses\/encrypted?page=1"},"meta":{"total":1,"count":1,"perPage":50,"currentPage":1,"firstPage":1,"lastPage":1}}

    public void testDecryptingSidorov() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
//        String mnemonic = "dawn absorb depart army describe blame barely erase battle scrub plate garment";
        String rawPassword = "654321";
        String IV = "Minter seed";

        String encrypted = "f1c985db8723f33de9c976ee5061b31afdc919243dbea2e92cd45b12d77c45dbcb122dfb4d1becb205b8fafdea8ff53a17978e5821603e9160817b6033153fe0b2454bfc6599e9a3f0eaa57d557d8c71";
        EncryptedString enc = new EncryptedString(encrypted);
//        assertEquals(
//                encrypted,
//                enc.getEncrypted()
//        );

//        481f6cc0511143ccdd7e2d1b1b94faf0a700a8b49cd13922a70b5ae28acaa8c5
        String decrypted = enc.decrypt(HashUtil.sha256Hex(rawPassword), IV);
        assertNotNull(decrypted);
//        assertEquals(mnemonic, decrypted);
    }


    public void testEncryptingSidorov() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String mnemonic = "dawn absorb depart army describe blame barely erase battle scrub plate garment";
        String rawPassword = "123123";
        String IV = "Minter seed";
        String encKey = "481f6cc0511143ccdd7e2d1b1b94faf0a700a8b49cd13922a70b5ae28acaa8c5";

        EncryptedString enc = new EncryptedString(mnemonic, HashUtil.sha256Hex(rawPassword), IV);
        String encrypted = "f1c985db8723f33de9c976ee5061b31afdc919243dbea2e92cd45b12d77c45dbcb122dfb4d1becb205b8fafdea8ff53a17978e5821603e9160817b6033153fe0b2454bfc6599e9a3f0eaa57d557d8c71";

        assertEquals(
                encrypted,
                enc.getEncrypted()
        );
    }
}
