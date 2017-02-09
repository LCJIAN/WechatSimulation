package com.lcjian.wechatsimulation.job.background;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.utils.DownloadUtils;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RequestLinkJob {

    public void run(final SmackClient smackClient, final JobData jobData) {
        Observable.just(jobData)
                .map(new Func1<JobData, JobData>() {
                    @Override
                    public JobData call(JobData data) {
                        if (data.method != null && TextUtils.equals("post", data.method.toLowerCase())) {
                            data.response = DownloadUtils.post(data.url, data.headers, data.params);
                        } else {
                            data.response = DownloadUtils.get(data.url, data.headers, data.params);
                        }
                        return data;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<JobData>() {
                    @Override
                    public void call(JobData data) {
                        smackClient.sendMessage(new Gson().toJson(new Response(0, "Job was finished", data)));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        smackClient.sendMessage(new Gson().toJson(new Response(2, throwable.getMessage(), jobData)));
                    }
                });
    }
}
