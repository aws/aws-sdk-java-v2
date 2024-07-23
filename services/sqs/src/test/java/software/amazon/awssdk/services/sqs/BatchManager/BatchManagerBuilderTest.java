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

package software.amazon.awssdk.services.sqs.BatchManager;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import software.amazon.awssdk.services.sqs.BatchManager.common.BatchManagerTestUtils;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestsBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponsesBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchAndSend;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.core.BatchManagerType;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static software.amazon.awssdk.services.sqs.BatchManager.common.BatchManagerTestUtils.batchKeyMapper;
import static software.amazon.awssdk.services.sqs.BatchManager.common.BatchManagerTestUtils.responseMapper;


class BatchManagerBuilderTest {

    private ScheduledExecutorService scheduledExecutor;
    private BatchOverrideConfiguration overrideConfiguration;
    private BatchAndSend<String, BatchManagerTestUtils.BatchResponse> batchFunction;

    @BeforeEach
    void setUp() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("batch-buffer").build();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        overrideConfiguration = BatchOverrideConfiguration.builder()
                                                          .maxBatchItems(10)
                                                          .maxBatchOpenInMs(Duration.ofMillis(1000))
                                                          .build();

        batchFunction = (requests, destination) -> CompletableFuture.completedFuture(new BatchManagerTestUtils.BatchResponse());
    }

    @Test
    void creationOfRequestBatchManager() {
        BatchManager<String, String, BatchManagerTestUtils.BatchResponse> batchManager = BatchManager.builder(String.class,
                                                                                                              String.class,
                                                                                                              BatchManagerTestUtils.BatchResponse.class)
                                                                                                     .overrideConfiguration(overrideConfiguration)
                                                                                                     .scheduledExecutor(scheduledExecutor)
                                                                                                     .batchFunction(batchFunction)
                                                                                                     .responseMapper(responseMapper)
                                                                                                     .batchKeyMapper(batchKeyMapper)
                                                                                                     .batchManagerType(BatchManagerType.REQUEST)
                                                                                                     .build();

        assertInstanceOf(RequestsBatchManager.class, batchManager);
    }

    @Test
    void creationOfResponseBatchManager() {
        BatchManager<String, String, BatchManagerTestUtils.BatchResponse> batchManager = BatchManager.builder(String.class,
                                                                                                              String.class,
                                                                                                              BatchManagerTestUtils.BatchResponse.class)
                                                                                                     .overrideConfiguration(overrideConfiguration)
                                                                                                     .scheduledExecutor(scheduledExecutor)
                                                                                                     .batchFunction(batchFunction)
                                                                                                     .responseMapper(responseMapper)
                                                                                                     .batchKeyMapper(batchKeyMapper)
                                                                                                     .batchManagerType(BatchManagerType.RESPONSE)
                                                                                                     .build();

        assertInstanceOf(ResponsesBatchManager.class, batchManager);
    }

    @ParameterizedTest
    @MethodSource("provideBuildersForExceptionTests")
    void testExceptionsWhenBuildingBatchManager(Runnable builderAction, Class<? extends Throwable> expectedException,
                                                String expectedMessage) {
        Throwable exception = assertThrows(expectedException, builderAction::run);
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    private static Stream<Arguments> provideBuildersForExceptionTests() {
        BatchOverrideConfiguration overrideConfiguration = BatchOverrideConfiguration.builder()
                                                                                     .maxBatchItems(10)
                                                                                     .maxBatchOpenInMs(Duration.ofMillis(1000))
                                                                                     .build();

        BatchAndSend<String, BatchManagerTestUtils.BatchResponse> batchFunction = (requests, destination) ->
            CompletableFuture.completedFuture(new BatchManagerTestUtils.BatchResponse());

        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        return Stream.of(
            Arguments.of((Runnable) () ->
                             BatchManager.builder(String.class, String.class, BatchManagerTestUtils.BatchResponse.class)
                                         .overrideConfiguration(overrideConfiguration)
                                         .scheduledExecutor(scheduledExecutor)
                                         .batchFunction(batchFunction)
                                         .responseMapper(responseMapper)
                                         .batchKeyMapper(batchKeyMapper)
                                         .build(),
                         IllegalArgumentException.class, "Type must be specified as either RESPONSE or REQUEST"),
            Arguments.of((Runnable) () ->
                             BatchManager.builder(String.class, String.class, BatchManagerTestUtils.BatchResponse.class)
                                         .overrideConfiguration(overrideConfiguration)
                                         .scheduledExecutor(scheduledExecutor)
                                         .responseMapper(responseMapper)
                                         .batchKeyMapper(batchKeyMapper)
                                         .batchManagerType(BatchManagerType.REQUEST)
                                         .build(),
                         NullPointerException.class, "Null batchFunction"),
            Arguments.of((Runnable) () ->
                             BatchManager.builder(String.class, String.class, BatchManagerTestUtils.BatchResponse.class)
                                         .overrideConfiguration(overrideConfiguration)
                                         .scheduledExecutor(scheduledExecutor)
                                         .batchFunction(batchFunction)
                                         .batchKeyMapper(batchKeyMapper)
                                         .batchManagerType(BatchManagerType.REQUEST)
                                         .build(),
                         NullPointerException.class, "Null responseMapper"),
            Arguments.of((Runnable) () ->
                             BatchManager.builder(String.class, String.class, BatchManagerTestUtils.BatchResponse.class)
                                         .overrideConfiguration(overrideConfiguration)
                                         .scheduledExecutor(scheduledExecutor)
                                         .batchFunction(batchFunction)
                                         .responseMapper(responseMapper)
                                         .batchManagerType(BatchManagerType.REQUEST)
                                         .build(),
                         NullPointerException.class, "Null batchKeyMapper"),
            Arguments.of((Runnable) () ->
                             BatchManager.builder(String.class, String.class, BatchManagerTestUtils.BatchResponse.class)
                                         .overrideConfiguration(overrideConfiguration)
                                         .batchFunction(batchFunction)
                                         .responseMapper(responseMapper)
                                         .batchKeyMapper(batchKeyMapper)
                                         .batchManagerType(BatchManagerType.REQUEST)
                                         .build(),
                         NullPointerException.class, "Null scheduledExecutor")
        );
    }
}