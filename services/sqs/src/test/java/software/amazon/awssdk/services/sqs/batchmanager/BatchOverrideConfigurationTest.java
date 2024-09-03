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

package software.amazon.awssdk.services.sqs.batchmanager;

import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BatchOverrideConfigurationTest {

    private static Stream<Arguments> provideConfigurations() {
        return Stream.of(
            Arguments.of(10,
                         5,
                         Duration.ofMillis(200),
                         Duration.ofSeconds(30),
                         Duration.ofSeconds(20),
                         Duration.ofMillis(50),
                         Arrays.asList("msgAttr1"),
                         Arrays.asList(MessageSystemAttributeName.SENDER_ID),
                         true,
                         10,
                         5),
            Arguments.of(null, null, null, null, null, null, null, null, null, null, null),
            Arguments.of(1,
                         1,
                         Duration.ofMillis(1),
                         Duration.ofMillis(1),
                         Duration.ofMillis(1),
                         Duration.ofMillis(1),
                         Collections.emptyList(),
                         Collections.singletonList(MessageSystemAttributeName.SEQUENCE_NUMBER),
                         false,
                         5,
                         2)
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void testBatchOverrideConfiguration(Integer maxBatchItems,
                                        Integer maxBatchKeys,
                                        Duration maxOutboundBatchCollectionDuration,
                                        Duration visibilityTimeout,
                                        Duration longPollWaitTimeout,
                                        Duration minReceiveWaitTime,
                                        List<String> receiveMessageAttributeNames,
                                        List<MessageSystemAttributeName> messageSystemAttributeNames,
                                        Boolean adaptivePrefetching,
                                        Integer maxInflightReceiveBatches,
                                        Integer maxDoneReceiveBatches) {

        BatchOverrideConfiguration config = BatchOverrideConfiguration.builder()
                                                                      .outboundBatchSizeLimit(maxBatchItems)
                                                                      .maxBatchKeys(maxBatchKeys)
                                                                      .maxOutboundBatchCollectionDuration(maxOutboundBatchCollectionDuration)
                                                                      .visibilityTimeout(visibilityTimeout)
                                                                      .longPollWaitTimeout(longPollWaitTimeout)
                                                                      .minReceiveWaitTime(minReceiveWaitTime)
                                                                      .receiveMessageAttributeNames(receiveMessageAttributeNames)
                                                                      .messageSystemAttributeName(messageSystemAttributeNames)
                                                                      .adaptivePrefetching(adaptivePrefetching)
                                                                      .maxInflightReceiveBatches(maxInflightReceiveBatches)
                                                                      .maxDoneReceiveBatches(maxDoneReceiveBatches)
                                                                      .build();

        assertEquals(maxBatchItems, config.outboundBatchSizeLimit());
        assertEquals(maxBatchKeys, config.maxBatchKeys());
        assertEquals(maxOutboundBatchCollectionDuration, config.outboundBatchCollectionDuration());
        assertEquals(visibilityTimeout, config.receiveMessageVisibilityTimeout());
        assertEquals(longPollWaitTimeout, config.receiveMessageLongPollWaitDuration());
        assertEquals(minReceiveWaitTime, config.receiveMessageMinWaitTime());
        assertEquals(Optional.ofNullable(receiveMessageAttributeNames).orElse(Collections.emptyList()),
                     config.receiveMessageAttributeNames());
        assertEquals(Optional.ofNullable(messageSystemAttributeNames).orElse(Collections.emptyList()),
                     config.receiveMessageSystemAttributeNames());
        assertEquals(adaptivePrefetching, config.adaptivePrefetching());
        assertEquals(maxInflightReceiveBatches, config.maxInflightReceiveBatches());
        assertEquals(maxDoneReceiveBatches, config.maxDoneReceiveBatches());
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
                                                                              .outboundBatchSizeLimit(10)
                                                                              .maxBatchKeys(5)
                                                                              .maxOutboundBatchCollectionDuration(Duration.ofMillis(200))
                                                                              .visibilityTimeout(Duration.ofSeconds(30))
                                                                              .longPollWaitTimeout(Duration.ofSeconds(20))
                                                                              .minReceiveWaitTime(Duration.ofMillis(50))
                                                                              .receiveMessageAttributeNames(Arrays.asList(
                                                                                  "msgAttr1"))
                                                                              .messageSystemAttributeName(Collections.singletonList(
                                                                                  MessageSystemAttributeName.SENDER_ID))
                                                                              .adaptivePrefetching(true)
                                                                              .maxInflightReceiveBatches(10)
                                                                              .maxDoneReceiveBatches(5)
                                                                              .build();

        BatchOverrideConfiguration.Builder builder = originalConfig.toBuilder();
        BatchOverrideConfiguration newConfig = builder.build();
        assertEquals(originalConfig, newConfig);
        // Ensure that modifying the builder does not affect the original config
        builder.outboundBatchSizeLimit(20);
        assertNotEquals(originalConfig.outboundBatchSizeLimit(), builder.build().outboundBatchSizeLimit());
    }
}
