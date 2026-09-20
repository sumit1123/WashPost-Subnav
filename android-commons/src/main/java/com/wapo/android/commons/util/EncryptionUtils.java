/*
 *
 *   Copyright (C) 2014 . The Washington Post. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.wapo.android.commons.util;

import com.wapo.android.commons.exceptions.EncryptionException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by coxt2 on 8/5/14.
 */
public class EncryptionUtils {
    final protected static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private EncryptionUtils() {
    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] encrypt(byte[] raw, byte[] clear) throws EncryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(clear);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new EncryptionException(e);
        } catch (IllegalBlockSizeException e) {
            throw new EncryptionException(e);
        } catch (BadPaddingException e) {
            throw new EncryptionException(e);
        } catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        }
    }

    public static byte[] decrypt(byte[] key, byte[] encrypted) throws EncryptionException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(encrypted);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new EncryptionException(e);
        } catch (InvalidKeyException e) {
            throw new EncryptionException(e);
        } catch (IllegalBlockSizeException e) {
            throw new EncryptionException(e);
        } catch (BadPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public static String decrypt(String key, String encrypted) throws Exception {
        byte[] encryptedKey = EncryptionUtils.hexStringToByteArray(key);
        byte[] hexStringToByteArrayEncryptedInputHex = EncryptionUtils.hexStringToByteArray(encrypted);
        byte[] decryptedInputByteArray = decrypt(encryptedKey, hexStringToByteArrayEncryptedInputHex);
        return new String(decryptedInputByteArray);
    }

    public static String generateKey() throws EncryptionException {
        String key=null;
        try {
            KeyGenerator keyGenerator=KeyGenerator.getInstance("AES");
            SecretKey secretKey=keyGenerator.generateKey();
            key=bytesToHex(secretKey.getEncoded());
        }catch (NoSuchAlgorithmException e){
            throw new EncryptionException(e);
        }
        return key;
    }
}
