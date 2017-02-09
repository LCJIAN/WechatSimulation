package com.lcjian.wechatsimulation.job;

import com.lcjian.wechatsimulation.entity.JobData;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJob implements Job {

    private JobData jobData;

    private boolean cancelled;

    private boolean error;

    private boolean finished;

    private List<JobListener> mJobListeners;

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
    public final synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public final synchronized boolean isError() {
        return error;
    }

    @Override
    public final synchronized boolean isFinished() {
        return finished;
    }

    @Override
    public void addJobListener(JobListener listener) {
        if (mJobListeners == null) {
            mJobListeners = new ArrayList<>();
        }
        this.mJobListeners.add(listener);
    }

    @Override
    public void removeJobListener(JobListener listener) {
        if (mJobListeners != null) {
            mJobListeners.remove(listener);
        }
    }

    public void notifyFinish() {
        synchronized (this) {
            if (!isFinished() && !isCancelled() && !isError()) {
                finished = true;
                if (mJobListeners != null) {
                    for (JobListener listener : mJobListeners) {
                        listener.onFinished();
                    }
                }
            }
        }
    }

    public void notifyCancel() {
        synchronized (this) {
            if (!isFinished() && !isCancelled() && !isError()) {
                cancelled = true;
                if (mJobListeners != null) {
                    for (JobListener listener : mJobListeners) {
                        listener.onCancelled();
                    }
                }
            }
        }
    }

    public void notifyError(Throwable t) {
        synchronized (this) {
            if (!isFinished() && !isCancelled() && !isError()) {
                error = true;
                if (mJobListeners != null) {
                    for (JobListener listener : mJobListeners) {
                        listener.onError(t);
                    }
                }
            }
        }
    }

}
