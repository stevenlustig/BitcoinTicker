package com.stevenlustig.bitcointicker.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.stevenlustig.bitcointicker.ForegroundServiceListener;
import com.stevenlustig.bitcointicker.HttpClient;
import com.stevenlustig.bitcointicker.NetworkBroadcastReceiver;
import com.stevenlustig.bitcointicker.Result;
import com.stevenlustig.bitcointicker.SharedPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class ForegroundService extends Service {
    private Binder mBinder;
    private Handler mHandler;
    private NetworkBroadcastReceiver mNetworkBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new Binder();
        mHandler = new Handler();

        mNetworkBroadcastReceiver = NetworkBroadcastReceiver.register(ForegroundService.this);
    }

    // This is called every time startService is called (even if the service is already running)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.removeCallbacks(runnable);
        mHandler.post(runnable);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(runnable);

        // Start a more permanent service if the background service is enabled
        if (SharedPreferenceManager.isBgServiceEnabled(ForegroundService.this)) {
            BackgroundService.enqueuePeriodicWork(ForegroundService.this);
        }

        if (mNetworkBroadcastReceiver != null) {
            NetworkBroadcastReceiver.unregister(ForegroundService.this, mNetworkBroadcastReceiver);
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // UI Thread
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Background HTTP Thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mBinder.mCurrentResult = HttpClient.getCurrentRate(ForegroundService.this);

                        // Return on UI thread so that callers don't need to handle threading
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                for (ForegroundServiceListener listener : mBinder.mListeners) {
                                    listener.onSuccess(mBinder.mCurrentResult);
                                }

                                mHandler.postDelayed(runnable, 30 * 1000);
                            }
                        });

                    } catch (final Exception e) {
                        mBinder.mCurrentResult = null;

                        // Return on UI thread so that callers don't need to handle threading
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                for (ForegroundServiceListener listener : mBinder.mListeners) {
                                    listener.onFailure(e);
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    };

    public static class Binder extends android.os.Binder {
        private List<ForegroundServiceListener> mListeners = new ArrayList<>();
        private Result mCurrentResult;

        public void addListener(ForegroundServiceListener listener) {
            mListeners.add(listener);
        }

        public void removeListener(ForegroundServiceListener listener) {
            mListeners.remove(listener);
        }

        public Result getCurrentResult() {
            return mCurrentResult;
        }
    }
}
