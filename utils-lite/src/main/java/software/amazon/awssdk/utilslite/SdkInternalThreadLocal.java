/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.utilslite;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Utility for thread-local context storage.
 */
@ThreadSafe
@SdkProtectedApi
public final class SdkInternalThreadLocal {
    private static final ThreadLocal<Map<String, String>> STORAGE = new InheritableThreadLocal<>();

    private SdkInternalThreadLocal() {
    }

    public static void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Map<String, String> map = STORAGE.get();
        if (value == null) {
            if (map != null) {
                map.remove(key);
                if (map.isEmpty()) {
                    STORAGE.remove();
                }
            }
        } else {
            if (map == null) {
                map = new HashMap<>();
                STORAGE.set(map);
            }
            map.put(key, value);
        }
    }

    public static String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Map<String, String> map = STORAGE.get();
        return map != null ? map.get(key) : null;
    }

    public static String remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Map<String, String> map = STORAGE.get();
        return map != null ? map.remove(key) : null;
    }

    public static void clear() {
        STORAGE.remove();
    }

    public static boolean containsKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        Map<String, String> map = STORAGE.get();
        return map != null && map.containsKey(key);
    }
}