package com.lcjian.wechatsimulation.entity;

import com.google.gson.annotations.SerializedName;

public class JobAndData {

    @SerializedName("job")
    public String job;
    @SerializedName("data")
    public JobData data;
}
