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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentCaptor.forClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ReceiveMessageBatchManagerTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);


    private ReceiveMessageBatchManager receiveMessageBatchManager;


    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideBatchOverrideConfigurations")
    @DisplayName("Test BatchRequest with various configurations")
    void testBatchRequest_WhenBufferingDisabledAndInCompatible_ShouldNotUseBatchManager(String testCaseName,
                                                                                               BatchOverrideConfiguration overrideConfig,
                                                                                               ReceiveMessageRequest request,
                                                                                               boolean useBatchManager) throws Exception {

        // Initialize the ResponseBatchConfiguration and ReceiveMessageBatchManager
        ResponseBatchConfiguration config = ResponseBatchConfiguration.builder()
            .messageSystemAttributeNames(overrideConfig.receiveMessageSystemAttributeNames())
            .receiveMessageAttributeNames(overrideConfig.receiveMessageAttributeNames())
            .visibilityTimeout(overrideConfig.receiveMessageVisibilityTimeout())
            .messageMinWaitDuration(overrideConfig.receiveMessageMinWaitDuration()).build();

        receiveMessageBatchManager = new ReceiveMessageBatchManager(sqsClient, executor, overrideConfig);

        CompletableFuture<ReceiveMessageResponse> mockResponse =
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build());
        String visibilityTimeout = "1";
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(mockResponse);
        if(useBatchManager) {
            mockGetQueueAttributesResponse("0", visibilityTimeout);
        }


        CompletableFuture<ReceiveMessageResponse> result = receiveMessageBatchManager.batchRequest(request);
        result.get(2, TimeUnit.SECONDS);

        // Enough time to make sure any spawned task after receiving response is completed
        Thread.sleep(500);

        // Capture the argument passed to receiveMessage
        ArgumentCaptor<ReceiveMessageRequest> requestCaptor = forClass(ReceiveMessageRequest.class);

        if (useBatchManager) {
            verify(sqsClient, atLeast(1)).receiveMessage(requestCaptor.capture());

            // Assertions to verify the behavior when batch manager is used
            assertEquals(config.maxBatchItems(), requestCaptor.getValue().maxNumberOfMessages());
            assertEquals(Integer.parseInt(visibilityTimeout), requestCaptor.getValue().visibilityTimeout());
        } else {
            verify(sqsClient, times(1)).receiveMessage(requestCaptor.capture());

            // Assertions to verify the behavior when batch manager is not used
            assertEquals(request.maxNumberOfMessages(), requestCaptor.getValue().maxNumberOfMessages());
            assertEquals(request.visibilityTimeout(), requestCaptor.getValue().visibilityTimeout());
            assertNotEquals(config.maxBatchItems(),
                            requestCaptor.getValue().maxNumberOfMessages());
        }
    }



    private static Stream<Arguments> provideBatchOverrideConfigurations() {
        return Stream.of(
            Arguments.of(
                "Buffering enabled, compatible system and message attributes, and no visibility timeout",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(MessageSystemAttributeName.SENDER_ID)
                                     .build(),
                true
            ),
            Arguments.of(
                "Buffering , compatible attributes, and no visibility timeout",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNamesWithStrings(Collections.singletonList("SenderId"))
                                     // attributeNames which is Deprecated api not supported for Batching
                    .attributeNames(QueueAttributeName.ALL)
                                     .build(),
                false
            ),
            Arguments.of(
                "Buffering disabled, incompatible system attributes, and no visibility timeout",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENT_TIMESTAMP))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNamesWithStrings(Collections.singletonList("SenderId"))  //
                                     // Incompatible system attribute
                                     .build(),
                false
            ),
            Arguments.of(
                "Buffering disabled, compatible attributes, but visibility timeout is set",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                     .visibilityTimeout(30)  // Visibility timeout is set
                                     .build(),
                false
            ),
            Arguments.of(
                "Buffering disabled, compatible attributes, no visibility timeout, but request has attribute names",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageAttributeNames(Collections.singletonList("attr1"))
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .messageAttributeNames(Collections.singletonList("attr1"))
                                     .messageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                     .attributeNamesWithStrings("All")  // Request has attribute names
                                     .build(),
                false
            ),
            Arguments.of(
                "Buffering enabled, with messageSystemAttributeName in Config and simple ReceiveMessageRequest",
                BatchOverrideConfiguration.builder()
                                          .receiveMessageSystemAttributeNames(Collections.singletonList(MessageSystemAttributeName.SENDER_ID))
                                          .build(),
                ReceiveMessageRequest.builder()
                                     .queueUrl("testQueueUrl")
                                     .maxNumberOfMessages(3)
                                     .build(),
                true
            )
        );
    }

    private void mockGetQueueAttributesResponse(String receiveMessageWaitTimeSeconds, String visibilityTimeout) {
        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, receiveMessageWaitTimeSeconds);
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, visibilityTimeout);

        GetQueueAttributesResponse response = GetQueueAttributesResponse.builder()
                                                                        .attributes(attributes)
                                                                        .build();

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
    }


}
