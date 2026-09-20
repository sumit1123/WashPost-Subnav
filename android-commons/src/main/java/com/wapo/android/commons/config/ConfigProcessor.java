package com.wapo.android.commons.config;

import android.content.Context;
import android.text.TextUtils;

import com.wapo.android.commons.config.sec.helper.SSLSocketFactoryProvider;
import com.wapo.android.commons.util.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.wapo.android.commons.util.FileUtils;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class ConfigProcessor implements Runnable {
    private static final String TAG = ConfigProcessor.class.getSimpleName();
    private final Context context;
    private final Constants.ConfigType configType;
    private final String configUrl;
    private final String localFileName;
    private final OnCompleteListener completionCallback;

    public ConfigProcessor(Context context, Constants.ConfigType configType, String configUrl, String localFileName, OnCompleteListener completionCallback) {
        this.context = context;
        this.configType = configType;
        this.configUrl = configUrl;
        this.completionCallback = completionCallback;
        this.localFileName = localFileName;
    }

    @Override
    public void run() {
        try {
            if (checkIsInterrupted(false)) {
                return;
            }

            if (!TextUtils.isEmpty(configUrl)) {
                updateConfigWithRules();
            }

            if (checkIsInterrupted(false)) {
                return;
            }
        } catch (InterruptedException e) {
            //
            // this exception is originated in our code and we didn't clear interrupted flag, so do nothing here, just complete the task.
        } catch (Exception e) {
            Logger.e(TAG, Log.getStackTraceString(e));
            completionCallback.onFailure(e);
        } finally {
            completionCallback.onComplete();
        }
    }

    private void updateConfigWithRules() throws JSONException, InterruptedException, IOException,
            GeneralSecurityException {
        boolean validateLastModifiedRule = true;
        boolean validateChecksumRule = true;
        boolean validateVersionCheckRule = true;

        boolean updateRequired = false;

        // Local file status
        boolean localFileExists = false;
        File localFile = new File(context.getFilesDir(), localFileName);
        localFileExists = localFile != null && localFile.exists();
        if (!localFileExists) {
            Logger.d(TAG, "Config - ( " + configType.name() + " ) No local file exists!");
            validateLastModifiedRule = validateChecksumRule = validateVersionCheckRule = localFileExists;
        }

        // Download Remote file
        ConfigFile configFile = downloadConfig(configUrl);
        if (configFile.content == null) {
            Logger.d(TAG, "Config - ( " + configType.name() + " ) content is null!");
            return;
        }

        // Validate Last Modified Date
        if (validateLastModifiedRule) {
            updateRequired = validateLastModifiedDates(configFile.lastModified);
            if (!updateRequired) {
                Logger.d(TAG, "Config - ( " + configType.name() + " ) LastModifiedDates are same!");
                return;
            } else {
                Logger.d(TAG, "Config - ( " + configType.name() + " ) LastModifiedDates are not same!");
            }
        }

        checkIsInterrupted();

        // Filenames
        String tempLocalFileName = localFileName;
        if (validateChecksumRule || validateVersionCheckRule) {
            tempLocalFileName = localFileName + ".temp";
        }
        String localJson = null;
        {
            File file = new File(context.getFilesDir(), tempLocalFileName);
            PrintWriter out = null;
            try {
                out = new PrintWriter(file);
                out.print(configFile.content);
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (file != null && configFile.lastModified != -1) {
                        file.setLastModified(configFile.lastModified);
                    }
                } catch (Exception e) {
                }
            }
        }

        if (!validateChecksumRule && !validateVersionCheckRule) {
            Logger.d(TAG, "Config - ( " + configType.name() + " ) File created!");
            completionCallback.onSuccess(new JSONObject(configFile.content), configType);
            return;
        }

        // Read local file content
        StringBuffer sb = new StringBuffer();
        if (validateVersionCheckRule && localFileExists) {
            BufferedReader br = new BufferedReader(new FileReader(localFile));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            localJson = sb.toString();
        }

        checkIsInterrupted();

        // Validate Checksum
        {
            if (validateChecksumRule) {
                File newFile = new File(context.getFilesDir(), tempLocalFileName);
                if (localFileExists && newFile.exists()) {
                    updateRequired = validateChecksums(localFile, newFile);
                    if (!updateRequired) {
                        // Delete new file
                        boolean status = newFile.delete();
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) ChecksSums are same!");
                        return;
                    }
                } else {
                    // Local file doesn't exist. Just rename new file.
                    if (newFile.exists()) {
                        replaceFile(localFile, newFile, configFile.content, configFile.lastModified);
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) No local file to validate Checksums! newFile renamed.");
                        return; // Return as there is no local file to validate versions.
                    } else {
                        // No new file exists. Just return;
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) No new file to validate Checksums! localFile.");
                        return;
                    }
                }
                if (updateRequired) {
                    Logger.d(TAG, "Config - ( " + configType.name() + " ) Checksums are not same!");
                    if (!validateVersionCheckRule) {
                        boolean status = localFile.delete();
                        if (status) {
                            replaceFile(localFile, newFile, configFile.content, configFile.lastModified);
                            Logger.d(TAG, "Config - ( " + configType.name() + " ) Checksums are not same! newFile renamed and replaced.");
                        }
                    }
                }
            } // VALIDATE_CHECKSUM_RULE
        } // Block

        checkIsInterrupted();

        // Validate Version
        {
            if (validateVersionCheckRule) {
                File newFile = new File(context.getFilesDir(), tempLocalFileName);

                if (localFileExists && newFile.exists()) {

                    updateRequired = validateVersions(localJson, configFile.content);
                    if (updateRequired) {
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) Versions are not same!");
                        boolean status = localFile.delete();
                        if (status) {
                            replaceFile(localFile, newFile, configFile.content, configFile.lastModified);
                            Logger.d(TAG, "Config - ( " + configType.name() + " ) Versions are not same! newFile renamed and replaced.");
                        }
                    } else {
                        // Delete new file
                        boolean status = newFile.delete();
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) Versions are same! newFile deleted.");
                        return;
                    }
                } else {
                    // Local file doesn't exist. Just rename new file.
                    if (newFile.exists()) {
                        replaceFile(localFile, newFile, configFile.content, configFile.lastModified);
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) No local file to validate Versions! newFile renamed");
                    } else {
                        Logger.d(TAG, "Config - ( " + configType.name() + " ) No new file to validate Versions! localFile.");
                    }
                    return;
                }
            } // VALIDATE_VERSION_CHECK_RULE
        } // Block

        checkIsInterrupted();
    }

    private void replaceFile(File localFile, File newFile, String responseJson, long remoteFileLastModifiedVal) throws JSONException {
        if (remoteFileLastModifiedVal != -1) {
            newFile.setLastModified(remoteFileLastModifiedVal);
        }
        boolean status = newFile.renameTo(localFile);
        if (status) {
            completionCallback.onSuccess(new JSONObject(responseJson), configType);
        }
    }

    private ConfigFile downloadConfig(String url) throws IOException, JSONException, InterruptedException,
            GeneralSecurityException {
        int connTs = 15000;
        int readTs = 10000;
        int tsStep = 10000;

        IOException exception = null;
        ConfigFile configFile = null;
        for (int i = 0; i < 3; i++, connTs += tsStep, readTs += tsStep) {
            checkIsInterrupted();

            try {
                configFile = downloadConfig(url, connTs, readTs);
                exception = null;
                break;
            } catch (ConnectTimeoutException e) {
                exception = e;
            } catch (SocketTimeoutException e) {
                exception = e;
            }
        }

        checkIsInterrupted();

        if (exception != null) {
            throw exception;
        }

        return configFile;
    }

    private ConfigFile downloadConfig(String urlStr, int connTs, int readTs) throws IOException, GeneralSecurityException {
        URL url = new URL(urlStr);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        SSLSocketFactory sslSocketFactory = SSLSocketFactoryProvider.INSTANCE.getSslSocketFactory();
        if (conn instanceof HttpsURLConnection && sslSocketFactory != null) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
        }
        conn.setConnectTimeout(connTs);
        conn.setReadTimeout(readTs);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        Map<String, String> defaultHeaders = DefaultHeadersInterceptor.Companion.getHeaders();
        Set<String> keys = defaultHeaders.keySet();
        for (String key : keys) {
            conn.setRequestProperty(key, defaultHeaders.get(key));
        }

        try {
            int responseCode = conn.getResponseCode();
            long lastModified = conn.getLastModified();
            if (responseCode == -1) {
                // -1 is returned by getResponseCode() if the response code could not be retrieved.
                // Signal to the caller that something was wrong with the connection.
                throw new IOException("Could not retrieve response code from HttpUrlConnection.");
            }
            return new ConfigFile(lastModified, FileUtils.getContentsFromFile(conn.getInputStream()));
        } finally {
            conn.disconnect();
        }
    }

    private static boolean checkIsInterrupted() throws InterruptedException {
        return checkIsInterrupted(true);
    }

    private static boolean checkIsInterrupted(boolean shouldThrow) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            if (shouldThrow) {
                throw new InterruptedException();
            }
            return true;
        }

        return false;
    }

    private boolean validateLastModifiedDates(long remoteFileLastModifiedVal) {
        // Get local file last modified date
        File file = new File(context.getFilesDir(), localFileName);

        if (file != null && file.exists()) {
            Date localFileModifiedDate = new Date(file.lastModified());
            Date remoteFileModifiedDate = new Date(remoteFileLastModifiedVal);

            // Compare dates, if modified, return true, else false.
            if (localFileModifiedDate != null && remoteFileModifiedDate != null
                    && localFileModifiedDate.compareTo(remoteFileModifiedDate) >= 0) {
                return false;
            }
        }

        return true;
    }

    private boolean validateChecksums(File localFile, File newFile) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String localFileDigest = getDigest(new FileInputStream(localFile), md, 2048);
            String newFileDigest = getDigest(new FileInputStream(newFile), md, 2048);
            if (!TextUtils.isEmpty(localFileDigest) && !TextUtils.isEmpty(newFileDigest)) {
                if (localFileDigest.equals(newFileDigest)) {
                    return false;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String getDigest(InputStream is, MessageDigest md, int byteArraySize)
            throws NoSuchAlgorithmException, IOException {

        md.reset();
        byte[] bytes = new byte[byteArraySize];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            md.update(bytes, 0, numBytes);
        }
        byte[] digest = md.digest();
        String result = new String(Hex.encodeHex(digest));
        return result;
    }

    private boolean validateVersions(String localJson, String responseJson) {
        int localVersionNum = getVersionNumber(localJson);
        int newVersionNum = getVersionNumber(responseJson);
        if (localVersionNum > newVersionNum) {
            return false;
        } else {
            return true;
        }
    }

    private int getVersionNumber(String jsonContent) {
        try {
            JsonParser parser = new JsonParser();
            Object obj = parser.parse(jsonContent);
            JsonObject jsonObject = (JsonObject) obj;
            JsonElement version = jsonObject.get(Constants.VERSION_KEY_NAME);
            if (version != null && version.isJsonPrimitive()) {
                return version.getAsInt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private class ConfigFile {
        long lastModified;
        String content;

        public ConfigFile(long lastModified, String content) {
            this.lastModified = lastModified;
            this.content = content;
        }
    }

    public interface OnCompleteListener {
        void onComplete();

        void onSuccess(JSONObject jsonObject, Constants.ConfigType configType) throws JSONException;

        void onFailure(Exception e);
    }
}
