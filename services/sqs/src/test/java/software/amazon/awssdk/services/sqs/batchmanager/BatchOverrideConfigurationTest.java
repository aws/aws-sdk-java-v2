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
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchOverrideConfigurationTest {

    private static Stream<Arguments> provideConfigurations() {
        return Stream.of(
            Arguments.of(10,
                         Duration.ofMillis(200),
                         Duration.ofSeconds(30),
                         Duration.ofMillis(50),
                         Arrays.asList("msgAttr1"),
                         Arrays.asList(MessageSystemAttributeName.SENDER_ID)),
            Arguments.of(null, null, null, null, null, null),
            Arguments.of(1,
                         Duration.ofMillis(1),
                         Duration.ofMillis(1),
                         Duration.ofMillis(1),
                         Collections.emptyList(),
                         Collections.singletonList(MessageSystemAttributeName.SEQUENCE_NUMBER))
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurations")
    void testBatchOverrideConfiguration(Integer maxBatchSize,
                                        Duration sendRequestFrequency,
                                        Duration receiveMessageVisibilityTimeout,
                                        Duration receiveMessageMinWaitDuration,
                                        List<String> receiveMessageAttributeNames,
                                        List<MessageSystemAttributeName> receiveMessageSystemAttributeNames) {

        BatchOverrideConfiguration config = BatchOverrideConfiguration.builder()
                                                                      .maxBatchSize(maxBatchSize)
                                                                      .sendRequestFrequency(sendRequestFrequency)
                                                                      .receiveMessageVisibilityTimeout(receiveMessageVisibilityTimeout)
                                                                      .receiveMessageMinWaitDuration(receiveMessageMinWaitDuration)
                                                                      .receiveMessageAttributeNames(receiveMessageAttributeNames)
                                                                      .receiveMessageSystemAttributeNames(receiveMessageSystemAttributeNames)
                                                                      .build();

        assertEquals(maxBatchSize, config.maxBatchSize());
        assertEquals(sendRequestFrequency, config.sendRequestFrequency());
        assertEquals(receiveMessageVisibilityTimeout, config.receiveMessageVisibilityTimeout());
        assertEquals(receiveMessageMinWaitDuration, config.receiveMessageMinWaitDuration());
        assertEquals(Optional.ofNullable(receiveMessageAttributeNames).orElse(Collections.emptyList()),
                     config.receiveMessageAttributeNames());
        assertEquals(Optional.ofNullable(receiveMessageSystemAttributeNames).orElse(Collections.emptyList()),
                     config.receiveMessageSystemAttributeNames());
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
                                                                              .maxBatchSize(10)
                                                                              .sendRequestFrequency(Duration.ofMillis(200))
                                                                              .receiveMessageVisibilityTimeout(Duration.ofSeconds(30))
                                                                              .receiveMessageMinWaitDuration(Duration.ofMillis(50))
                                                                              .receiveMessageAttributeNames(Arrays.asList("msgAttr1"))
                                                                              .receiveMessageSystemAttributeNames(Collections.singletonList(
                                                                                  MessageSystemAttributeName.SENDER_ID))
                                                                              .build();

        BatchOverrideConfiguration.Builder builder = originalConfig.toBuilder();
        BatchOverrideConfiguration newConfig = builder.build();
        assertEquals(originalConfig, newConfig);
        // Ensure that modifying the builder does not affect the original config
        builder.maxBatchSize(9);
        assertNotEquals(originalConfig.maxBatchSize(), builder.build().maxBatchSize());
        // Ensure that all other fields are still equal after modifying the maxBatchSize
        assertEquals(originalConfig.sendRequestFrequency(), builder.build().sendRequestFrequency());
        assertEquals(originalConfig.receiveMessageVisibilityTimeout(), builder.build().receiveMessageVisibilityTimeout());
        assertEquals(originalConfig.receiveMessageMinWaitDuration(), builder.build().receiveMessageMinWaitDuration());
        assertEquals(originalConfig.receiveMessageAttributeNames(), builder.build().receiveMessageAttributeNames());
        assertEquals(originalConfig.receiveMessageSystemAttributeNames(), builder.build().receiveMessageSystemAttributeNames());
    }

    @Test
    void testMaxBatchSizeExceedsLimitThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            BatchOverrideConfiguration.builder()
                                      .maxBatchSize(11) // Set an invalid max batch size (exceeds limit)
                                      .build();         // This should throw IllegalArgumentException
        });

        // Assert that the exception message matches the expected output
        assertEquals("The maxBatchSize must be less than or equal to 10. A batch can contain up to 10 messages.",
                     exception.getMessage());
    }


}