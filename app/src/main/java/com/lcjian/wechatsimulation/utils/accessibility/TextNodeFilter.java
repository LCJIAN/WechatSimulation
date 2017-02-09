package com.lcjian.wechatsimulation.utils.accessibility;

import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;

public class TextNodeFilter extends NodeFilter {

    private String text;

    public TextNodeFilter(String text) {
        this.text = text;
    }

    @Override
    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
        return AccessibilityNodeInfoUtils.getNodeText(node) != null && text.equals(AccessibilityNodeInfoUtils.getNodeText(node).toString());
    }
}
