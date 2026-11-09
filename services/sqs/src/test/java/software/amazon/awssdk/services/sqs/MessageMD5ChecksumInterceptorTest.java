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

package software.amazon.awssdk.services.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.services.sqs.endpoints.SqsClientContextParams;
import software.amazon.awssdk.services.sqs.internal.MessageMD5ChecksumInterceptor;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Verifies the functionality of {@link MessageMD5ChecksumInterceptor}.
 */
public class MessageMD5ChecksumInterceptorTest {
    private static final String CANARY_BODY = "{\"token\":\"CANARY-BODY-123\"}\r\nSECOND-LINE";
    private static final String CANARY_BODY_MD5 = "cb4a8b7a28c2b3ff5491f8f42ad90864";
    private static final String CANARY_ATTRIBUTE_NAME = "canaryAttrName";
    private static final String CANARY_ATTRIBUTE_VALUE = "CANARY-ATTR-456";
    private static final String[] CANARY_CONTENT = {"CANARY-BODY-123", "SECOND-LINE", CANARY_ATTRIBUTE_NAME,
                                                    CANARY_ATTRIBUTE_VALUE};

    @Test
    public void sendMessagePassesValidChecksums() {
        SendMessageRequest request = SendMessageRequest.builder()
                                                       .messageBody(messageBody())
                                                       .messageAttributes(messageAttributes())
                                                       .build();

        SendMessageResponse response = SendMessageResponse.builder()
                                                          .md5OfMessageBody(messageBodyChecksum())
                                                          .md5OfMessageAttributes(messageAttributesChecksum())
                                                          .build();

        assertSuccess(request, response);
    }

    @Test
    public void sendMessageFailsInvalidBodyChecksum() {
        SendMessageRequest request = SendMessageRequest.builder()
                                                       .messageBody(messageBody())
                                                       .messageAttributes(messageAttributes())
                                                       .build();

        SendMessageResponse response = SendMessageResponse.builder()
                                                          .md5OfMessageBody("bad")
                                                          .md5OfMessageAttributes(messageAttributesChecksum())
                                                          .build();

        assertFailure(request, response);
    }

    @Test
    public void sendMessageFailsInvalidAttributeChecksum() {
        SendMessageRequest request = SendMessageRequest.builder()
                                                       .messageBody(messageBody())
                                                       .messageAttributes(messageAttributes())
                                                       .build();

        SendMessageResponse response = SendMessageResponse.builder()
                                                          .md5OfMessageBody(messageBodyChecksum())
                                                          .md5OfMessageAttributes("bad")
                                                          .build();

        assertFailure(request, response);
    }

    @Test
    public void sendMessageBatchPassesValidChecksums() {
        SendMessageBatchRequestEntry requestEntry = SendMessageBatchRequestEntry.builder()
                                                                                .messageBody(messageBody())
                                                                                .messageAttributes(messageAttributes())
                                                                                .build();

        SendMessageBatchResultEntry resultEntry = SendMessageBatchResultEntry.builder()
                                                                             .md5OfMessageBody(messageBodyChecksum())
                                                                             .md5OfMessageAttributes(messageAttributesChecksum())
                                                                             .build();

        SendMessageBatchRequest request = SendMessageBatchRequest.builder()
                                                                 .entries(requestEntry, requestEntry)
                                                                 .build();

        SendMessageBatchResponse response = SendMessageBatchResponse.builder()
                                                                    .successful(resultEntry, resultEntry)
                                                                    .build();

        assertSuccess(request, response);
    }

