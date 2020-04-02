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

package software.amazon.awssdk.services.s3.internal.handlers;

import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_CHECKSUM_REQUEST_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.S3_MD5_CHECKSUM_LENGTH;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerRequest;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerResponse;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class EnableTrailingChecksumInterceptor implements ExecutionInterceptor {

    /**
     * Append trailing checksum header for {@link GetObjectRequest} if trailing checksum is enabled from config.
     */
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        if (getObjectChecksumEnabledPerRequest(context.request(), executionAttributes)) {
            return context.httpRequest().toBuilder().putHeader(ENABLE_CHECKSUM_REQUEST_HEADER,
                                                               ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                          .build();
        }

        return context.httpRequest();
    }

    /**
     * Subtract the contentLength of {@link GetObjectResponse} if trailing checksums is enabled.
     */
    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        SdkHttpResponse httpResponse = context.httpResponse();

        if (getObjectChecksumEnabledPerResponse(context.request(), httpResponse)) {
            GetObjectResponse getResponse = (GetObjectResponse) response;
            Long contentLength = getResponse.contentLength();
            Validate.notNull(contentLength, "Service returned null 'Content-Length'.");
            return getResponse.toBuilder()
                              .contentLength(contentLength - S3_MD5_CHECKSUM_LENGTH)
                              .build();
        }

        return response;
    }
}
