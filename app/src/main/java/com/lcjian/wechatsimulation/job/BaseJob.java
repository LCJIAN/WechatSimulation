package com.lcjian.wechatsimulation.job;

import com.lcjian.wechatsimulation.entity.JobData;

abstract class BaseJob implements Job {

    private JobData jobData;

    private boolean cancelled;

    private boolean error;

    private boolean finished;

    private JobListener mJobListener;

    @Override
    public void cancel() {
        notifyCancel();
    }

    public JobData getJobData() {
        return jobData;
    }

    public void setJobData(JobData jobData) {
        this.jobData = jobData;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isError() {
        return error;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void setJobListener(JobListener listener) {
        this.mJobListener = listener;
    }

    void notifyFinish() {
        if (!isFinished() && !isCancelled() && !isError()) {
            finished = true;
            mJobListener.onFinished();
        }
    }

    void notifyCancel() {
        if (!isFinished() && !isCancelled() && !isError()) {
            cancelled = true;
            mJobListener.onCancelled();
        }
    }

    void notifyError(Throwable t) {
        if (!isFinished() && !isCancelled() && !isError()) {
            error = true;
            mJobListener.onError(t);
        }
    }

}
