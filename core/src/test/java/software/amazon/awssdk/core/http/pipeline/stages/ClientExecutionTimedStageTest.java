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

package software.amazon.awssdk.core.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.config.SyncClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpSyncClientDependencies;
import software.amazon.awssdk.core.http.NoopTestAwsRequest;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.http.SdkHttpFullRequest;

import java.time.Duration;

/**
 * Tests for {@link ClientExecutionTimedStage}.
 */
public class ClientExecutionTimedStageTest {

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
     * ClientExecutionTimedStage because it's not an interruption by the timer.
     *
     * @param exceptionToThrow The exception to throw from the wrapped pipeline.
     */
    private void nonTimerInterruption_interruptFlagIsPreserved(final Exception exceptionToThrow) throws Exception {
        RequestPipeline<SdkHttpFullRequest, Response<Void>> wrapped =
                (RequestPipeline<SdkHttpFullRequest, Response<Void>>) mock(RequestPipeline.class);
        ClientExecutionTimedStage<Void> stage = new ClientExecutionTimedStage<>(HttpSyncClientDependencies.builder()
                                                                                                          .clientExecutionTimer(new ClientExecutionTimer())
                                                                                                          .syncClientConfiguration(mock(SyncClientConfiguration.class))
                                                                                                          .capacityManager(mock(CapacityManager.class))
                                                                                                          .build(),
                                                                                wrapped);

        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class))).thenAnswer(invocationOnMock -> {
            Thread.currentThread().interrupt();
            throw exceptionToThrow;
        });

        SdkRequest originalRequest = NoopTestAwsRequest.builder()
                .requestOverrideConfig(AwsRequestOverrideConfig.builder()
                        .requestExecutionTimeout(Duration.ofMillis(0))
                        .build())
                .build();
        try {
            stage.execute(mock(SdkHttpFullRequest.class), RequestExecutionContext.builder()
                    .executionContext(mock(ExecutionContext.class))
                    .originalRequest(originalRequest)
                    .build());
        } finally {
            assertThat(Thread.interrupted());
        }
    }
}
