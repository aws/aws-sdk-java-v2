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

package software.amazon.awssdk.transfer.s3;

import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Represents a failed single file transfer in a multi-file transfer operation such as
 * {@link S3TransferManager#uploadDirectory}
 */
@SdkPublicApi
@SdkPreviewApi
public interface FailedFileTransfer<T extends TransferRequest> {

    /**
     * The exception thrown from a specific single file transfer
     *
     * @return the exception thrown
     */
    Throwable exception();

    /**
     * The failed {@link TransferRequest}.
     *
     * @return the failed request
     */
    T request();

    interface Builder<T extends TransferRequest> {
        /**
         * Specify the exception thrown from a specific single file transfer
         *
         * @param exception the exception thrown
         * @return this builder for method chaining.
         */
        Builder<T> exception(Throwable exception);

        /**
         * Specify the failed request
         *
         * @param request the failed request
         * @return this builder for method chaining.
         */
        Builder<T> request(T request);

        /**
         * Builds a {@link FailedFileTransfer} based on the properties supplied to this builder
         *
         * @return An initialized {@link FailedFileTransfer}
         */
        FailedFileTransfer<T> build();
    }
}
