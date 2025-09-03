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

package software.amazon.awssdk.utils;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Utility for thread-local context storage.
 */
@SdkInternalApi
public final class ThreadStorage {
    private static final ThreadLocal<Map<String, String>> STORAGE = ThreadLocal.withInitial(HashMap::new);

    private ThreadStorage() {
    }

    public static void put(String key, String value) {
        STORAGE.get().put(key, value);
    }

    public static String get(String key) {
        return STORAGE.get().get(key);
    }

    public static String remove(String key) {
        return STORAGE.get().remove(key);
    }

    public static void clear() {
        STORAGE.get().clear();
    }

    public static boolean containsKey(String key) {
        return STORAGE.get().containsKey(key);
    }
}