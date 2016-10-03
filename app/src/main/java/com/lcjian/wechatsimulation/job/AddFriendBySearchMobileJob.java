package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;
import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;

import java.util.Locale;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class AddFriendBySearchMobileJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_ADD_MORE_FRIENDS = 3;
    private static final int STATE_ADD_FRIEND = 4;
    private static final int STATE_ADD_FRIEND_INPUT_FINISH = 5;
    private static final int STATE_ADD_FRIEND_INPUT_READY = 6;
    private static final int STATE_SEARCHING_CONTACT = 7;
    private static final int STATE_CONTACT = 8;
    private static final int STATE_SAY_HI = 9;
    private static final int STATE_VALIDATION_MESSAGE_SET = 10;

    private int mState = STATE_NONE;

    @Override
    public void doWithEvent(final AccessibilityService service, AccessibilityEvent event) {
        try {
            final int eventType = event.getEventType();
            final String className = event.getClassName().toString();
            if (mState == STATE_NONE
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.LauncherUI".equals(className)) {

                mState = STATE_HOME;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/dp");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_HOME
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "android.widget.FrameLayout".equals(className)) {

                mState = STATE_HOME_MORE;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "添加朋友");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_HOME_MORE
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.subapp.ui.pluginapp.AddMoreFriendsUI".equals(className)) {

                mState = STATE_ADD_MORE_FRIENDS;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/ew");
                if (accessibilityNodeInfo != null) {
                    final Rect outBounds = new Rect();
                    accessibilityNodeInfo.getBoundsInScreen(outBounds);
                    Observable.just(true)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                                }
                            });
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_ADD_MORE_FRIENDS
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.search.ui.FTSAddFriendUI".equals(className)) {

                mState = STATE_ADD_FRIEND;
                ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("mobile", getJobData().mobile));
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/ew");
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_ADD_FRIEND
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className)) {

                mState = STATE_ADD_FRIEND_INPUT_FINISH;
            } else if (mState == STATE_ADD_FRIEND_INPUT_FINISH
                    && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && "android.widget.TextView".equals(className)) {

                mState = STATE_ADD_FRIEND_INPUT_READY;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/aq3");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo.getChild(0));
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_ADD_FRIEND_INPUT_READY
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.search.ui.FTSAddFriendUI".equals(className)) {

                if (AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/apx") != null) {
                    throw new JobException(JobException.MESSAGE_NO_THIS_USER_ERROR);
                }
            } else if (mState == STATE_ADD_FRIEND_INPUT_READY
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.base.p".equals(className)) {

                mState = STATE_SEARCHING_CONTACT;
            } else if (mState == STATE_SEARCHING_CONTACT
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(className)) {

                mState = STATE_CONTACT;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "添加到通讯录");
                if (AccessibilityHelper.findNodeInfoByText(event.getSource(), "发消息") != null) {
                    throw new JobException(JobException.MESSAGE_HAVE_BEEN_FRIEND_ERROR);
                }
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_CONTACT
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI".equals(className)) {

                mState = STATE_SAY_HI;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/c0y");
                if (accessibilityNodeInfo != null) {
                    final Rect outBounds = new Rect();
                    accessibilityNodeInfo.getBoundsInScreen(outBounds);
                    Observable.just(true)
                            .subscribeOn(Schedulers.io())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    ShellUtils.execCommand(
                                            String.format(Locale.getDefault(),
                                                    "input tap %d %d",
                                                    outBounds.right - DimenUtils.dipToPixels(30, service),
                                                    outBounds.centerY()),
                                            true);
                                }
                            });
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_SAY_HI
                    && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && "android.widget.EditText".equals(className)) {
                mState = STATE_VALIDATION_MESSAGE_SET;
                AccessibilityNodeInfo accessibilityNodeInfo = event.getSource();
                ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", getJobData().validationMessage));
                if (accessibilityNodeInfo != null) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_VALIDATION_MESSAGE_SET
                    && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                    && "android.widget.EditText".equals(className)) {
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/eg");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                    notifyFinish();
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            }
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

}
