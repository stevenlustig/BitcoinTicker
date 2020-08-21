package com.stevenlustig.bitcointicker;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stevenlustig.bitcointicker.service.BackgroundService;
import com.stevenlustig.bitcointicker.service.ForegroundService;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PHONE_STATE = 1;

    private Intent mIntent;
    private ForegroundService.Binder mBinder;
    private TextView vStatus;
    private TextView vCurrentRate;
    private Button vStartStopService;
    private TextView vAlertSymbol;
    private EditText vAlertRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        // Views
        vStatus = findViewById(R.id.main_status);
        vCurrentRate = findViewById(R.id.main_currentRate);
        vStartStopService = findViewById(R.id.main_startStopService);
        vAlertSymbol = findViewById(R.id.main_alertRateSymbol);
        vAlertRate = findViewById(R.id.main_alertRate);

        // Set View values & listeners
        vStatus.setText(getString(R.string.main_status_initializing));

        if (SharedPreferenceManager.isBgServiceEnabled(MainActivity.this)) {
            vStartStopService.setText(getString(R.string.main_startStopService_stop));
        }
        vStartStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionUtil.doWithPermission(MainActivity.this, REQUEST_CODE_PHONE_STATE, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, true, new PermissionUtil.PermissionResultCallback() {
                    @Override
                    public void onDenied(String[] deniedPermissions) {

                    }

                    @Override
                    public void onGranted(Activity useThisActivity) {
                        MainActivity mainActivity = (MainActivity) useThisActivity;
                        mainActivity.vStartStopService.setText(mainActivity.vStartStopService.getText().equals(getString(R.string.main_startStopService_start)) ? getString(R.string.main_startStopService_stop)
                                : getString(R.string.main_startStopService_start));
                    }
                });
            }
        });
        vAlertRate.setText(String.format(Locale.US, "%.2f", SharedPreferenceManager.getAlertFluctuationRate(MainActivity.this)));
    }

    @Override
    public void onResume() {
        super.onResume();
        PermissionUtil.onResume(MainActivity.this);

        // Cancel background service
        BackgroundService.cancelPeriodicWork(MainActivity.this);

        // Start foreground service
        mIntent = new Intent(MainActivity.this, ForegroundService.class);
        startService(mIntent);

        bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        // Update preferences (first), then stop the service
        boolean isBgServiceEnabled = vStartStopService.getText().equals(getString(R.string.main_startStopService_stop));
        SharedPreferenceManager.setBgServiceEnabled(isBgServiceEnabled, MainActivity.this);

        try {
            SharedPreferenceManager.setAlertFluctuationRate(Float.parseFloat(vAlertRate.getText().toString()), MainActivity.this);
        }
        catch (NumberFormatException e) {
            // This should never happen because the text is already validated
        }

        if (mBinder != null) {
            disconnectFromService();
            unbindService(mServiceConnection);
        }

        // When the service is stopped, it will restart a more permanent service if the background service option is enabled
        stopService(mIntent);

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtil.onRequestPermissionsResult(MainActivity.this, requestCode, permissions, grantResults);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (ForegroundService.Binder) service;
            mBinder.addListener(mServiceListener);

            // In case Service connects after HTTP call completed,
            // update it now rather than wait for the next call
            if (mBinder.getCurrentResult() != null) {
                mServiceListener.onSuccess(mBinder.getCurrentResult());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            disconnectFromService();
        }
    };

    private void disconnectFromService() {
        mBinder.removeListener(mServiceListener);
        mBinder = null;
    }

    private ForegroundServiceListener mServiceListener = new ForegroundServiceListener() {
        @Override
        public void onSuccess(Result result) {
            SharedPreferenceManager.setLastSeenRate(result.getCurrentRate(), MainActivity.this);

            vStatus.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.main_status_ok));
            vStatus.setText(getString(R.string.main_status_OK));
            vAlertSymbol.setText(result.getSymbol());
            vCurrentRate.setText(result.getCurrentRateUserFriendly());
        }

        @Override
        public void onFailure(Exception e) {
            vStatus.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.main_status_error));
            vStatus.setText(getString(R.string.main_status_error, e.getMessage()));
            vCurrentRate.setText(R.string.main_currentRate_sample);
        }
    };
}