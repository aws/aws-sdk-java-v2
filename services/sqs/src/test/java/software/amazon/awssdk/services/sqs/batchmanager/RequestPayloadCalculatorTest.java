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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestPayloadCalculator;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.ATTRIBUTE_MAPS_PAYLOAD_BYTES;

@TestInstance(Lifecycle.PER_CLASS)
class RequestPayloadCalculatorTest {

    @ParameterizedTest
    @MethodSource("provideRequestsForMessageSizeCalculation")
    @DisplayName("Test calculateMessageSize with different SendMessageRequest inputs")
    void testCalculateMessageSize(SendMessageRequest request, int expectedSize) {
        Optional<Integer> actualSize = RequestPayloadCalculator.calculateMessageSize(request);
        assertEquals(Optional.of(expectedSize), actualSize);
    }

    private Stream<Arguments> provideRequestsForMessageSizeCalculation() {
        return Stream.of(
            Arguments.of(
                SendMessageRequest.builder().messageBody("Test message").build(),
                "Test message".getBytes(StandardCharsets.UTF_8).length + ATTRIBUTE_MAPS_PAYLOAD_BYTES
            ),
            Arguments.of(
                SendMessageRequest.builder().messageBody("").build(),
                 ATTRIBUTE_MAPS_PAYLOAD_BYTES
            ),
            Arguments.of(
                SendMessageRequest.builder().messageBody(null).build(),
                 ATTRIBUTE_MAPS_PAYLOAD_BYTES
            ),
            Arguments.of(
                SendMessageRequest.builder().messageBody("Another test message").build(),
                "Another test message".getBytes(StandardCharsets.UTF_8).length + ATTRIBUTE_MAPS_PAYLOAD_BYTES
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideNonSendMessageRequest")
    @DisplayName("Test calculateMessageSize with non-SendMessageRequest inputs")
    void testCalculateMessageSizeWithNonSendMessageRequest(Object request) {
        Optional<Integer> actualSize = RequestPayloadCalculator.calculateMessageSize(request);
        assertEquals(Optional.empty(), actualSize);
    }

    private Stream<Arguments> provideNonSendMessageRequest() {
        return Stream.of(
            Arguments.of(ChangeMessageVisibilityRequest.builder()
                                                       .queueUrl("https://sqs.us-west-2.amazonaws.com/MyQueue")
                                                       .receiptHandle("some-receipt-handle")
                                                       .visibilityTimeout(60)
                                                       .build()),

            Arguments.of(DeleteMessageRequest.builder()
                                             .queueUrl("https://sqs.us-west-2.amazonaws.com/123456789012/MyQueue")
                                             .receiptHandle("some-receipt-handle")
                                             .build())
        );
    }
}
