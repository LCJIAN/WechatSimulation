package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;

import java.io.File;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class ModifyAccountInfoJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_ME = 2;
    private static final int STATE_SETTINGS_PERSONAL_INFO = 3;
    private static final int STATE_ALBUM_PREVIEW = 4;
    private static final int STATE_CROP_IMAGE_NEW = 5;

    private static final int STATE_SETTINGS_PERSONAL_INFO_1 = 6;
    private static final int STATE_SETTINGS_MODIFY_NAME = 7;
    private static final int STATE_SETTINGS_MODIFY_NAME_SET = 71;
    private static final int STATE_SETTINGS_MODIFY_NAME_SAVE = 72;

    private static final int STATE_SETTINGS_PERSONAL_INFO_2 = 8;
    private static final int STATE_SETTINGS_GENDER = 81;
    private static final int STATE_SETTINGS_GENDER_SAVE = 82;

    private static final int STATE_SETTINGS_PERSONAL_INFO_3 = 9;
    private static final int STATE_MULTI_STAGE_CITY_SELECT = 91;
    private static final int STATE_MULTI_STAGE_CITY_SAVE = 92;

    private static final int STATE_SETTINGS_PERSONAL_INFO_4 = 10;
    private static final int STATE_SETTINGS_MODIFY_SIGN = 101;
    private static final int STATE_SETTINGS_MODIFY_SIGN_SET = 102;
    private static final int STATE_SETTINGS_MODIFY_SIGN_SAVE = 104;

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
                        if ("我".equals(item.getText().toString())) {
                            AccessibilityHelper.performClick(item);
                            break;
                        }
                    }
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_HOME
                    && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    && event.getText().toString().contains("我")
                    && "android.widget.RelativeLayout".equals(className)) {
                mState = STATE_HOME_ME;

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/bl6");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(AccessibilityHelper.findParentNodeInfoByClassName(accessibilityNodeInfo, "android.widget.LinearLayout"));
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            }
            handleModifyAvatar(service, event);
            handleModifyName(service, event);
            handleModifyGender(service, event);
            handleModifyRegion(service, event);
            handleModifySign(service, event);
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleModifyAvatar(final AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_HOME_ME
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {
            mState = STATE_SETTINGS_PERSONAL_INFO;

            Observable.defer(new Func0<Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call() {
                    return Observable.just(DownloadUtils.download(getJobData().accountAvatar, mDestination));
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (aBoolean) {
                                service.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mDestination)));
                                AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                                if (list != null) {
                                    AccessibilityHelper.performClick(list.getChild(1));
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
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(className)) {

            mState = STATE_ALBUM_PREVIEW;
            AccessibilityNodeInfo photos = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/c1x");
            if (photos != null) {
                if (photos.getChildCount() > 1) {
                    AccessibilityHelper.performClick(photos.getChild(1));
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_ALBUM_PREVIEW
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.CropImageNewUI".equals(className)) {

            mState = STATE_CROP_IMAGE_NEW;
            AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "使用");
            if (nodeInfo != null) {
                AccessibilityHelper.performClick(nodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        }
    }

    private void handleModifyName(final AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_CROP_IMAGE_NEW
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_1;
            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(2));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_1
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsModifyNameUI".equals(className)) {

            mState = STATE_SETTINGS_MODIFY_NAME;
            ShellUtils.execCommand("input keyevent 67", true);

        } else if (mState == STATE_SETTINGS_MODIFY_NAME
                && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(className)) {
            if (TextUtils.isEmpty(event.getSource().getText())) {

                mState = STATE_SETTINGS_MODIFY_NAME_SET;
                ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("account_name", getJobData().accountName));
                event.getSource().performAction(AccessibilityNodeInfo.ACTION_PASTE);
            } else {
                ShellUtils.execCommand("input keyevent 67", true);
            }
        } else if (mState == STATE_SETTINGS_MODIFY_NAME_SET
                && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(className)) {

            mState = STATE_SETTINGS_MODIFY_NAME_SAVE;
            AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "保存");
            if (nodeInfo != null) {
                AccessibilityHelper.performClick(nodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        }
    }

    private void handleModifyGender(final AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_SETTINGS_MODIFY_NAME_SAVE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_2;
            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(7));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_2
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(className)) {

            mState = STATE_SETTINGS_GENDER;
        } else if (mState == STATE_SETTINGS_GENDER
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className)) {

            mState = STATE_SETTINGS_GENDER_SAVE;
            AccessibilityHelper.performClick(event.getSource().getChild(getJobData().accountGender));
        }
    }

    private void handleModifyRegion(final AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_SETTINGS_GENDER_SAVE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_3;
            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(8));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_3
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.MultiStageCitySelectUI".equals(className)) {

            mState = STATE_MULTI_STAGE_CITY_SELECT;
            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(3));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_MULTI_STAGE_CITY_SELECT
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.MultiStageCitySelectUI".equals(className)) {

            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(new Random().nextInt(list.getChildCount() - 1) + 1));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_MULTI_STAGE_CITY_SELECT
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {

            mState = STATE_MULTI_STAGE_CITY_SAVE;
        }
    }

    private void handleModifySign(final AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_MULTI_STAGE_CITY_SAVE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_4;
            AccessibilityNodeInfo list = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
            if (list != null) {
                AccessibilityHelper.performClick(list.getChild(9));
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_4
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.EditSignatureUI".equals(className)) {

            mState = STATE_SETTINGS_MODIFY_SIGN;
            ShellUtils.execCommand("input keyevent 67", true);
        } else if (mState == STATE_SETTINGS_MODIFY_SIGN
                && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(className)) {
            if (TextUtils.isEmpty(event.getSource().getText())) {

                mState = STATE_SETTINGS_MODIFY_SIGN_SET;
                ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("account_sign", getJobData().accountSign));
                event.getSource().performAction(AccessibilityNodeInfo.ACTION_PASTE);
            } else {
                ShellUtils.execCommand("input keyevent 67", true);
            }
        } else if (mState == STATE_SETTINGS_MODIFY_SIGN_SET
                && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(className)) {

            mState = STATE_SETTINGS_MODIFY_SIGN_SAVE;
            AccessibilityNodeInfo nodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "保存");
            if (nodeInfo != null) {
                AccessibilityHelper.performClick(nodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SETTINGS_MODIFY_SIGN_SAVE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(className)) {
            notifyFinish();
        }
    }

}
