package com.wapo.android.commons.util;


import com.wapo.android.commons.exceptions.EncryptionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EncryptionUtilsTest {

    private final String ENCRYPT_KEY = "8F1644079AE6DAAF60C907AD1CA9E4A5";


    @Test
    public void testEncryptionDecryption() throws EncryptionException {
        System.out.println("Test 1");
        testEncryptionDecryption("AKIAEXAMPLEKEYID1234");

        System.out.println("\n\nTest 2");
        testEncryptionDecryption("ExampleSecretKeyForRoundTripTestOnly0000");

    }

    private void decrypt(String name, String encrypted) throws EncryptionException {
        byte[] encryptedKey = EncryptionUtils.hexStringToByteArray(ENCRYPT_KEY);
        byte[] hexStringToByteArrayEncryptedInputHex = EncryptionUtils.hexStringToByteArray(encrypted);
        byte[] decryptedInputByteArray = EncryptionUtils.decrypt(encryptedKey, hexStringToByteArrayEncryptedInputHex);
        String decryptedInputString = new String(decryptedInputByteArray);
        System.out.println("################ After Decryption ################ ");
        System.out.println(name + " decrypted-> " + decryptedInputString);
    }
    
    private void testEncryptionDecryption(String input) throws EncryptionException {

        byte[] encryptedKey = EncryptionUtils.hexStringToByteArray(ENCRYPT_KEY);

        // Encryption process
        byte[] inputByteArray = input.getBytes();
        byte[] encryptedInput = EncryptionUtils.encrypt(encryptedKey, inputByteArray);
        String encryptedInputHex = EncryptionUtils.bytesToHex(encryptedInput);
        System.out.println("################ After Encryption ################ ");
        System.out.println("Input is -> " + input);
        System.out.println("Encrypted input is -> " + encryptedInputHex);

        System.out.println("\n");

        // Decryption Process
        byte[] hexStringToByteArrayEncryptedInputHex = EncryptionUtils.hexStringToByteArray(encryptedInputHex);
        byte[] decryptedInputByteArray = EncryptionUtils.decrypt(encryptedKey, hexStringToByteArrayEncryptedInputHex);
        String decryptedInputString = new String(decryptedInputByteArray);
        System.out.println("################ After Decryption ################ ");
        System.out.println("Input is -> " + input);
        System.out.println("Decrypted (Encrypted-input) is -> " + decryptedInputString);

        assertEquals(input, decryptedInputString);
    }
}