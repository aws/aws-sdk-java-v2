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

package software.amazon.awssdk.migration.recipe.utils;

import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

@SdkInternalApi
public final class NamingConversionUtils {

    private static final String V1_PACKAGE_PREFIX = "com.amazonaws.services";
    private static final String V2_PACKAGE_PREFIX = "software.amazon.awssdk.services";

    private NamingConversionUtils() {
    }

    public static String getV2Equivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf(".");
        String v1ClassName = currentFqcn.substring(lastIndexOfDot + 1, currentFqcn.length());
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);

        String v2ClassName;
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);

        if (Stream.of("Abstract", "Amazon", "AWS").anyMatch(v1ClassName::startsWith)) {
            v2ClassName = getV2ClientEquivalent(v1ClassName);
        } else {
            v2ClassName = v1ClassName.replace("Result", "Response");
        }

        return v2PackagePrefix + "." + v2ClassName;
    }

    private static String getV2ClientEquivalent(String className) {
        if (className.startsWith("Abstract")) {
            className = className.substring(8);
        }
        if (className.startsWith("Amazon")) {
            className = className.substring(6);
        } else if (className.startsWith("AWS")) {
            className = className.substring(3);
        }

        String v2Style = CodegenNamingUtils.pascalCase(className);

        if (!className.endsWith("Client") && !className.endsWith("Builder")) {
            v2Style = v2Style + "Client";
        }

        return v2Style;
    }
}
