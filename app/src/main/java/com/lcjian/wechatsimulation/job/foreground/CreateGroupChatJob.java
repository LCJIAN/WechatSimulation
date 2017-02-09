package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.Manager;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.Utils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 创建群 2016-11-11 completed
 *
 * @author LCJ
 */
public class CreateGroupChatJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_SELECT_CONTACT_ONE = 3;
    private static final int STATE_SELECT_CONTACT_TWO = 4;
    private static final int STATE_SELECT_CONTACT_FOCUS = 5;
    private static final int STATE_SELECT_CONTACT_READY = 6;
    private static final int STATE_SELECT_CONTACT_SELECTED = 7;
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
    public void doWithEvent(final AccessibilityService service, AccessibilityEvent mEvent) {
        super.doWithEvent(service, mEvent);
        try {
            handleFromHomeToChooseContacts();
            handleChooseContacts();
            handleChatSettings();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromHomeToChooseContacts() {
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
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("发起群聊")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }

    private void handleChooseContacts() {
        if ((mState == STATE_HOME_MORE && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && "com.tencent.mm.ui.contact.SelectContactUI".equals(mEventClassName))
                || (mState == STATE_SELECT_CONTACT_ONE && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && "android.widget.ListView".equals(mEventClassName))) {

            mState = STATE_SELECT_CONTACT_ONE;
            AccessibilityNodeInfoCompat contacts = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
            int childCount = contacts.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (addedNames.size() >= Utils.getValue(getJobData().groupChatMemberCount)) {
                    break;
                }
                AccessibilityNodeInfoCompat contact = contacts.getChild(i);
                AccessibilityNodeInfoCompat contactName = AccessibilityNodeInfoUtils.searchFromBfs(mService, contact, new ClassNameNodeFilter("android.widget.TextView"));
                AccessibilityNodeInfoCompat contactCheckBox = AccessibilityNodeInfoUtils.searchFromBfs(mService, contact, new ClassNameNodeFilter("android.widget.CheckBox"));
                if (contactName != null && contactCheckBox != null) {
                    String name = contactName.getText().toString();
                    if (!Manager.containGroupChatMember(name) && !addedNames.contains(name)) {
                        addedNames.add(name);
                    }
                }
            }
            if (addedNames.size() < Utils.getValue(getJobData().groupChatMemberCount)) {
                contacts.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
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
        } else if (mState == STATE_SELECT_CONTACT_TWO
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && ("android.widget.TextView".equals(mEventClassName) || "android.widget.ListView".equals(mEventClassName))) {

            if (!addedNames.isEmpty()) {
                String name = addedNames.get(0);
                ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("name", name));
                final Rect outBounds = new Rect();
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText")).getBoundsInScreen(outBounds);
                Observable.just(true)
                        .observeOn(Schedulers.io())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                            }
                        }, mAction1Throwable);
                mState = STATE_SELECT_CONTACT_FOCUS;
            } else {
                mState = STATE_SELECT_CONTACT_SELECTED;
            }
        } else if (mState == STATE_SELECT_CONTACT_FOCUS
                && (mEventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || (mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED))
                && "android.widget.EditText".equals(mEventClassName)) {
            mState = STATE_SELECT_CONTACT_READY;
            (new AccessibilityNodeInfoCompat(mEvent.getSource())).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_SELECT_CONTACT_READY
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.view.View".equals(mEventClassName)) {

            mState = STATE_NONE; // back to select all

            Set<String> names = new HashSet<>();
            names.add(addedNames.get(0));
            addedNames.clear();
            copyOfAddedNames.clear();
            Manager.addGroupChatMembers(names);

            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("返回").and(new ClassNameNodeFilter("android.widget.ImageView"))),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SELECT_CONTACT_READY
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName)) {

            mState = STATE_SELECT_CONTACT_TWO;   // back to select again
            AccessibilityNodeInfoCompat contacts = new AccessibilityNodeInfoCompat(mEvent.getSource());
            int childCount = contacts.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfoCompat contact = contacts.getChild(i);
                AccessibilityNodeInfoCompat contactName = AccessibilityNodeInfoUtils.searchFromBfs(mService, contact, new ClassNameNodeFilter("android.widget.TextView"));
                AccessibilityNodeInfoCompat contactCheckBox = AccessibilityNodeInfoUtils.searchFromBfs(mService, contact, new ClassNameNodeFilter("android.widget.CheckBox"));
                if (contactName != null && contactCheckBox != null) {
                    if (!contactCheckBox.isChecked()) {
                        contact.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        addedNames.remove(contactName.getText().toString());
                        break;
                    }
                }
            }
        }
    }

    private void handleChatSettings() {
        if (mState == STATE_SELECT_CONTACT_SELECTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName)) {

            mState = STATE_CHATTING;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.TextView")
                            .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                            .and(new NodeFilter() {
                                @Override
                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                    return AccessibilityNodeInfoUtils.getNodeText(node) != null
                                            && AccessibilityNodeInfoUtils.getNodeText(node).toString().contains("确定");
                                }
                            })).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_CHATTING
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName)) {

            mState = STATE_CHAT_ROOM_INFO;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.TextView")
                            .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                            .and(new TextNodeFilter("聊天信息")))
                    .performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_CHAT_ROOM_INFO && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(mEventClassName))
                || (mState == STATE_CHAT_ROOM_INFO_CYCLE && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && "android.widget.ListView".equals(mEventClassName))) {

            AccessibilityNodeInfoCompat groupName = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("群聊名称"));
            if (groupName != null) {
                mState = STATE_MOD_ROOM_NAME;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, groupName,
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            } else {
                mState = STATE_CHAT_ROOM_INFO_CYCLE;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_MOD_ROOM_NAME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.chatroom.ui.ModRemarkRoomNameUI".equals(mEventClassName)) {

            mState = STATE_ROOM_NAME_PASTED;
            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("name", getJobData().groupChatRoomName));
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText"))
                    .performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_ROOM_NAME_PASTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_ROOM_NAME_SET;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("保存")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_ROOM_NAME_SET
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.chatroom.ui.ChatroomInfoUI".equals(mEventClassName))
                || (mState == STATE_ROOM_NAME_SET_CYCLE
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            AccessibilityNodeInfoCompat messageMute = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("消息免打扰"));
            if (messageMute != null) {
                mState = STATE_CHECK_MUTE;
                check("消息免打扰");
            } else {
                mState = STATE_ROOM_NAME_SET_CYCLE;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_CHECK_MUTE || mState == STATE_CHECK_MUTE_CYCLE) {

            AccessibilityNodeInfoCompat accessibilityNodeInfo = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("置顶聊天"));
            if (accessibilityNodeInfo != null) {
                mState = STATE_CHECK_TOP;
                check("置顶聊天");
            } else {
                mState = STATE_CHECK_MUTE_CYCLE;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_CHECK_TOP || mState == STATE_CHECK_TOP_CYCLE) {

            AccessibilityNodeInfoCompat accessibilityNodeInfo = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("保存到通讯录"));
            if (accessibilityNodeInfo != null) {
                mState = STATE_CHECK_FINISH;
                check("保存到通讯录");
            } else {
                mState = STATE_CHECK_TOP_CYCLE;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if ((mState == STATE_CHECK_FINISH
                && mEventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                && "android.widget.Toast$TN".equals(mEventClassName))
                || (mState == STATE_CHECK_FINISH_CYCLE
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            AccessibilityNodeInfoCompat accessibilityNodeInfo = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("群主管理权转让"));
            if (accessibilityNodeInfo != null) {
                mState = STATE_SELECT_NEW_ROOM_OWNER;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, accessibilityNodeInfo,
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            } else {
                mState = STATE_CHECK_FINISH_CYCLE;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_SELECT_NEW_ROOM_OWNER
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.chatroom.ui.SelectNewRoomOwnerUI".equals(mEventClassName)) {

            mState = STATE_SEARCH_NEW_ROOM_OWNER;
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.TextView").and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE).and(new NodeFilter() {
                        @Override
                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                            return node.isEnabled() && node.isFocusable() && node.isLongClickable();
                        }
                    })).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SEARCH_NEW_ROOM_OWNER
                && mEventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_SEARCH_ROOM_OWNER_PASTED;
            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("owner", getJobData().groupChatRoomOwner));
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.EditText"))
                    .performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_SEARCH_ROOM_OWNER_PASTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName)) {

            mState = STATE_SEARCH_ROOM_OWNER_CONFIRM;
            new AccessibilityNodeInfoCompat(mEvent.getSource()).getChild(0).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SEARCH_ROOM_OWNER_CONFIRM
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            mState = STATE_FINISHED;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("确定")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_FINISHED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Manager.addGroupChatMembers(new HashSet<>(copyOfAddedNames));
            notifyFinish();
        }
    }

    private void check(final String text) {
        AccessibilityNodeInfoCompat itemCheck = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.view.View")
                .and(new TextNodeFilter("已关闭"))
                .and(new NodeFilter() {
                    @Override
                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                        return node.getParent() != null
                                && node.getParent().getChild(0) != null
                                && node.getParent().getChild(0).getChild(0) != null
                                && AccessibilityNodeInfoUtils.getNodeText(node.getParent().getChild(0).getChild(0)) != null
                                && text.equals(AccessibilityNodeInfoUtils.getNodeText(node.getParent().getChild(0).getChild(0)).toString());
                    }
                }));
        if (itemCheck != null) {
            final Rect outBounds = new Rect();
            itemCheck.getBoundsInScreen(outBounds);
            Observable.just(true)
                    .observeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                        }
                    }, mAction1Throwable);
        }
    }
}
