package com.lcjian.wechatsimulation.entity;

public class Response {

    public int code;

    public String message;

    public JobData data;

    public Response(int code, String message, JobData data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
