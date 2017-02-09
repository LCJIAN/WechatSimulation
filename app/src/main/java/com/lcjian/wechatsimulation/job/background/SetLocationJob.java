package com.lcjian.wechatsimulation.job.background;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.entity.Setting;
import com.lcjian.wechatsimulation.utils.DownloadUtils;

import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SetLocationJob {

    public void run(final SmackClient smackClient, final JobData jobData) {

        Observable.just(jobData)
                .map(new Func1<JobData, JobData>() {
                    @SuppressLint("CommitPrefEdits")
                    @Override
                    public JobData call(JobData data) {
                        if (data.latitude == null || data.longitude == null) {
                            throw new RuntimeException("Required String parameter latitude and longitude");
                        } else {
                            HashMap<String, String> params = new HashMap<>();
                            params.put("lat", String.valueOf(data.latitude));
                            params.put("lon", String.valueOf(data.longitude));
                            Setting setting = new Setting();
                            setting.location = new Setting.Location();
                            setting.location.latitude = data.latitude;
                            setting.location.longitude = data.longitude;
                            List<Setting.CellInfo> cellInfoList = new Gson().fromJson(
                                    DownloadUtils.get("http://api.cellocation.com/recell/?incoord=gcj02&coord=gcj02", null, params),
                                    new TypeToken<List<Setting.CellInfo>>() {}.getType());
                            if (cellInfoList != null && !cellInfoList.isEmpty()) {
                                setting.cellInfo = cellInfoList.get(0);
                            }
                            PreferenceManager.getDefaultSharedPreferences(APP.getInstance()).edit().putString("setting", new Gson().toJson(setting)).commit();
                            return data;
                        }
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
