package com.stevenlustig.bitcointicker;

public interface ForegroundServiceListener {
    void onSuccess(Result result);
    void onFailure(Exception e);
}
