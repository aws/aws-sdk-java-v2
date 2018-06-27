/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.util.Md5Utils;
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
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * SQS operations on sending and receiving messages will return the MD5 digest of the message body.
 * This custom request handler will verify that the message is correctly received by SQS, by
 * comparing the returned MD5 with the calculation according to the original request.
 */
public class MessageMD5ChecksumInterceptor implements ExecutionInterceptor {

    private static final int INTEGER_SIZE_IN_BYTES = 4;
    private static final byte STRING_TYPE_FIELD_INDEX = 1;
    private static final byte BINARY_TYPE_FIELD_INDEX = 2;
    private static final byte STRING_LIST_TYPE_FIELD_INDEX = 3;
    private static final byte BINARY_LIST_TYPE_FIELD_INDEX = 4;

    /*
     * Constant strings for composing error message.
     */
    private static final String MD5_MISMATCH_ERROR_MESSAGE =
            "MD5 returned by SQS does not match the calculation on the original request. " +
            "(MD5 calculated by the %s: \"%s\", MD5 checksum returned: \"%s\")";
    private static final String MD5_MISMATCH_ERROR_MESSAGE_WITH_ID =
            "MD5 returned by SQS does not match the calculation on the original request. " +
            "(Message ID: %s, MD5 calculated by the %s: \"%s\", MD5 checksum returned: \"%s\")";
    private static final String MESSAGE_BODY = "message body";
    private static final String MESSAGE_ATTRIBUTES = "message attributes";

