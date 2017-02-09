package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.Utils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 修改个人信息 2016-11-11 completed
 *
 * @author LCJ
 */
public class ModifyAccountInfoJob extends WeChatJob {

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
    public void doWithEvent(AccessibilityService service, AccessibilityEvent mEvent) {
        super.doWithEvent(service, mEvent);
        if (mDestination == null) {
            mDestination = service.getExternalFilesDir("download");
        }
        try {
            handleFromHomeToMe();
            handleModifyAvatar();
            handleModifyName();
            handleModifyGender();
            handleModifyRegion();
            handleModifySign();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToMe() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("我")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("我")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            mState = STATE_HOME_ME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
                    mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(
                            mService,
                            mRootNodeInfoCompat,
                            new ClassNameNodeFilter("android.widget.ImageView").and(new TextNodeFilter("查看二维码"))).getParent(),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleModifyAvatar() {
        if (mState == STATE_HOME_ME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {
            mState = STATE_SETTINGS_PERSONAL_INFO;

            Observable.defer(new Func0<Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call() {
                    FileUtils.deleteDir(mDestination);
                    boolean result = DownloadUtils.download(getJobData().accountAvatar, mDestination) != null;
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
                                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("头像")),
                                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
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
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(mEventClassName)) {

            mState = STATE_ALBUM_PREVIEW;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.GridView".equals(node.getClassName().toString()) && node.isFocused();
                }
            }).getChild(1).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_ALBUM_PREVIEW
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.CropImageNewUI".equals(mEventClassName)) {

            mState = STATE_CROP_IMAGE_NEW;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("使用")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleModifyName() {
        if (mState == STATE_CROP_IMAGE_NEW
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_1;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("昵称")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_1
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsModifyNameUI".equals(mEventClassName)) {

            mState = STATE_SETTINGS_MODIFY_NAME;
            Observable.just(true).delay(1, TimeUnit.SECONDS).observeOn(Schedulers.io()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    ShellUtils.execCommand("input keyevent 67", true);
                }
            }, mAction1Throwable);
        } else if (mState == STATE_SETTINGS_MODIFY_NAME
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {
            if (TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(new AccessibilityNodeInfoCompat(mEvent.getSource())))) {

                mState = STATE_SETTINGS_MODIFY_NAME_SET;
                ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("account_name", getJobData().accountName));
                new AccessibilityNodeInfoCompat(mEvent.getSource()).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
            } else {
                Observable.just(true).delay(1, TimeUnit.SECONDS).observeOn(Schedulers.io()).subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        ShellUtils.execCommand("input keyevent 67", true);
                    }
                }, mAction1Throwable);
            }
        } else if (mState == STATE_SETTINGS_MODIFY_NAME_SET
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_SETTINGS_MODIFY_NAME_SAVE;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("保存")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleModifyGender() {
        if (mState == STATE_SETTINGS_MODIFY_NAME_SAVE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_2;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("性别")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_2
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            mState = STATE_SETTINGS_GENDER;
        } else if (mState == STATE_SETTINGS_GENDER
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName)) {

            mState = STATE_SETTINGS_GENDER_SAVE;
            new AccessibilityNodeInfoCompat(mEvent.getSource()).getChild(Utils.getValue(getJobData().accountGender))
                    .performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleModifyRegion() {
        if (mState == STATE_SETTINGS_GENDER_SAVE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_3;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("地区")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SETTINGS_PERSONAL_INFO_3
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.MultiStageCitySelectUI".equals(mEventClassName)) {

            mState = STATE_MULTI_STAGE_CITY_SELECT;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.ListView".equals(node.getClassName().toString()) && node.isFocused();
                }
            }).getChild(3).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_MULTI_STAGE_CITY_SELECT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.tools.MultiStageCitySelectUI".equals(mEventClassName)) {

            AccessibilityNodeInfoCompat list = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.ListView".equals(node.getClassName().toString()) && node.isFocused();
                }
            });
            list.getChild(new Random().nextInt(list.getChildCount() - 1) + 1).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_MULTI_STAGE_CITY_SELECT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {

            mState = STATE_MULTI_STAGE_CITY_SAVE;
        }
    }

    private void handleModifySign() {
        if (mState == STATE_MULTI_STAGE_CITY_SAVE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {

            mState = STATE_SETTINGS_PERSONAL_INFO_4;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("个性签名")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_SETTINGS_PERSONAL_INFO_4
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.EditSignatureUI".equals(mEventClassName))
                || (mState == STATE_SETTINGS_MODIFY_SIGN
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName))) {

            mState = STATE_SETTINGS_MODIFY_SIGN;
            AccessibilityNodeInfoCompat sign = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText"));
            if (TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(sign))) {
                mState = STATE_SETTINGS_MODIFY_SIGN_SET;
                ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("account_sign", getJobData().accountSign));
                sign.performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
            } else {
                Observable.just(true).delay(1, TimeUnit.SECONDS).observeOn(Schedulers.io()).subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        ShellUtils.execCommand("input keyevent 67", true);
                    }
                }, mAction1Throwable);
            }
        } else if (mState == STATE_SETTINGS_MODIFY_SIGN_SET
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_SETTINGS_MODIFY_SIGN_SAVE;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("保存")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SETTINGS_MODIFY_SIGN_SAVE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI".equals(mEventClassName)) {
            notifyFinish();
        }
    }

}
