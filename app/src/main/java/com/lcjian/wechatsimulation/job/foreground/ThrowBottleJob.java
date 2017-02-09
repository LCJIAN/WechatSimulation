package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import timber.log.Timber;

/**
 * 扔漂流瓶 2016-11-11 uncompleted
 *
 * @author LCJ
 */
public class ThrowBottleJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_DISCOVER = 2;
    private static final int STATE_BOTTLE = 3;

    private int mState = STATE_NONE;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
            handleFromHomeToBottle();
            handleBottle();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToBottle() {
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
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("漂流瓶")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleBottle() {
        if (mState == STATE_HOME_DISCOVER
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.bottle.ui.BottleBeachUI".equals(mEventClassName)) {

            mState = STATE_BOTTLE;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("开始查看")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }
}
