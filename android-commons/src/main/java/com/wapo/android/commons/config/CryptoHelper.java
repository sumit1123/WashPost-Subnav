package com.wapo.android.commons.config;

import android.text.TextUtils;
import android.util.Base64;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class to encrypt/decrypt given text
 *
 * Encryption: Text(Input) -> AES -> Base64Encoding -> EncryptedText(Output)
 * Decryption: EncryptedText(Input) -> Base64Decoding -> AES -> Text(Output)
 *
 * External CryptoHelper tool can be used to encrypt/decrypt the file content.
 * Online tool: http://aesencryption.net
 */

public class CryptoHelper {

    private final static String CRYPTO_ALGORITHM = "AES/ECB/NoPadding";
    private final static String CRYPTO_ALGORITHM_KEY_SPEC = "AES";
    private final static String STRING_ENCODING = "UTF-8";
    private final static String KEY = "SecurePassword12";

    private static String padBytesIfRequired(String text) {
        int reqRemainingPaddingBytesCount = 16 - text.length()%16;
        if(reqRemainingPaddingBytesCount > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(text);
            while (reqRemainingPaddingBytesCount > 0) {
                sb.append(" ");
                reqRemainingPaddingBytesCount--;
            }
            return sb.toString();
        }
        return text;
    }

    public static String encrypt(String text) {
        if(TextUtils.isEmpty(text)) {
            return "";
        }
        text = padBytesIfRequired(text);
        byte[] encryptedText = encryptData(text);
        String base64EncodedText = Base64.encodeToString(encryptedText, Base64.DEFAULT);
        return base64EncodedText;
    }

    public static String decrypt(String base64EncodedText) {
        if(TextUtils.isEmpty(base64EncodedText)) {
            return "";
        }
        byte[] encryptedText = Base64.decode(base64EncodedText, Base64.DEFAULT);
        String text = decryptData(encryptedText);
        if(!TextUtils.isEmpty(text)) {
            text = text.trim();
        }
        return text;
    }

    private static byte[] trimKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] keyBytes = KEY.getBytes(STRING_ENCODING);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        keyBytes = sha.digest(keyBytes);
        // use only first 128 bit/16 bytes
        keyBytes = Arrays.copyOf(keyBytes, 16);
        return keyBytes;
    }

    private static byte[] encryptData(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();;
        CipherOutputStream cos = null;
        PrintWriter pw = null;

        try {
            SecretKeySpec sks = new SecretKeySpec(KEY.getBytes(STRING_ENCODING), CRYPTO_ALGORITHM_KEY_SPEC);
            // Create cipher
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            // Wrap the output stream
            cos = new CipherOutputStream(baos, cipher);
            pw = new PrintWriter(cos);
            // Write text
            pw.write(text);

        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(NoSuchPaddingException e) {
            e.printStackTrace();
        } catch(InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            // Flush and close streams.
            if(pw != null) {
                pw.flush();
                pw.close();
            }
            if(cos != null) {
                try {
                    cos.flush();
                    cos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(baos != null) {
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }

    private static String decryptData(byte[] encryptedText) {
        ByteArrayInputStream bais = new ByteArrayInputStream(encryptedText);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CipherInputStream cis = null;
        StringBuffer text = new StringBuffer();
        try {
            SecretKeySpec sks = new SecretKeySpec(KEY.getBytes(STRING_ENCODING), CRYPTO_ALGORITHM_KEY_SPEC);
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            cis = new CipherInputStream(bais, cipher);
            int b;
            byte[] d = new byte[8];
            while((b = cis.read(d)) != -1) {
                baos.write(d, 0, b);
            }

             text.append(new String(baos.toByteArray(), STRING_ENCODING));
        } catch(IOException e) {
            e.printStackTrace();
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(NoSuchPaddingException e) {
            e.printStackTrace();
        } catch(InvalidKeyException e) {
            e.printStackTrace();
        }
        finally {
            // Flush and close streams.
            if(baos != null) {
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(cis != null) {
                try {
                    cis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return text.toString();
    }
}
