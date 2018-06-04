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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class AsyncExecutionFailureExceptionReportingStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> {

    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped;

    public AsyncExecutionFailureExceptionReportingStage(RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public CompletableFuture<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        return wrapped.execute(input, context).whenComplete((o, t) -> {
            if (t != null) {
                Context.FailedExecution failedContext =
                        new DefaultFailedExecutionContext(context.executionContext().interceptorContext(), t);
                context.interceptorChain().onExecutionFailure(failedContext, context.executionAttributes());
            }
        });
    }
}
