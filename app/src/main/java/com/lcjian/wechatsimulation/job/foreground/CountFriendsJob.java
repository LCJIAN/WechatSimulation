package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import timber.log.Timber;

/**
 * 统计微信好友数量 2016-11-09 completed
 *
 * @author LCJ
 */
public class CountFriendsJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS_LOADING = 11;
    private static final int STATE_HOME_CONTACTS_PERFORMED_SCROLL = 12;

    private int mState = STATE_NONE;

    private Subscription mSubscription;

    private Subscription mDebSubscription;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
            handleFromHomeToContacts();
            handleCount();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToContacts() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("通讯录")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            mState = STATE_HOME_CONTACTS_LOADING;
            Observable.just(true).delay(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("通讯录")),
                            AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                }
            }, mAction1Throwable);
        }
    }

    private void handleCount() {
        if ((mState == STATE_HOME_CONTACTS_LOADING
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(mEventClassName))
                || (mState == STATE_HOME_CONTACTS_PERFORMED_SCROLL
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            if (mDebSubscription != null) {
                mDebSubscription.unsubscribe();
            }
            mDebSubscription = Observable.defer(new Func0<Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call() {
                    Observable<Boolean> result = Observable.just(true);
                    if (mState == STATE_HOME_CONTACTS_LOADING) {
                        result.delay(5, TimeUnit.SECONDS);
                    }
                    return result;
                }
            }).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                            new ClassNameNodeFilter("android.widget.ListView").and(new NodeFilter() {
                                @Override
                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                    return node.isFocused();
                                }
                            })).performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                    mState = STATE_HOME_CONTACTS_PERFORMED_SCROLL;

                    mSubscription = Observable.just(true).delay(5, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    AccessibilityNodeInfoCompat count = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                            new NodeFilter() {
                                                @Override
                                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                                    return AccessibilityNodeInfoUtils.getNodeText(node) != null
                                                            && AccessibilityNodeInfoUtils.getNodeText(node).toString().contains("位联系人");
                                                }
                                            });
                                    getJobData().friendsCount = Integer.parseInt(AccessibilityNodeInfoUtils.getNodeText(count).toString().replace("位联系人", "").trim());
                                    notifyFinish();
                                }
                            }, mAction1Throwable);
                }
            }, mAction1Throwable);
        }
    }
}
