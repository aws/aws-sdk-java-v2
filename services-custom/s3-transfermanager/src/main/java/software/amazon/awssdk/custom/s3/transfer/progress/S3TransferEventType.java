/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.custom.s3.transfer.progress;


import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.progress.ProgressEventType;

/**
 * Progress event types that are used to track the transfer progress.
 */
@SdkPublicApi
public enum  S3TransferEventType implements ProgressEventType {

    /**
     *
     */
     TRANSFER_PREPARING_EVENT,

    /**
     *
     */
     TRANSFER_STARTED_EVENT,

    /**
     *
     */
     TRANSFER_COMPLETED_EVENT,

    /**
     *
     */
     TRANSFER_FAILED_EVENT,
    /**
     *
     */
     TRANSFER_CANCELED_EVENT,

    /**
     * A transfer event denotes the transfer has completed.
     */
      TRANSFER_PART_STARTED_EVENT,

    /**
     * A transfer event denotes the transfer has completed.
     */
     TRANSFER_PART_COMPLETED_EVENT,

    /**
     * A transfer event denotes the transfer has failed.
     */
     TRANSFER_PART_FAILED_EVENT
}
