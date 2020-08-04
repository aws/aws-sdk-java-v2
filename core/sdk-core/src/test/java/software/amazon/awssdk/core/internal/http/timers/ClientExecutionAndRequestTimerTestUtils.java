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

package software.amazon.awssdk.core.internal.http.timers;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.ErrorDuringUnmarshallingResponseHandler;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Useful asserts and utilities for verifying behavior or the client execution timeout and request
 * timeout features
 */
public class ClientExecutionAndRequestTimerTestUtils {

    /**
     * Can take a little bit for ScheduledThreadPoolExecutor to update it's internal state
     */
    private static final int WAIT_BEFORE_ASSERT_ON_EXECUTOR = 500;

    /**
     * Waits until a little after the thread pools keep alive time and then asserts that all thre
     *
     * @param timerExecutor Executor used by timer implementation
     */
    public static void assertCoreThreadsShutDownAfterBeingIdle(ScheduledThreadPoolExecutor timerExecutor) {
        try {
            Thread.sleep(timerExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS) + 1000);
        } catch (InterruptedException ignored) {
            // Ignored.
        }
        assertEquals(0, timerExecutor.getPoolSize());
    }

    /**
     * If the request completes successfully then the timer task should be canceled and should be
     * removed from the thread pool to prevent build up of canceled tasks
     *
     * @param timerExecutor Executor used by timer implementation
     */
    public static void assertCanceledTasksRemoved(ScheduledThreadPoolExecutor timerExecutor) {
        waitBeforeAssertOnExecutor();
        assertEquals(0, timerExecutor.getQueue().size());
    }

    /**
     * Asserts the timer never went off (I.E. no timeout was exceeded and no timer task was
     * executed)
     *
     * @param timerExecutor Executor used by timer implementation
     */
    public static void assertTimerNeverTriggered(ScheduledThreadPoolExecutor timerExecutor) {
        assertNumberOfTasksTriggered(timerExecutor, 0);
    }

    private static void assertNumberOfTasksTriggered(ScheduledThreadPoolExecutor timerExecutor,
                                                     int expectedNumberOfTasks) {
        waitBeforeAssertOnExecutor();
        assertEquals(expectedNumberOfTasks, timerExecutor.getCompletedTaskCount());
    }

    public static SdkHttpFullRequest.Builder createMockGetRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:0"))
                                 .method(SdkHttpMethod.GET);
    }

    /**
     * Execute the request with a dummy response handler and error response handler
     */
    public static void execute(AmazonSyncHttpClient httpClient, SdkHttpFullRequest request) {
        httpClient.requestExecutionBuilder()
                .request(request)
                .originalRequest(NoopTestRequest.builder().build())
                  .executionContext(executionContext(request))
                .execute(combinedSyncResponseHandler(new ErrorDuringUnmarshallingResponseHandler(),
                                                     new NullErrorResponseHandler()));
    }

    public static ExecutionContext executionContext(SdkHttpFullRequest request) {
        InterceptorContext incerceptorContext =
                InterceptorContext.builder()
                                  .request(NoopTestRequest.builder().build())
                                  .httpRequest(request)
                                  .build();
        return ExecutionContext.builder()
                               .signer(new NoOpSigner())
                               .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(incerceptorContext)
                               .metricCollector(MetricCollector.create("ApiCall"))
                               .build();
    }

    private static void waitBeforeAssertOnExecutor() {
        try {
            Thread.sleep(WAIT_BEFORE_ASSERT_ON_EXECUTOR);
        } catch (InterruptedException ignored) {
            // Ignored.
        }
    }

    public static void interruptCurrentThreadAfterDelay(final long delay) {
        final Thread currentThread = Thread.currentThread();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(delay);
                    currentThread.interrupt();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
