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

import static software.amazon.awssdk.transfer.s3.internal.TransferConfigurationOption.DEFAULT_DELIMITER;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class DirectoryHelperUtils {

    private DirectoryHelperUtils() {
    }

    /**
     * If the prefix is not empty AND the key contains the delimiter, normalize the key by stripping the prefix from the key. If a
     * delimiter is null (not provided by user), use "/" by default. For example: given a request with prefix = "notes/2021"  or
     * "notes/2021/", delimiter = "/" and key = "notes/2021/1.txt", the normalized key should be "1.txt".
     */
    static String normalizeKey(ListObjectsV2Request listObjectsRequest,
                                      String key,
                                      String delimiter) {
        Validate.paramNotNull(listObjectsRequest, "listObjectsRequest must not be null");
        String delimiterToUse = Validate.getOrDefault(delimiter, () -> DEFAULT_DELIMITER);

        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(listObjectsRequest.prefix())) {
            return key;
        }

        String prefix = listObjectsRequest.prefix();

        if (!key.contains(delimiterToUse)) {
            return key;
        }

        if (!key.startsWith(prefix)) {
            return key;
        }

        String stripped = key.substring(prefix.length());
        if (prefix.endsWith(delimiterToUse) || !stripped.startsWith(delimiterToUse)) {
            return stripped;
        }

        return stripped.substring(1);
    }

}
