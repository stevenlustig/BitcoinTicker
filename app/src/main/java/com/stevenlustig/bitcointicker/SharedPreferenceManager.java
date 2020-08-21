package com.stevenlustig.bitcointicker;

import android.content.Context;
import android.preference.PreferenceManager;

public class SharedPreferenceManager {
    private static final String PREF_BG_SERVICE_ENABLED = "isBgServiceEnabled";
    private static final String PREF_CURRENT_RATE = "currentRate";
    private static final String PREF_ALERT_FLUCTUATION_RATE = "alertFluctuationRate";

    public static boolean isBgServiceEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_BG_SERVICE_ENABLED, false);
    }

    public static void setBgServiceEnabled(boolean enabled, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_BG_SERVICE_ENABLED, enabled).apply();
    }

    public static float getLastSeenRate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(PREF_CURRENT_RATE, 0);
    }

    public static void setLastSeenRate(float currentRate, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(PREF_CURRENT_RATE, currentRate).apply();
    }

    public static float getAlertFluctuationRate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(PREF_ALERT_FLUCTUATION_RATE, 0.05f);
    }

    public static void setAlertFluctuationRate(float alertFluctuationRate, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(PREF_ALERT_FLUCTUATION_RATE, alertFluctuationRate).apply();
    }
}
