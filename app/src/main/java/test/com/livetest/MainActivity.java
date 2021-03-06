package test.com.livetest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mPushBtn;
    private Button mPush2Btn;
    private Button mPush2Btn2;
    private Button mPlayBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPushBtn = (Button) findViewById(R.id.push_stream_btn);
        mPlayBtn = (Button) findViewById(R.id.play_stream_btn);
        mPush2Btn = (Button) findViewById(R.id.push_stream2_btn);
        mPush2Btn2 = (Button) findViewById(R.id.push_stream2_btn2);
        mPushBtn.setOnClickListener(this);
        mPush2Btn.setOnClickListener(this);
        mPush2Btn2.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.push_stream_btn:
                startActivity(new Intent(this, CameraTestActivity.class));
//                startActivity(new Intent(this, CameraInitActivity.class));
                break;
            case R.id.push_stream2_btn:
                startActivity(new Intent(this, CameraTestNoDisplayActivity.class));
                break;
            case R.id.push_stream2_btn2:
                startActivity(new Intent(this, CameraTestNoDisplayActivity2.class));
                break;
            case R.id.play_stream_btn:
                startActivity(new Intent(this, PlayerActivity.class));
                break;
        }
    }
}
