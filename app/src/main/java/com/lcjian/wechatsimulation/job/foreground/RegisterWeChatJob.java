package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import github.nisrulz.easydeviceinfo.base.EasyDeviceMod;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * 注册微信 2016-11-23 completed
 *
 * @author LCJ
 */
public class RegisterWeChatJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_REGISTER = 2;
    private static final int STATE_NICK_NAME_INPUTTED = 3;
    private static final int STATE_MOBILE_INPUTTED = 4;
    private static final int STATE_PASSWORD_INPUTTED = 5;
    private static final int STATE_SEND_SMS = 6;
    private static final int STATE_SEND_SMS_SENT = 7;
    private static final int STATE_SEND_SMS_SENT_NEXT = 8;

    private int mState = STATE_NONE;

    private BroadcastReceiver mSmsObserver;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        if (mSmsObserver == null) {
            mSmsObserver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                        Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            try {
                                Object[] pdu = (Object[]) bundle.get("pdus");
                                if (pdu != null) {
                                    for (Object aPdu : pdu) {
                                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) aPdu);
                                        String messageBody = smsMessage.getMessageBody();
                                        Pattern pattern = Pattern.compile("验证码([0-9]{5,})");
                                        Matcher matcher = pattern.matcher(messageBody);
                                        if (matcher.find()) {
                                            Observable.just(matcher.group(1))
                                                    .delay(1, TimeUnit.SECONDS)
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .map(new Func1<String, String>() {
                                                        @Override
                                                        public String call(String string) {
                                                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                                                    new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                                                            return string;
                                                        }
                                                    })
                                                    .delay(3, TimeUnit.SECONDS)
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(new Action1<String>() {
                                                        @Override
                                                        public void call(String string) {
                                                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("code", string));
                                                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                                                    new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                                                        }
                                                    }, mAction1Throwable);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                notifyError(e);
                            }
                        }
                    }
                }
            };
            mService.registerReceiver(mSmsObserver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

            addJobListener(new JobListener() {
                @Override
                public void onCancelled() {
                    if (mSmsObserver != null) {
                        mService.unregisterReceiver(mSmsObserver);
                        mSmsObserver = null;
                    }
                }

                @Override
                public void onFinished() {
                    if (mSmsObserver != null) {
                        mService.unregisterReceiver(mSmsObserver);
                        mSmsObserver = null;
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (mSmsObserver != null) {
                        mService.unregisterReceiver(mSmsObserver);
                        mSmsObserver = null;
                    }
                }
            });
        }
        try {
            handleRegister();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleRegister() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            Observable.just(true)
                    .delay(5, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("注册")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.account.RegByMobileRegAIOUI".equals(mEventClassName)) {

            mState = STATE_REGISTER;
            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("nick_name", getJobData().nickName));
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.EditText").and(new NodeFilter() {
                        @Override
                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                            return new TextNodeFilter("昵称").accept(context, node.getParent().getChild(0));
                        }
                    })).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_REGISTER
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_NICK_NAME_INPUTTED;
            Observable.just(true)
                    .delay(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Func1<Boolean, Boolean>() {
                        @Override
                        public Boolean call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText").and(new TextNodeFilter("你的手机号码"))).performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
                            return aBoolean;
                        }
                    })
                    .delay(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE))
                                    .setPrimaryClip(ClipData.newPlainText("mobile",
                                            TextUtils.isEmpty(getJobData().phoneNo) ? new EasyDeviceMod(mService).getPhoneNo() : getJobData().phoneNo));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText").and(new TextNodeFilter("你的手机号码"))).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_NICK_NAME_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_MOBILE_INPUTTED;
            Observable.just(true)
                    .delay(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Func1<Boolean, Boolean>() {
                        @Override
                        public Boolean call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText").and(new NodeFilter() {
                                        @Override
                                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                            return new TextNodeFilter("密码").accept(context, node.getParent().getChild(0));
                                        }
                                    })).performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
                            return aBoolean;
                        }
                    })
                    .delay(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE))
                                    .setPrimaryClip(ClipData.newPlainText("password", getJobData().password));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText").and(new NodeFilter() {
                                        @Override
                                        public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                            return new TextNodeFilter("密码").accept(context, node.getParent().getChild(0));
                                        }
                                    })).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_MOBILE_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_PASSWORD_INPUTTED;
            Observable.just(true)
                    .delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                            new ClassNameNodeFilter("android.widget.Button").and(new TextNodeFilter("注册"))),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_PASSWORD_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            mState = STATE_SEND_SMS;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("确定")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SEND_SMS
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.account.mobile.MobileVerifyUI".equals(mEventClassName)) {

            mState = STATE_SEND_SMS_SENT;
        } else if (mState == STATE_SEND_SMS_SENT
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_SEND_SMS_SENT_NEXT;
            Observable.just(true)
                    .delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("下一步")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_SEND_SMS_SENT_NEXT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.base.h".equals(mEventClassName)) {

            if (AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("了解更多")) != null) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                        AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("好")),
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                notifyFinish();
            }
        }
    }
}
