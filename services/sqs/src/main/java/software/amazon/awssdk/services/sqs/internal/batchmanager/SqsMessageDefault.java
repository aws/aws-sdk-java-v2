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

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class SqsMessageDefault {

    public static final int MAX_SUPPORTED_SQS_RECEIVE_MSG = 10;

    /**
     * https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_SendMessageBatch.html
     * We can send upto up to 10 messages thus the buffer size is set to 10
     */
    public static final int MAX_SEND_MESSAGE_BATCH_SIZE = 10;


    public static final long MAX_PAYLOAD_SIZE_BYTES = 262_144; // 256 KiB

    private SqsMessageDefault() {
    }
}
