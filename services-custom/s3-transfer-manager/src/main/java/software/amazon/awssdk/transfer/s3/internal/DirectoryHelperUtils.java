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

package software.amazon.awssdk.transfer.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class DirectoryHelperUtils {

    private DirectoryHelperUtils() {
    }

    /**
     * If the prefix is not empty AND the key contains the delimiter, normalize the key by stripping the prefix from the key. If a
     * delimiter is null (not provided by user), use "/" by default.
     * For example: given a request with prefix = "notes/2021"  or
     * "notes/2021/", delimiter = "/" and key = "notes/2021/1.txt", the normalized key should be "1.txt".
     * If the prefix is not the full name of the folder, the folder name will be truncated. For example: given a request
     * with prefix = "top-" , delimiter = "/" and key = "top-level/sub-folder/1.txt", the normalized key should be
     * "level/sub-folder/1.txt"
     */
    static String normalizeKey(String prefix,
                                      String key,
                                      String delimiter) {
        Validate.notNull(delimiter, "delimiter must not be null");

        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(prefix)) {
            return key;
        }

        if (!key.startsWith(prefix)) {
            return key;
        }

        if (!key.contains(delimiter)) {
            return key;
        }

        String stripped = key.substring(prefix.length());
        if (prefix.endsWith(delimiter) || !stripped.startsWith(delimiter)) {
            return stripped;
        }

        return stripped.substring(1);
    }

}
