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

package software.amazon.awssdk.archtests;

import java.util.regex.Pattern;

public final class ArchUtils {

    private ArchUtils() {
    }

    public static Pattern classNameToPattern(Class<?> clazz) {
        return Pattern.compile(".*/" + clazz.getCanonicalName().replace('.', '/') + ".class");
    }

    public static boolean resideInSameRootPackage(String pkg1, String pkg2) {
        if (pkg1.equals(pkg2) || pkg1.startsWith(pkg2) || pkg2.startsWith(pkg1)) {
            return true;
        }
        String root1 = findRootPackage(pkg1);
        String root2 = findRootPackage(pkg2);
        return root1.equals(root2);
    }

    public static String findRootPackage(String pkg) {
        if (pkg.startsWith("software.amazon.awssdk.services.")) {
            int serviceLength = pkg.replace("software.amazon.awssdk.services.", "").indexOf(".");
            return serviceLength == 0 ? pkg : pkg.substring(0, "software.amazon.awssdk.services.".length() + serviceLength);
        }

        int moduleLength = pkg.replace("software.amazon.awssdk.", "").indexOf(".");
        return  pkg.substring(0, "software.amazon.awssdk.".length() + moduleLength);
    }
}
