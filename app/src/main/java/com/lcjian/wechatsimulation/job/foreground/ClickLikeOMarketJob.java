package com.lcjian.wechatsimulation.job.foreground;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.ClassNameNodeFilter;
import com.lcjian.wechatsimulation.utils.accessibility.TextNodeFilter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

public class ClickLikeOMarketJob extends OMarketJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;

    private static final int STATE_PRODUCT_DETAIL = 2;
    private static final int STATE_PRODUCT_DETAIL_LOADED = 21;
    private static final int STATE_PRODUCT_DETAIL_COMMENT_FIND = 22;
    private static final int STATE_PRODUCT_DETAIL_COMMENT_FIND_NOT = 23;

    private static final int STATE_COMMENT = 3;
    private static final int STATE_COMMENT_LOADED = 31;

    private int mState = STATE_NONE;

    private Subscription mSubscription;

    private boolean mJobListenerAdded;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        super.doWithEvent(service, event);
        if (!mJobListenerAdded) {
            addJobListener(new JobListener() {
                @Override
                public void onCancelled() {
                    if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                        mSubscription.unsubscribe();
                    }
                }

                @Override
                public void onFinished() {
                    if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                        mSubscription.unsubscribe();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (mSubscription != null && !mSubscription.isUnsubscribed()) {
                        mSubscription.unsubscribe();
                    }
                }
            });
            mJobListenerAdded = true;
        }
        try {
            handleClickLike();
        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleClickLike() {
        if (mState == STATE_NONE
                && mEventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.oppo.market.activity.MainMenuActivity".equals(mEventClassName)) {

            mState = STATE_HOME;
            openOMarket(getJobData().packageNameOMarket);
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
            if (mSubscription == null) {
                mSubscription = Observable.interval(2, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long aLong) {
                                AccessibilityNodeInfoCompat infoCompat =
                                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter(getJobData().commentOMarket));
                                if (infoCompat == null) {
                                    if (AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat, new TextNodeFilter("没有了")) != null) {
                                        notifyError(new RuntimeException("Not found"));
                                    } else {
                                        AccessibilityNodeInfoUtils.searchFromBfs(mService, mRootNodeInfoCompat,
                                                new ClassNameNodeFilter("android.widget.ListView")).performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                                    }
                                } else {
                                    AccessibilityNodeInfoUtils.searchFromBfs(mService, infoCompat.getParent().getParent(),
                                            new ClassNameNodeFilter("android.widget.FrameLayout")).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                                    notifyFinish();
                                }
                            }
                        }, mAction1Throwable);
            }
        }
    }

    private void openOMarket(String packageName) {
        mService.startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("market://details?id=" + packageName))
                .setComponent(new ComponentName("com.oppo.market", "com.oppo.market.activity.ProductDetailActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}
