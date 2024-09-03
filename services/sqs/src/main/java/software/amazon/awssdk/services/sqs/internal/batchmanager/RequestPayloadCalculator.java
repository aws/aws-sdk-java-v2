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

import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.ATTRIBUTE_MAPS_PAYLOAD_BYTES;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@SdkInternalApi
public final class RequestPayloadCalculator {

    private RequestPayloadCalculator() {
    }

    /**
     * Evaluates the total size of the message body, message attributes, and message system attributes for a SendMessageRequest.
     * If the request is not a SendMessageRequest, returns an empty Optional.
     *
     * @param request    the request to evaluate
     * @param <RequestT> the type of the request
     * @return an Optional containing the total size in bytes if the request is a SendMessageRequest, otherwise an empty Optional
     */
    public static <RequestT> Optional<Integer> calculateMessageSize(RequestT request) {
        if (!(request instanceof SendMessageRequest)) {
            return Optional.empty();
        }
        SendMessageRequest sendMessageRequest = (SendMessageRequest) request;
        Integer totalSize = calculateBodySize(sendMessageRequest) + ATTRIBUTE_MAPS_PAYLOAD_BYTES;
        return Optional.of(totalSize);
    }

    private static int calculateBodySize(SendMessageRequest request) {
        return request.messageBody() != null ? request.messageBody().getBytes(StandardCharsets.UTF_8).length : 0;
    }

}
