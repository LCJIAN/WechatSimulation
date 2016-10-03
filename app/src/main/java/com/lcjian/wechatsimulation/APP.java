package com.lcjian.wechatsimulation;

import android.app.Application;

import com.lcjian.wechatsimulation.tinker.BuildInfo;

import timber.log.Timber;

public class APP extends Application {

    private static APP INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        if (BuildInfo.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static APP getInstance() {
        return INSTANCE;
    }
}
