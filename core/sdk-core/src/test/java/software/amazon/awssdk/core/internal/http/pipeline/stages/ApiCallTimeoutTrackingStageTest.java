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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ApiCallTimeoutTrackingStageTest {

    @Mock
    private RequestPipeline<SdkHttpFullRequest, Response<Void>> wrapped;

    private ApiCallTimeoutTrackingStage<Void> stage;

    @Before
    public void setUp() throws Exception {
        final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
            .threadNamePrefix("sdk-ScheduledExecutor-test").build());
        stage = new ApiCallTimeoutTrackingStage<>(HttpClientDependencies.builder()
                                                                        .clientConfiguration(SdkClientConfiguration.builder()
                                                                                                                   .option
                                                                                                                       (SdkClientOption
                                                                                                                            .SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor)
                                                                                                                   .build())
                                                                        .build(),
                                                  wrapped);
    }

    @Test
    public void timedOut_shouldThrowApiCallTimeoutException() throws Exception {
        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
            .thenAnswer(invocationOnMock -> {
                Thread.sleep(600);
                return null;
            });

        RequestExecutionContext context = requestContext(500);
        assertThatThrownBy(() -> stage.execute(mock(SdkHttpFullRequest.class), context)).isInstanceOf(ApiCallTimeoutException
                                                                                                          .class);
        assertThat(context.apiCallTimeoutTracker().hasExecuted()).isTrue();
    }

    @Test
    public void timeoutDisabled_shouldNotExecuteTimer() throws Exception {
        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
            .thenAnswer(invocationOnMock -> null);

        RequestExecutionContext context = requestContext(0);
        stage.execute(mock(SdkHttpFullRequest.class), context);
        assertThat(context.apiCallTimeoutTracker().hasExecuted()).isFalse();
    }

    @Test(expected = RuntimeException.class)
    public void nonTimerInterruption_RuntimeExceptionThrown_interruptFlagIsPreserved() throws Exception {
        nonTimerInterruption_interruptFlagIsPreserved(new RuntimeException());
    }

    @Test(expected = AbortedException.class)
    public void nonTimerInterruption_InterruptedExceptionThrown_interruptFlagIsPreserved() throws Exception {
        nonTimerInterruption_interruptFlagIsPreserved(new InterruptedException());
    }

    /**
     * Test to ensure that if the execution *did not* expire but the
     * interrupted flag is set that it's not cleared by
     * ApiCallTimedStage because it's not an interruption by the timer.
     *
     * @param exceptionToThrow The exception to throw from the wrapped pipeline.
     */
    private void nonTimerInterruption_interruptFlagIsPreserved(final Exception exceptionToThrow) throws Exception {
        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
            .thenAnswer(invocationOnMock -> {
                Thread.currentThread().interrupt();
                throw exceptionToThrow;
            });

        RequestExecutionContext context = requestContext(500);

        try {
            stage.execute(mock(SdkHttpFullRequest.class), context);
            fail("No exception");
        } finally {
            assertThat(Thread.interrupted()).isTrue();
            assertThat(context.apiCallTimeoutTracker().hasExecuted()).isFalse();
        }
    }

    private RequestExecutionContext requestContext(long timeout) {
        SdkRequestOverrideConfiguration.Builder configBuilder = SdkRequestOverrideConfiguration.builder();

        if (timeout > 0) {
            configBuilder.apiCallTimeout(Duration.ofMillis(timeout));
        }

        SdkRequest originalRequest =
            NoopTestRequest.builder()
                           .overrideConfiguration(configBuilder
                                                      .build())
                           .build();
        return RequestExecutionContext.builder()
                                      .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(null))
                                      .originalRequest(originalRequest)
                                      .build();
    }
}
