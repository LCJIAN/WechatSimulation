package com.lcjian.wechatsimulation.xposed.module;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EventModule {

    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            XposedBridge.log(activity.getClass().getName() + ": " + ToStringBuilder.reflectionToString(activity.getIntent()));
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

    public void hook(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        XposedHelpers.findAndHookMethod(loadPackageParam.appInfo.className, loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application application = (Application) param.thisObject;
                application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
                application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
                super.afterHookedMethod(param);
            }
        });
    }
}
