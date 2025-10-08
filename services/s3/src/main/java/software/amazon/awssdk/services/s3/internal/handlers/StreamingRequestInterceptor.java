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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Interceptor to add an 'Expect: 100-continue' header to the HTTP Request if it represents a PUT Object request.
 */
@SdkInternalApi
//TODO: This should be generalized for all streaming requests
public final class StreamingRequestInterceptor implements ExecutionInterceptor {

    private static final String DECODED_CONTENT_LENGTH_HEADER = "x-amz-decoded-content-length";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        if (shouldAddExpectContinueHeader(context)) {
            return context.httpRequest().toBuilder().putHeader("Expect", "100-continue").build();
        }
        return context.httpRequest();
    }

    private boolean shouldAddExpectContinueHeader(Context.ModifyHttpRequest context) {
        // Only applies to streaming operations
        if (!(context.request() instanceof PutObjectRequest
              || context.request() instanceof UploadPartRequest)) {
            return false;
        }
        return getContentLengthHeader(context.httpRequest())
            .map(Long::parseLong)
            .map(length -> length != 0L)
            .orElse(true);
    }

    // Check x-amz-decoded-content-length first, fall back to Content-Length
    private Optional<String> getContentLengthHeader(SdkHttpRequest httpRequest) {
        Optional<String> decodedLength = httpRequest.firstMatchingHeader(DECODED_CONTENT_LENGTH_HEADER);
        return decodedLength.isPresent()
               ? decodedLength
               : httpRequest.firstMatchingHeader(CONTENT_LENGTH_HEADER);
    }
}
