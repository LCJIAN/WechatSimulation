package com.lcjian.wechatsimulation;

import android.util.Log;

import com.umeng.analytics.MobclickAgent;

import timber.log.Timber;

class ErrorTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority >= Log.ERROR && t != null) {
            MobclickAgent.reportError(APP.getInstance(), t);
        }
    }
}
