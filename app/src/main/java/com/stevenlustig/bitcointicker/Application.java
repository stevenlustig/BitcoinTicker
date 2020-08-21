package com.stevenlustig.bitcointicker;

public class Application extends android.app.Application {
    public void onCreate() {
        super.onCreate();

        Notificationer.createNotificationChannel(this);
    }
}
