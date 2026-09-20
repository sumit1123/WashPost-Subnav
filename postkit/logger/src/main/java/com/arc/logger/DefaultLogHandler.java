package com.arc.logger;

import android.text.TextUtils;
import com.wapo.android.commons.util.LogUtil;

public class DefaultLogHandler extends Logger.LogHandler {

    protected static final String TAG = "Logger";

    @Override
    protected void log(int level, String msg, Payload payload) {
        String tag = getTag(payload);
        String log = getMessage(msg, payload);
        switch (level) {
            case Logger.LEVEL_VERBOSE:
                Log.v(tag, log);
                break;
            case Logger.LEVEL_DEBUG:
                LogUtil.d(tag, log);
                break;
            case Logger.LEVEL_WARNING:
                Log.w(tag, log);
                break;
            case Logger.LEVEL_ERROR:
                LogUtil.e(tag, log);
                break;
            default:
                LogUtil.d(TAG, level + "; " + log);
                break;
        }
    }

    protected String getTag(Payload payload) {
        if (payload == null) return TAG;
        if (TextUtils.isEmpty(payload.getTag())) return TAG;
        return payload.getTag();
    }

    protected String getMessage(String msg, Payload payload) {

        StringBuilder sb = new StringBuilder(msg);
        if (payload != null && payload.getError() != null) {
            sb.append("; Error: ").append(payload.getError().getMessage());
        }

        return sb.toString();
    }
}
