package com.stevenlustig.bitcointicker.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.stevenlustig.bitcointicker.HttpClient;
import com.stevenlustig.bitcointicker.Notificationer;
import com.stevenlustig.bitcointicker.SharedPreferenceManager;

import java.util.concurrent.TimeUnit;

public class BackgroundService extends Worker {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private static final String UNIQUE_NAME = BackgroundService.class.getSimpleName();


    public static void enqueuePeriodicWork(Context context) {
        PeriodicWorkRequest.Builder workRequestBuilder = new PeriodicWorkRequest.Builder(BackgroundService.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(30, TimeUnit.SECONDS)
                .addTag(TAG);

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequestBuilder.build());
    }

    public static void cancelPeriodicWork(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
    }

    public BackgroundService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            com.stevenlustig.bitcointicker.Result result = HttpClient.getCurrentRate(getApplicationContext());
            float rateChange = Math.abs(result.getCurrentRate() - result.getLastSeenRate());

            if (rateChange >= SharedPreferenceManager.getAlertFluctuationRate(getApplicationContext())) {
                Notificationer.sendNotification(result.getRateChangeUserFriendly(), getApplicationContext());
            }

            return androidx.work.ListenableWorker.Result.success();
        } catch (Exception e) {
            return androidx.work.ListenableWorker.Result.retry();
        }
    }
}
