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
import java.util.concurrent.CompletionException;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.interceptor.DefaultFailedExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class AsyncExecutionFailureExceptionReportingStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> {
    private static final Logger log = Logger.loggerFor(AsyncExecutionFailureExceptionReportingStage.class);

    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped;

    public AsyncExecutionFailureExceptionReportingStage(RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public CompletableFuture<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        return wrapped.execute(input, context).handle((o, t) -> {
            if (t != null) {
                if (t instanceof CompletionException) {
                    t = t.getCause();
                }

                reportFailureToInterceptors(context, t);

                if (t instanceof RuntimeException) {
                    throw ((RuntimeException) t);
                }

                throw new RuntimeException(t);
            } else {
                return o;
            }
        });
    }

    /**
     * Report the failure to the execution interceptors. Swallow any exceptions thrown from the interceptor since we don't
     * want to replace the execution failure.
     *
     * @param context The execution context.
     * @param err     The execution failure.
     */
    private static void reportFailureToInterceptors(RequestExecutionContext context, Throwable err) {
        try {
            Context.FailedExecution failedContext =
                    new DefaultFailedExecutionContext(context.executionContext().interceptorContext(), err);
            context.interceptorChain().onExecutionFailure(failedContext, context.executionAttributes());
        } catch (Throwable t) {
            log.warn(() -> "Interceptor chain threw an error from onExecutionFailure().", err);
        }
    }
}
