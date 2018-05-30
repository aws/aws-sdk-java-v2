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

import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Pair;

public class AfterTransmissionExecutionInterceptorsStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {
    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                                 RequestExecutionContext context) throws Exception {
        // Update interceptor context
        InterceptorContext interceptorContext =
                context.executionContext().interceptorContext().copy(b -> b.httpResponse(input.right()));

        // interceptors.afterTransmission
        context.interceptorChain().afterTransmission(interceptorContext, context.executionAttributes());

        // interceptors.modifyHttpResponse
        interceptorContext = context.interceptorChain().modifyHttpResponse(interceptorContext, context.executionAttributes());

        // Store updated context
        context.executionContext().interceptorContext(interceptorContext);

        // TODO: Why do we do this for sync, but not async? Are there other places it should be? Not having this fails the
        // AbortedExceptionClientExecutionTimerIntegrationTest
        InterruptMonitor.checkInterrupted(interceptorContext.httpResponse());

        return Pair.of(input.left(), interceptorContext.httpResponse());
    }
}
