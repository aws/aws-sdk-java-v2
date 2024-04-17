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

package software.amazon.awssdk.migration.internal.utils;

import java.util.regex.Pattern;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Type creation and checking utilities.
 */
@SdkInternalApi
public final class SdkTypeUtils {
    private static final Pattern V1_SERVICE_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");
    private static final Pattern V1_SERVICE_MODEL_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.model\\.[a-zA-Z0-9]+");
    private static final Pattern V2_MODEL_BUILDER_PATTERN =
        Pattern.compile("software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.model\\.[a-zA-Z0-9]+\\.Builder");
    private static final Pattern V2_MODEL_CLASS_PATTERN = Pattern.compile(
        "software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.model\\..[a-zA-Z0-9]+");

    private SdkTypeUtils() {
    }

    public static boolean isV1Class(JavaType type) {
        return type != null && type.isAssignableFrom(V1_SERVICE_CLASS_PATTERN);
    }

    public static boolean isV1ModelClass(JavaType type) {
        return type != null
                && type instanceof JavaType.FullyQualified
                && type.isAssignableFrom(V1_SERVICE_MODEL_CLASS_PATTERN);
    }

    public static boolean isV2ModelBuilder(JavaType type) {
        return type != null
                && type.isAssignableFrom(V2_MODEL_BUILDER_PATTERN);
    }

    public static boolean isV2ModelClass(JavaType type) {
        return type != null
                && type.isAssignableFrom(V2_MODEL_CLASS_PATTERN);
    }

    public static JavaType.FullyQualified asV2Type(JavaType.FullyQualified type) {
        if (!isV1ModelClass(type)) {
            throw new IllegalArgumentException(String.format("%s is not a V1 SDK model type", type));
        }

        String className = type.getClassName();
        String packageName = type.getPackageName();

        packageName = StringUtils.replaceOnce(packageName, "com.amazonaws", "software.amazon.awssdk");

        return TypeUtils.asFullyQualified(JavaType.buildType(String.format("%s.%s", packageName, className)));
    }

    public static JavaType.FullyQualified v2ModelBuilder(JavaType.FullyQualified type) {
        if (!isV2ModelClass(type)) {
            throw new IllegalArgumentException(String.format("%s is not a V2 model class", type));
        }

        String fqcn = String.format("%s.%s", type.getFullyQualifiedName(), "Builder");

        return TypeUtils.asFullyQualified(JavaType.buildType(fqcn));
    }
}
