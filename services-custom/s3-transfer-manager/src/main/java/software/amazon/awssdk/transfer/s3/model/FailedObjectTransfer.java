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

package software.amazon.awssdk.transfer.s3.model;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Represents a failed single file transfer in a multi-file transfer operation such as
 * {@link S3TransferManager#uploadDirectory}
 */
@SdkPublicApi
public interface FailedObjectTransfer {

    /**
     * The exception thrown from a specific single file transfer
     *
     * @return the exception thrown
     */
    Throwable exception();

    /**
     * The failed {@link TransferObjectRequest}.
     *
     * @return the failed request
     */
    TransferObjectRequest request();
}
