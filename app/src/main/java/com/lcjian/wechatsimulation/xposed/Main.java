package com.lcjian.wechatsimulation.xposed;

import android.text.TextUtils;

import com.lcjian.wechatsimulation.BuildConfig;
import com.lcjian.wechatsimulation.xposed.module.EventModule;
import com.lcjian.wechatsimulation.xposed.module.FakeLocationModule;
import com.lcjian.wechatsimulation.xposed.module.HideModule;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {

    public static final String WE_CHAT_PACKAGE_NAME = "com.tencent.mm";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (TextUtils.equals(loadPackageParam.packageName, BuildConfig.APPLICATION_ID)) {
            return;
        }
        if (TextUtils.equals(loadPackageParam.packageName, WE_CHAT_PACKAGE_NAME)) {
            new HideModule().hook(loadPackageParam);
            new EventModule().hook(loadPackageParam);
        }
        new FakeLocationModule().hook(loadPackageParam);
    }
}
