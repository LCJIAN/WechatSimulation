package com.lcjian.wechatsimulation.job.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.jaredrummler.apkparser.ApkParser;
import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.Manager;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.PackageUtils;

import java.io.File;
import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class UpdateSelfJob {

    public void run(final SmackClient smackClient, final JobData jobData) {
        Observable.just(jobData.apkUrl)
                .map(new Func1<String, File>() {
                    @Override
                    public File call(String s) {
                        File destination = APP.getInstance().getExternalFilesDir("download");
                        FileUtils.deleteDir(destination);
                        return DownloadUtils.download(s, destination);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        if (file != null) {
                            restartProcess();
                            ApkParser apkParser = ApkParser.create(file);
                            try {
                                long versionCode = apkParser.getApkMeta().versionCode;
                                long currentVersionCode = APP.getInstance().getPackageManager().getPackageInfo(APP.getInstance().getPackageName(), 0).versionCode;
                                if (versionCode > currentVersionCode) {
                                    Manager.setUpdateVersionCode(versionCode, new Gson().toJson(jobData));
                                    PackageUtils.installSilent(APP.getInstance(), file.getAbsolutePath());
                                }
                            } catch (IOException | PackageManager.NameNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            throw new RuntimeException("Download error");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        smackClient.sendMessage(new Gson().toJson(new Response(2, throwable.getMessage(), jobData)));
                    }
                });
    }

    /**
     * you can restart your process through service or broadcast
     */
    private void restartProcess() {
        PendingIntent restartIntent = PendingIntent.getActivity(
                APP.getInstance(),
                0,
                APP.getInstance().getPackageManager().getLaunchIntentForPackage(
                        APP.getInstance().getPackageName()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager mgr = (AlarmManager) APP.getInstance().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000 * 60, restartIntent);
    }
}
