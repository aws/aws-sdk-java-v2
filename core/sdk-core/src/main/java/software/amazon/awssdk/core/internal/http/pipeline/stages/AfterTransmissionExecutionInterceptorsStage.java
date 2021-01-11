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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class AfterTransmissionExecutionInterceptorsStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {
    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                                 RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        // Update interceptor context
        InterceptorContext interceptorContext =
                context.executionContext().interceptorContext().copy(b -> b.httpResponse(input.right())
                                                                           .responseBody(input.right()
                                                                                              .content()
                                                                                              .orElse(null)));

        // interceptors.afterTransmission
        context.interceptorChain().afterTransmission(interceptorContext, context.executionAttributes());

        // interceptors.modifyHttpResponse
        interceptorContext = context.interceptorChain().modifyHttpResponse(interceptorContext, context.executionAttributes());

        // Store updated context
        context.executionContext().interceptorContext(interceptorContext);

        InterruptMonitor.checkInterrupted((SdkHttpFullResponse) interceptorContext.httpResponse());

        SdkHttpFullResponse response = (SdkHttpFullResponse) interceptorContext.httpResponse();

        if (interceptorContext.responseBody().isPresent()) {
            response = response.toBuilder().content(AbortableInputStream.create(interceptorContext.responseBody().get())).build();
        }

        return Pair.of(input.left(), response);
    }
}
