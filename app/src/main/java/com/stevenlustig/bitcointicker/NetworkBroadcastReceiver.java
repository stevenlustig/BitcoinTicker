package com.stevenlustig.bitcointicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.stevenlustig.bitcointicker.service.ForegroundService;

/**
 * Try to connect again, when service comes back
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {
    public static NetworkBroadcastReceiver register(Context context) {
        NetworkBroadcastReceiver broadcastReceiver = new NetworkBroadcastReceiver();
        context.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        return broadcastReceiver;
    }

    public static void unregister(Context context, BroadcastReceiver broadcastReceiver) {
        context.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        context.startService(serviceIntent);
    }
}
