package com.wapo.flagship.util;


/**
 * Created by Artur Glyzin a.glyzin@eastbanctech.ru
 */
public class Util {

    public static boolean isFirePhone() {
        if("firePhone".equalsIgnoreCase(com.washingtonpost.android.BuildConfig.FLAVOR)) {
            return true;
        }
        return false;
    }
}
