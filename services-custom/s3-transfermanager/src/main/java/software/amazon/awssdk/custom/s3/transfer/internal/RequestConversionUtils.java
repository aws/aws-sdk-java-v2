/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/**
 * Utility class for converting between similar S3 requests. E.g. GetObjectRequest -> HeadObjectRequest.
 */
//TODO: This can be replaced by code generation, which will also be more reliable
@SdkInternalApi
final class RequestConversionUtils {
    private RequestConversionUtils() {
    }

    /**
     * Convert {@link GetObjectRequest} to a {@link HeadObjectRequest}.
     */
    static HeadObjectRequest toHeadObjectRequest(GetObjectRequest getObjectRequest) {
        return HeadObjectRequest.builder()
                .bucket(getObjectRequest.bucket())
                .ifMatch(getObjectRequest.ifMatch())
                .ifModifiedSince(getObjectRequest.ifModifiedSince())
                .ifNoneMatch(getObjectRequest.ifNoneMatch())
                .ifUnmodifiedSince(getObjectRequest.ifUnmodifiedSince())
                .key(getObjectRequest.key())
                .partNumber(getObjectRequest.partNumber())
                .range(getObjectRequest.range())
                .requestPayer(getObjectRequest.requestPayerAsString())
                .sseCustomerAlgorithm(getObjectRequest.sseCustomerAlgorithm())
                .sseCustomerKey(getObjectRequest.sseCustomerKey())
                .sseCustomerKeyMD5(getObjectRequest.sseCustomerKeyMD5())
                .versionId(getObjectRequest.versionId())
                .build();
    }
}
