package com.washingtonpost.android.paywall.util;

import com.wapo.android.commons.util.Base64;
import com.wapo.android.commons.util.Base64DecoderException;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {

    private static final String ALGO = "AES";

    public static String encrypt(String Data)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
                   IllegalBlockSizeException, InvalidKeySpecException {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = Base64.encode(encVal);
        return encryptedValue;
    }

    public static String decrypt(String encryptedData)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            Base64DecoderException, BadPaddingException, IllegalBlockSizeException {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = Base64.decode(encryptedData);
        byte[] decValue = c.doFinal(decordedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    private static Key generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        String TAG = "PBKDF2WithHmacSHA1F1pAywalL";
        KeySpec spec = new PBEKeySpec(TAG.toCharArray(), TAG.getBytes(), 128, 256);
        SecretKey tmp = skf.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGO);
    }
}
