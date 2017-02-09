package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.FileUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.Utils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * 浏览公众号文章及点赞 2016-11-14 completed
 *
 * @author LCJ
 */
public class ViewArticleJob extends WeChatJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_MORE = 2;
    private static final int STATE_SCAN = 21;
    private static final int STATE_CHOOSE_QR_CODE = 22;
    private static final int STATE_ALBUM_PREVIEW = 23;

    private static final int STATE_WEB_VIEW = 3;
    private static final int STATE_WEB_VIEW_LOADED = 31;
    private static final int STATE_WEB_VIEW_SCROLLED = 32;

    private static final int STATE_WEB_VIEW_AD_CLICKED = 4;

    private int mState = STATE_NONE;

    private int mCurrentScrollY;

    private int mMaxScrollY;

    private boolean containViewSource;
    private boolean containAd;
    private boolean containComment;

    private int mScreenWidth;
    private int mScreenHeight;

    private Subscription mSubscription;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        if (mScreenWidth == 0) {
            mScreenWidth = DimenUtils.getScreenWidth(mService);
        }
        if (mScreenHeight == 0) {
            mScreenHeight = DimenUtils.getScreenHeight(mService);
        }
        try {
            handleFromToArticle();
            if (Utils.getValue(getJobData().viewArticleType) == 5) {
                handleGetMaxScrollY();
            } else {
                handleArticle();
            }
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromToArticle() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(mEventClassName)) {

            mState = STATE_HOME;
            Observable.just(getJobData().articleUrl)
                    .map(new Func1<String, String>() {
                        @Override
                        public String call(String s) {
                            return DownloadUtils.getContent(s);
                        }
                    })
                    .map(new Func1<String, String>() {
                        @Override
                        public String call(String s) {
                            containViewSource = s != null && s.contains("js_view_source");
                            containAd = Utils.getValue(getJobData().containAd);
                            containComment = s != null && s.contains("js_cmt_area");
                            return s;
                        }
                    })
                    .map(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            File destination = mService.getExternalFilesDir("download");
                            FileUtils.deleteDir(destination);
                            File qrCodeFile = FileUtils.generateQRCode(getJobData().articleUrl, destination);
                            if (qrCodeFile != null) {
                                mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + destination)));
                            }
                            return qrCodeFile != null;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .delay(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (aBoolean) {
                                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多功能按钮")),
                                        AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                            } else {
                                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
                            }
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_HOME
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.widget.FrameLayout".equals(mEventClassName)) {

            mState = STATE_HOME_MORE;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, new AccessibilityNodeInfoCompat(mEvent.getSource()), new TextNodeFilter("扫一扫")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_HOME_MORE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.scanner.ui.BaseScanUI".equals(mEventClassName)) {

            mState = STATE_SCAN;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("更多")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_SCAN
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "android.support.design.widget.c".equals(mEventClassName)) {

            mState = STATE_CHOOSE_QR_CODE;
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(mService,
                    AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("从相册选取二维码")),
                    AccessibilityNodeInfoUtils.FILTER_CLICKABLE).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_CHOOSE_QR_CODE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(mEventClassName)) {

            Observable.just(true).delay(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {

                            mState = STATE_ALBUM_PREVIEW;
                            AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new NodeFilter() {
                                @Override
                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                    return "android.widget.GridView".equals(node.getClassName().toString()) && node.isFocused();
                                }
                            }).getChild(1).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_ALBUM_PREVIEW
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.webview.ui.tools.WebViewUI".equals(mEventClassName)) {

            mState = STATE_WEB_VIEW;
        }
    }

    private void handleGetMaxScrollY() {
        if (mState == STATE_WEB_VIEW
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "org.chromium.content.browser.ContentViewCore".equals(mEventClassName)) {

            getJobData().maxScrollY = mEvent.getScrollY();
        } else if (mState == STATE_WEB_VIEW
                && mEventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && "android.widget.LinearLayout".equals(mEventClassName)
                && mEvent.getText().toString().contains("返回")) {
            notifyFinish();
        }
    }

    private void handleArticle() {
        if (mState == STATE_WEB_VIEW
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "org.chromium.content.browser.ContentViewCore".equals(mEventClassName)) {

            mCurrentScrollY = mEvent.getScrollY();
        }

        if (mState == STATE_WEB_VIEW
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.webview.ui.tools.WebViewUI".equals(mEventClassName)) {

            mSubscription = Observable.just(true)
                    .delay(Utils.getValue(getJobData().viewArticleType) == 1 ? 5 : 15, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (Utils.getValue(getJobData().viewArticleType) == 1) {
                                notifyFinish();
                            } else {
                                mState = STATE_WEB_VIEW_LOADED;

                                int fixedX = mScreenWidth - DimenUtils.dipToPixels(10, mService);
                                int startY = mScreenHeight - DimenUtils.dipToPixels(100, mService);
                                int endY = startY - 100;
                                if (mCurrentScrollY > 0) {
                                    startY = endY;
                                    endY = startY + 100;
                                }
                                ShellUtils.execCommand(String.format(Locale.getDefault(), "input swipe %d %d %d %d", fixedX, startY, fixedX, endY), true);
                                Timber.d("input swipe %d %d %d %d", fixedX, startY, fixedX, endY);
                            }
                        }
                    }, mAction1Throwable);
        } else if ((mState == STATE_WEB_VIEW_LOADED || mState == STATE_WEB_VIEW_SCROLLED)
                && mEventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "org.chromium.content.browser.ContentViewCore".equals(mEventClassName)) {

            mState = STATE_WEB_VIEW_SCROLLED;
            mCurrentScrollY = mEvent.getScrollY();
            if (containComment) {
                mMaxScrollY = Utils.getValue(getJobData().maxScrollY);
                if (mMaxScrollY == 0) {
                    mMaxScrollY = mEvent.getMaxScrollY();
                }
            } else {
                mMaxScrollY = mEvent.getMaxScrollY();
            }
            if (mSubscription != null) {
                mSubscription.unsubscribe();
            }
            mSubscription = Observable.just(true).delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            int diff = mMaxScrollY - mCurrentScrollY;
                            Timber.d("mMaxScrollY: %d  mCurrentScrollY:%d diff:%d", mMaxScrollY, mCurrentScrollY, diff);
                            if (Math.abs(diff) < ViewConfiguration.get(mService).getScaledTouchSlop() * 2 || mCurrentScrollY == mMaxScrollY) {
                                if (mCurrentScrollY == mMaxScrollY) {
                                    diff = 0;
                                }
                                int y = DimenUtils.dipToPixels(28, mService);
                                if (containComment) {
                                    y = y + DimenUtils.dipToPixels(80, mService);
                                }
                                if (containAd) {
                                    y = y + DimenUtils.dipToPixels(158, mService);
                                }
                                y = y - diff;
                                int x = DimenUtils.dipToPixels(120, mService);
                                if (containViewSource) {
                                    x = x + DimenUtils.dipToPixels(75, mService);
                                }

                                if (Utils.getValue(getJobData().viewArticleType) == 1) {
                                    notifyFinish();
                                } else if (Utils.getValue(getJobData().viewArticleType) == 2) {
                                    if (containAd) {
                                        mState = STATE_WEB_VIEW_AD_CLICKED;
                                        ShellUtils.execCommand(String.format(Locale.getDefault(),
                                                "input tap %d %d",
                                                mScreenWidth / 2,
                                                mScreenHeight - DimenUtils.dipToPixels((containComment ? 50 + 80 : 50), mService) - diff),
                                                true);
                                    } else {
                                        notifyFinish();
                                    }
                                } else if (Utils.getValue(getJobData().viewArticleType) == 3) {
                                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", x, mScreenHeight - y), true);
                                    notifyFinish();
                                } else if (Utils.getValue(getJobData().viewArticleType) == 4) {
                                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", x, mScreenHeight - y), true);
                                    if (containAd) {
                                        mState = STATE_WEB_VIEW_AD_CLICKED;
                                        ShellUtils.execCommand(String.format(Locale.getDefault(),
                                                "input tap %d %d",
                                                mScreenWidth / 2,
                                                mScreenHeight - DimenUtils.dipToPixels((containComment ? 50 + 80 : 50), mService) - diff),
                                                true);
                                    } else {
                                        notifyFinish();
                                    }
                                }
                            } else {
                                int bottom = mScreenHeight - DimenUtils.dipToPixels(100, mService);
                                int top = DimenUtils.dipToPixels(100, mService);
                                if (diff > 0) {
                                    if (bottom - top > diff) {
                                        top = bottom - diff;
                                    }
                                } else if (diff < 0) {
                                    if (bottom - top > Math.abs(diff)) {
                                        bottom = top - diff;
                                    }
                                    bottom = bottom + top;
                                    top = bottom - top;
                                    bottom = bottom - top;
                                }
                                if (!containComment) {
                                    bottom = mScreenHeight - DimenUtils.dipToPixels(100, mService);
                                    top = DimenUtils.dipToPixels(100, mService);
                                }
                                final int fixedX = DimenUtils.dipToPixels(4, mService);
                                final int startY = bottom;
                                final int endY = top;
                                ShellUtils.execCommand(String.format(Locale.getDefault(), "input swipe %d %d %d %d", fixedX, startY, fixedX, endY), true);
                                Timber.d("input swipe %d %d %d %d", fixedX, startY, fixedX, endY);
                            }
                        }
                    }, mAction1Throwable);
        } else if (mState == STATE_WEB_VIEW_AD_CLICKED
                && mEventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.TextView".equals(mEventClassName)) {
            notifyFinish();
        }
    }
}
