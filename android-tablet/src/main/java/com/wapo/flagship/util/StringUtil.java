package com.wapo.flagship.util;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: maxx
 * Date: 3/15/13
 * Time: 1:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class StringUtil {

    public static String capitalizeWords(String str) {
        if(str == null) {
            return null;
        }

        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);
            if (ch == ' ') {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }
}
