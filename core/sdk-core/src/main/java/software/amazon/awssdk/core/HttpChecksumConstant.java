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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.internal.signer.SigningMethod;

/**
 * Defines all the constants that are used while adding and validating Http checksum for an operation.
 */
@SdkInternalApi
public final class HttpChecksumConstant {

    public static final String HTTP_CHECKSUM_HEADER_PREFIX = "x-amz-checksum";
    public static final String X_AMZ_TRAILER = "x-amz-trailer";
    public static final String CONTENT_SHA_256_FOR_UNSIGNED_TRAILER = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";

    public static final String AWS_CHUNKED_HEADER = "aws-chunked";

    public static final ExecutionAttribute<String> HTTP_CHECKSUM_VALUE =
        new ExecutionAttribute<>("HttpChecksumValue");

    public static final ExecutionAttribute<SigningMethod> SIGNING_METHOD =
        new ExecutionAttribute<>("SigningMethod");

    public static final String HEADER_FOR_TRAILER_REFERENCE = "x-amz-trailer";

    /**
     * Default chunk size for Async trailer based checksum data transfer*
     */
    public static final int DEFAULT_ASYNC_CHUNK_SIZE = 16 * 1024;

    private HttpChecksumConstant() {
    }
}
