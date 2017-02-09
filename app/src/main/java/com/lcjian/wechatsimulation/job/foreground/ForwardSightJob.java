package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * 转发小视频 2016-11-14 completed
 *
 * @author LCJ
 */
public class ForwardSightJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS_LOADING = 2;
    private static final int STATE_HOME_CONTACTS = 21;
    private static final int STATE_CHAT_ROOM_CONTACT = 3;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND = 31;
    private static final int STATE_CHAT_ROOM_CONTACT_FIND_NOT = 32;

    private static final int STATE_ROOM_CHATTING = 4;
    private static final int STATE_SIGHT_FIND = 41;
    private static final int STATE_SIGHT_FIND_NOT = 42;

    private static final int STATE_FORWARD_FIND = 5;
    private static final int STATE_FORWARD_FIND_NOT = 51;

    private static final int STATE_SIGHT_FIND_AFTER_DIALOG_BACK = 52;
    private static final int STATE_DOWNLOADING = 54;
    private static final int STATE_DOWNLOADING_WIFI_TIP = 55;
    private static final int STATE_DOWNLOADED = 57;

    private static final int STATE_SELECT_CONVERSATION = 6;
    private static final int STATE_SELECT_CHAT = 7;

    private static final int STATE_DESTINATION_GROUP_FIND = 8;
    private static final int STATE_DESTINATION_GROUP_FIND_NOT = 81;

    private int mState = STATE_NONE;

    private Subscription mSubscription;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        try {

            handleFromHomeToRoomChat();
            handleFromRoomChatToSight();
            handleSightFind();
            handleForwardFind();
            handleForwardNotFind();

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
            AccessibilityNodeInfoCompat roomName = AccessibilityNodeInfoUtils.searchFromBfs(mService, rooms, new TextNodeFilter(getJobData().roomNameForForwardSightFrom));
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

    private void handleFromRoomChatToSight() {
        if ((mState == STATE_ROOM_CHATTING
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName))         // 第一次进入群聊界面
                || (mState == STATE_SIGHT_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))                       // 向上滚动群聊界面
                || (mState == STATE_DOWNLOADED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName))         // 下载完毕
                || (mState == STATE_SIGHT_FIND_AFTER_DIALOG_BACK
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.ListView".equals(mEventClassName))) {              // 直接播放的视频

            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            mSubscription = Observable.just(true).delay(4, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoCompat messages = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
                            int childCount = messages.getChildCount();
                            for (int i = childCount - 1; i >= 0; i--) {
                                AccessibilityNodeInfoCompat message = messages.getChild(i);
                                AccessibilityNodeInfoCompat lastSight = AccessibilityNodeInfoUtils
                                        .searchFromBfs(mService, message, new ClassNameNodeFilter("android.widget.FrameLayout")
                                                .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                                                .and(new NodeFilter() {
                                                    @Override
                                                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                                        return node.isLongClickable()
                                                                && node.getChildCount() == 4
                                                                && TextUtils.equals(node.getChild(0).getClassName().toString(), "android.widget.ImageView")
                                                                && TextUtils.equals(node.getChild(1).getClassName().toString(), "android.widget.ImageView")
                                                                && TextUtils.equals(node.getChild(2).getClassName().toString(), "android.widget.ImageView")
                                                                && TextUtils.equals(node.getChild(3).getClassName().toString(), "android.widget.LinearLayout");
                                                    }
                                                }));
                                if (lastSight == null) {
                                    lastSight = AccessibilityNodeInfoUtils
                                            .searchFromBfs(mService, message, new ClassNameNodeFilter("android.widget.RelativeLayout")
                                                    .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                                                    .and(new NodeFilter() {
                                                        @Override
                                                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                                            return node.isLongClickable()
                                                                    && node.getChildCount() == 2
                                                                    && TextUtils.equals(node.getChild(0).getClassName().toString(), "android.view.View")
                                                                    && TextUtils.equals(node.getChild(1).getClassName().toString(), "android.widget.ImageView");
                                                        }
                                                    }));
                                }
                                if (lastSight != null) {

                                    mState = STATE_SIGHT_FIND;
                                    lastSight.performAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
                                    break;
                                }
                            }
                            if (mState != STATE_SIGHT_FIND) {

                                mState = STATE_SIGHT_FIND_NOT;
                                messages.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                            }
                        }
                    }, mAction1Throwable);
        }
    }

    private void handleSightFind() {
        if (mState == STATE_SIGHT_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.k".equals(mEventClassName)) {

            AccessibilityNodeInfoCompat forward = AccessibilityNodeInfoUtils
                    .searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("转发"));
            if (forward == null) {
                mState = STATE_FORWARD_FIND_NOT;
                mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            } else {
                mState = STATE_FORWARD_FIND;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        forward, AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        }
    }

    private void handleForwardFind() {
        if (mState == STATE_FORWARD_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.transmit.SelectConversationUI".equals(mEventClassName)) {

            mState = STATE_SELECT_CONVERSATION;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("创建新聊天")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SELECT_CONVERSATION
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.SelectContactUI".equals(mEventClassName)) {

            mState = STATE_SELECT_CHAT;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("选择一个群")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_SELECT_CHAT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.contact.GroupCardSelectUI".equals(mEventClassName))
                || (mState == STATE_DESTINATION_GROUP_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(mEventClassName))) {

            AccessibilityNodeInfoCompat rooms = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
            AccessibilityNodeInfoCompat roomName = AccessibilityNodeInfoUtils.searchFromBfs(mService, rooms, new TextNodeFilter(getJobData().roomNameForForwardSightTo));
            if (roomName == null) {
                mState = STATE_DESTINATION_GROUP_FIND_NOT;
                rooms.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            } else {
                mState = STATE_DESTINATION_GROUP_FIND;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        roomName, AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        } else if (mState == STATE_DESTINATION_GROUP_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.LinearLayout".equals(mEventClassName)) {

            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("发送")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            notifyFinish();
        }
    }

    private void handleForwardNotFind() {
        if (mState == STATE_FORWARD_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(mEventClassName)) {                 // 从视频长按弹出框返回聊天界面

            AccessibilityNodeInfoCompat messages = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ListView"));
            int childCount = messages.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                AccessibilityNodeInfoCompat message = messages.getChild(i);
                AccessibilityNodeInfoCompat lastSight = AccessibilityNodeInfoUtils
                        .searchFromBfs(mService, message, new ClassNameNodeFilter("android.widget.FrameLayout")
                                .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                                .and(new NodeFilter() {
                                    @Override
                                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                        return node.isLongClickable()
                                                && node.getChildCount() == 4
                                                && TextUtils.equals(node.getChild(0).getClassName().toString(), "android.widget.ImageView")
                                                && TextUtils.equals(node.getChild(1).getClassName().toString(), "android.widget.ImageView")
                                                && TextUtils.equals(node.getChild(2).getClassName().toString(), "android.widget.ImageView")
                                                && TextUtils.equals(node.getChild(3).getClassName().toString(), "android.widget.LinearLayout");
                                    }
                                }));
                if (lastSight == null) {
                    lastSight = AccessibilityNodeInfoUtils
                            .searchFromBfs(mService, message, new ClassNameNodeFilter("android.widget.RelativeLayout")
                                    .and(AccessibilityNodeInfoUtils.FILTER_CLICKABLE)
                                    .and(new NodeFilter() {
                                        @Override
                                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                            return node.isLongClickable()
                                                    && node.getChildCount() == 2
                                                    && TextUtils.equals(node.getChild(0).getClassName().toString(), "android.view.View")
                                                    && TextUtils.equals(node.getChild(1).getClassName().toString(), "android.widget.ImageView");
                                        }
                                    }));
                }
                if (lastSight != null) {

                    mState = STATE_SIGHT_FIND_AFTER_DIALOG_BACK;
                    lastSight.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                    break;
                }
            }
            if (mState != STATE_SIGHT_FIND_AFTER_DIALOG_BACK) {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);           // 由于是返回的，所以不滑动聊天消息
            }
        } else if ((mState == STATE_SIGHT_FIND_AFTER_DIALOG_BACK || mState == STATE_DOWNLOADING_WIFI_TIP)
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.gallery.ImageGalleryUI".equals(mEventClassName)) {

            mState = STATE_DOWNLOADING;
            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            mSubscription = Observable.just(true).delay(30, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            mState = STATE_DOWNLOADED;
                            mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_DOWNLOADING
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            mState = STATE_DOWNLOADING_WIFI_TIP;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("确定")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
    }
}
