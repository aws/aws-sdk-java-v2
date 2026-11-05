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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * An upload transfer of a single object to S3.
 */
@SdkPublicApi
public interface FileUpload extends ObjectTransfer {

    /**
     * Pauses the current upload operation and return the information that can
     * be used to resume the upload at a later time.
     * <p>
     * The information object is serializable for persistent storage until it should be resumed.
     * See {@link ResumableFileUpload} for supported formats.
     *
     * <p>
     * For a Java-based multipart-enabled {@link S3AsyncClient}, if the pause stops the upload, the returned
     * {@link ResumableFileUpload} has a multipart upload ID and {@link #completionFuture()} completes exceptionally
     * with {@link java.util.concurrent.CancellationException}. If the upload failed before
     * {@code CompleteMultipartUpload} started, the result can still have a multipart upload ID in case the multipart
     * upload still exists, and {@link #completionFuture()} keeps the original failure. If no multipart upload ID is
     * returned, the pause had no effect, and {@link #completionFuture()} reports the upload's result.
     *
     * <p>
     * Currently, it's only supported if the underlying {@link S3AsyncClient} is CRT-based (created via
     * {@link S3AsyncClient#crtBuilder()} or {@link S3AsyncClient#crtCreate()}), OR the underlying
     * {@link S3AsyncClient} has multipart enabled ({@link S3AsyncClientBuilder#multipartConfiguration(MultipartConfiguration)}
     * or {@link S3AsyncClientBuilder#multipartEnabled(Boolean)}).
     * It will throw {@link UnsupportedOperationException} if the {@link S3TransferManager} is created
     * with a non CRT-based S3 client (created via {@link S3AsyncClient#builder()}) and does not have
     * multipart configuration enabled.
     *
     * @return A {@link ResumableFileUpload} that can be used to resume the upload.
     */
    ResumableFileUpload pause();
    
    @Override
    CompletableFuture<CompletedFileUpload> completionFuture();
}
