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

    public static final int MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES = 262_144; // 256 KiB

    /**
     * <a href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-message-attributes">
     * AWS SQS Message Attributes Documentation</a>
     *
     * Rounding up max payload due to attribute maps.
     * This was not done in V1, thus an issue was reported where batch messages failed with payload size exceeding the maximum.
     */
    public static final int ATTRIBUTE_MAPS_PAYLOAD_BYTES = 16 * 1024; // 16 KiB

    private SqsMessageDefault() {
    }
}
