package com.lcjian.wechatsimulation.job;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.PackageUtils;

import java.io.File;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class UpdateSelfJob {

    public void run(final SmackClient smackClient, String apkUrl) {
        Observable.just(apkUrl)
                .map(new Func1<String, File>() {
                    @Override
                    public File call(String s) {
                        return DownloadUtils.download(s, APP.getInstance().getExternalFilesDir("download"));
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        if (file != null) {
                            restartProcess();
                            PackageUtils.installSilent(APP.getInstance(), file.getAbsolutePath());
                        } else {
                            throw new RuntimeException("Download error");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        smackClient.sendMessage(new Gson().toJson(new Response(2, throwable.getMessage(), null)));
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
