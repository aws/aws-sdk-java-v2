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

package software.amazon.awssdk.custom.s3.transfer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;

/**
 * Utility methods used for the transfer manager.
 */
@SdkInternalApi
public final class TransferManagerUtilities {
    private static final ApiName API_NAME = ApiName.builder().name("S3TransferManager").version("1.0").build();

    private TransferManagerUtilities() {
    }

    /**
     * Create a formatted byte range header value using the given start and end
     * positions.
     *
     * @param start The start position in the range.
     * @param end The end position in the range.
     *
     * @return The formatted {@code Range} header value.
     */
    // TODO: This is useful for outside of Transfer Manager as well
    public static String rangeHeaderValue(long start, long end) {
        return String.format("bytes=%d-%d", start, end);
    }

    public static ApiName apiName() {
        return API_NAME;
    }
}
