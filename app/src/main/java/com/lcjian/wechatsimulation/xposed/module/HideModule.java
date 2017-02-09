package com.lcjian.wechatsimulation.xposed.module;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import com.lcjian.wechatsimulation.BuildConfig;
import com.lcjian.wechatsimulation.xposed.Main;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HideModule {

    public void hook(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List originApplicationInfoList = (List) param.getResult();
                List<ApplicationInfo> modifiedApplicationInfoList = new ArrayList<>();

                for (Object object : originApplicationInfoList) {
                    ApplicationInfo applicationInfo = (ApplicationInfo) object;
                    String packageName = applicationInfo.packageName;
                    if (isTarget(packageName)) {
                        XposedBridge.log("Hide Application: " + packageName);
                    } else {
                        modifiedApplicationInfoList.add(applicationInfo);
                    }
                }
                param.setResult(modifiedApplicationInfoList);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List originPackageInfoList = (List) param.getResult();
                List<PackageInfo> modifiedPackageInfoList = new ArrayList<>();

                for (Object object : originPackageInfoList) {
                    PackageInfo packageInfo = (PackageInfo) object;
                    String packageName = packageInfo.packageName;
                    if (isTarget(packageName)) {
                        XposedBridge.log("Hide Package: " + packageName);
                    } else {
                        modifiedPackageInfoList.add(packageInfo);
                    }
                }
                param.setResult(modifiedPackageInfoList);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ActivityManager", loadPackageParam.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List originRunningAppProcessInfoList = (List) param.getResult();
                List<ActivityManager.RunningAppProcessInfo> modifiedRunningAppProcessInfoList = new ArrayList<>();

                for (Object object : originRunningAppProcessInfoList) {
                    ActivityManager.RunningAppProcessInfo runningAppProcessInfo = (ActivityManager.RunningAppProcessInfo) object;
                    String processName = runningAppProcessInfo.processName;
                    if (isTarget(processName)) {
                        XposedBridge.log("Hide RunningAppProcess: " + processName);
                    } else {
                        modifiedRunningAppProcessInfoList.add(runningAppProcessInfo);
                    }
                }
                param.setResult(modifiedRunningAppProcessInfoList);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ActivityManager", loadPackageParam.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List originRunningServiceInfoList = (List) param.getResult();
                List<ActivityManager.RunningServiceInfo> modifiedRunningServiceInfoList = new ArrayList<>();

                for (Object object : originRunningServiceInfoList) {
                    ActivityManager.RunningServiceInfo runningServiceInfo = (ActivityManager.RunningServiceInfo) object;
                    String serviceName = runningServiceInfo.process;
                    if (isTarget(serviceName)) {
                        XposedBridge.log("Hide RunningService: " + serviceName);
                    } else {
                        modifiedRunningServiceInfoList.add(runningServiceInfo);
                    }
                }
                param.setResult(modifiedRunningServiceInfoList);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ActivityManager", loadPackageParam.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List originRunningTaskInfoList = (List) param.getResult();
                List<ActivityManager.RunningTaskInfo> modifiedRunningTaskInfoList = new ArrayList<>();

                for (Object object : originRunningTaskInfoList) {
                    ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) object;
                    String taskName = runningTaskInfo.baseActivity.flattenToString();
                    if (isTarget(taskName)) {
                        XposedBridge.log("Hide RunningTask: " + taskName);
                    } else {
                        modifiedRunningTaskInfoList.add(runningTaskInfo);
                    }
                }
                param.setResult(modifiedRunningTaskInfoList);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getPackageInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = (String) param.args[0];
                if (isTarget(packageName)) {
                    param.args[0] = Main.WE_CHAT_PACKAGE_NAME;
                    XposedBridge.log("Fake package: " + packageName + " as " + param.args[0]);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getApplicationInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = (String) param.args[0];
                if (isTarget(packageName)) {
                    param.args[0] = Main.WE_CHAT_PACKAGE_NAME;
                    XposedBridge.log("Fake package: " + packageName + " as " + param.args[0]);
                }
            }
        });
    }

    private static boolean isTarget(String target) {
        return target.contains(BuildConfig.APPLICATION_ID) || target.contains("xposed") || target.contains("lcjian");
    }
}
