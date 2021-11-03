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

import java.util.Collection;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A completed directory-based transfer.
 * 
 * @see CompletedDirectoryUpload
 */
@SdkPublicApi
@SdkPreviewApi
public interface CompletedDirectoryTransfer extends CompletedTransfer {

    /**
     * An immutable collection of failed transfers with error details, request metadata about each file that is failed to
     * transfer.
     *
     * <p>
     * Failed single object transfers can be retried by calling {@link S3TransferManager#uploadFile(UploadFileRequest)} or
     * {@link S3TransferManager#downloadFile(DownloadFileRequest)}.
     *
     * <pre>
     * {@code
     * // Retrying failed uploads if the exception is retryable
     * List<CompletableFuture<CompletedUpload>> futures =
     *     completedDirectoryUpload.failedTransfers()
     *                             .stream()
     *                             .filter(failedSingleFileUpload -> isRetryable(failedSingleFileUpload.exception()))
     *                             .map(failedSingleFileUpload ->
     *                                  tm.upload(failedSingleFileUpload.request()).completionFuture())
     *                             .collect(Collectors.toList());
     * CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
     * }
     * </pre>
     *
     * @return a list of failed transfers
     */
    Collection<? extends FailedObjectTransfer> failedTransfers();
}
