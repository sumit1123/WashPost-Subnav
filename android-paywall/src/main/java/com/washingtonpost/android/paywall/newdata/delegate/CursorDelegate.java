package com.washingtonpost.android.paywall.newdata.delegate;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.util.Base64DecoderException;
import com.washingtonpost.android.paywall.util.EncryptUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public abstract class CursorDelegate<PT> {

    static final String TAG = CursorDelegate.class.getSimpleName();
    protected Cursor cursor;
    private Map<String, Integer> columnNameToIndexMap = new HashMap<String, Integer>();

    public CursorDelegate(Cursor cursor) {
        if (cursor == null) {
            throw new NullPointerException("Cursor cannot be null.");
        }
        this.cursor = cursor;
        cursor.moveToFirst();
    }

    public abstract PT getObject();

    public abstract List<PT> getObjectList();

    protected Integer getIndex(String columnName) {
        if(!columnNameToIndexMap.containsKey(columnName)) {
            columnNameToIndexMap.put(columnName, cursor.getColumnIndex(columnName));
        }
        return columnNameToIndexMap.get(columnName);
    }

    protected String getString(String columnName) {
        Integer index = getIndex(columnName);
        if (index == null || index == -1) {
            Logger.e(TAG, "attempted to retrieve non-existent column: " + columnName);
            return null;
        }
        return cursor.getString(index);
    }

    public String getStringDecrypt(String columnName)
            throws NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
                   Base64DecoderException, InvalidKeyException, InvalidKeySpecException {

        String encrypted = getString(columnName);
        return encrypted == null ? null : EncryptUtil.decrypt(encrypted);
    }

    public Long getLongDecrypt(String columnName)
            throws NoSuchPaddingException, Base64DecoderException, NoSuchAlgorithmException, IllegalBlockSizeException,
                   BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        return Long.parseLong(getStringDecrypt(columnName));
    }

    public static String encrypt(String value)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
                   InvalidKeyException, InvalidKeySpecException {

        return value == null ? null : EncryptUtil.encrypt(value);
    }

    public static String encrypt(Long value)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
                   InvalidKeyException, InvalidKeySpecException {
        return encrypt(Long.toString(value));
    }

    protected Integer getInteger(String columnName) {
        Integer index = getIndex(columnName);
        if (index == null || index == -1) {
            Logger.e(TAG, "attempted to retrieve non-existent column: " + columnName);
            return null;
        }
        return cursor.getInt(index);
    }

    protected Long getLong(String columnName) {
        Integer index = getIndex(columnName);
        if (index == null || index == -1) {
            Logger.e(TAG, "attempted to retrieve non-existent column: " + columnName);
            return null;
        }
        return cursor.getLong(index);
    }


    protected Boolean getBoolean(String columnName, Boolean defaultValue) {
        Integer index = getIndex(columnName);
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getInt(index) == 1;
    }

    protected Boolean getBoolean(String columnName) {
        return getBoolean(columnName, null);
    }

    protected Long asLong(Integer i) {
        if (i == null) {
            return null;
        }
        return i.longValue();
    }
}