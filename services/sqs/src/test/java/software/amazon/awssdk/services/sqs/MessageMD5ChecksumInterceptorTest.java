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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
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

/**
 * Verifies the functionality of {@link MessageMD5ChecksumInterceptor}.
 */
public class MessageMD5ChecksumInterceptorTest {
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

    private void assertSuccess(SdkRequest request, SdkResponse response) {
        callInterceptor(request, response);
    }

    private void assertFailure(SdkRequest request, SdkResponse response) {
        assertThatThrownBy(() -> callInterceptor(request, response))
                .isInstanceOf(SdkClientException.class);
    }

    private void callInterceptor(SdkRequest request, SdkResponse response) {
        new MessageMD5ChecksumInterceptor().afterExecution(InterceptorContext.builder()
                                                                             .request(request)
                                                                             .response(response)
                                                                             .build(),
                                                           new ExecutionAttributes());
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
