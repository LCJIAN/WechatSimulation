package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.Locale;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 搜索加好友 2016-11-09 completed
 *
 * @author LCJ
 */
public class AddFriendBySearchMobileJob extends WeChatJob {

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
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {
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
                        AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(event.getSource()), new TextNodeFilter("添加朋友")),
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            } else if (mState == STATE_HOME_MORE
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.subapp.ui.pluginapp.AddMoreFriendsUI".equals(mEventClassName)) {

                mState = STATE_ADD_MORE_FRIENDS;
                final Rect outBounds = new Rect();
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("微信号/QQ号/手机号")).getBoundsInScreen(outBounds);
                Observable.just(true)
                        .observeOn(Schedulers.io())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                            }
                        }, mAction1Throwable);
            } else if (mState == STATE_ADD_MORE_FRIENDS
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.search.ui.FTSAddFriendUI".equals(mEventClassName)) {

                mState = STATE_ADD_FRIEND;
                ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("mobile", getJobData().mobile));
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                        new TextNodeFilter("搜索").and(new ClassNameNodeFilter("android.widget.EditText"))).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
            } else if (mState == STATE_ADD_FRIEND
                    && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(mEventClassName)) {

                mState = STATE_ADD_FRIEND_INPUT_FINISH;
            } else if (mState == STATE_ADD_FRIEND_INPUT_FINISH
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && "android.widget.TextView".equals(mEventClassName)) {

                mState = STATE_ADD_FRIEND_INPUT_READY;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .getChild(0).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            } else if (mState == STATE_ADD_FRIEND_INPUT_READY
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.search.ui.FTSAddFriendUI".equals(mEventClassName)) {

                if (AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("该用户不存在")) != null) {
                    throw new JobException(JobException.MESSAGE_NO_THIS_USER_ERROR);
                }
            } else if (mState == STATE_ADD_FRIEND_INPUT_READY
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.base.p".equals(mEventClassName)) {

                mState = STATE_SEARCHING_CONTACT;
            } else if (mState == STATE_SEARCHING_CONTACT
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(mEventClassName)) {

                mState = STATE_CONTACT;
                if (AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发消息")) == null) {
                    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("添加到通讯录")),
                            AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                } else {
                    throw new JobException(JobException.MESSAGE_HAVE_BEEN_FRIEND_ERROR);
                }
            } else if (mState == STATE_CONTACT
                    && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI".equals(mEventClassName)) {

                mState = STATE_SAY_HI;
                final Rect outBounds = new Rect();
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText")).getBoundsInScreen(outBounds);
                Observable.just(true)
                        .observeOn(Schedulers.io())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                ShellUtils.execCommand(
                                        String.format(Locale.getDefault(),
                                                "input tap %d %d",
                                                outBounds.right - DimenUtils.dipToPixels(30, mService),
                                                outBounds.centerY()),
                                        true);
                            }
                        }, mAction1Throwable);
            } else if (mState == STATE_SAY_HI
                    && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && "android.widget.EditText".equals(mEventClassName)) {

                mState = STATE_VALIDATION_MESSAGE_SET;
                ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", getJobData().validationMessage));
                new AccessibilityNodeInfoCompat(event.getSource()).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
            } else if (mState == STATE_VALIDATION_MESSAGE_SET
                    && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                    && "android.widget.EditText".equals(mEventClassName)) {

                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发送")),
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                notifyFinish();
            }
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

}
