package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * 打招呼 2016-11-11 uncompleted（过滤已打招呼的）
 *
 * @author LCJ
 */
public class AddFriendNearby extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_DISCOVER = 2;
    private static final int STATE_NEARBY = 3;
    private static final int STATE_NEARBY_TIP = 4;
    private static final int STATE_NEARBY_FRIENDS = 5;
    private static final int STATE_CONTACT_INFO = 6;
    private static final int STATE_SAY_HI = 7;
    private static final int STATE_CONTACT_INFO_AGAIN = 8;

    private int mState = STATE_NONE;

    private int mTotal = new Random().nextInt(6) + 5;

    private int mCount;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
            handleFromHomeToNearby();
            handleNearbyFriends();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToNearby() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发现")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("发现")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            mState = STATE_HOME_DISCOVER;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("附近的人")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME_DISCOVER
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.nearby.ui.NearbyFriendsIntroUI".equals(mEventClassName)) {

            mState = STATE_NEARBY;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("开始查看")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_HOME_DISCOVER || mState == STATE_NEARBY)
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            mState = STATE_NEARBY_TIP;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("确定")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleNearbyFriends() {
        if ((mState == STATE_NEARBY_TIP || mState == STATE_CONTACT_INFO_AGAIN)
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.nearby.ui.NearbyFriendsUI".equals(mEventClassName)) {

            mState = STATE_NEARBY_FRIENDS;
            Observable.just(true).delay(2, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoCompat nearby = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
                            if (mCount < mTotal && mCount < nearby.getChildCount()) {
                                nearby.getChild(mCount++).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            } else {
                                notifyFinish();
                            }
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_NEARBY_FRIENDS
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(mEventClassName)) {

            mState = STATE_CONTACT_INFO;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("打招呼")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_CONTACT_INFO
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.SayHiEditUI".equals(mEventClassName)) {

            mState = STATE_SAY_HI;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发送")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SAY_HI
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(mEventClassName)) {

            mState = STATE_CONTACT_INFO_AGAIN;
            mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }
}
