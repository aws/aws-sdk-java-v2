/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sqs.buffered;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/** this class converts sqs batch entry results to individual results. */
class ResultConverter {

    static SendMessageResponse convert(SendMessageBatchResultEntry br) {
        SendMessageResponse.Builder toReturnBuilder = SendMessageResponse.builder()
            .md5OfMessageBody(br.md5OfMessageBody())
            .messageId(br.messageId())
            .md5OfMessageAttributes(br.md5OfMessageAttributes());
        return toReturnBuilder.build();
    }

    static Exception convert(BatchResultErrorEntry be) {
        AmazonServiceException toReturn = new AmazonServiceException(be.message());

        toReturn.setErrorCode(be.code());
        toReturn.setErrorType(be.senderFault() ? ErrorType.Client : ErrorType.Service);
        toReturn.setServiceName("AmazonSQS");

        return toReturn;

    }

    public static <X extends AmazonWebServiceRequest> X appendUserAgent(X request, String userAgent) {
        request.getRequestClientOptions().appendUserAgent(userAgent);
        return request;
    }

}
