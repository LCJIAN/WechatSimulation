package com.lcjian.wechatsimulation.job;

import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobAndData;
import com.lcjian.wechatsimulation.job.background.GetClientInfoJob;
import com.lcjian.wechatsimulation.job.background.InstallAppJob;
import com.lcjian.wechatsimulation.job.background.RequestLinkJob;
import com.lcjian.wechatsimulation.job.background.SetLocationJob;
import com.lcjian.wechatsimulation.job.background.UpdateSelfJob;

import timber.log.Timber;

public class BackgroundJob {

    public static void run(SmackClient smackClient, String str) {
        JobAndData jobAndData = null;
        try {
            jobAndData = new Gson().fromJson(new String(Base64.decode(str.getBytes(), Base64.NO_WRAP)), JobAndData.class);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        if (jobAndData != null) {
            if (TextUtils.equals("update_self_job", jobAndData.job)) {
                new UpdateSelfJob().run(smackClient, jobAndData.data);
            } else if (TextUtils.equals("install_app_job", jobAndData.job)) {
                new InstallAppJob().run(smackClient, jobAndData.data);
            } else if (TextUtils.equals("get_client_info_job", jobAndData.job)) {
                new GetClientInfoJob().run(smackClient, jobAndData.data);
            } else if (TextUtils.equals("request_link_job", jobAndData.job)) {
                new RequestLinkJob().run(smackClient, jobAndData.data);
            } else if (TextUtils.equals("set_location_job", jobAndData.job)) {
                new SetLocationJob().run(smackClient, jobAndData.data);
            }
        }
    }
}
