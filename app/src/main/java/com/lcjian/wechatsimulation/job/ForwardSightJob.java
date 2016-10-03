package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;

import java.util.List;

import timber.log.Timber;

public class ForwardSightJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS = 2;
    private static final int STATE_HOME_CONTACTS_ROOMS_FIND = 21;
    private static final int STATE_HOME_CONTACTS_ROOMS_FIND_NOT = 22;
    private static final int STATE_CHAT_ROOM_CONTACT = 3;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND = 31;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND_NOT = 32;

    private static final int STATE_ROOM_CHATTING = 4;
    private static final int STATE_SIGHT_FIND = 41;
    private static final int STATE_SIGHT_FIND_NOT = 42;

    private static final int STATE_FORWARD_FIND = 5;
    private static final int STATE_FORWARD_FIND_NOT = 51;

    private static final int STATE_SIGHT_FIND_AFTER_DIALOG_BACK = 52;

    private static final int STATE_DOWNLOADING_IMAGE_GALLERY = 54;
    private static final int STATE_DOWNLOADING_WIFI_TIP = 55;
    private static final int STATE_DOWNLOADING_BACK_TO_IMAGE_GALLERY = 56;
    private static final int STATE_DOWNLOADED = 57;

    private static final int STATE_SELECT_CONVERSATION = 6;
    private static final int STATE_SELECT_CHAT = 7;

    private static final int STATE_DESTINATION_GROUP_FIND = 8;
    private static final int STATE_DESTINATION_GROUP_FIND_NOT = 81;


    private int mState = STATE_NONE;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        try {

            handleFromHomeToRoomChat(service, event);
            handleFromRoomChatToSight(service, event);
            handleSightFind(service, event);
            handleForwardFind(service, event);
            handleForwardNotFind(service, event);
            handleWifi(service, event);

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
                        if (getJobData().roomNameForForwardSightFrom.equals(name)) {

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

    private void handleFromRoomChatToSight(AccessibilityService service, AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if ((mState == STATE_ROOM_CHATTING
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className))         // 第一次进入群聊界面
                || (mState == STATE_SIGHT_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className))                       // 向上滚动群聊界面
                || (mState == STATE_DOWNLOADED
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className))         // 下载完毕
                || (mState == STATE_SIGHT_FIND_AFTER_DIALOG_BACK
                && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.ListView".equals(className))) {              // 直接播放的视频

            AccessibilityNodeInfo messages = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/y6");
            if (messages != null) {
                int childCount = messages.getChildCount();
                for (int i = childCount - 1; i >= 0; i--) {
                    AccessibilityNodeInfo message = messages.getChild(i);
                    AccessibilityNodeInfo lastSight = AccessibilityHelper.findNodeInfoById(message, "com.tencent.mm:id/a0s");
                    if (lastSight != null && lastSight.getClassName().equals("android.widget.RelativeLayout")) {

                        mState = STATE_SIGHT_FIND;
                        lastSight.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                        break;
                    }
                }
                if (mState != STATE_SIGHT_FIND) {

                    mState = STATE_SIGHT_FIND_NOT;
                    messages.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        }
    }

    private void handleSightFind(AccessibilityService service, AccessibilityEvent event) {

        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_SIGHT_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.k".equals(className)) {

            AccessibilityNodeInfo actions = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/aex");
            if (actions != null) {
                int childCount = actions.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo action = actions.getChild(i);
                    AccessibilityNodeInfo actionText = AccessibilityHelper.findNodeInfoById(action, "com.tencent.mm:id/dq");
                    if (actionText != null && "转发".equals(actionText.getText().toString())) {

                        mState = STATE_FORWARD_FIND;
                        AccessibilityHelper.performClick(action);
                        break;
                    }
                }
                if (mState != STATE_FORWARD_FIND) {

                    mState = STATE_FORWARD_FIND_NOT;
                    AccessibilityHelper.performBack(service);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        }
    }

    private void handleForwardFind(AccessibilityService service, AccessibilityEvent event) {

        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_FORWARD_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.transmit.SelectConversationUI".equals(className)) {

            mState = STATE_SELECT_CONVERSATION;
            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "创建新聊天");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SELECT_CONVERSATION
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.SelectContactUI".equals(className)) {

            mState = STATE_SELECT_CHAT;
            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "选择一个群");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if ((mState == STATE_SELECT_CHAT
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.GroupCardSelectUI".equals(className))
                || (mState == STATE_DESTINATION_GROUP_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className))) {

            AccessibilityNodeInfo rooms = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/ayd");
            if (rooms != null) {
                int childCount = rooms.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo room = rooms.getChild(i);
                    AccessibilityNodeInfo roomName = AccessibilityHelper.findNodeInfoById(room, "com.tencent.mm:id/ayg");
                    if (roomName != null) {
                        String name = roomName.getText().toString();
                        if (getJobData().roomNameForForwardSightTo.equals(name)) {
                            AccessibilityHelper.performClick(room);

                            mState = STATE_DESTINATION_GROUP_FIND;
                            break;
                        }
                    }
                }
                if (mState != STATE_DESTINATION_GROUP_FIND) {

                    mState = STATE_DESTINATION_GROUP_FIND_NOT;
                    rooms.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_DESTINATION_GROUP_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.LinearLayout".equals(className)) {

            AccessibilityNodeInfo sendButton = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/a7b");
            if (sendButton != null) {
                AccessibilityHelper.performClick(sendButton);
                notifyFinish();
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        }
    }

    private void handleForwardNotFind(final AccessibilityService service, AccessibilityEvent event) {

        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_FORWARD_FIND_NOT
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className)) {                 // 从视频长按弹出框返回聊天界面

            AccessibilityNodeInfo messages = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/y6");
            if (messages != null) {
                int childCount = messages.getChildCount();
                for (int i = childCount - 1; i >= 0; i--) {
                    AccessibilityNodeInfo message = messages.getChild(i);
                    AccessibilityNodeInfo lastSight = AccessibilityHelper.findNodeInfoById(message, "com.tencent.mm:id/a0s");
                    if (lastSight != null) {

                        mState = STATE_SIGHT_FIND_AFTER_DIALOG_BACK;
                        lastSight.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                }
                if (mState != STATE_SIGHT_FIND_AFTER_DIALOG_BACK) {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);           // 由于是返回的，所以不滑动聊天消息
                }
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_SIGHT_FIND_AFTER_DIALOG_BACK
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.gallery.ImageGalleryUI".equals(className)) {

            mState = STATE_DOWNLOADING_IMAGE_GALLERY;
        } else if (mState == STATE_DOWNLOADING_IMAGE_GALLERY
                && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && ("android.view.View".equals(className) || "android.widget.TextView".equals(className))) {

            mState = STATE_DOWNLOADED;
            AccessibilityHelper.performBack(service);
        }
    }

    private void handleWifi(final AccessibilityService service, AccessibilityEvent event) {

        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        if (mState == STATE_DOWNLOADING_IMAGE_GALLERY
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(className)) {

            mState = STATE_DOWNLOADING_WIFI_TIP;
            AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "确定");
            if (accessibilityNodeInfo != null) {
                AccessibilityHelper.performClick(accessibilityNodeInfo);
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }
        } else if (mState == STATE_DOWNLOADING_WIFI_TIP
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.gallery.ImageGalleryUI".equals(className)) {

            mState = STATE_DOWNLOADING_BACK_TO_IMAGE_GALLERY;
        } else if (mState == STATE_DOWNLOADING_BACK_TO_IMAGE_GALLERY
                && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.view.View".equals(className)) {

            mState = STATE_DOWNLOADED;
            AccessibilityHelper.performBack(service);
        }
    }
}
