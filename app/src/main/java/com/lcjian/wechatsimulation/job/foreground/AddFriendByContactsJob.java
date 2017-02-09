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
 * 通讯录加好友 2016-11-09 completed
 *
 * @author LCJ
 */
public class AddFriendByContactsJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_ADD_MORE_FRIENDS = 3;
    private static final int STATE_OTHER_WAY = 4;
    private static final int STATE_MOBILE_FRIEND = 5;

    private int mState = STATE_NONE;

    private int mCount;

    private Subscription mSubscription;

    private Subscription mDebSubscription;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
            handleFromHomeToAddMoreFriends();
            handleFromAddMoreFriends();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToAddMoreFriends() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多功能按钮")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.FrameLayout".equals(mEventClassName)) {

            mState = STATE_HOME_MORE;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("添加朋友")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleFromAddMoreFriends() {
        if (mState == STATE_HOME_MORE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.subapp.ui.pluginapp.AddMoreFriendsUI".equals(mEventClassName)) {

            mState = STATE_ADD_MORE_FRIENDS;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("手机联系人")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME_MORE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.subapp.ui.pluginapp.AddMoreFriendsByOtherWayUI".equals(mEventClassName)) {

            mState = STATE_OTHER_WAY;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("添加手机联系人")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_OTHER_WAY
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.bindmobile.MobileFriendUI".equals(mEventClassName))
                || (mState == STATE_MOBILE_FRIEND
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && "android.widget.FrameLayout".equals(mEventClassName))
                || (mState == STATE_MOBILE_FRIEND
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            if (mDebSubscription != null) {
                mDebSubscription.unsubscribe();
            }
            mState = STATE_MOBILE_FRIEND;
            mDebSubscription = Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("暂无手机联系人")) != null) {
                                notifyFinish();
                            } else {
                                AccessibilityNodeInfoCompat mobileContacts = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                        new ClassNameNodeFilter("android.widget.ListView"));
                                AccessibilityNodeInfoCompat add = AccessibilityNodeInfoUtils.searchFromBfs(mService, mobileContacts,
                                        new ClassNameNodeFilter("android.widget.TextView").and(new TextNodeFilter("添加")));
                                if (add == null) {
                                    mobileContacts.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                                    mSubscription = Observable.just(true).delay(5, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new Action1<Boolean>() {
                                                @Override
                                                public void call(Boolean aBoolean) {
                                                    notifyFinish();
                                                }
                                            }, mAction1Throwable);
                                } else {
                                    if (mCount < 10) {
                                        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, add,
                                                AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                                        mCount++;
                                    } else {
                                        notifyFinish();
                                    }
                                }
                            }
                        }
                    }, mAction1Throwable);
        }
    }
}
