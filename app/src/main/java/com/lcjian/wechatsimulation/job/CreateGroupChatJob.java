package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.lcjian.wechatsimulation.Manager;
import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;
import com.lcjian.wechatsimulation.utils.ShellUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

public class CreateGroupChatJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_SELECT_CONTACT_ONE = 3;
    private static final int STATE_SELECT_CONTACT_TWO = 4;
    private static final int STATE_SELECT_CONTACT_FOCUS = 5;
    private static final int STATE_SELECT_CONTACT_READY = 6;
    private static final int STATE_SELECT_CONTACT_SELECTED = 7;
    private static final int STATE_SELECT_CONTACT_NO_CONTACT = 71;
    private static final int STATE_CHATTING = 8;
    private static final int STATE_CHAT_ROOM_INFO = 9;
    private static final int STATE_CHAT_ROOM_INFO_CYCLE = 91;
    private static final int STATE_MOD_ROOM_NAME = 10;
    private static final int STATE_ROOM_NAME_PASTED = 11;
    private static final int STATE_ROOM_NAME_SET = 12;
    private static final int STATE_ROOM_NAME_SET_CYCLE = 121;
    private static final int STATE_CHECK_MUTE = 13;
    private static final int STATE_CHECK_MUTE_CYCLE = 131;
    private static final int STATE_CHECK_TOP = 14;
    private static final int STATE_CHECK_TOP_CYCLE = 141;
    private static final int STATE_CHECK_FINISH = 15;
    private static final int STATE_CHECK_FINISH_CYCLE = 151;
    private static final int STATE_SELECT_NEW_ROOM_OWNER = 16;
    private static final int STATE_SEARCH_NEW_ROOM_OWNER = 17;
    private static final int STATE_SEARCH_ROOM_OWNER_PASTED = 18;
    private static final int STATE_SEARCH_ROOM_OWNER_CONFIRM = 19;
    private static final int STATE_FINISHED = 20;

    private int mState = STATE_NONE;

    private List<String> addedNames = new ArrayList<>();

    private List<String> copyOfAddedNames = new ArrayList<>();

    @Override
    public void doWithEvent(final AccessibilityService service, AccessibilityEvent event) {
        try {
            final int eventType = event.getEventType();
            final String className = event.getClassName().toString();
            if ((mState == STATE_NONE || mState == STATE_SELECT_CONTACT_NO_CONTACT)
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
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "发起群聊");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if ((mState == STATE_HOME_MORE && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && "com.tencent.mm.ui.contact.SelectContactUI".equals(className))
                    || (mState == STATE_SELECT_CONTACT_ONE && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && "android.widget.ListView".equals(className))) {

                mState = STATE_SELECT_CONTACT_ONE;
                AccessibilityNodeInfo contacts = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/bby");
                if (contacts != null) {
                    addMember(contacts);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_SELECT_CONTACT_TWO
                    && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && ("android.widget.TextView".equals(className) || "android.widget.ListView".equals(className))) {

                if (!addedNames.isEmpty()) {
                    String name = addedNames.get(0);
                    ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("name", name));
                    AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/bm_");
                    if (accessibilityNodeInfo != null) {
                        Rect outBounds = new Rect();
                        accessibilityNodeInfo.getBoundsInScreen(outBounds);
                        ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                        mState = STATE_SELECT_CONTACT_FOCUS;
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                } else {
                    mState = STATE_SELECT_CONTACT_SELECTED;
                }
            } else if (mState == STATE_SELECT_CONTACT_FOCUS
                    && (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED))
                    && "android.widget.EditText".equals(className)) {
                mState = STATE_SELECT_CONTACT_READY;
                event.getSource().performAction(AccessibilityNodeInfo.ACTION_PASTE);
            } else if (mState == STATE_SELECT_CONTACT_READY
                    && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && "android.view.View".equals(className)) {

                mState = STATE_SELECT_CONTACT_NO_CONTACT;

                Set<String> names = new HashSet<>();
                names.add(addedNames.get(0));
                addedNames.clear();
                copyOfAddedNames.clear();
                Manager.addGroupChatMembers(names);

                AccessibilityNodeInfo backNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/f0");
                AccessibilityHelper.performClick(backNodeInfo);
            } else if (mState == STATE_SELECT_CONTACT_READY
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className)) {

                mState = STATE_SELECT_CONTACT_TWO;   // back to select again
                AccessibilityNodeInfo contacts = event.getSource();
                int childCount = contacts.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo contact = contacts.getChild(i);
                    AccessibilityNodeInfo contactName = AccessibilityHelper.findNodeInfoById(contact, "com.tencent.mm:id/i1");
                    AccessibilityNodeInfo contactCheck = AccessibilityHelper.findNodeInfoById(contact, "com.tencent.mm:id/l6");
                    if (contactName != null && contactCheck != null) {
                        if (!contactCheck.isChecked()) {
                            AccessibilityHelper.performClick(contact);
                            addedNames.remove(contactName.getText().toString());
                            break;
                        }
                    }
                }
            } else if (mState == STATE_SELECT_CONTACT_SELECTED
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className)) {

                mState = STATE_CHATTING;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/eg");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_CHATTING
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.chatting.ChattingUI".equals(className)) {

                mState = STATE_CHAT_ROOM_INFO;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "聊天信息");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if ((mState == STATE_CHAT_ROOM_INFO && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(className))
                    || (mState == STATE_CHAT_ROOM_INFO_CYCLE && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && "android.widget.ListView".equals(className))) {

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(event.getSource(), "群聊名称");
                if (accessibilityNodeInfo != null) {
                    mState = STATE_MOD_ROOM_NAME;
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(event.getSource(), "android:id/list");
                    if (info != null) {
                        mState = STATE_CHAT_ROOM_INFO_CYCLE;
                        info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if (mState == STATE_MOD_ROOM_NAME
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.chatroom.ui.ModRemarkRoomNameUI".equals(className)) {

                mState = STATE_ROOM_NAME_PASTED;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/bl2");
                if (accessibilityNodeInfo != null) {
                    ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("name", getJobData().groupChatRoomName));
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_ROOM_NAME_PASTED
                    && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                    && "android.widget.EditText".equals(className)) {

                mState = STATE_ROOM_NAME_SET;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/eg");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if ((mState == STATE_ROOM_NAME_SET
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(className))
                    || (mState == STATE_ROOM_NAME_SET_CYCLE
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className))) {

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "消息免打扰");
                if (accessibilityNodeInfo != null) {
                    mState = STATE_CHECK_MUTE;
                    check(service.getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/g6"), "消息免打扰");
                } else {
                    AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                    if (info != null) {
                        mState = STATE_ROOM_NAME_SET_CYCLE;
                        info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if (mState == STATE_CHECK_MUTE || mState == STATE_CHECK_MUTE_CYCLE) {

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "置顶聊天");
                if (accessibilityNodeInfo != null) {
                    mState = STATE_CHECK_TOP;
                    check(service.getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/g6"), "置顶聊天");
                } else {
                    AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                    if (info != null) {
                        mState = STATE_CHECK_MUTE_CYCLE;
                        info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if (mState == STATE_CHECK_TOP || mState == STATE_CHECK_TOP_CYCLE) {

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "保存到通讯录");
                if (accessibilityNodeInfo != null) {
                    mState = STATE_CHECK_FINISH;
                    check(service.getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/g6"), "保存到通讯录");
                } else {
                    AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                    if (info != null) {
                        mState = STATE_CHECK_TOP_CYCLE;
                        info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if ((mState == STATE_CHECK_FINISH
                    && eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                    && "android.widget.Toast$TN".equals(className))
                    || (mState == STATE_CHECK_FINISH_CYCLE
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className))) {

                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoByText(service.getRootInActiveWindow(), "群主管理权转让");
                if (accessibilityNodeInfo != null) {
                    mState = STATE_SELECT_NEW_ROOM_OWNER;
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    AccessibilityNodeInfo info = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "android:id/list");
                    if (info != null) {
                        mState = STATE_CHECK_FINISH_CYCLE;
                        info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                    }
                }
            } else if (mState == STATE_SELECT_NEW_ROOM_OWNER
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.plugin.chatroom.ui.SelectNewRoomOwnerUI".equals(className)) {

                mState = STATE_SEARCH_NEW_ROOM_OWNER;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/ag");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_SEARCH_NEW_ROOM_OWNER
                    && eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                    && "android.widget.EditText".equals(className)) {

                mState = STATE_SEARCH_ROOM_OWNER_PASTED;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(service.getRootInActiveWindow(), "com.tencent.mm:id/ew");
                if (accessibilityNodeInfo != null) {
                    ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("owner", getJobData().groupChatRoomOwner));
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_SEARCH_ROOM_OWNER_PASTED
                    && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && "android.widget.ListView".equals(className)) {

                mState = STATE_SEARCH_ROOM_OWNER_CONFIRM;
                AccessibilityNodeInfo members = event.getSource();
                int childCount = members.getChildCount();
                if (childCount > 0) {
                    AccessibilityHelper.performClick(members.getChild(0));
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_SEARCH_ROOM_OWNER_CONFIRM
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && "com.tencent.mm.ui.base.h".equals(className)) {

                mState = STATE_FINISHED;
                AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityHelper.findNodeInfoById(event.getSource(), "com.tencent.mm:id/bhe");
                if (accessibilityNodeInfo != null) {
                    AccessibilityHelper.performClick(accessibilityNodeInfo);
                } else {
                    throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                }
            } else if (mState == STATE_FINISHED
                    && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Manager.addGroupChatMembers(new HashSet<>(copyOfAddedNames));
                notifyFinish();
            }
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void addMember(final AccessibilityNodeInfo contacts) {
        int childCount = contacts.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (addedNames.size() >= getJobData().groupChatMemberCount) {
                break;
            }
            AccessibilityNodeInfo contact = contacts.getChild(i);
            AccessibilityNodeInfo contactName = AccessibilityHelper.findNodeInfoById(contact, "com.tencent.mm:id/i1");
            if (contactName != null) {
                String name = contactName.getText().toString();
                if (!Manager.containGroupChatMember(name) && !addedNames.contains(name)) {
                    addedNames.add(name);
                }
            }
        }
        if (addedNames.size() < getJobData().groupChatMemberCount) {
            contacts.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        } else {
            if (!addedNames.contains(getJobData().groupChatAdMember)) {
                addedNames.add(getJobData().groupChatAdMember);
            }
            if (!addedNames.contains(getJobData().groupChatRoomOwner)) {
                addedNames.add(getJobData().groupChatRoomOwner);
            }
            Collections.shuffle(addedNames);
            copyOfAddedNames.addAll(addedNames);
            mState = STATE_SELECT_CONTACT_TWO;
        }
    }

    private void check(List<AccessibilityNodeInfo> list, String text) {
        int childCount = list.size();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo item = list.get(i);
            AccessibilityNodeInfo itemName = AccessibilityHelper.findNodeInfoById(item, "android:id/title");
            AccessibilityNodeInfo itemCheck = AccessibilityHelper.findNodeInfoById(item, "com.tencent.mm:id/ds");
            if (itemName != null && itemCheck != null) {
                if (text.equals(itemName.getText().toString())) {
                    Rect outBounds = new Rect();
                    itemCheck.getBoundsInScreen(outBounds);
                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                    break;
                }
            }
        }
    }
}