    @Test
    public void sendMessageBatchFailsInvalidBodyChecksums() {
        SendMessageBatchRequestEntry requestEntry = SendMessageBatchRequestEntry.builder()
                                                                                .messageBody(messageBody())
                                                                                .messageAttributes(messageAttributes())
                                                                                .build();

        SendMessageBatchResultEntry resultEntry = SendMessageBatchResultEntry.builder()
                                                                             .md5OfMessageBody(messageBodyChecksum())
                                                                             .md5OfMessageAttributes(messageAttributesChecksum())
                                                                             .build();

        SendMessageBatchResultEntry badResultEntry = SendMessageBatchResultEntry.builder()
                                                                                .md5OfMessageBody("bad")
                                                                                .md5OfMessageAttributes(messageAttributesChecksum())
                                                                                .build();

        SendMessageBatchRequest request = SendMessageBatchRequest.builder()
                                                                 .entries(requestEntry, requestEntry)
                                                                 .build();

        SendMessageBatchResponse response = SendMessageBatchResponse.builder()
                                                                    .successful(resultEntry, badResultEntry)
                                                                    .build();

        assertFailure(request, response);
    }

    @Test
    public void sendMessageBatchFailsInvalidAttributeChecksums() {
        SendMessageBatchRequestEntry requestEntry = SendMessageBatchRequestEntry.builder()
                                                                                .messageBody(messageBody())
                                                                                .messageAttributes(messageAttributes())
                                                                                .build();

        SendMessageBatchResultEntry resultEntry = SendMessageBatchResultEntry.builder()
                                                                             .md5OfMessageBody(messageBodyChecksum())
                                                                             .md5OfMessageAttributes(messageAttributesChecksum())
                                                                             .build();

        SendMessageBatchResultEntry badResultEntry = SendMessageBatchResultEntry.builder()
                                                                                .md5OfMessageBody(messageBodyChecksum())
                                                                                .md5OfMessageAttributes("bad")
                                                                                .build();

        SendMessageBatchRequest request = SendMessageBatchRequest.builder()
                                                                 .entries(requestEntry, requestEntry)
                                                                 .build();

        SendMessageBatchResponse response = SendMessageBatchResponse.builder()
                                                                    .successful(resultEntry, badResultEntry)
                                                                    .build();

        assertFailure(request, response);
    }

    @Test
    public void receiveMessagePassesValidChecksums() {
        Message message = Message.builder()
                                 .body(messageBody())
                                 .messageAttributes(messageAttributes())
                                 .md5OfBody(messageBodyChecksum())
                                 .md5OfMessageAttributes(messageAttributesChecksum())
                                 .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(message, message)
                                                                .build();

        assertSuccess(ReceiveMessageRequest.builder().build(), response);
    }

    @Test
    public void receiveMessageFailsInvalidBodyChecksum() {
        Message message = Message.builder()
                                 .body(messageBody())
                                 .messageAttributes(messageAttributes())
                                 .md5OfBody(messageBodyChecksum())
                                 .md5OfMessageAttributes(messageAttributesChecksum())
                                 .build();
        Message badMessage = Message.builder()
                                    .body(messageBody())
                                    .messageAttributes(messageAttributes())
                                    .md5OfBody("bad")
                                    .md5OfMessageAttributes(messageAttributesChecksum())
                                    .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(message, badMessage)
                                                                .build();

        assertFailure(ReceiveMessageRequest.builder().build(), response);
    }

    @Test
    public void receiveMessageFailsInvalidAttributeChecksum() {
        Message message = Message.builder()
                                 .body(messageBody())
                                 .messageAttributes(messageAttributes())
                                 .md5OfBody(messageBodyChecksum())
                                 .md5OfMessageAttributes(messageAttributesChecksum())
                                 .build();
        Message badMessage = Message.builder()
                                    .body(messageBody())
                                    .messageAttributes(messageAttributes())
                                    .md5OfBody(messageBodyChecksum())
                                    .md5OfMessageAttributes("bad")
                                    .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                                                                .messages(message, badMessage)
                                                                .build();

        assertFailure(ReceiveMessageRequest.builder().build(), response);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("canaryContentExchanges")
    public void md5Validation_withCanaryContent_doesNotLogMessageContent(String operation, SdkRequest request,
                                                                         SdkResponse response) {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            assertThatThrownBy(() -> callInterceptor(request, response))
                .isInstanceOf(SdkClientException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(CANARY_CONTENT));

            List<String> messages = logCaptor.loggedEvents()
                                             .stream()
                                             .map(e -> e.getMessage().getFormattedMessage())
                                             .collect(Collectors.toList());

            // Sanity check that DEBUG capture of this interceptor is active for both the body and attribute paths.
            assertThat(messages).anySatisfy(m -> assertThat(m).contains("Expected  MD5 of message body"));
            assertThat(messages).anySatisfy(m -> assertThat(m).contains("Expected  MD5 of message attributes"));
            assertThat(messages).allSatisfy(m -> assertThat(m).doesNotContain(CANARY_CONTENT));
        }
    }

