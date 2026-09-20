package com.wapo.flagship;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.washingtonpost.android.paywall.PaywallService;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TestPaywallActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        setContentView(root);

        Button button = new Button(this);
        root.addView(button);
        button.setText("Click me and wait for 3 sec");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //make a handler and send a message after the activity has been already stopped
                        final Handler handler = getPaywallTimeoutHandler();
                        final Message msg = new Message();
                        msg.what = PaywallService.RUN_PAYWALL_SERVICE_RESULT;
                        msg.obj = Bundle.EMPTY;
                        handler.sendMessage(msg);
                    }
                }, 3000);

                minimizeApp();
            }
        });
    }

    private void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
