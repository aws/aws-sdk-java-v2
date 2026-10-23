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

package software.amazon.awssdk.core.client.config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * An {@link Executor}-like object that is given the {@link ExecutionAttributes} of the execution (i.e. the API call) whose
 * {@link CompletableFuture} it is completing.
 * <p>
 * Combined with an {@link ExecutionInterceptor}, whose {@link ExecutionInterceptor#beforeExecution} method runs on the
 * caller's thread, this allows capturing context bound to the caller's thread (e.g. a logging MDC, a tracing context or the
 * thread context class loader) when an execution starts, and restoring it on the thread that completes the future returned by
 * the async service client. For example:
 *
 * {@snippet :
 * static final ExecutionAttribute<Map<String, String>> MDC_CONTEXT = new ExecutionAttribute<>("MdcContext");
 *
 * // In an ExecutionInterceptor, which runs on the caller's thread
 * public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
 *     executionAttributes.putAttribute(MDC_CONTEXT, MDC.getCopyOfContextMap());
 * }
 *
 * // The future completion executor restores what the interceptor captured
 * ExecutionAwareExecutor executor = (command, executionAttributes) -> pool.execute(() -> {
 *     Map<String, String> mdc = executionAttributes.getAttribute(MDC_CONTEXT);
 *     if (mdc != null) {
 *         MDC.setContextMap(mdc);
 *     }
 *     try {
 *         command.run();
 *     } finally {
 *         MDC.clear();
 *     }
 * });
 * }
 *
 * <p>
 * The executor is invoked at the end of each HTTP attempt, so it can be invoked several times for an execution that is
 * retried; the same {@link ExecutionAttributes} are given for every attempt. Besides completing the future returned by the
 * async service client, the SDK's own processing of the attempt result runs on the thread the command is dispatched to:
 * for a failed attempt that will be retried, retry bookkeeping and
 * {@link software.amazon.awssdk.core.async.AsyncResponseTransformer#exceptionOccurred(Throwable)}; for the last attempt,
 * the {@link ExecutionInterceptor} hooks that run after the response is received (e.g.
 * {@link ExecutionInterceptor#afterExecution}, {@link ExecutionInterceptor#onExecutionFailure}) and metric publishing.
 * Subsequent attempts themselves are always scheduled on the SDK's scheduled executor. This is the same as for
 * {@link SdkAdvancedAsyncClientOption#FUTURE_COMPLETION_EXECUTOR}, but should be considered when dispatching to threads
 * that must not block, such as an event loop.
 *
 * @see SdkAdvancedAsyncClientOption#EXECUTION_AWARE_FUTURE_COMPLETION_EXECUTOR
 */
@SdkPublicApi
@FunctionalInterface
public interface ExecutionAwareExecutor {
    /**
     * Executes the given command, which completes an attempt of the execution described by the given execution attributes.
     * <p>
     * This method is invoked by the async HTTP client's I/O threads (e.g., EventLoops), which must be reserved for
     * non-blocking behavior. Implementations should dispatch the command to another thread rather than running it inline.
     *
     * @param command the runnable task
     * @param executionAttributes the execution attributes of the execution whose attempt is being completed
     */
    void execute(Runnable command, ExecutionAttributes executionAttributes);
}
