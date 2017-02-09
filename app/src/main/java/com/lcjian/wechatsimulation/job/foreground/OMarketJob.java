package com.lcjian.wechatsimulation.job.foreground;

import com.lcjian.wechatsimulation.job.BaseJob;

public class OMarketJob extends BaseJob {

    @Override
    public String getComponentPackageName() {
        return "com.oppo.market";
    }

    @Override
    public String getComponentClassName() {
        return "com.oppo.market.activity.MainActivity";
    }
}
