package com.lcjian.wechatsimulation.job.background;

import com.google.gson.Gson;
import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.SmackClient;
import com.lcjian.wechatsimulation.entity.JobData;
import com.lcjian.wechatsimulation.entity.Response;
import com.lcjian.wechatsimulation.service.SimulationService;
import com.lcjian.wechatsimulation.utils.PackageUtils2;
import com.lcjian.wechatsimulation.utils.ShellUtils;

public class GetClientInfoJob {

    public void run(SmackClient smackClient, JobData data) {
        String weChatVersionName = PackageUtils2.getVersionName(APP.getInstance(), "com.tencent.mm");
        String appVersionName = PackageUtils2.getVersionName(APP.getInstance());
        boolean haveRootPermission = ShellUtils.checkRootPermission();
        boolean accessibilityOpened = SimulationService.isRunning();

        data.weChatVersionName = weChatVersionName;
        data.appVersionName = appVersionName;
        data.haveRootPermission = haveRootPermission;
        data.accessibilityOpened = accessibilityOpened;

        smackClient.sendMessage(new Gson().toJson(new Response(0, "Job was finished", data)));
    }
}
