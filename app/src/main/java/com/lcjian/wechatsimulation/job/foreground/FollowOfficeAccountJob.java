package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 关注公众号 2016-11-14 completed
 *
 * @author LCJ
 */
public class FollowOfficeAccountJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_ADD_MORE_FRIENDS = 3;
    private static final int STATE_SEARCH = 4;
    private static final int STATE_INPUTTED = 5;
    private static final int STATE_SEARCH_RESULT = 6;

    private int mState = STATE_NONE;

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
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("公众号")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME_MORE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.webview.ui.tools.fts.FTSSearchTabWebViewUI".equals(mEventClassName)) {

            Observable.just(true).delay(2, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            mState = STATE_SEARCH;
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("office_account", getJobData().officeAccount));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_SEARCH
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_INPUTTED;
            Observable.just(true).delay(1, TimeUnit.SECONDS).observeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ShellUtils.execCommand("input keyevent " + KeyEvent.KEYCODE_ENTER, true);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.FrameLayout".equals(mEventClassName)) {

            mState = STATE_SEARCH_RESULT;
            final Rect outBounds = new Rect();
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.webkit.WebView")).getBoundsInScreen(outBounds);
            Observable.just(true)
                    .observeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ShellUtils.execCommand(
                                    String.format(Locale.getDefault(),
                                            "input tap %d %d",
                                            outBounds.centerX(),
                                            outBounds.top + DimenUtils.dipToPixels(48 + 72, mService)),
                                    true);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_SEARCH_RESULT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(mEventClassName)) {

            if (AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("进入公众号")) == null) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("关注")),
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                notifyFinish();
            } else {
                notifyFinish();
            }
        }
    }
}

