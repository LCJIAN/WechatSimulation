package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * 获取群聊二维码 2016-11-14 completed
 *
 * @author LCJ
 */
public class GetRoomQrCodeJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS_LOADING = 2;
    private static final int STATE_HOME_CONTACTS = 21;
    private static final int STATE_CHAT_ROOM_CONTACT = 3;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND = 31;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND_NOT = 32;

    private static final int STATE_ROOM_CHATTING = 4;
    private static final int STATE_ROOM_INFO = 5;
    private static final int STATE_ROOM_INFO_QR_FIND = 51;
    private static final int STATE_ROOM_INFO_QR_FIND_NOT = 52;
    private static final int STATE_ROOM_QR = 6;
    private static final int STATE_SAVE_ROOM_QR = 7;

    private int mState;

    @Override
    public void doWithEvent(AccessibilityService mService, AccessibilityEvent mEvent) {
        super.doWithEvent(mService, mEvent);
        try {
            handleFromHomeToRoomChat();
            handleFromRoomChatToRoomInfoThenQr();
            handleRoomQr();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToRoomChat() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("通讯录")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            mState = STATE_HOME_CONTACTS_LOADING;
            Observable.just(true).delay(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("通讯录")),
                            AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                }
            }, mAction1Throwable);
        } else if (mState == STATE_HOME_CONTACTS_LOADING
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && mEvent.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(mEventClassName)) {

            Observable.just(true).delay(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    mState = STATE_HOME_CONTACTS;
                    AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("群聊")),
                            AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                }
            }, mAction1Throwable);
        } else if ((mState == STATE_HOME_CONTACTS
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.ChatroomContactUI".equals(mEventClassName))
                || (mState == STATE_CHAT_ROOM_CONTACT_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            mState = STATE_CHAT_ROOM_CONTACT;
            AccessibilityNodeInfoCompat rooms = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
            AccessibilityNodeInfoCompat roomName = AccessibilityNodeInfoUtils.searchFromBfs(mService, rooms, new TextNodeFilter(getJobData().roomNameForGettingQr));
            if (roomName == null) {
                mState = STATE_CHAT_ROOM_CONTACT_FIND_NOT;
                rooms.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            } else {
                mState = STATE_CHAT_ROOM_CONTACT_FIND;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, roomName, AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        } else if (mState == STATE_CHAT_ROOM_CONTACT_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName)) {

            mState = STATE_ROOM_CHATTING;
        }
    }

    private void handleFromRoomChatToRoomInfoThenQr() {
        if (mState == STATE_ROOM_CHATTING
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName)) {

            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("聊天信息")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_ROOM_CHATTING
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(mEventClassName))
                || (mState == STATE_ROOM_INFO_QR_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            mState = STATE_ROOM_INFO;
            AccessibilityNodeInfoCompat qrCode = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("群二维码"));
            if (qrCode != null) {
                mState = STATE_ROOM_INFO_QR_FIND;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, qrCode, AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                        .performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            } else {
                mState = STATE_ROOM_INFO_QR_FIND_NOT;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, AccessibilityNodeInfoUtils.FILTER_SCROLLABLE)
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_ROOM_INFO_QR_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SelfQRCodeUI".equals(mEventClassName)) {

            mState = STATE_ROOM_QR;
        }
    }

    private void handleRoomQr() {
        if (mState == STATE_ROOM_QR
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SelfQRCodeUI".equals(mEventClassName)) {

            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_ROOM_QR
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//                && "android.widget.FrameLayout".equals(mEventClassName)) {   // 老版本微信
                && "android.support.design.widget.c".equals(mEventClassName)) {  // 新版本微信

            mState = STATE_SAVE_ROOM_QR;
//            AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()),
//                    new TextNodeFilter("保存到手机")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);        // 老版本微信
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new TextNodeFilter("保存到手机")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);          // 新版本微信
        } else if (mState == STATE_SAVE_ROOM_QR
                && mEventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                && "android.widget.Toast$TN".equals(mEventClassName)) {

            getJobData().roomNameQrLocalDirectory = mEvent.getText().get(0).toString().replace("图片已保存至", "").replace("文件夹", "").trim();
            notifyFinish();
        }
    }
}
