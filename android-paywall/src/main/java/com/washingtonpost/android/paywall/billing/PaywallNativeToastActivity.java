package com.washingtonpost.android.paywall.billing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.washingtonpost.android.paywall.R;

public class PaywallNativeToastActivity extends Activity {

    public static final String PARAM_IS_PRIME=PaywallNativeToastActivity.class.getSimpleName()+".isPrime";

    TextView titleTextView;
    TextView contentTextView;
    TextView subText;
    Button closeButton;
    Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paywall_native_toast);
        titleTextView = (TextView)findViewById(R.id.toast_title_message);
        contentTextView=(TextView)findViewById(R.id.toast_content_message);
        subText=(TextView)findViewById(R.id.toast_subtext);
        closeButton=(Button)findViewById(R.id.close_toast_button);
        continueButton=(Button)findViewById(R.id.continue_btn);

        Intent intent=getIntent();
        boolean isPrime=intent.getBooleanExtra(PARAM_IS_PRIME,false);
        if(isPrime){
            subText.setText(getResources().getString(R.string.pw_success));
        }else{
            titleTextView.setVisibility(View.GONE);
            contentTextView.setText(getResources().getString(R.string.pw_other_message));
            subText.setVisibility(View.VISIBLE);
            subText.setMovementMethod(LinkMovementMethod.getInstance());
            continueButton.setVisibility(View.GONE);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


}
