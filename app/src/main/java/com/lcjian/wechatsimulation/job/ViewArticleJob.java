package com.lcjian.wechatsimulation.job;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import com.lcjian.wechatsimulation.exception.JobException;
import com.lcjian.wechatsimulation.utils.AccessibilityHelper;
import com.lcjian.wechatsimulation.utils.DimenUtils;
import com.lcjian.wechatsimulation.utils.DownloadUtils;
import com.lcjian.wechatsimulation.utils.ShellUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityNodeInfoUtils;
import com.lcjian.wechatsimulation.utils.accessibility.AccessibilityServiceCompatUtils;
import com.lcjian.wechatsimulation.utils.accessibility.NodeFilter;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class ViewArticleJob extends BaseJob {

    private static final int STATE_NONE = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_HOME_CONTACTS = 11;
    private static final int STATE_CONTACT_DETAIL = 2;

    private static final int STATE_CHATTING = 3;
    private static final int STATE_CHATTING_INPUT_FOCUSED = 31;
    private static final int STATE_CHATTING_INPUT_READY = 32;
    private static final int STATE_CHATTING_ARTICLE_SEND = 33;

    private static final int STATE_ARTICLE_FIND = 41;

    private static final int STATE_WEB_VIEW = 5;
    private static final int STATE_WEB_VIEW_FOCUS_CLEARED = 6;
    private static final int STATE_WEB_VIEW_ACCESSIBILITY_FOCUSED = 61;
    private static final int STATE_WEB_VIEW_LOADED = 62;
    private static final int STATE_WEB_VIEW_FOCUSED = 7;
    private static final int STATE_WEB_VIEW_SCROLLED = 8;

    private static final int STATE_WEB_VIEW_AD_CLICKED = 9;
    private static final int STATE_WEB_VIEW_AD_FOCUS_CLEARED = 10;

    private int mState = STATE_NONE;

    private int mCurrentScrollY;

    private int mMaxScrollY;

    private boolean mFirst = true;

    private boolean containViewSource;
    private boolean containAd;
    private boolean containComment;

    private Subscription mSubscription;

    @Override
    public void doWithEvent(AccessibilityService service, AccessibilityEvent event) {
        try {

            AccessibilityNodeInfoCompat rootNodeInfoCompat = AccessibilityServiceCompatUtils.getRootInActiveWindow(service);
            int eventType = event.getEventType();
            String className = event.getClassName().toString();

            handleFromHomeToChat(service, event, rootNodeInfoCompat, eventType, className);
            handleFromChatToArticle(service, event, rootNodeInfoCompat, eventType, className);
            handleFromArticleToAd(service, event, rootNodeInfoCompat, eventType, className);

        } catch (Exception e) {
            notifyError(e);
        }
        Timber.d("state:%d", mState);
    }

    private void handleFromArticleToAd(final AccessibilityService service,
                                       AccessibilityEvent event,
                                       AccessibilityNodeInfoCompat rootNodeInfoCompat,
                                       int eventType,
                                       String className) {
        if (mState == STATE_ARTICLE_FIND
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.webview.ui.tools.WebViewUI".equals(className)) {

            mState = STATE_WEB_VIEW;
        } else if (mState == STATE_WEB_VIEW
                && eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
                && "android.webkit.WebView".equals(className)) {

            mState = STATE_WEB_VIEW_FOCUS_CLEARED;
        } else if (mState == STATE_WEB_VIEW_FOCUS_CLEARED
                && eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                && "android.webkit.WebView".equals(className)) {

            mState = STATE_WEB_VIEW_ACCESSIBILITY_FOCUSED;
        } else if (mState == STATE_WEB_VIEW_ACCESSIBILITY_FOCUSED
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "org.chromium.content.browser.ContentViewCore".equals(className)) {

            mCurrentScrollY = event.getScrollY();
        } else if (mState == STATE_WEB_VIEW_ACCESSIBILITY_FOCUSED
                && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "org.chromium.content.browser.ContentViewCore".equals(className)) {

            if (mFirst) {
                mState = STATE_CHATTING;
                mFirst = false;

                final Rect outBounds = new Rect();
                AccessibilityNodeInfoCompat webViewNodeParentParent = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, AccessibilityNodeInfoUtils.FILTER_SCROLLABLE);
                if (webViewNodeParentParent != null) {
                    webViewNodeParentParent.getBoundsInScreen(outBounds);
                }
                Observable.just(true).delay(6, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> call(Boolean aBoolean) {
                                String content = DownloadUtils.getContent(getJobData().articleUrl);
                                containViewSource = content != null && content.contains("js_view_source");
                                containAd = getJobData().containAd;
                                containComment = content != null && content.contains("js_cmt_area");
                                return Observable.just(aBoolean);
                            }
                        })
                        .delay(1, TimeUnit.SECONDS)
                        .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> call(Boolean aBoolean) {
                                ShellUtils.execCommand(String.format(Locale.getDefault(),
                                        "input swipe %d %d %d %d", outBounds.centerX(), outBounds.top + 20, outBounds.centerX(), outBounds.bottom - 20), true);
                                return Observable.just(aBoolean);
                            }
                        })
                        .delay(3, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                AccessibilityHelper.performBack(service);
                            }
                        });
            } else {
                mState = STATE_WEB_VIEW_LOADED;
                mMaxScrollY = event.getMaxScrollY();
            }
        }
        if ((mState == STATE_WEB_VIEW_LOADED)
                || ((mState == STATE_WEB_VIEW_FOCUSED || mState == STATE_WEB_VIEW_SCROLLED)
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "org.chromium.content.browser.ContentViewCore".equals(className))) {

            if (mState == STATE_WEB_VIEW_LOADED) {
                mState = STATE_WEB_VIEW_FOCUSED;
            } else {
                mState = STATE_WEB_VIEW_SCROLLED;
                mCurrentScrollY = event.getScrollY();
                if (!containComment) {
                    mMaxScrollY = event.getMaxScrollY();
                }
            }
            final int mCurrentMaxScrollY = event.getMaxScrollY();
            AccessibilityNodeInfoCompat webViewNodeParentParent = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, AccessibilityNodeInfoUtils.FILTER_SCROLLABLE);
            if (webViewNodeParentParent != null) {
                final Rect outBounds = new Rect();
                webViewNodeParentParent.getBoundsInScreen(outBounds);
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
                                if (Math.abs(diff) < ViewConfiguration.get(service).getScaledTouchSlop() * 2 || mCurrentScrollY == mCurrentMaxScrollY) {
                                    if (mCurrentScrollY == mCurrentMaxScrollY) {
                                        diff = 0;
                                    }
                                    int y = DimenUtils.dipToPixels(28, service);
                                    if (containComment) {
                                        y = y + DimenUtils.dipToPixels(80, service);
                                    }
                                    if (containAd) {
                                        y = y + DimenUtils.dipToPixels(158, service);
                                    }
                                    y = y - diff;
                                    int x = DimenUtils.dipToPixels(120, service);
                                    if (containViewSource) {
                                        x = x + DimenUtils.dipToPixels(75, service);
                                    }

                                    if (getJobData().viewArticleType == 1) {
                                        notifyFinish();
                                    } else if (getJobData().viewArticleType == 2) {
                                        if (containAd) {
                                            mState = STATE_WEB_VIEW_AD_CLICKED;
                                            ShellUtils.execCommand(String.format(Locale.getDefault(),
                                                    "input tap %d %d",
                                                    outBounds.centerX(),
                                                    outBounds.bottom - DimenUtils.dipToPixels((containComment ? 50 + 80 : 50), service) - diff),
                                                    true);
                                        } else {
                                            notifyFinish();
                                        }
                                    } else if (getJobData().viewArticleType == 3) {
                                        ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", x, outBounds.bottom - y), true);
                                        notifyFinish();
                                    } else {
                                        ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", x, outBounds.bottom - y), true);
                                        if (containAd) {
                                            mState = STATE_WEB_VIEW_AD_CLICKED;
                                            ShellUtils.execCommand(String.format(Locale.getDefault(),
                                                    "input tap %d %d",
                                                    outBounds.centerX(),
                                                    outBounds.bottom - DimenUtils.dipToPixels((containComment ? 50 + 80 : 50), service) - diff),
                                                    true);
                                        } else {
                                            notifyFinish();
                                        }
                                    }
                                } else {
                                    int bottom = outBounds.bottom - 20;
                                    int top = outBounds.top + 20;
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
                                        bottom = outBounds.bottom - 20;
                                        top = outBounds.top + 20;
                                    }
                                    final int centerX = outBounds.centerX();
                                    final int startY = bottom;
                                    final int endY = top;
                                    ShellUtils.execCommand(String.format(Locale.getDefault(), "input swipe %d %d %d %d", centerX, startY, centerX, endY), true);
                                    Timber.d("input swipe %d %d %d %d", centerX, startY, centerX, endY);
                                }
                            }
                        });
            } else {
                throw new JobException(JobException.MESSAGE_SYSTEM_ERROR);
            }

        } else if (mState == STATE_WEB_VIEW_AD_CLICKED
                && eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
                && "android.webkit.WebView".equals(className)) {

            mState = STATE_WEB_VIEW_AD_FOCUS_CLEARED;
        } else if (mState == STATE_WEB_VIEW_AD_FOCUS_CLEARED
                && eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                && "android.webkit.WebView".equals(className)) {
            notifyFinish();
        }
    }

    private void handleFromChatToArticle(final AccessibilityService service,
                                         AccessibilityEvent event,
                                         final AccessibilityNodeInfoCompat rootNodeInfoCompat,
                                         int eventType,
                                         String className) {
        if (mState == STATE_CHATTING
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.chatting.ChattingUI".equals(className)) {

            if (mFirst) {
                mState = STATE_CHATTING_INPUT_FOCUSED;
                AccessibilityNodeInfoCompat input = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                    @Override
                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                        return "android.widget.EditText".equals(node.getClassName().toString());
                    }
                });
                input.performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
            } else {
                mState = STATE_ARTICLE_FIND;
                Observable.just(true).delay(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                AccessibilityNodeInfoCompat messages = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                                    @Override
                                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                        return "android.widget.ListView".equals(node.getClassName().toString());
                                    }
                                });
                                AccessibilityNodeInfoCompat lastMessage = AccessibilityNodeInfoUtils.searchFromBfs(service, messages.getChild(messages.getChildCount() - 1), new NodeFilter() {
                                    @Override
                                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                        return "android.widget.TextView".equals(node.getClassName().toString()) && node.isClickable() && node.isEnabled();
                                    }
                                });
                                Rect outBounds = new Rect();
                                lastMessage.getBoundsInScreen(outBounds);
                                ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                            }
                        });
            }
        } else if (mState == STATE_CHATTING_INPUT_FOCUSED
                && eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                && "android.widget.EditText".equals(className)) {

            mState = STATE_CHATTING_INPUT_READY;
            ((ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("article_url", getJobData().articleUrl));
            new AccessibilityNodeInfoCompat(event.getSource()).performAction(AccessibilityNodeInfoCompat.ACTION_PASTE);
        } else if (mState == STATE_CHATTING_INPUT_READY
                && eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && "android.widget.EditText".equals(className)) {

            mState = STATE_CHATTING_ARTICLE_SEND;
            AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.Button".equals(node.getClassName().toString())
                            && node.isClickable()
                            && node.isEnabled()
                            && (AccessibilityNodeInfoUtils.getNodeText(node) != null && "发送".equals(AccessibilityNodeInfoUtils.getNodeText(node).toString()));
                }
            }).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if (mState == STATE_CHATTING_ARTICLE_SEND
                && eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && "android.widget.ListView".equals(className)) {

            mState = STATE_ARTICLE_FIND;
            Observable.just(true).delay(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            AccessibilityNodeInfoCompat messages = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                                @Override
                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                    return "android.widget.ListView".equals(node.getClassName().toString());
                                }
                            });
                            AccessibilityNodeInfoCompat lastMessage = AccessibilityNodeInfoUtils.searchFromBfs(service, messages.getChild(messages.getChildCount() - 1), new NodeFilter() {
                                @Override
                                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                                    return "android.widget.TextView".equals(node.getClassName().toString()) && node.isClickable() && node.isEnabled();
                                }
                            });
                            Rect outBounds = new Rect();
                            lastMessage.getBoundsInScreen(outBounds);
                            ShellUtils.execCommand(String.format(Locale.getDefault(), "input tap %d %d", outBounds.centerX(), outBounds.centerY()), true);
                        }
                    });
        }
    }

    private void handleFromHomeToChat(AccessibilityService service,
                                      AccessibilityEvent event,
                                      AccessibilityNodeInfoCompat rootNodeInfoCompat,
                                      int eventType,
                                      String className) {
        if (mState == STATE_NONE
                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.ui.LauncherUI".equals(className)) {

            mState = STATE_HOME;
            AccessibilityNodeInfoCompat contactsTab = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return AccessibilityNodeInfoUtils.getNodeText(node) != null && "通讯录".equals(AccessibilityNodeInfoUtils.getNodeText(node).toString());
                }
            });
            AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(service, contactsTab, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return AccessibilityNodeInfoUtils.isClickable(node);
                }
            }).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        } else if ((mState == STATE_HOME
                && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                && event.getText().toString().contains("通讯录")
                && "android.widget.RelativeLayout".equals(className))
                || (mState == STATE_HOME_CONTACTS
                && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className))) {

            mState = STATE_HOME_CONTACTS;
            AccessibilityNodeInfoCompat contacts = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.ListView".equals(node.getClassName().toString()) && node.isFocused();
                }
            });
            AccessibilityNodeInfoCompat contactName = AccessibilityNodeInfoUtils.searchFromBfs(service, contacts, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return AccessibilityNodeInfoUtils.getNodeText(node) != null && getJobData().contactNameForArticle.equals(AccessibilityNodeInfoUtils.getNodeText(node).toString());
                }
            });
            if (contactName != null) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(service, contactName, new NodeFilter() {
                    @Override
                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                        return AccessibilityNodeInfoUtils.isClickable(node);
                    }
                }).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                mState = STATE_CONTACT_DETAIL;
            } else {
                contacts.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        } else if (mState == STATE_CONTACT_DETAIL
                && ((eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(className))
                || (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && "android.widget.ListView".equals(className)))) {

            AccessibilityNodeInfoCompat contactDetail = AccessibilityNodeInfoUtils.searchFromBfs(service, rootNodeInfoCompat, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return "android.widget.ListView".equals(node.getClassName().toString());
                }
            });
            AccessibilityNodeInfoCompat sendMessage = AccessibilityNodeInfoUtils.searchFromBfs(service, contactDetail, new NodeFilter() {
                @Override
                public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                    return AccessibilityNodeInfoUtils.getNodeText(node) != null && "发消息".equals(AccessibilityNodeInfoUtils.getNodeText(node).toString());
                }
            });
            if (sendMessage != null) {
                AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(service, sendMessage, new NodeFilter() {
                    @Override
                    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
                        return AccessibilityNodeInfoUtils.isClickable(node);
                    }
                }).performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                mState = STATE_CHATTING;
            } else {
                contactDetail.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
        }
    }

}
