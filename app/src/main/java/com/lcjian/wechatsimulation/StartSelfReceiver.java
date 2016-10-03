package com.lcjian.wechatsimulation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.lcjian.wechatsimulation.service.JobService;

public class StartSelfReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            String username = context.getSharedPreferences("user_info", Context.MODE_APPEND).getString("username", "");
            if (!TextUtils.isEmpty(username)) {
                context.startService(new Intent(context, JobService.class)
                        .putExtra("start", true)
                        .putExtra("username", username)
                        .putExtra("password", "123456"));
            }
        }
    }
}