package com.lcjian.wechatsimulation.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class PackageUtils2 {

    public static boolean isFirstStartup(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int currentVersion = info.versionCode;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int lastVersion = prefs.getInt("version_code", 0);
            if (currentVersion > lastVersion) { // 如果当前版本大于上次版本，该版本属于第一次启动
                // 将当前版本写入preference中，则下次启动的时候，据此判断，不再为首次启动
                prefs.edit().putInt("version_code", currentVersion).apply();
                return true;
            } else {
                return false;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取versionCode
     *
     * @return 版本号
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取versionName
     *
     * @return 版本名字
     */
    public static String getVersionName(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取versionName
     *
     * @return 版本名字
     */
    public static String getVersionName(Context context, String packageName) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }
}
