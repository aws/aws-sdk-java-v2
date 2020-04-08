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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.ApiCallTimeoutTracker;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTask;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class TimeoutExceptionHandlingStageTest {

    @Mock
    private RequestPipeline<SdkHttpFullRequest, Response<String>> requestPipeline;

    @Mock
    private TimeoutTask apiCallTimeoutTask;

    @Mock
    private TimeoutTask apiCallAttemptTimeoutTask;

    @Mock
    private ScheduledFuture scheduledFuture;


    private TimeoutExceptionHandlingStage<String> stage;

    @Before
    public void setup() {
        stage = new TimeoutExceptionHandlingStage<>(HttpClientDependencies.builder()
                                                                          .clientConfiguration(SdkClientConfiguration.builder().build())
                                                                          .build(), requestPipeline);
    }

    @Test
    public void IOException_causedByApiCallTimeout_shouldThrowInterruptedException() throws Exception {
        when(apiCallTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new SocketException());
        verifyExceptionThrown(InterruptedException.class);
    }

    @Test
    public void IOException_causedByApiCallAttemptTimeout_shouldThrowApiCallAttemptTimeoutException() throws Exception {
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new IOException());
        verifyExceptionThrown(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void IOException_bothTimeouts_shouldThrowInterruptedException() throws Exception {
        when(apiCallTimeoutTask.hasExecuted()).thenReturn(true);
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new IOException());
        verifyExceptionThrown(InterruptedException.class);
    }

    @Test
    public void IOException_notCausedByTimeouts_shouldPropagate() throws Exception {
        when(requestPipeline.execute(any(), any())).thenThrow(new SocketException());
        verifyExceptionThrown(SocketException.class);
    }

    @Test
    public void AbortedException_notCausedByTimeouts_shouldPropagate() throws Exception {
        when(requestPipeline.execute(any(), any())).thenThrow(AbortedException.create(""));
        verifyExceptionThrown(AbortedException.class);
    }

    @Test
    public void AbortedException_causedByAttemptTimeout_shouldThrowApiCallAttemptTimeoutException() throws Exception {
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(AbortedException.create(""));
        verifyExceptionThrown(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void AbortedException_causedByCallTimeout_shouldThrowInterruptedException() throws Exception {
        when(apiCallTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(AbortedException.create(""));
        verifyExceptionThrown(InterruptedException.class);
    }

    @Test
    public void nonTimeoutCausedException_shouldPropagate() throws Exception {
        when(requestPipeline.execute(any(), any())).thenThrow(new RuntimeException());
        verifyExceptionThrown(RuntimeException.class);
    }

    @Test
    public void interruptedException_notCausedByTimeouts_shouldPreserveInterruptFlag() throws Exception {
        when(requestPipeline.execute(any(), any())).thenThrow(new InterruptedException());
        verifyExceptionThrown(AbortedException.class);
        verifyInterruptStatusPreserved();
    }

    @Test
    public void interruptedException_causedByApiCallTimeout_shouldPropagate() throws Exception {
        when(apiCallTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new InterruptedException());
        verifyExceptionThrown(InterruptedException.class);
    }

    @Test
    public void interruptedException_causedByAttemptTimeout_shouldThrowApiAttempt() throws Exception {
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new InterruptedException());
        verifyExceptionThrown(ApiCallAttemptTimeoutException.class);
        verifyInterruptStatusClear();
    }

    @Test
    public void interruptFlagWasSet_causedByAttemptTimeout_shouldThrowApiAttempt() throws Exception {
        Thread.currentThread().interrupt();
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new RuntimeException());
        verifyExceptionThrown(ApiCallAttemptTimeoutException.class);
        verifyInterruptStatusClear();
    }

    @Test
    public void interruptFlagWasSet_causedByApiCallTimeout_shouldThrowInterruptException() throws Exception {
        Thread.currentThread().interrupt();
        when(apiCallTimeoutTask.hasExecuted()).thenReturn(true);
        when(apiCallAttemptTimeoutTask.hasExecuted()).thenReturn(true);
        when(requestPipeline.execute(any(), any())).thenThrow(new RuntimeException());
        verifyExceptionThrown(InterruptedException.class);
        verifyInterruptStatusPreserved();
    }


    private void verifyInterruptStatusPreserved() {
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    private void verifyInterruptStatusClear() {
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    private void verifyExceptionThrown(Class exceptionToAssert) {
        RequestExecutionContext context = requestContext();
        context.apiCallTimeoutTracker(new ApiCallTimeoutTracker(apiCallTimeoutTask, scheduledFuture));
        context.apiCallAttemptTimeoutTracker(new ApiCallTimeoutTracker(apiCallAttemptTimeoutTask, scheduledFuture));

        assertThatThrownBy(() -> stage.execute(ValidSdkObjects.sdkHttpFullRequest().build(), context))
            .isExactlyInstanceOf(exceptionToAssert);
    }

    private RequestExecutionContext requestContext() {
        SdkRequestOverrideConfiguration.Builder configBuilder = SdkRequestOverrideConfiguration.builder();

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
