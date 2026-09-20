package com.wapo.android.commons.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.wapo.android.commons.util.Logger;
import com.wapo.android.commons.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class ConfigHelper {

    private final static String TAG = ConfigHelper.class.getSimpleName();

    public static JSONObject updateAndLoadConfig(Context context, int configRawFileResId, Constants.ConfigType type) throws JSONException {
        boolean updatedRequired = getConfigUpdateStatus(context, type);
        JSONObject jsonObject = null;

        if(updatedRequired) {
            // Clear old local config file.
            boolean status = clearOldConfig(context, type);
            // Just load config from raw file. On an assumption like local file was deleted.
            // Give priority to load using configRawFileResId. Dependent libraries can pass their raw file ResId.
            jsonObject = readConfigFromResources(context, configRawFileResId, type);
        } else {
            // Versions(stored & current) are same. Load config data from local file.
            jsonObject = readConfig(context, configRawFileResId, type);
        }
        return jsonObject;
    }

    public static String getFileName(Constants.ConfigType configType) {
        if(configType != null) {
            return configType.name() + ".json";
        }
        return "";
    }

    private static boolean getConfigUpdateStatus(Context context, Constants.ConfigType type) {
        int oldVersionCode = getOldConfigVersionCode(context, type);
        int currentVersionCode = Utils.getAppVersionCode(context);

        boolean updateNeeded = oldVersionCode != currentVersionCode;
        if(updateNeeded) {
            setCurrentConfigVersionCode(context, currentVersionCode, type);
        }
        return updateNeeded;
    }

    public static boolean clearOldConfig(Context ctx, Constants.ConfigType type) {
        boolean status = false;
        try {
            File localConfig = new File(ctx.getFilesDir(), getFileName(type));
            if(localConfig.exists()) {
                status = localConfig.delete();
            }
        } catch(Exception e) {
            Logger.w(TAG, "Unable to delete old config" + e.toString());
        }
        return status;
    }

    private static JSONObject readConfigFromResources(Context context, int resId, Constants.ConfigType type) {

        if(resId == -1) {
            // Return null if there is no local res config file.
            return null;
        }

        try {
            InputStream is = context.getResources().openRawResource(resId);
            return readConfigFromStream(context, is, type);
        } catch (JSONException e) {
            throw new RuntimeException("Local(raw) config parse error", e);
        }
    }

    private static JSONObject readConfigFromStream(Context context, InputStream is, Constants.ConfigType type) throws JSONException {
        String configString = Utils.inputStreamToString(is);

        // Decrypt content if config is of type secure before passing to jsonObject.
        if(type.name().toLowerCase().contains("secure")) {
            configString = CryptoHelper.decrypt(configString);
        }

        return new JSONObject(configString);
    }

    private static JSONObject readConfig(Context context, int resId, Constants.ConfigType type) {
        File localConfig = new File(context.getFilesDir(), getFileName(type));
        if (localConfig.exists()) {
            try {
                FileInputStream is = new FileInputStream(localConfig);
                return readConfigFromStream(context, is, type);
            } catch (java.io.FileNotFoundException fnfe) {
                Logger.e(TAG, "Local config read error", fnfe);
                clearOldConfig(context, type);
            } catch (JSONException e) {
                Logger.e(TAG, "Local config parse error", e);
                clearOldConfig(context, type);
            }
        }
        //if fails to read local config, then try to read config from resources.
        return readConfigFromResources(context, resId, type);
    }

    private static int getOldConfigVersionCode(Context context, Constants.ConfigType type) {
        SharedPreferences preference = context.getSharedPreferences(Constants.GENERAL_PREFERENCES, Context.MODE_PRIVATE);
        return preference.getInt(Constants.PREF_KEY_CURRENT_VERSION_CODE + type.ordinal(), -1);
    }

    private static void setCurrentConfigVersionCode(Context context, int currentVersionCode, Constants.ConfigType type) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(Constants.GENERAL_PREFERENCES, Context.MODE_PRIVATE).edit();
        prefsEditor.putInt(Constants.PREF_KEY_CURRENT_VERSION_CODE + type.ordinal(), currentVersionCode);
        prefsEditor.commit();
    }
}