    private static Stream<Arguments> canaryContentExchanges() {
        return Stream.of(
            Arguments.of("SendMessage",
                         SendMessageRequest.builder().messageBody(CANARY_BODY).messageAttributes(canaryAttributes()).build(),
                         SendMessageResponse.builder().md5OfMessageBody(CANARY_BODY_MD5).md5OfMessageAttributes("bad").build()),
            Arguments.of("SendMessageBatch",
                         SendMessageBatchRequest.builder()
                                                .entries(e -> e.id("1").messageBody(CANARY_BODY)
                                                               .messageAttributes(canaryAttributes()))
                                                .build(),
                         SendMessageBatchResponse.builder()
                                                 .successful(e -> e.id("1").md5OfMessageBody(CANARY_BODY_MD5)
                                                                   .md5OfMessageAttributes("bad"))
                                                 .build()),
            Arguments.of("ReceiveMessage",
                         ReceiveMessageRequest.builder().build(),
                         ReceiveMessageResponse.builder()
                                               .messages(m -> m.body(CANARY_BODY).md5OfBody(CANARY_BODY_MD5)
                                                               .messageAttributes(canaryAttributes())
                                                               .md5OfMessageAttributes("bad"))
                                               .build()));
    }

    private static Map<String, MessageAttributeValue> canaryAttributes() {
        return Collections.singletonMap(CANARY_ATTRIBUTE_NAME, MessageAttributeValue.builder()
                                                                                    .stringValue(CANARY_ATTRIBUTE_VALUE)
                                                                                    .dataType("String")
                                                                                    .build());
    }

    private void assertSuccess(SdkRequest request, SdkResponse response) {
        callInterceptor(request, response);
    }

    private void assertFailure(SdkRequest request, SdkResponse response) {
        assertThatThrownBy(() -> callInterceptor(request, response))
                .isInstanceOf(SdkClientException.class);
    }

    private void callInterceptor(SdkRequest request, SdkResponse response) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, AttributeMap.builder().build());
        new MessageMD5ChecksumInterceptor().afterExecution(InterceptorContext.builder()
                                                                             .request(request)
                                                                             .response(response)
                                                                             .build(),
                                                           executionAttributes);
    }

    private String messageBody() {
        return "Body";
    }

    private String messageBodyChecksum() {
        return "ac101b32dda4448cf13a93fe283dddd8";
    }

    private Map<String, MessageAttributeValue> messageAttributes() {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("String", MessageAttributeValue.builder()
                                                             .stringValue("Value")
                                                             .dataType("String")
                                                             .build());
        messageAttributes.put("Binary", MessageAttributeValue.builder()
                                                             .binaryValue(SdkBytes.fromByteArray(new byte[] { 5 }))
                                                             .dataType("Binary")
                                                             .build());
        messageAttributes.put("StringList", MessageAttributeValue.builder()
                                                                 .stringListValues("ListValue")
                                                                 .dataType("String")
                                                                 .build());
        messageAttributes.put("ByteList", MessageAttributeValue.builder()
                                                               .binaryListValues(SdkBytes.fromByteArray(new byte[] { 3 }))
                                                               .dataType("Binary")
                                                               .build());
        return messageAttributes;
    }

    private String messageAttributesChecksum() {
        return "4b6959cf7735fdade89bc099b85b3234";
    }
}
