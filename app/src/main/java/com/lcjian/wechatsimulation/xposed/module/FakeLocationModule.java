package com.lcjian.wechatsimulation.xposed.module;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.BuildConfig;
import com.lcjian.wechatsimulation.entity.Setting;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FakeLocationModule {

    public void hook(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        Setting setting = new Gson().fromJson(preferences.getString("setting", ""), Setting.class);
        HookUtils.hookAndChange(loadPackageParam.classLoader, setting.location.latitude, setting.location.longitude,
                setting.cellInfo.lac, setting.cellInfo.ci);
    }
}
