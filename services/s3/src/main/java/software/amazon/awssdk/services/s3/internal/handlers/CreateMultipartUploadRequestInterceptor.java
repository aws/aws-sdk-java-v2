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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;

@SdkInternalApi
public class CreateMultipartUploadRequestInterceptor implements ExecutionInterceptor {

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {
        if (context.request() instanceof CreateMultipartUploadRequest) {
            return Optional.of(RequestBody.fromInputStream(new ByteArrayInputStream(new byte[0]), 0));
        }

        return context.requestBody();
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        if (context.request() instanceof CreateMultipartUploadRequest) {
            SdkHttpRequest.Builder builder = context.httpRequest()
                                                    .toBuilder()
                                                    .putHeader(CONTENT_LENGTH, String.valueOf(0));

            if (!context.httpRequest().firstMatchingHeader(CONTENT_TYPE).isPresent()) {
                builder.putHeader(CONTENT_TYPE, "binary/octet-stream");
            }

            return builder.build();
        }

        return context.httpRequest();
    }
}
