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

package software.amazon.awssdk.custom.s3.transfer;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.custom.s3.transfer.internal.DefaultS3TransferManager;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The S3 Transfer Manager is a library that allows users to easily and
 * optimally upload and downloads to and from S3.
 * <p>
 * The list of features includes:
 * <ul>
 * <li>Parallel uploads and downloads</li>
 * <li>Bandwidth limiting</li>
 * <li>Pause and resume of transfers</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * // Create using all default configuration values
 * S3TransferManager tm = S3TranferManager.create();
 *
 * // Using custom configuration values to set max download speed
 * S3TransferManager tm = S3TransferManager.builder()
 *         .configuration(TransferManagerConfiguration.builder()
 *             .maximumDownloadBytesSecond(5 * 1024 * 1024) // 5MiB
 *             .build()
 *         .build();
 * }
 * </pre>
 */
@SdkPublicApi
public interface S3TransferManager extends ToCopyableBuilder<S3TransferManager.Builder, S3TransferManager>, SdkAutoCloseable {
    /**
     * Download an object identified by the bucket and key from S3 to the given
     * file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate transfer
     * Download myFileDownload = tm.download(BUCKET, KEY, Paths.get("/tmp/myFile.txt");
     * // Wait for transfer to complete
     * myFileDownload().completionFuture().join();
     * }
     * </pre>
     */
    default Download download(String bucket, String key, Path file) {
        return download(DownloadRequest.builder()
                        .downloadSpecification(DownloadObjectSpecification.fromApiRequest(
                                GetObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(key)
                                        .build()
                        ))
                        .build(),
                file);
    }

    /**
     * Download an object using an S3 presigned URL to the given file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate transfer
     * Download myFileDownload = tm.download(myPresignedUrl, Paths.get("/tmp/myFile.txt");
     * // Wait for transfer to complete
     * myFileDownload()completionFuture().join();
     * }
     * </pre>
     */
    default Download download(URL presignedUrl, Path file) {
        throw new UnsupportedOperationException();
    }

    /**
     * Download an object in S3 to the given file.
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate the transfer
     * Download myDownload = tm.download(DownloadObjectRequest.builder()
     *         .downloadObjectSpecification(DownloadObjectSpecification.fromApiRequest(
     *             GetObjectRequest.builder()
     *                 .bucket(BUCKET)
     *                 .key(KEY)
     *                 .build()))
     *          // Set the known length of the object to avoid a HeadObject call
     *         .size(1024 * 1024 * 5)
     *         .build(),
     *         Paths.get("/tmp/myFile.txt"));
     * // Wait for the transfer to complete
     * myDownload.completionFuture().join();
     * }
     * </pre>
     */
    Download download(DownloadRequest request, Path file);

    /**
     * Resume a previously paused object download.
     */
    Download resumeDownload(DownloadState downloadState);

    /**
     * Download the set of objects from the bucket with the given prefix to a directory.
     * <p>
     * The transfer manager will use '/' as the path delimiter.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * DownloadDirectory myDownload = downloadDirectory(myBucket, myPrefix, Paths.get("/tmp");
     * myDowload.completionFuture().join();
     * }
     * </pre>
     *
     * @param bucket               The bucket.
     * @param prefix               The prefix.
     * @param destinationDirectory The directory where the objects will be downloaded to.
     */
    DownloadDirectory downloadDirectory(String bucket, String prefix, Path destinationDirectory);

    /**
     * Upload a file to S3.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload myUpload = tm.upload(myBucket, myKey, Paths.get("myFile.txt"));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(String bucket, String key, Path file) {
        return upload(UploadRequest.builder()
                        .uploadSpecification(UploadObjectSpecification.fromApiRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()))
                        .build(),
                file);
    }

    /**
     * Upload a file to S3 using the given presigned URL.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload myUpload = tm.upload(myPresignedUrl, Paths.get("myFile.txt"));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(URL presignedUrl, Path file) {
        throw new UnsupportedOperationException();
    }

    /**
     * Upload a file to S3.
     */
    Upload upload(UploadRequest request, Path file);

    /**
     * Resume a previously paused object upload.
     */
    Upload resumeUpload(UploadState uploadState);

    /**
     * Upload the given directory to the S3 bucket under the given prefix.
     *
     * @param bucket          The bucket.
     * @param prefix          The prefix.
     * @param sourceDirectory The directory containing the objects to be uploaded.
     */
    UploadDirectory uploadDirectory(String bucket, String prefix, Path sourceDirectory);

    /**
     * Create an {@code S3TransferManager} using the default values.
     */
    static S3TransferManager create() {
        return builder().build();
    }

    static S3TransferManager.Builder builder() {
        return DefaultS3TransferManager.builder();
    }

    interface Builder extends CopyableBuilder<Builder, S3TransferManager> {
        /**
         * The custom S3AsyncClient this transfer manager will use to make calls
         * to S3.
         */
        Builder s3client(S3AsyncClient s3Client);

        /**
         * The max number of requests the Transfer Manager will have at any
         * point in time. This must be less than or equal to the max concurrent
         * setting on the S3 client.
         */
        Builder maxConcurrency(Integer maxConcurrency);

        /**
         * The aggregate max upload rate in bytes per second over all active
         * upload transfers. The default is unlimited.
         */
        Builder maxUploadBytesPerSecond(Long maxUploadBytesPerSecond);

        /**
         * The aggregate max download rate in bytes per second over all active
         * download transfers. The default value is unlimited.
         */
        Builder maxDownloadBytesPerSecond(Long maxDownloadBytesPerSecond);

        /**
         * The multipart download configuration.
         */
        Builder multipartDownloadConfiguration(MultipartDownloadConfiguration multipartDownloadConfiguration);

        /**
         *  This is a convenience which creates an instance of the {@link MultipartDownloadConfiguration.Builder} avoiding
         * the need to create one manually via {@link MultipartDownloadConfiguration#builder()}
         */
        default Builder multipartDownloadConfiguration(Consumer<MultipartDownloadConfiguration.Builder> configuration) {
            return multipartDownloadConfiguration(MultipartDownloadConfiguration.builder()
                                                                                .applyMutation(configuration)
                                                                                .build());
        }

        /**
         * The multipart upload configuration.
         */
        Builder multipartUploadConfiguration(MultipartUploadConfiguration multipartUploadConfiguration);

        /**
         * The multipart upload configuration.
         */
        default Builder multipartUploadConfiguration(Consumer<MultipartUploadConfiguration.Builder> configuration) {
            return multipartUploadConfiguration(MultipartUploadConfiguration.builder()
                                                                            .applyMutation(configuration)
                                                                            .build());
        }

        /**
         * Add a progress listener to the currently configured list of
         * listeners.
         */
        Builder addProgressListener(TransferProgressListener progressListener);

        /**
         * Set the list of progress listeners.
         */
        Builder progressListeners(Collection<? extends TransferProgressListener> progressListeners);
    }
}
