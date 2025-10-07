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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Interceptor to add an 'Expect: 100-continue' header to the HTTP Request if it represents a PUT Object request.
 */
@SdkInternalApi
//TODO: This should be generalized for all streaming requests
public final class StreamingRequestInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        if (shouldAddExpectContinueHeader(context)) {
            return context.httpRequest().toBuilder().putHeader("Expect", "100-continue").build();
        }
        return context.httpRequest();
    }

    /**
     * Determines whether to add 'Expect: 100-continue' header to streaming requests.
     *
     * Per RFC 9110 Section 10.1.1, clients MUST NOT send 100-continue for requests without content.
     *
     * Note: Empty Content length check currently applies to sync clients only. Sync HTTP clients (e.g., Apache HttpClient) may
     * reuse connections, and sending empty content with Expect header can cause issues if the server has already closed the
     * connection.
     *
     * @param context the HTTP request modification context
     * @return true if Expect header should be added, false otherwise
     */
    private boolean shouldAddExpectContinueHeader(Context.ModifyHttpRequest context) {
        // Must be a streaming request type
        if (context.request() instanceof PutObjectRequest
            || context.request() instanceof UploadPartRequest) {
            // Zero Content length check
            return context.requestBody()
                          .flatMap(RequestBody::optionalContentLength)
                          .map(length -> length != 0L)
                          .orElse(true);
        }
        return false;
    }




}
