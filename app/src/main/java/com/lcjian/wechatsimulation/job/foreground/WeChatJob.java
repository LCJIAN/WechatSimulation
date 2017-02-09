package com.lcjian.wechatsimulation.job.foreground;

import com.lcjian.wechatsimulation.job.BaseJob;

public class WeChatJob extends BaseJob {

    @Override
    public String getComponentPackageName() {
        return "com.tencent.mm";
    }

    @Override
    public String getComponentClassName() {
        return "com.tencent.mm.ui.LauncherUI";
    }
}
