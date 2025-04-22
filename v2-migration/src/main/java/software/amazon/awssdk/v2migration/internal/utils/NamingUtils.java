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

package software.amazon.awssdk.v2migration.internal.utils;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

@SdkInternalApi
public final class NamingUtils {
    private NamingUtils() {
    }

    public static String removeWith(String name) {
        return removePrefix(name, "with");
    }

    public static String removeSet(String name) {
        return removePrefix(name, "set");
    }

    public static String removeGet(String name) {
        return removePrefix(name, "get");
    }

    public static String removeEntity(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }

        if (!name.endsWith("Entity")) {
            return name;
        }

        return name.substring(0, name.length() - 6);
    }

    private static String removePrefix(String name, String prefix) {
        if (StringUtils.isBlank(name)) {
            return name;
        }

        if (!name.startsWith(prefix)) {
            return name;
        }

        name = StringUtils.replaceOnce(name, prefix, "");

        return StringUtils.uncapitalize(CodegenNamingUtils.pascalCase(name));
    }

    public static boolean isWither(String name) {
        return !StringUtils.isBlank(name) && name.startsWith("with");
    }

    public static boolean isSetter(String name) {
        return !StringUtils.isBlank(name) && name.startsWith("set");
    }

    public static boolean isGetter(String name) {
        return !StringUtils.isBlank(name) && name.startsWith("get") && !name.equals("get");
    }
}
