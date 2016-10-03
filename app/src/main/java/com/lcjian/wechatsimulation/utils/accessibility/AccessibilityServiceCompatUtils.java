/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.lcjian.wechatsimulation.utils.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityNodeInfo;

public class AccessibilityServiceCompatUtils {

    /**
     * @return root node of the Application window
     */
    public static AccessibilityNodeInfoCompat getRootInActiveWindow(AccessibilityService service) {
        if (service == null) return null;

        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return null;
        return new AccessibilityNodeInfoCompat(root);
    }

}
