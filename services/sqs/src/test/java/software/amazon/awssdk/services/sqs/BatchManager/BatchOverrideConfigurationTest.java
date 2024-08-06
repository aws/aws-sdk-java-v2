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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import nl.jqno.equalsverifier.EqualsVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchOverrideConfigurationTest {

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void testBatchOverrideConfiguration(Integer maxBatchItems, Integer maxBatchKeys, Integer maxBufferSize,
                                        Duration maxBatchOpenInMs, Long maxBatchSizeBytes, Integer visibilityTimeoutSeconds,
                                        Integer longPollWaitTimeoutSeconds, Integer minReceiveWaitTimeMs,
                                        List<String> receiveAttributeNames, List<String> receiveMessageAttributeNames,
                                        Boolean adaptivePrefetching, Boolean flushOnShutdown) {

        BatchOverrideConfiguration config = BatchOverrideConfiguration.builder()
                                                                      .maxBatchItems(maxBatchItems)
                                                                      .maxBatchKeys(maxBatchKeys)
                                                                      .maxBufferSize(maxBufferSize)
                                                                      .maxBatchOpenInMs(maxBatchOpenInMs)
                                                                      .maxBatchSizeBytes(maxBatchSizeBytes)
                                                                      .visibilityTimeoutSeconds(visibilityTimeoutSeconds)
                                                                      .longPollWaitTimeoutSeconds(longPollWaitTimeoutSeconds)
                                                                      .minReceiveWaitTimeMs(minReceiveWaitTimeMs)
                                                                      .receiveAttributeNames(receiveAttributeNames)
                                                                      .receiveMessageAttributeNames(receiveMessageAttributeNames)
                                                                      .adaptivePrefetching(adaptivePrefetching)
                                                                      .flushOnShutdown(flushOnShutdown)
                                                                      .build();

        assertEquals(Optional.ofNullable(maxBatchItems), config.maxBatchItems());
        assertEquals(Optional.ofNullable(maxBatchKeys), config.maxBatchKeys());
        assertEquals(Optional.ofNullable(maxBufferSize), config.maxBufferSize());
        assertEquals(Optional.ofNullable(maxBatchOpenInMs), config.maxBatchOpenInMs());
        assertEquals(Optional.ofNullable(maxBatchSizeBytes), config.maxBatchSizeBytes());
        assertEquals(Optional.ofNullable(visibilityTimeoutSeconds), config.visibilityTimeoutSeconds());
        assertEquals(Optional.ofNullable(longPollWaitTimeoutSeconds), config.longPollWaitTimeoutSeconds());
        assertEquals(Optional.ofNullable(minReceiveWaitTimeMs), config.minReceiveWaitTimeMs());
        assertEquals(Optional.ofNullable(receiveAttributeNames).orElse(Collections.emptyList()), config.receiveAttributeNames().orElse(Collections.emptyList()));
        assertEquals(Optional.ofNullable(receiveMessageAttributeNames).orElse(Collections.emptyList()), config.receiveMessageAttributeNames().orElse(Collections.emptyList()));
        assertEquals(Optional.ofNullable(adaptivePrefetching), config.adaptivePrefetching());
        assertEquals(Optional.ofNullable(flushOnShutdown), config.flushOnShutdown());
    }

    private static Stream<Arguments> provideConfigurations() {
        return Stream.of(
            Arguments.of(10, 5, 1000, Duration.ofMillis(200), 1024L, 30, 20, 50, Arrays.asList("attr1"), Arrays.asList("msgAttr1"), true, false),
            Arguments.of(null, null, null, null, null, null, null, null, null, null, null, null),
            Arguments.of(1, 1, 1, Duration.ofMillis(1), 1L, 1, 1, 1, Collections.emptyList(), Collections.emptyList(), false, true)
        );
    }

   @Test
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(BatchOverrideConfiguration.class)
                      .withPrefabValues(Duration.class, Duration.ofMillis(1), Duration.ofMillis(2))
                      .verify();
    }
}
