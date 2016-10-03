package com.lcjian.wechatsimulation.job;

import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.lcjian.wechatsimulation.entity.JobData;

import timber.log.Timber;

public class JobFactory {

    public static Job createJob(String str) {
        JobAndData jobAndData = null;
        try {
            String body = new String(Base64.decode(str.getBytes(), Base64.NO_WRAP));
            Timber.d(body);
            jobAndData = new Gson().fromJson(body, JobAndData.class);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
        if (jobAndData == null) {
            return null;
        }
        Job job = null;
        if (TextUtils.equals("add_friend_by_search_mobile_job", jobAndData.job)) {
            job = new AddFriendBySearchMobileJob();
        } else if (TextUtils.equals("create_group_chat_job", jobAndData.job)) {
            job = new CreateGroupChatJob();
        } else if (TextUtils.equals("forward_sight_job", jobAndData.job)) {
            job = new ForwardSightJob();
        } else if (TextUtils.equals("get_room_qr_code_job", jobAndData.job)) {
            job = new GetRoomQrCodeJob();
        } else if (TextUtils.equals("modify_account_info_job", jobAndData.job)) {
            job = new ModifyAccountInfoJob();
        } else if (TextUtils.equals("create_moment_job", jobAndData.job)) {
            job = new CreateMomentJob();
        } else if (TextUtils.equals("view_article_job", jobAndData.job)) {
            job = new ViewArticleJob();
        }
        if (job != null) {
            job.setJobData(jobAndData.data);
        }
        return job;
    }

    private static class JobAndData {
        @SerializedName("job")
        String job;
        @SerializedName("data")
        JobData data;
    }
}
