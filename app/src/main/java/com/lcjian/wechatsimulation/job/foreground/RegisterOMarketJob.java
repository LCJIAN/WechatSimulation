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
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * 注册OMarket 2016-11-30 completed
 *
 * @author LCJ
 */
public class RegisterOMarketJob extends OMarketJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_MAIN = 1;
    private static final int STATE_LOGIN = 2;
    private static final int STATE_REGISTER = 3;
    private static final int STATE_PHONE_INPUTTED = 4;
    private static final int STATE_CODE_SENT = 5;
    private static final int STATE_CODE_INPUTTED = 6;
    private static final int STATE_SET_PWD = 7;

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
                                        Pattern pattern = Pattern.compile("验证码是：([0-9]{4,})");
                                        Matcher matcher = pattern.matcher(messageBody);
                                        if (matcher.find()) {
                                            Observable.just(matcher.group(1))
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
        if (mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfoCompat pass = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("跳过"));
            if (pass != null) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, pass,
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        }
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.MainMenuActivity".equals(mEventClassName)) {

            mState = STATE_MAIN;
            Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Func1<Boolean, Boolean>() {
                        @Override
                        public Boolean call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("我")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            return aBoolean;
                        }
                    })
                    .delay(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("点击登录")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_MAIN
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.nearme.ucplugin.activity.LoginActivity".equals(mEventClassName)) {

            mState = STATE_LOGIN;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("注册新帐号")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_LOGIN
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.nearme.ucplugin.activity.RegGetVerifyCodeActivity".equals(mEventClassName)) {

            mState = STATE_REGISTER;
            Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("phone_no", getJobData().phoneNoOMarket));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_REGISTER
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_PHONE_INPUTTED;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("获取验证码")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_PHONE_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.nearme.ucplugin.activity.RegSendVerifyCodeActivity".equals(mEventClassName)) {

            mState = STATE_CODE_SENT;
        } else if (mState == STATE_CODE_SENT
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_CODE_INPUTTED;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("提交验证码")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_CODE_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.nearme.ucplugin.activity.RegSendPasswordActivity".equals(mEventClassName)) {

            mState = STATE_SET_PWD;
            Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("password", getJobData().passwordOMarket));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_SET_PWD
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            Observable.just(true).delay(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("完成")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            notifyFinish();
                        }
                    }, mAction1Throwable);
        }
    }
}
