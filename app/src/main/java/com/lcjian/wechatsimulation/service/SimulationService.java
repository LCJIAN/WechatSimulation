package com.lcjian.wechatsimulation.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Parcelable;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.Constants;
import com.lcjian.wechatsimulation.RxBus;
import com.lcjian.wechatsimulation.job.Job;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class SimulationService extends AccessibilityService {

    private Job mJob;

    private CompositeSubscription mSubscriptions;

    private Subscription mSubscription;

    private static SimulationService service;

    @Override
    public void onCreate() {
        Timber.d("onCreate");
        mSubscriptions = new CompositeSubscription();
        ConnectableObservable<Object> eventEmitter = RxBus.getInstance().toObservable().publish();
        mSubscriptions.add(eventEmitter.subscribe(new Action1<Object>() {
            @Override
            public void call(Object event) {
                if (event instanceof Job) {
                    mJob = (Job) event;
                }
            }
        }));
        mSubscriptions.add(eventEmitter.connect());
        super.onCreate();
        service = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Timber.d(event.toString());
        final int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            final Parcelable data = event.getParcelableData();
            if (data != null && data instanceof Notification) {
                final CharSequence tickerText = ((Notification) data).tickerText;
                if (!TextUtils.isEmpty(tickerText) && tickerText.toString().contains(Constants.JOB)) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                    if (mSubscription != null) {
                        mSubscription.unsubscribe();
                    }
                    mSubscription = Observable.just(true).delay(3, TimeUnit.SECONDS).subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            try {
                                ((Notification) data).contentIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                                Timber.e(e, e.getMessage());
                            }
                            NotificationManagerCompat.from(SimulationService.this).cancelAll();
                        }
                    });
                }
            }
        }
        if (mJob != null && !mJob.isFinished() && !mJob.isCancelled() && !mJob.isError()) {
            mJob.doWithEvent(this, event);
        }
    }

    @Override
    public void onInterrupt() {
        Timber.d("onInterrupt");
        if (mJob != null) {
            mJob.cancel();
        }
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy");
        service = null;
        if (mSubscriptions != null) {
            mSubscriptions.unsubscribe();
        }
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
        super.onDestroy();
    }

    public static boolean isRunning() {
        return service != null;
    }
}
