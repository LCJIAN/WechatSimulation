package com.lcjian.wechatsimulation.job;

import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;

import timber.log.Timber;

public class BackgroundJob {

    public static void run(SmackClient smackClient, String str) {
        BackgroundJob.JobAndData jobAndData = null;
        try {
            jobAndData = new Gson().fromJson(new String(Base64.decode(str.getBytes(), Base64.NO_WRAP)), BackgroundJob.JobAndData.class);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        if (jobAndData != null) {
            if (TextUtils.equals("update_self", jobAndData.job)) {
                new UpdateSelfJob().run(smackClient, jobAndData.data.apkUrl);
            } else if (TextUtils.equals("update_we_chat", jobAndData.job)) {
                new UpdateWeChatJob().run(smackClient, jobAndData.data.apkUrl);
            }
        }
    }

    private static class JobAndData {
        @SerializedName("job")
        String job;
        @SerializedName("data")
        JobData data;
    }
}
