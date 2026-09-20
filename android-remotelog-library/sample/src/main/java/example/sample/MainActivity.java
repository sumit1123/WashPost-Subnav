package example.sample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.wapo.android.remotelog.logger.LoggerConfig;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.android.remotelog.splunk.SplunkHECUploader;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.splunk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                splunk();
            }
        });

    }

    private void splunk() {
        SplunkHECUploader splunkUploader = new SplunkHECUploader("https://hec.washpost.com/services/collector/raw", "7C1E15D7-D62D-4D53-8838-01461E65A43B");
        RemoteLog.initialize(config, splunkUploader);
        RemoteLog.d("It's working!!!", this);
        Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
    }

}
