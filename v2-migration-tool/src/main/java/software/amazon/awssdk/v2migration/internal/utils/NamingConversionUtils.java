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

        String v2ClassName = CodegenNamingUtils.pascalCase(v1ClassName);
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        v2PackagePrefix = checkPackageServiceNameForV2Suffix(v2PackagePrefix);

        if (Stream.of("Abstract", "Amazon", "AWS").anyMatch(v1ClassName::startsWith)) {
            v2ClassName = getV2ClientOrExceptionEquivalent(v1ClassName);
        } else if (v1ClassName.endsWith("Result")) {
            int lastIndex = v1ClassName.lastIndexOf("Result");
            v2ClassName = v1ClassName.substring(0, lastIndex) + "Response";
        }

        return v2PackagePrefix + "." + v2ClassName;
    }

    /**
     * Edge cases in v1 package names
     */
    private static String checkPackageServiceNameForV2Suffix(String v2PackagePrefix) {
        if (v2PackagePrefix.contains("dynamodbv2")) {
            return v2PackagePrefix.replace("dynamodbv2", "dynamodb");
        }
        if (v2PackagePrefix.contains("cloudsearchv2")) {
            return v2PackagePrefix.replace("cloudsearchv2", "cloudsearch");
        }
        return v2PackagePrefix;
    }

    public static String getV2ModelPackageWildCardEquivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf(".");
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        return v2PackagePrefix + ".*";
    }

    private static String getV2ClientOrExceptionEquivalent(String className) {
        if (className.startsWith("Abstract")) {
            className = className.substring(8);
        }
        if (className.startsWith("Amazon")) {
            className = className.substring(6);
        } else if (className.startsWith("AWS")) {
            className = className.substring(3);
        }

        String v2Style = CodegenNamingUtils.pascalCase(className);

        if (className.endsWith("Exception")) {
            return v2Style;
        }

        if (!className.endsWith("Client") && !className.endsWith("Builder")) {
            v2Style = v2Style + "Client";
        }

        return v2Style;
    }
}
