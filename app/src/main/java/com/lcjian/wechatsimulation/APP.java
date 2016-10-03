package com.lcjian.wechatsimulation;

import android.app.Application;

import timber.log.Timber;

public class APP extends Application {

    private static APP INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static APP getInstance() {
        return INSTANCE;
    }
}
