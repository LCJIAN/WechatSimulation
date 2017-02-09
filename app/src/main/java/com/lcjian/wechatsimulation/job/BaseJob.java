package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityServiceCompatUtils;

import rx.functions.Action1;

public abstract class BaseJob extends AbstractJob {

    protected AccessibilityService mService;
    protected AccessibilityEvent mEvent;
    protected AccessibilityNodeInfoCompat mRootNodeInfoCompat;
    protected int mEventType;
    protected String mEventClassName;
    protected Action1<Throwable> mAction1Throwable = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            notifyError(throwable);
        }
    };

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        mService = service;
        mEvent = event;
        mRootNodeInfoCompat = AccessibilityServiceCompatUtils.getRootInActiveWindow(service);
        mEventType = event.getEventType();
        mEventClassName = event.getClassName().toString();
    }
}