    private static final Logger log = LoggerFactory.getLogger(MessageMD5ChecksumInterceptor.class);

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        SdkRequest originalRequest = context.request();
        if (response != null) {
            if (originalRequest instanceof SendMessageRequest) {
                SendMessageRequest sendMessageRequest = (SendMessageRequest) originalRequest;
                SendMessageResponse sendMessageResult = (SendMessageResponse) response;
                sendMessageOperationMd5Check(sendMessageRequest, sendMessageResult);

            } else if (originalRequest instanceof ReceiveMessageRequest) {
                ReceiveMessageResponse receiveMessageResult = (ReceiveMessageResponse) response;
                receiveMessageResultMd5Check(receiveMessageResult);

            } else if (originalRequest instanceof SendMessageBatchRequest) {
                SendMessageBatchRequest sendMessageBatchRequest = (SendMessageBatchRequest) originalRequest;
                SendMessageBatchResponse sendMessageBatchResult = (SendMessageBatchResponse) response;
                sendMessageBatchOperationMd5Check(sendMessageBatchRequest, sendMessageBatchResult);
            }
        }
    }

    /**
     * Throw an exception if the MD5 checksums returned in the SendMessageResponse do not match the
     * client-side calculation based on the original message in the SendMessageRequest.
     */
    private static void sendMessageOperationMd5Check(SendMessageRequest sendMessageRequest,
                                                     SendMessageResponse sendMessageResult) {
        String messageBodySent = sendMessageRequest.messageBody();
        String bodyMd5Returned = sendMessageResult.md5OfMessageBody();
        String clientSideBodyMd5 = calculateMessageBodyMd5(messageBodySent);
        if (!clientSideBodyMd5.equals(bodyMd5Returned)) {
            throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE, MESSAGE_BODY, clientSideBodyMd5,
                                                          bodyMd5Returned));
        }

        Map<String, MessageAttributeValue> messageAttrSent = sendMessageRequest.messageAttributes();
        if (messageAttrSent != null && !messageAttrSent.isEmpty()) {
            String clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrSent);
            String attrMd5Returned = sendMessageResult.md5OfMessageAttributes();
            if (!clientSideAttrMd5.equals(attrMd5Returned)) {
                throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE, MESSAGE_ATTRIBUTES,
                                                              clientSideAttrMd5, attrMd5Returned));
            }
        }
    }

    /**
     * Throw an exception if the MD5 checksums included in the ReceiveMessageResponse do not match the
     * client-side calculation on the received messages.
     */
    private static void receiveMessageResultMd5Check(ReceiveMessageResponse receiveMessageResult) {
        if (receiveMessageResult.messages() != null) {
            for (Message messageReceived : receiveMessageResult.messages()) {
                String messageBody = messageReceived.body();
                String bodyMd5Returned = messageReceived.md5OfBody();
                String clientSideBodyMd5 = calculateMessageBodyMd5(messageBody);
                if (!clientSideBodyMd5.equals(bodyMd5Returned)) {
                    throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE, MESSAGE_BODY,
                                                                  clientSideBodyMd5, bodyMd5Returned));
                }

                Map<String, MessageAttributeValue> messageAttr = messageReceived.messageAttributes();
                if (messageAttr != null && !messageAttr.isEmpty()) {
                    String attrMd5Returned = messageReceived.md5OfMessageAttributes();
                    String clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttr);
                    if (!clientSideAttrMd5.equals(attrMd5Returned)) {
                        throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE, MESSAGE_ATTRIBUTES,
                                                                      clientSideAttrMd5, attrMd5Returned));
                    }
                }
            }
        }
    }

    /**
     * Throw an exception if the MD5 checksums returned in the SendMessageBatchResponse do not match
     * the client-side calculation based on the original messages in the SendMessageBatchRequest.
     */
    private static void sendMessageBatchOperationMd5Check(SendMessageBatchRequest sendMessageBatchRequest,
                                                          SendMessageBatchResponse sendMessageBatchResult) {
        Map<String, SendMessageBatchRequestEntry> idToRequestEntryMap = new HashMap<>();
        if (sendMessageBatchRequest.entries() != null) {
            for (SendMessageBatchRequestEntry entry : sendMessageBatchRequest.entries()) {
                idToRequestEntryMap.put(entry.id(), entry);
            }
        }

        if (sendMessageBatchResult.successful() != null) {
            for (SendMessageBatchResultEntry entry : sendMessageBatchResult.successful()) {
                String messageBody = idToRequestEntryMap.get(entry.id()).messageBody();
                String bodyMd5Returned = entry.md5OfMessageBody();
                String clientSideBodyMd5 = calculateMessageBodyMd5(messageBody);
                if (!clientSideBodyMd5.equals(bodyMd5Returned)) {
                    throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE_WITH_ID, MESSAGE_BODY,
                                                                  entry.id(), clientSideBodyMd5, bodyMd5Returned));
                }

                Map<String, MessageAttributeValue> messageAttr = idToRequestEntryMap.get(entry.id())
                                                                                    .messageAttributes();
                if (messageAttr != null && !messageAttr.isEmpty()) {
                    String attrMd5Returned = entry.md5OfMessageAttributes();
                    String clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttr);
                    if (!clientSideAttrMd5.equals(attrMd5Returned)) {
                        throw new SdkClientException(String.format(MD5_MISMATCH_ERROR_MESSAGE_WITH_ID,
                                                                      MESSAGE_ATTRIBUTES, entry.id(), clientSideAttrMd5,
                                                                      attrMd5Returned));
                    }
                }
            }
        }
    }

    /**
     * Returns the hex-encoded MD5 hash String of the given message body.
     */
    private static String calculateMessageBodyMd5(String messageBody) {
        if (log.isDebugEnabled()) {
            log.debug("Message body: " + messageBody);
        }
        byte[] expectedMd5;
        try {
            expectedMd5 = Md5Utils.computeMD5Hash(messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new SdkClientException("Unable to calculate the MD5 hash of the message body. " + e.getMessage(),
                                            e);
        }
        String expectedMd5Hex = BinaryUtils.toHex(expectedMd5);
        if (log.isDebugEnabled()) {
            log.debug("Expected  MD5 of message body: " + expectedMd5Hex);
        }
        return expectedMd5Hex;
    }

    /**
     * Returns the hex-encoded MD5 hash String of the given message attributes.
     */
    private static String calculateMessageAttributesMd5(final Map<String, MessageAttributeValue> messageAttributes) {
        if (log.isDebugEnabled()) {
            log.debug("Message attributes: " + messageAttributes);
        }
        List<String> sortedAttributeNames = new ArrayList<>(messageAttributes.keySet());
        Collections.sort(sortedAttributeNames);

        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");

            for (String attrName : sortedAttributeNames) {
                MessageAttributeValue attrValue = messageAttributes.get(attrName);

                // Encoded Name
                updateLengthAndBytes(md5Digest, attrName);

                // Encoded Type
                updateLengthAndBytes(md5Digest, attrValue.dataType());

                // Encoded Value
                if (attrValue.stringValue() != null) {
                    md5Digest.update(STRING_TYPE_FIELD_INDEX);
                    updateLengthAndBytes(md5Digest, attrValue.stringValue());
                } else if (attrValue.binaryValue() != null) {
                    md5Digest.update(BINARY_TYPE_FIELD_INDEX);
                    updateLengthAndBytes(md5Digest, attrValue.binaryValue());
                } else if (attrValue.stringListValues() != null &&
                           attrValue.stringListValues().size() > 0) {
                    md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX);
                    for (String strListMember : attrValue.stringListValues()) {
                        updateLengthAndBytes(md5Digest, strListMember);
                    }
                } else if (attrValue.binaryListValues() != null &&
                           attrValue.binaryListValues().size() > 0) {
                    md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX);
                    for (ByteBuffer byteListMember : attrValue.binaryListValues()) {
                        updateLengthAndBytes(md5Digest, byteListMember);
                    }
                }
            }
        } catch (Exception e) {
            throw new SdkClientException("Unable to calculate the MD5 hash of the message attributes. "
                                            + e.getMessage(), e);
        }

        String expectedMd5Hex = BinaryUtils.toHex(md5Digest.digest());
        if (log.isDebugEnabled()) {
            log.debug("Expected  MD5 of message attributes: " + expectedMd5Hex);
        }
        return expectedMd5Hex;
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input String and the actual utf8-encoded byte values.
     */
    private static void updateLengthAndBytes(MessageDigest digest, String str) {
        byte[] utf8Encoded = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(utf8Encoded.length);
        digest.update(lengthBytes.array());
        digest.update(utf8Encoded);
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input ByteBuffer and all the bytes it contains.
     */
    private static void updateLengthAndBytes(MessageDigest digest, ByteBuffer binaryValue) {
        ByteBuffer readOnlyBuffer = binaryValue.asReadOnlyBuffer();
        int size = readOnlyBuffer.remaining();
        ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(size);
        digest.update(lengthBytes.array());
        digest.update(readOnlyBuffer);
    }
}
