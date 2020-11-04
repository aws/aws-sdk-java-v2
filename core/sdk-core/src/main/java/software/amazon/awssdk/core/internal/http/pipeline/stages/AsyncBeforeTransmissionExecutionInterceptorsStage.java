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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public class AsyncBeforeTransmissionExecutionInterceptorsStage implements
        RequestPipeline<CompletableFuture<SdkHttpFullRequest>, CompletableFuture<SdkHttpFullRequest>> {
    @Override
    public CompletableFuture<SdkHttpFullRequest> execute(CompletableFuture<SdkHttpFullRequest> input,
                                                         RequestExecutionContext context) throws Exception {

        CompletableFuture<SdkHttpFullRequest> future = new CompletableFuture<>();

        input.whenComplete((r, t) -> {
            if (t != null) {
                return;
            }

            try {
                context.interceptorChain().beforeTransmission(context.executionContext().interceptorContext(),
                        context.executionAttributes());
                future.complete(r);
            } catch (Throwable interceptorException) {
                future.completeExceptionally(interceptorException);
            }
        });

        return CompletableFutureUtils.forwardExceptionTo(future, input);
    }
}
