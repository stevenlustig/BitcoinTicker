package com.stevenlustig.bitcointicker;

import java.text.DecimalFormat;
import java.util.Locale;

public class Result {
    private float mCurrentRate;
    private float mLastSeenRate;
    private String mSymbol;

    public Result(float currentRate, float lastSeenRate, String symbol) {
        this.mCurrentRate = currentRate;
        this.mLastSeenRate = lastSeenRate;
        this.mSymbol = symbol;
    }

    public float getCurrentRate() {
        return mCurrentRate;
    }

    public String getCurrentRateUserFriendly() {
        return getUserFriendly(getCurrentRate(), false);
    }

    public float getLastSeenRate() {
        return mLastSeenRate;
    }

    public float getRateChange() {
        return getCurrentRate() - getLastSeenRate();
    }

    public String getRateChangeUserFriendly() {
        return getUserFriendly(getRateChange(), true);
    }

    public String getSymbol() {
        return mSymbol;
    }

    private String getUserFriendly(float amount, boolean showPlus) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        decimalFormat.setMinimumFractionDigits(2);

        String plusMinus = amount < 0 ? "- " : showPlus ? "+ " : "";

        return String.format(Locale.US, "%s%s%s", plusMinus, getSymbol(), decimalFormat.format(Math.abs(amount)));
    }
}
