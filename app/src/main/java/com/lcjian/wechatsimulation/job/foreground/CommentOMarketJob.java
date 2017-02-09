package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.APP;
import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.PackageUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class CommentOMarketJob extends OMarketJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;

    private static final int STATE_PRODUCT_DETAIL = 2;
    private static final int STATE_PRODUCT_DETAIL_LOADED = 21;
    private static final int STATE_PRODUCT_DETAIL_COMMENT_FIND = 22;
    private static final int STATE_PRODUCT_DETAIL_COMMENT_FIND_NOT = 23;

    private static final int STATE_COMMENT = 3;
    private static final int STATE_COMMENT_LOADED = 31;

    private static final int STATE_COMMENT_SUBMIT = 4;
    private static final int STATE_COMMENT_SUBMIT_INPUTTED = 41;

    private int mState = STATE_NONE;

    private File mDestination;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        if (mDestination == null) {
            mDestination = service.getExternalFilesDir("download");
        }
        try {
            handleComment();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleComment() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.MainMenuActivity".equals(mEventClassName)) {

            mState = STATE_HOME;
            Observable.just(getJobData().apkUrlOMarket)
                    .map(new Func1<String, File>() {
                        @Override
                        public File call(String s) {
                            FileUtils.deleteDir(mDestination);
                            return DownloadUtils.download(s, mDestination);
                        }
                    })
                    .map(new Func1<File, Integer>() {
                        @Override
                        public Integer call(File file) {
                            if (file != null) {
                                return PackageUtils.installSilent(APP.getInstance(), file.getAbsolutePath());
                            } else {
                                throw new RuntimeException("Download error");
                            }
                        }
                    })
                    .map(new Func1<Integer, Boolean>() {
                        @Override
                        public Boolean call(Integer integer) {
                            return integer == PackageUtils.INSTALL_SUCCEEDED;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (aBoolean) {
                                openOMarket(getJobData().packageNameOMarket);
                            } else {
                                notifyError(new RuntimeException("Install failed"));
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            notifyError(throwable);
                        }
                    });
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.ProductDetailActivity".equals(mEventClassName)) {

            mState = STATE_PRODUCT_DETAIL;
        } else if ((mState == STATE_PRODUCT_DETAIL
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.GridView".equals(mEventClassName))
                || (mState == STATE_PRODUCT_DETAIL_COMMENT_FIND_NOT
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ScrollView".equals(mEventClassName))) {

            mState = STATE_PRODUCT_DETAIL_LOADED;
            AccessibilityNodeInfoCompat comment = AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("评论"));
            if (comment == null) {
                mState = STATE_PRODUCT_DETAIL_COMMENT_FIND_NOT;
                AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new ClassNameNodeFilter("android.widget.ScrollView"))
                        .performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            } else {
                mState = STATE_PRODUCT_DETAIL_COMMENT_FIND;
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService, comment,
                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
            }
        } else if (mState == STATE_PRODUCT_DETAIL_COMMENT_FIND
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.CommentActivity".equals(mEventClassName)) {

            mState = STATE_COMMENT;
        } else if (mState == STATE_COMMENT
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.ListView".equals(mEventClassName)) {

            mState = STATE_COMMENT_LOADED;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发布评论")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_COMMENT_LOADED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.AppraisalActivity".equals(mEventClassName)) {

            mState = STATE_COMMENT_SUBMIT;
            Observable.just(getJobData().commentOMarket)
                    .delay(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String string) {
                            ((ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("comment", string));
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                    new ClassNameNodeFilter("android.widget.EditText")).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_COMMENT_SUBMIT
                && mEventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(mEventClassName)) {

            mState = STATE_COMMENT_SUBMIT_INPUTTED;
            final Rect outBounds = new Rect();
            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                    new ClassNameNodeFilter("android.widget.RatingBar")).getBoundsInScreen(outBounds);
            Observable.just(true)
                    .delay(2, TimeUnit.SECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            ShellUtils.execCommand(
                                    String.format(Locale.getDefault(),
                                            "input tap %d %d",
                                            outBounds.right - DimenUtils.dipToPixels(8, mService),
                                            outBounds.centerY()),
                                    true);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_COMMENT_SUBMIT_INPUTTED
                && mEventType == AccessibilityEvent.TYPE_VIEW_SELECTED
                && "android.widget.RatingBar".equals(mEventClassName)) {

            Observable.just(true)
                    .delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("发布")),
                                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            notifyFinish();
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_COMMENT_LOADED
                && mEventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                && "android.widget.Toast$TN".equals(mEventClassName)) {
            notifyError(new RuntimeException("not installed"));
        }
    }

    private void openOMarket(String packageName) {
        mService.startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("market://details?id=" + packageName))
                .setComponent(new ComponentName("com.oppo.market", "com.oppo.market.activity.ProductDetailActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}
