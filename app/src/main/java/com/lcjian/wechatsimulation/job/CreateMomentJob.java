package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class CreateMomentJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_DISCOVER = 2;
    private static final int STATE_MOMENTS = 3;
    private static final int STATE_SELECT_MOMENT_TYPES = 4;
    private static final int STATE_SELECT_PHOTOS = 5;
    private static final int STATE_SELECTING_PHOTOS = 51;

    private static final int STATE_UPLOAD = 6;
    private static final int STATE_MOMENT_TEXT_FOCUSED = 7;

    private static final int STATE_MOMENT_TEXT_SET = 8;

    private int mState;

    private File mDestination;

    @Override
    public void doWithEvent(final AccessibilityService service, AccessibilityEvent event) {
        if (mDestination == null) {
            mDestination = service.getExternalFilesDir("download");
        }
        try {
            final int eventType = event.getEventType();
            final String className = event.getClassName().toString();
            if (mState == STATE_NONE
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.LauncherUI".equals(className)) {

                mState = STATE_HOME;
                List<AccessibilityNodeInfo> nodeInfo = event.getSource().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bhj");
                if (nodeInfo != null && !nodeInfo.isEmpty()) {
                    for (AccessibilityNodeInfo item : nodeInfo) {
                        if ("发现".equals(item.getText().toString())) {
                            AccessibilityHelper.performClick(item);
                            break;
                        }
                    }
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_HOME
                    && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && event.getText().toString().contains("发现")
                    && "android.widget.RelativeLayout".equals(className)) {

                mState = STATE_HOME_DISCOVER;
                AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                if (list != null) {
                    AccessibilityHelper.performClick(list.getChild(1));
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_HOME_DISCOVER
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(className)) {

                mState = STATE_MOMENTS;
                Observable.defer(new Func0<Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call() {
                        boolean result = true;
                        for (String url : getJobData().momentPhotos) {
                            result = result && DownloadUtils.download(url, mDestination);
                        }
                        return Observable.just(result);
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if (aBoolean) {
                                    service.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mDestination)));
                                    AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "更多功能按钮");
                                    if (nodeInfo != null) {
                                        AccessibilityHelper.performClick(nodeInfo);
                                    } else {
                                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                                    }
                                } else {
                                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                notifyError(throwable);
                            }
                        });
            } else if (mState == STATE_MOMENTS
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.base.k".equals(className)) {

                mState = STATE_SELECT_MOMENT_TYPES;
                AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "照片");
                if (nodeInfo != null) {
                    AccessibilityHelper.performClick(nodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if ((mState == STATE_SELECT_MOMENT_TYPES
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(className))
                    || (mState == STATE_SELECTING_PHOTOS
                    && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && "android.view.View".equals(className))) {

                mState = STATE_SELECT_PHOTOS;
                AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/c1x");
                if (list != null) {
                    for (int i = 1; i < list.getChildCount(); i++) {
                        AccessibilityNodeInfo checkBox = AccessibilityHelper.findNodeInfoById(list.getChild(i), "com.tencent.mm:id/ayx");
                        if (checkBox != null && !checkBox.isChecked()) {
                            mState = STATE_SELECTING_PHOTOS;
                            AccessibilityHelper.performClick(AccessibilityHelper.findNodeInfoById(list.getChild(i), "com.tencent.mm:id/ayy"));
                            break;
                        }
                    }
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
                if (mState == STATE_SELECT_PHOTOS) {
                    AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "完成");
                    if (nodeInfo != null) {
                        AccessibilityHelper.performClick(nodeInfo);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if (mState == STATE_SELECT_PHOTOS
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.sns.ui.SnsUploadUI".equals(className)) {

                mState = STATE_UPLOAD;
                AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/b5e");
                if (nodeInfo != null) {
                    final Rect outBounds = new Rect();
                    nodeInfo.getBoundsInScreen(outBounds);
                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_UPLOAD
                    && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && "android.widget.EditText".equals(className)) {

                mState = STATE_MOMENT_TEXT_FOCUSED;
                ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("moment_text", getJobData().momentText));
                event.getSource().performAction(AccessibilityNodeInfo.ACTION_PASTE);
            } else if (mState == STATE_MOMENT_TEXT_FOCUSED
                    && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                    && "android.widget.EditText".equals(className)) {

                mState = STATE_MOMENT_TEXT_SET;
                AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "发送");
                if (nodeInfo != null) {
                    AccessibilityHelper.performClick(nodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_MOMENT_TEXT_SET
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(className)) {
                notifyFinish();
            }
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }
}
