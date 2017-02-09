package com.lcjian.wechatsimulation.job.background;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.PackageUtils;

import java.io.File;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class InstallAppJob {

    private File mDestination = APP.getInstance().getExternalFilesDir("download");

    public void run(final SmackClient smackClient, final JobData jobData) {
        Observable.just(jobData.apkUrl)
                .map(new Func1<String, File>() {
                    @Override
                    public File call(String s) {
                        FileUtils.deleteDir(mDestination);
                        return DownloadUtils.download(s, mDestination);
                    }
                })
                .map(new Func1<File, Integer>() {
                    @Override
                    public Integer call(File file) {
                        if (file != null) {
                            return PackageUtils.installSilent(APP.getInstance(), file.getAbsolutePath());
                        } else {
                            throw new RuntimeException("Download error");
                        }
                    }
                })
                .map(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        return integer == PackageUtils.INSTALL_SUCCEEDED;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            smackClient.sendMessage(new Gson().toJson(new Response(0, "Job was finished", jobData)));
                        } else {
                            smackClient.sendMessage(new Gson().toJson(new Response(2, "update WeChat failed", jobData)));
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        smackClient.sendMessage(new Gson().toJson(new Response(2, throwable.getMessage(), jobData)));
                    }
                });
    }
}
