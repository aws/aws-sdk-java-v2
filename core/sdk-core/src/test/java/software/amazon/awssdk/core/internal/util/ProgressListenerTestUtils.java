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

package software.amazon.awssdk.core.internal.util;

import java.net.URI;
import java.util.ArrayList;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.progress.listener.DefaultProgressUpdater;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public final class ProgressListenerTestUtils {

    public static final AsyncRequestBody ASYNC_REQUEST_BODY = AsyncRequestBody.fromString("TestBody");
    public static final RequestBody REQUEST_BODY = RequestBody.fromString("TestBody");

    private ProgressListenerTestUtils() {
    }

    public static SdkResponse.Builder createSdkResponseBuilder() {
        return VoidSdkResponse.builder();
    }

    public static SdkRequest.Builder createSdkHttpRequest(SdkRequestOverrideConfiguration config) {
        return NoopTestRequest.builder()
                              .overrideConfiguration(config);
    }

    public static RequestExecutionContext progressListenerContext(boolean isAsyncStreaming, SdkRequest sdkRequest,
                                                                          DefaultProgressUpdater defaultProgressUpdater) {

        RequestExecutionContext.Builder builder =
            RequestExecutionContext.builder().
                                   executionContext(ExecutionContext.builder()
                                                                    .interceptorContext(InterceptorContext.builder()
                                                                                                          .request(sdkRequest)
                                                                                                          .build())
                                                                    .interceptorChain(new ExecutionInterceptorChain(new ArrayList<>()))
                                                                    .build())
                                   .originalRequest(sdkRequest);
        if (isAsyncStreaming) {
            builder.requestProvider(ASYNC_REQUEST_BODY);
        }

        RequestExecutionContext context = builder.build();
        context.progressUpdater(defaultProgressUpdater);
        return context;
    }

    public static SdkHttpFullRequest.Builder createHttpRequestBuilder() {
        return SdkHttpFullRequest.builder().uri(URI.create("https://endpoint.host"))
                                 .method(SdkHttpMethod.GET)
                                 .contentStreamProvider(REQUEST_BODY.contentStreamProvider());
    }
}
