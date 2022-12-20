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

import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A completed directory-based transfer.
 *
 * @see CompletedDirectoryUpload
 */
@SdkPublicApi
public interface CompletedDirectoryTransfer extends CompletedTransfer {

    /**
     * A list of failed transfer details, including the {@link FailedObjectTransfer#exception()} responsible for the failure and
     * the {@link FailedObjectTransfer#request()} that initiated the transfer.
     *
     * @return an immutable list of failed transfers
     */
    List<? extends FailedObjectTransfer> failedTransfers();
}
