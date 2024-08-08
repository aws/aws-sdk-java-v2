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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BatchOverrideConfigurationTest {

    private static Stream<Arguments> provideConfigurations() {
        return Stream.of(
            Arguments.of(10, 5, Duration.ofMillis(200), Duration.ofSeconds(30), 20, Duration.ofMillis(50), Arrays.asList("attr1"),
                         Arrays.asList("msgAttr1"), true,
                         false, 10, 5),
            Arguments.of(null, null, null, null, null, null, null, null, null, null, null, null),
            Arguments.of(1, 1, Duration.ofMillis(1), Duration.ofMillis(1), 1, Duration.ofMillis(1), Collections.emptyList(),
                         Collections.emptyList(),
                         false, true
                , 5, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void testBatchOverrideConfiguration(Integer maxBatchItems, Integer maxBatchKeys,
                                        Duration maxBatchOpenDuration, Duration visibilityTimeout,
                                        Integer longPollWaitTimeoutSeconds, Duration minReceiveWaitTimeMs,
                                        List<String> receiveAttributeNames, List<String> receiveMessageAttributeNames,
                                        Boolean adaptivePrefetching, Boolean longPoll, Integer maxInflightReceiveBatches,
                                        Integer maxDoneReceiveBatches) {

        BatchOverrideConfiguration config = BatchOverrideConfiguration.builder()
                                                                      .maxBatchItems(maxBatchItems)
                                                                      .maxBatchKeys(maxBatchKeys)
                                                                      .maxBatchOpenInMs(maxBatchOpenDuration)
                                                                      .visibilityTimeout(visibilityTimeout)
                                                                      .longPollWaitTimeoutSeconds(longPollWaitTimeoutSeconds)
                                                                      .minReceiveWaitTimeMs(minReceiveWaitTimeMs)
                                                                      .receiveAttributeNames(receiveAttributeNames)
                                                                      .receiveMessageAttributeNames(receiveMessageAttributeNames)
                                                                      .adaptivePrefetching(adaptivePrefetching)
                                                                      .longPoll(longPoll)
                                                                      .maxInflightReceiveBatches(maxInflightReceiveBatches)
                                                                      .maxDoneReceiveBatches(maxDoneReceiveBatches)
                                                                      .build();

        assertEquals(Optional.ofNullable(maxBatchItems), config.maxBatchItems());
        assertEquals(Optional.ofNullable(maxBatchKeys), config.maxBatchKeys());
        assertEquals(Optional.ofNullable(maxBatchOpenDuration), config.maxBatchOpenInMs());
        assertEquals(Optional.ofNullable(visibilityTimeout), config.visibilityTimeout());
        assertEquals(Optional.ofNullable(longPollWaitTimeoutSeconds), config.longPollWaitTimeoutSeconds());
        assertEquals(Optional.ofNullable(minReceiveWaitTimeMs), config.minReceiveWaitTimeMs());
        assertEquals(Optional.ofNullable(receiveAttributeNames).orElse(Collections.emptyList()),
                     config.receiveAttributeNames().orElse(Collections.emptyList()));
        assertEquals(Optional.ofNullable(receiveMessageAttributeNames).orElse(Collections.emptyList()),
                     config.receiveMessageAttributeNames().orElse(Collections.emptyList()));
        assertEquals(Optional.ofNullable(adaptivePrefetching), config.adaptivePrefetching());
        assertEquals(Optional.ofNullable(longPoll), config.longPoll());
        assertEquals(Optional.ofNullable(maxInflightReceiveBatches), config.maxInflightReceiveBatches());
        assertEquals(Optional.ofNullable(maxDoneReceiveBatches), config.maxDoneReceiveBatches());
    }

    @Test
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(BatchOverrideConfiguration.class)
                      .withPrefabValues(Duration.class, Duration.ofMillis(1), Duration.ofMillis(2))
                      .verify();
    }

    @Test
    void testToBuilder() {
        BatchOverrideConfiguration originalConfig = BatchOverrideConfiguration.builder()
                                                                              .maxBatchItems(10)
                                                                              .maxBatchKeys(5)
                                                                              .maxBatchOpenInMs(Duration.ofMillis(200))
                                                                              .visibilityTimeout(Duration.ofSeconds(30))
                                                                              .longPollWaitTimeoutSeconds(20)
                                                                              .minReceiveWaitTimeMs(Duration.ofMillis(50))
                                                                              .receiveAttributeNames(Arrays.asList("attr1"))
                                                                              .receiveMessageAttributeNames(Arrays.asList(
                                                                                  "msgAttr1"))
                                                                              .adaptivePrefetching(true)
                                                                              .longPoll(true)
                                                                              .maxInflightReceiveBatches(10)
                                                                              .maxDoneReceiveBatches(5)
                                                                              .build();

        BatchOverrideConfiguration.Builder builder = originalConfig.toBuilder();
        BatchOverrideConfiguration newConfig = builder.build();
        assertEquals(originalConfig, newConfig);
        // Ensure that modifying the builder does not affect the original config
        builder.maxBatchItems(20);
        assertNotEquals(originalConfig.maxBatchItems(), builder.build().maxBatchItems());
    }
}
