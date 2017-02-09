package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * 自动接受好友请求 2016-11-09 completed
 *
 * @author LCJ
 */
public class AcceptFriendRequestJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS_LOADING = 11;
    private static final int STATE_HOME_CONTACTS = 12;
    private static final int STATE_HOME_CONTACTS_PERFORMED_SCROLL = 2;
    private static final int STATE_NEW_FRIENDS_ACCEPT_CLICK = 21;
    private static final int STATE_CONTACT_INFO = 3;

    private int mState = STATE_NONE;

    private Subscription mSubscription;

    private Subscription mDebSubscription;

    private int mScrollTimes;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
            handleFromHomeToContacts();
            handleAcceptRequest();
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

    private void handleAcceptRequest() {
        if (mState == STATE_HOME_CONTACTS_LOADING
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            mState = STATE_HOME_CONTACTS;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("新的朋友")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (((mState == STATE_HOME_CONTACTS || mState == STATE_CONTACT_INFO)
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.subapp.ui.friend.FMessageConversationUI".equals(mEventClassName))
                || (mState == STATE_HOME_CONTACTS_PERFORMED_SCROLL
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            if (mDebSubscription != null) {
                mDebSubscription.unsubscribe();
            }
            mDebSubscription = Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoCompat newFriends = AccessibilityNodeInfoUtils.searchFromBfs(mService,
                                    mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
                            AccessibilityNodeInfoCompat accept = AccessibilityNodeInfoUtils.searchFromBfs(mService, newFriends,
                                    new TextNodeFilter("接受").and(new ClassNameNodeFilter("android.widget.Button")));
                            if (accept == null) {
                                if (mScrollTimes < 10) {
                                    newFriends.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                                    mSubscription = Observable.just(true).delay(5, TimeUnit.SECONDS)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new Action1<Boolean>() {
                                                @Override
                                                public void call(Boolean aBoolean) {
                                                    notifyFinish();
                                                }
                                            }, mAction1Throwable);
                                    mState = STATE_HOME_CONTACTS_PERFORMED_SCROLL;
                                    mScrollTimes++;
                                } else {
                                    notifyFinish();
                                }
                            } else {
                                mState = STATE_NEW_FRIENDS_ACCEPT_CLICK;
                                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, accept,
                                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            }
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_NEW_FRIENDS_ACCEPT_CLICK
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(mEventClassName)) {

            mState = STATE_CONTACT_INFO;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("返回")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }
}
