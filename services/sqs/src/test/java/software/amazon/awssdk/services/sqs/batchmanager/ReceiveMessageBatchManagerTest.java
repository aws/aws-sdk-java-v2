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


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ReceiveMessageBatchManager;
import software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.testutils.LogCaptor;

@ExtendWith(MockitoExtension.class)
class ReceiveMessageBatchManagerTest {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(4);

    @Mock
    private SqsAsyncClient sqsClient;

    private ReceiveMessageBatchManager receiveMessageBatchManager;

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideBatchOverrideConfigurations")
    @DisplayName("Test BatchRequest with various configurations")
    void testBatchRequest(String testCaseName,
                          ResponseBatchConfiguration overrideConfig,
                          ReceiveMessageRequest request,
                          boolean useBatchManager,
                          String inEligibleReason) throws Exception {

        setupBatchManager(overrideConfig);

        CompletableFuture<ReceiveMessageResponse> mockResponse = CompletableFuture.completedFuture(
            ReceiveMessageResponse.builder().build());
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(mockResponse);

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {

            if (useBatchManager) {
                mockQueueAttributes("0", "1");
            }

            CompletableFuture<ReceiveMessageResponse> result = receiveMessageBatchManager.batchRequest(request);
            result.get(2, TimeUnit.SECONDS);
            Thread.sleep(500);

            ArgumentCaptor<ReceiveMessageRequest> requestCaptor = forClass(ReceiveMessageRequest.class);

            if (useBatchManager) {
                verifyBatchManagerUsed(requestCaptor);
            } else {
                verifyBatchManagerNotUsed(request, requestCaptor, logCaptor, inEligibleReason);
            }
        }
    }

    private void setupBatchManager(ResponseBatchConfiguration overrideConfig) {
        receiveMessageBatchManager = new ReceiveMessageBatchManager(sqsClient, EXECUTOR, overrideConfig);
    }

    private void verifyBatchManagerUsed(ArgumentCaptor<ReceiveMessageRequest> requestCaptor) {
        verify(sqsClient, atLeast(1)).receiveMessage(requestCaptor.capture());
        assertEquals(ResponseBatchConfiguration.MAX_DONE_RECEIVE_BATCHES_DEFAULT,
                     requestCaptor.getValue().maxNumberOfMessages());
    }

    private void verifyBatchManagerNotUsed(ReceiveMessageRequest request,
                                           ArgumentCaptor<ReceiveMessageRequest> requestCaptor,
                                           LogCaptor logCaptor,
                                           String inEligibleReason) {
        verify(sqsClient, times(1)).receiveMessage(requestCaptor.capture());
        assertEquals(request.maxNumberOfMessages(), requestCaptor.getValue().maxNumberOfMessages());
        assertEquals(request.visibilityTimeout(), requestCaptor.getValue().visibilityTimeout());
        assertThat(logCaptor.loggedEvents())
            .anySatisfy(logEvent -> assertThat(logEvent.getMessage().getFormattedMessage())
                .contains(inEligibleReason));
    }

    private void mockQueueAttributes(String receiveMessageWaitTimeSeconds, String visibilityTimeout) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, receiveMessageWaitTimeSeconds);
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, visibilityTimeout);

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
    }

    private static Stream<Arguments> provideBatchOverrideConfigurations() {
        return Stream.of(
            Arguments.of(
                "Buffering enabled, compatible system and message attributes, no visibility timeout",
                ResponseBatchConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(MessageSystemAttributeName.SENDER_ID)
                                     .build(),
                true,
                ""
            ),
            Arguments.of(
                "Buffering enabled, compatible attributes, no visibility timeout but deprecated attributeNames",
                ResponseBatchConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNamesWithStrings(Collections.singletonList("SenderId"))
                                     .attributeNames(QueueAttributeName.ALL) // Deprecated api not supported for Batching
                                     .build(),
                false,
                "Incompatible attributes."
            ),
            Arguments.of(
                "Buffering disabled, incompatible system attributes, no visibility timeout",
                ResponseBatchConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENT_TIMESTAMP))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNamesWithStrings(Collections.singletonList("SenderId"))
                                     .build(),
                false,
                "Incompatible attributes."
            ),
            Arguments.of(
                "Buffering disabled, compatible attributes, visibility timeout is set",
                ResponseBatchConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                     .visibilityTimeout(30)
                                     .build(),
                false,
                "Visibility timeout is set."
            ),
            Arguments.of(
                "Buffering disabled, compatible attributes, no visibility timeout but has attribute names",
                ResponseBatchConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                     .attributeNamesWithStrings("All")
                                     .build(),
                false,
                "Incompatible attributes."
            ),
            Arguments.of(
                "Buffering enabled, simple ReceiveMessageRequest, no visibility timeout",
                ResponseBatchConfiguration.builder()
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .maxNumberOfMessages(3)
                                     .build(),
                true,
                ""
            ),
            Arguments.of(
                "Buffering disabled, request has override config",
                ResponseBatchConfiguration.builder()
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .maxNumberOfMessages(3)
                                     .overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(2)))
                                     .build(),
                false,
                "Request has override configurations."
            ),
            Arguments.of(
                "Buffering enabled, with waitTimeSeconds in ReceiveMessageRequest",
                ResponseBatchConfiguration.builder()
                                          .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .maxNumberOfMessages(3)
                                     .waitTimeSeconds(3)
                                     .build(),
                true,
                ""
            )
        );
    }
}
