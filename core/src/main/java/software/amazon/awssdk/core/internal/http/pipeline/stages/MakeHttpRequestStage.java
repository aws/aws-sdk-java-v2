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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import software.amazon.awssdk.core.internal.http.HttpSyncClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.utils.Pair;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
public class MakeHttpRequestStage
    implements RequestPipeline<SdkHttpFullRequest, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {

    private final SdkHttpClient sdkHttpClient;

    public MakeHttpRequestStage(HttpSyncClientDependencies dependencies) {
        this.sdkHttpClient = dependencies.syncClientConfiguration().httpClient();
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(SdkHttpFullRequest request,
                                                                 RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        final SdkHttpFullResponse httpResponse = executeHttpRequest(request, context);
        return Pair.of(request, httpResponse);
    }

    private SdkHttpFullResponse executeHttpRequest(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        final AbortableCallable<SdkHttpFullResponse> requestCallable = sdkHttpClient
                .prepareRequest(request, SdkRequestContext.builder().build());

        return requestCallable.call();
    }
}
