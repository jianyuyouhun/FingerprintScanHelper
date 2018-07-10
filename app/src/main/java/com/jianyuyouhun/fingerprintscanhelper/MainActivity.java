package com.jianyuyouhun.fingerprintscanhelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.jianyuyouhun.fingerprintscan.library.FingerprintScanHelper;
import com.jianyuyouhun.fingerprintscan.library.OnAuthResultListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.auth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FingerprintScanHelper(MainActivity.this)
                        .startAuth(new OnAuthResultListener() {
                            @Override
                            public void onSuccess() {
                                showToast("success");
                            }

                            @Override
                            public void onInputPwd(String pwd) {
                                showToast(pwd);
                            }

                            @Override
                            public void onFailed(String msg) {
                                showToast(msg);
                            }

                            @Override
                            public void onDeviceNotSupport() {
                                showToast("设备不支持");
                            }
                        });
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
