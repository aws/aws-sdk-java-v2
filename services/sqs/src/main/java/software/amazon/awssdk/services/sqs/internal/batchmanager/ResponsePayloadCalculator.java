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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class ResponsePayloadCalculator {

    private static final int MAX_SIZE_BYTES = 262_144; // 256 KiB

    /**
     * Evaluates if the total size of the message body, message attributes, and message system attributes
     * exceeds the maximum allowed size for a SendMessageRequest. If the request is not a SendMessageRequest,
     * it returns -1.
     *
     * @param request the request to evaluate
     * @param <RequestT> the type of the request
     * @return the total size in bytes if the request is a SendMessageRequest, otherwise -1
     */
    public static <RequestT> long calculateMessageSize(RequestT request) {
        if (!(request instanceof SendMessageRequest)) {
            return -1;
        }

        SendMessageRequest sendMessageRequest = (SendMessageRequest) request;
        long totalSize = 0;

        // Calculate size of messageBody
        if (sendMessageRequest.messageBody() != null) {
            totalSize += sendMessageRequest.messageBody().getBytes(StandardCharsets.UTF_8).length;
        }

        // Calculate size of messageAttributes
        if (sendMessageRequest.messageAttributes() != null) {
            totalSize += sendMessageRequest.messageAttributes().entrySet().stream()
                                           .mapToInt(entry -> {
                                               String key = entry.getKey();
                                               MessageAttributeValue value = entry.getValue();
                                               return key.getBytes(StandardCharsets.UTF_8).length +
                                                      value.dataType().getBytes(StandardCharsets.UTF_8).length +
                                                      (value.stringValue() != null ?
                                                       value.stringValue().getBytes(StandardCharsets.UTF_8).length : 0);
                                           }).sum();
        }

        // Calculate size of messageSystemAttributes
        if (sendMessageRequest.messageSystemAttributes() != null) {
            totalSize += sendMessageRequest.messageSystemAttributes().entrySet().stream()
                                           .mapToInt(entry -> {
                                               String key = entry.getKey().toString();
                                               MessageSystemAttributeValue value = entry.getValue();
                                               return key.getBytes(StandardCharsets.UTF_8).length +
                                                      (value.stringValue() != null ?
                                                       value.stringValue().getBytes(StandardCharsets.UTF_8).length : 0);
                                           }).sum();
        }

        return totalSize;
    }

}