package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;

import java.util.List;

import timber.log.Timber;

public class GetRoomQrCodeJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS = 2;
    private static final int STATE_HOME_CONTACTS_ROOMS_FIND = 21;
    private static final int STATE_HOME_CONTACTS_ROOMS_FIND_NOT = 22;
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
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        try {
            handleFromHomeToRoomChat(service, event);
            handleFromRoomChatToRoomInfoThenQr(service, event);
            handleRoomQr(event);
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToRoomChat(AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_NONE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(className)) {

            mState = STATE_HOME;
            List<AccessibilityNodeInfo> nodeInfo = event.getSource().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bhj");
            if (nodeInfo != null && !nodeInfo.isEmpty()) {
                for (AccessibilityNodeInfo item : nodeInfo) {
                    if ("通讯录".equals(item.getText().toString())) {
                        AccessibilityHelper.performClick(item);
                        break;
                    }
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if ((mState == STATE_HOME
                && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && event.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(className))
                || (mState == STATE_HOME_CONTACTS_ROOMS_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className))) {

            mState = STATE_HOME_CONTACTS;
            AccessibilityNodeInfo contacts = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/fu");
            if (contacts != null) {
                if (0 < contacts.getChildCount()) {
                    AccessibilityNodeInfo headContact = contacts.getChild(0);
                    AccessibilityNodeInfo groupChat = AccessibilityHelper.findNodeInfoByText(headContact, "群聊");
                    if (groupChat != null) {

                        mState = STATE_HOME_CONTACTS_ROOMS_FIND;
                        AccessibilityHelper.performClick(groupChat);
                    }
                }
                if (mState != STATE_HOME_CONTACTS_ROOMS_FIND) {

                    mState = STATE_HOME_CONTACTS_ROOMS_FIND_NOT;
                    contacts.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if ((mState == STATE_HOME_CONTACTS_ROOMS_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.ChatroomContactUI".equals(className))
                || (mState == STATE_CHAT_ROOM_CONTACT_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className))) {

            mState = STATE_CHAT_ROOM_CONTACT;
            AccessibilityNodeInfo rooms = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/fu");
            if (rooms != null) {
                int childCount = rooms.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo room = rooms.getChild(i);
                    AccessibilityNodeInfo roomName = AccessibilityHelper.findNodeInfoById(room, "com.tencent.mm:id/gi");
                    if (roomName != null) {
                        String name = roomName.getText().toString();
                        if (getJobData().roomNameForGettingQr.equals(name)) {

                            mState = STATE_CHAT_ROOM_CONTACT_FIND;
                            AccessibilityHelper.performClick(room);
                            break;
                        }
                    }
                }
                if (mState != STATE_CHAT_ROOM_CONTACT_FIND) {

                    mState = STATE_CHAT_ROOM_CONTACT_FIND_NOT;
                    rooms.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_CHAT_ROOM_CONTACT_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className)) {

            mState = STATE_ROOM_CHATTING;
        }
    }

    private void handleFromRoomChatToRoomInfoThenQr(AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_ROOM_CHATTING
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className)) {

            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "聊天信息");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if ((mState == STATE_ROOM_CHATTING
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(className))
                || (mState == STATE_ROOM_INFO_QR_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.ListView".equals(className))) {

            mState = STATE_ROOM_INFO;
            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "群二维码");
            if (accessibilityNodeInfo != null) {

                mState = STATE_ROOM_INFO_QR_FIND;
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {

                mState = STATE_ROOM_INFO_QR_FIND_NOT;
                AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                if (info != null) {
                    info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            }
        } else if (mState == STATE_ROOM_INFO_QR_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SelfQRCodeUI".equals(className)) {

            mState = STATE_ROOM_QR;
        }
    }

    private void handleRoomQr(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_ROOM_QR
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.setting.ui.setting.SelfQRCodeUI".equals(className)) {

            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "更多");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_ROOM_QR
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.FrameLayout".equals(className)) {

            mState = STATE_SAVE_ROOM_QR;
            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "保存到手机");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SAVE_ROOM_QR
                && eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                && "android.widget.Toast$TN".equals(className)) {

            getJobData().roomNameQrLocalDirectory = event.getText().get(0).toString().replace("图片已保存至", "").replace("文件夹", "").trim();
            notifyFinish();
        }
    }

}
