package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.entity.JobData;

public interface Job {

    void doWithEvent(AccessibilityService service, AccessibilityEvent event);

    void cancel();

    JobData getJobData();

    void setJobData(JobData jobData);

    boolean isCancelled();

    boolean isFinished();

    boolean isError();

    void setJobListener(JobListener listener);

    interface JobListener {

        void onCancelled();

        void onFinished();

        void onError(Throwable t);
    }

}
