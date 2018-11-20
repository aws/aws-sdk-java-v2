/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_CHECKSUM_REQUEST_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.S3_MD5_CHECKSUM_LENGTH;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@SdkInternalApi
public class EnableTrailingChecksumInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        if (context.request() instanceof GetObjectRequest && checksumValidationEnabled(executionAttributes)) {
            return context.httpRequest().toBuilder().putHeader(ENABLE_CHECKSUM_REQUEST_HEADER,
                                                               ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                          .build();
        }

        return context.httpRequest();
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        SdkHttpResponse httpResponse = context.httpResponse();

        if (response instanceof GetObjectResponse && httpResponse.firstMatchingHeader(CHECKSUM_ENABLED_RESPONSE_HEADER)
                                                                 .isPresent()) {
            GetObjectResponse getResponse = (GetObjectResponse) response;
            return getResponse.toBuilder().contentLength(getResponse.contentLength() - S3_MD5_CHECKSUM_LENGTH).build();
        }

        return response;
    }

    private boolean checksumValidationEnabled(ExecutionAttributes executionAttributes) {

        S3Configuration serviceConfiguration =
            (S3Configuration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);

        return serviceConfiguration == null || serviceConfiguration.checksumValidationEnabled();
    }
}
