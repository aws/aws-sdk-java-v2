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

package software.amazon.awssdk.benchmark.apache5.utility;

import software.amazon.awssdk.utils.JavaSystemSetting;

public final class BenchmarkUtilities {

    private BenchmarkUtilities() {
    }

    public static boolean isJava21OrHigher() {
        String version = JavaSystemSetting.JAVA_VERSION.getStringValueOrThrow();
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dotPos = version.indexOf('.');
        int majorVersion;
        if (dotPos != -1) {
            majorVersion = Integer.parseInt(version.substring(0, dotPos));
        } else {
            majorVersion = Integer.parseInt(version);
        }
        return majorVersion >= 21;
    }
}

