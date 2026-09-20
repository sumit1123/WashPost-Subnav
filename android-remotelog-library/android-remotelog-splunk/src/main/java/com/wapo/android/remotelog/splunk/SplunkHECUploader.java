/**
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 * @author created by Hardip Singh on 08/14/2019
 */
package com.wapo.android.remotelog.splunk;

import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.wapo.android.remotelog.logger.LogFileUploader;

import java.util.UUID;

import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.util.FileUtils;
import com.wapo.android.commons.util.HttpUtils;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class SplunkHECUploader implements LogFileUploader.UploadHelper {
    private String hecURL;
    private String hecToken;

    private static final String TAG = "SplunkHECUploader";

    public SplunkHECUploader(String hecURL, String hecToken) {
        this.hecURL = hecURL;
        this.hecToken = hecToken;
    }

    @Override
    public boolean upload_gZipCompressedFile(File file) {
        String response = null;
        try {
            URL endpointURL = new URL(hecURL);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Encoding", "gzip");
            headers.put("Authorization", "Splunk " + this.hecToken);

            UUID randomUUID = UUID.randomUUID();
            headers.put("X-Splunk-Request-Channel", randomUUID.toString());
            headers.putAll(DefaultHeadersInterceptor.Companion.getHeaders());

            byte[] content = FileUtils.getBytesFromFile(file);
            response = HttpUtils.invokeHttpRequest(endpointURL, "POST", headers, content);

            if (response == null) {
                Logger.e(TAG, "Response from Splunk was null.");
                return false;
            }

            JSONObject responseJSON = new JSONObject(response);
            if (responseJSON == null) {
                Logger.e(TAG, "Splunk response could not be parsed as JSON: " + response);
                return false;
            }

            if (!responseJSON.getString("text").equals("Success") || responseJSON.getInt("code") != 0) {
                Logger.e(TAG, "Did not receive success response from Splunk: " + response);
                return false;
            }

        } catch (Exception e) {
            Logger.e(TAG, "uploadFile ", e);
            return false;
        }

        return true;
    }
}