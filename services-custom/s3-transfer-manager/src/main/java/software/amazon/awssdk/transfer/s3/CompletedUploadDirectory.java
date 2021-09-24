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

import java.util.List;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A completed download directory transfer.
 */
@SdkPublicApi
@SdkPreviewApi
public interface CompletedUploadDirectory extends CompletedTransfer {

    /**
     * A list of failed single file uploads associated with one upload directory transfer
     * @return A list of failed uploads
     */
    List<FailedUpload> failedUploads();

    /**
     * A list of successful single file uploads associated with one upload directory transfer
     * @return a list of successful  uploads
     */
    List<CompletedUpload> successfulObjects();
}
