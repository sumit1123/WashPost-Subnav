package com.wapo.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.MediaController;

public class MediaControllerEx extends MediaController {
    private static final int MSG_NOTIFY_HIDE = 1;

    private OnVisibilityChangedListener _listener = null;

    private final Handler.Callback _callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_NOTIFY_HIDE) {
                notifyVisibilityChanged(false);
            }
            return false;
        }
    };

    private final Handler _handler = new Handler(Looper.getMainLooper(), _callback);

    public MediaControllerEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaControllerEx(Context context, boolean useFastForward) {
        super(context, useFastForward);
    }

    public MediaControllerEx(Context context) {
        super(context);
    }

    @Override
    public void show(int timeout) {
        _handler.removeMessages(MSG_NOTIFY_HIDE);
        super.show(timeout);
        notifyVisibilityChanged(isShowing());
        _handler.sendEmptyMessageDelayed(MSG_NOTIFY_HIDE, timeout);
    }

    @Override
    public void hide() {
        _handler.removeMessages(MSG_NOTIFY_HIDE);
        super.hide();
        notifyVisibilityChanged(isShowing());
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
        _listener = listener;
    }

    private void notifyVisibilityChanged(boolean isShowing) {
        if (_listener != null) {
            _listener.onVisibilityChanged(isShowing);
        }
    }


    public interface OnVisibilityChangedListener {
        void onVisibilityChanged(boolean isShowing);
    }
}
