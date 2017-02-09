package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;


/**
 * 发朋友圈 2016-11-11 completed
 *
 * @author LCJ
 */
public class CreateMomentJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_DISCOVER = 2;
    private static final int STATE_MOMENTS = 3;
    private static final int STATE_MOMENTS_TEXT_INTRO = 31;
    private static final int STATE_SELECT_MOMENT_TYPES = 4;
    private static final int STATE_SELECT_PHOTOS = 5;
    private static final int STATE_SELECTING_PHOTOS = 51;

    private static final int STATE_UPLOAD = 6;
    private static final int STATE_MOMENT_TEXT_FOCUSED = 7;

    private static final int STATE_MOMENT_TEXT_SET = 8;

    private int mState;

    private File mDestination;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        if (mDestination == null) {
            mDestination = mService.getExternalFilesDir("download");
        }
        try {
            handleFromHomeToMoments();
            if (getJobData().momentPhotos == null || getJobData().momentPhotos.isEmpty()) {
                handleMoment();
            } else {
                handleMomentPhotos();
            }
            handleCreateMoment();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToMoments() {
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
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("朋友圈")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleMoment() {
        if (mState == STATE_HOME_DISCOVER
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(mEventClassName)) {

            mState = STATE_MOMENTS;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多功能按钮")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
        } else if (mState == STATE_MOMENTS
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.sns.ui.SnsLongMsgUI".equals(mEventClassName)) {

            mState = STATE_MOMENTS_TEXT_INTRO;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("我知道了")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleMomentPhotos() {
        if (mState == STATE_HOME_DISCOVER
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(mEventClassName)) {

            mState = STATE_MOMENTS;
            Observable.defer(new Func0<Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call() {
                    FileUtils.deleteDir(mDestination);
                    boolean result = true;
                    for (String url : getJobData().momentPhotos) {
                        result = result && DownloadUtils.download(url, mDestination) != null;
                    }
                    if (result) {
                        mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mDestination)));
                    }
                    return Observable.just(result);
                }
            }).subscribeOn(Schedulers.io())
                    .delay(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (aBoolean) {
                                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多功能按钮")),
                                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            } else {
                                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                            }
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_MOMENTS
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.k".equals(mEventClassName)) {

            mState = STATE_SELECT_MOMENT_TYPES;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("照片")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_SELECT_MOMENT_TYPES
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(mEventClassName))
                || (mState == STATE_SELECTING_PHOTOS
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && "android.view.View".equals(mEventClassName))) {

            mState = STATE_SELECT_PHOTOS;
            AccessibilityNodeInfoCompat photos = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.GridView".equals(node.getClassName().toString()) && node.isFocused();
                }
            });
            for (int i = 1; i < photos.getChildCount(); i++) {
                AccessibilityNodeInfoCompat checkBox = AccessibilityNodeInfoUtils.searchFromBfs(mService, photos.getChild(i),
                        new ClassNameNodeFilter("android.widget.CheckBox").and(new NodeFilter() {
                            @Override
                            public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                return !node.isChecked();
                            }
                        }));
                if (checkBox != null) {
                    mState = STATE_SELECTING_PHOTOS;
                    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, photos.getChild(i), new ClassNameNodeFilter("android.view.View")),
                            AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                    break;
                }
            }
            if (mState == STATE_SELECT_PHOTOS) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                            @Override
                            public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                return AccessibilityNodeInfoUtils.getNodeText(node) != null && AccessibilityNodeInfoUtils.getNodeText(node).toString().contains("完成");
                            }
                        }),
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        }
    }

    private void handleCreateMoment() {
        if ((mState == STATE_MOMENTS || mState == STATE_MOMENTS_TEXT_INTRO || mState == STATE_SELECT_PHOTOS)
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.sns.ui.SnsUploadUI".equals(mEventClassName)) {

            mState = STATE_UPLOAD;
            final Rect outBounds = new Rect();
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.EditText")).getBoundsInScreen(outBounds);
            Observable.just(true)
                    .observeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_UPLOAD
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_MOMENT_TEXT_FOCUSED;
            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("moment_text", getJobData().momentText));
            new AccessibilityNodeInfoCompat(mEvent.getSource()).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_MOMENT_TEXT_FOCUSED
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_MOMENT_TEXT_SET;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发送")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_MOMENT_TEXT_SET
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(mEventClassName)) {
            notifyFinish();
        }
    }
}
