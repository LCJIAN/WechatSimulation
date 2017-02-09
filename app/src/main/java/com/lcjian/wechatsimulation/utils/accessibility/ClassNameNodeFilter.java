package com.lcjian.wechatsimulation.utils.accessibility;

import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;

public class ClassNameNodeFilter extends NodeFilter {

    private String className;

    public ClassNameNodeFilter(String className) {
        this.className = className;
    }

    @Override
    public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
        return className.equals(node.getClassName().toString());
    }
}
