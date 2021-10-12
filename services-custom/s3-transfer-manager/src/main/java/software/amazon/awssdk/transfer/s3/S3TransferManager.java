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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.internal.DefaultS3TransferManager;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * The S3 Transfer Manager is a library that allows users to easily and
 * optimally upload and downloads to and from S3.
 *
 * <b>Usage Example:</b>
 *
 * <pre>
 * {@code
 * // Create using all default configuration values
 * S3TransferManager transferManager = S3TransferManager.create();
 *
 * // If you wish to configure settings, we recommend using the builder instead:
 * S3TransferManager transferManager =
 *                  S3TransferManager.builder()
 *                                   .s3ClientConfiguration(b -> b.credentialsProvider(credentialProvider)
 *                                   .region(Region.US_WEST_2)
 *                                   .targetThroughputInGbps(20.0)
 *                                   .minimumPartSizeInBytes(10 * MB))
 *                                   .build();
 *
 * // Download an S3 object to a file
 * Download download =
 *     transferManager.download(b -> b.destination(Paths.get("myFile.txt"))
 *                                    .getObjectRequest(r -> r.bucket("bucket")
 *                                                            .key("key")));
 * download.completionFuture().join();
 *
 * // Upload a file to S3
 * Upload upload = transferManager.upload(b -> b.source(Paths.get("myFile.txt"))
 *                                              .putObjectRequest(r -> r.bucket("bucket")
 *                                                                      .key("key")));
 *
 * upload.completionFuture().join();
 * }
 * </pre>
 */
@SdkPublicApi
@SdkPreviewApi
public interface S3TransferManager extends SdkAutoCloseable {
    /**
     * Download an object identified by the bucket and key from S3 to the given
     * file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate the transfer
     * Download download =
     *     transferManager.download(DownloadRequest.builder()
     *                                             .destination(Paths.get("myFile.txt"))
     *                                             .getObjectRequest(GetObjectRequest.builder()
     *                                                                               .bucket("bucket")
     *                                                                               .key("key")
     *                                                                               .build())
     *                                             .build());
     * // Wait for the transfer to complete
     * download.completionFuture().join();
     * }
     * </pre>
     * @see #download(Consumer)
     */
    default Download download(DownloadRequest downloadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Download an object identified by the bucket and key from S3 to the given
     * file.
     *
     * <p>
     * This is a convenience method that creates an instance of the {@link DownloadRequest} builder avoiding the
     * need to create one manually via {@link DownloadRequest#builder()}.
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate the transfer
     * Download download =
     *     transferManager.download(b -> b.destination(Paths.get("myFile.txt"))
     *                                    .getObjectRequest(r -> r.bucket("bucket")
     *                                                            .key("key")));
     * // Wait for the transfer to complete
     * download.completionFuture().join();
     * }
     * </pre>
     * @see #download(DownloadRequest)
     */
    default Download download(Consumer<DownloadRequest.Builder> request) {
        return download(DownloadRequest.builder().applyMutation(request).build());
    }

    /**
     * Upload a file to S3.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload upload =
     *     transferManager.upload(UploadRequest.builder()
     *                                        .source(Paths.get("myFile.txt"))
     *                                        .putObjectRequest(PutObjectRequest.builder()
     *                                                                          .bucket("bucket")
     *                                                                          .key("key")
     *                                                                          .build())
     *                                        .build());
     * // Wait for the transfer to complete
     * upload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(UploadRequest uploadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Upload a file to S3.
     *
     * <p>
     * This is a convenience method that creates an instance of the {@link UploadRequest} builder avoiding the
     * need to create one manually via {@link UploadRequest#builder()}.
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload upload =
     *       transferManager.upload(b -> b.putObjectRequest(req -> req.bucket("bucket")
     *                                                                .key("key"))
     *                                    .source(Paths.get("myFile.txt")));
     * // Wait for the transfer to complete
     * upload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(Consumer<UploadRequest.Builder> request) {
        return upload(UploadRequest.builder().applyMutation(request).build());
    }

    /**
     * Upload all files under the given directory to the provided S3 bucket. The key name transformation depends on the optional
     * prefix and delimiter provided in the {@link UploadDirectoryRequest}. By default, all subdirectories will be uploaded
     * recursively, and symbolic links are not followed automatically. This behavior can be configured in
     * {@link UploadDirectoryOverrideConfiguration}
     * at request level via {@link UploadDirectoryRequest.Builder#overrideConfiguration(UploadDirectoryOverrideConfiguration)} or
     * client level via {@link S3TransferManager.Builder#transferConfiguration(S3TransferManagerOverrideConfiguration)} Note
     * that request-level configuration takes precedence over client-level configuration.
     *
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * source directory provided does not exist for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed uploads in the successfully completed response. As a result,
     * you should check for errors in the response via {@link CompletedUploadDirectory#failedUploads()}
     * even when the future completes successfully.
     *
     * <p>
     * The current user must have read access to all directories and files
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * UploadDirectoryTransfer uploadDirectory =
     *       transferManager.uploadDirectory(UploadDirectoryRequest.builder()
     *                                                             .sourceDirectory(Paths.get("."))
     *                                                             .bucket("bucket")
     *                                                             .prefix("prefix")
     *                                                             .build());
     * // Wait for the transfer to complete
     * CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().join();
     *
     * // Print out the failed uploads
     * completedUploadDirectory.failedUploads().forEach(System.out::println);
     *
     * }
     * </pre>
     *
     * @param uploadDirectoryRequest the upload directory request
     * @see #uploadDirectory(Consumer)
     * @see UploadDirectoryOverrideConfiguration
     */
    default UploadDirectoryTransfer uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Upload all files under the given directory to the provided S3 bucket. The key name transformation depends on the optional
     * prefix and delimiter provided in the {@link UploadDirectoryRequest}. By default, all subdirectories will be uploaded
     * recursively, and symbolic links are not followed automatically. This behavior can be configured in
     * {@link UploadDirectoryOverrideConfiguration}
     * at request level via {@link UploadDirectoryRequest.Builder#overrideConfiguration(UploadDirectoryOverrideConfiguration)} or
     * client level via {@link S3TransferManager.Builder#transferConfiguration(S3TransferManagerOverrideConfiguration)} Note
     * that request-level configuration takes precedence over client-level configuration.
     *
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * source directory provided does not exist for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed uploads in the successfully completed response. As a result,
     * you should check for errors in the response via {@link CompletedUploadDirectory#failedUploads()}
     * even when the future completes successfully.
     *
     * <p>
     * The current user must have read access to all directories and files
     *
     * <p>
     * This is a convenience method that creates an instance of the {@link UploadDirectoryRequest} builder avoiding the
     * need to create one manually via {@link UploadDirectoryRequest#builder()}.
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * UploadDirectoryTransfer uploadDirectory =
     *       transferManager.uploadDirectory(b -> b.sourceDirectory(Paths.get("."))
     *                                             .bucket("key")
     *                                             .prefix("prefix"));
     * // Print out the failed uploads
     * completedUploadDirectory.failedUploads().forEach(System.out::println);
     *
     * }
     * </pre>
     * @param requestBuilder the upload directory request builder
     * @see #uploadDirectory(UploadDirectoryRequest)
     * @see UploadDirectoryOverrideConfiguration
     */
    default UploadDirectoryTransfer uploadDirectory(Consumer<UploadDirectoryRequest.Builder> requestBuilder) {
        Validate.paramNotNull(requestBuilder, "requestBuilder");
        return uploadDirectory(UploadDirectoryRequest.builder().applyMutation(requestBuilder).build());
    }

    /**
     * Create an {@code S3TransferManager} using the default values.
     */
    static S3TransferManager create() {
        return builder().build();
    }

    /**
     * Creates a default builder for {@link S3TransferManager}.
     */
    static S3TransferManager.Builder builder() {
        return DefaultS3TransferManager.builder();
    }

    /**
     * The builder definition for a {@link S3TransferManager}.
     */
    interface Builder {

        /**
         * Configuration values for the low level S3 client. The {@link S3TransferManager} already provides sensible
         * defaults. All values are optional.
         *
         * @param configuration the configuration to use
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #s3ClientConfiguration(Consumer)
         */
        Builder s3ClientConfiguration(S3ClientConfiguration configuration);

        /**
         * Configuration values for the low level S3 client. The {@link S3TransferManager} already provides sensible
         * defaults. All values are optional.
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link S3ClientConfiguration} builder avoiding the
         * need to create one manually via {@link S3ClientConfiguration#builder()}.
         *
         * @param configuration the configuration to use
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #s3ClientConfiguration(S3ClientConfiguration)
         */
        default Builder s3ClientConfiguration(Consumer<S3ClientConfiguration.Builder> configuration) {
            S3ClientConfiguration.Builder builder = S3ClientConfiguration.builder();
            configuration.accept(builder);
            s3ClientConfiguration(builder.build());
            return this;
        }

        /**
         * Configuration settings for how {@link S3TransferManager} should process the request. The
         * {@link S3TransferManager} already provides sensible defaults. All values are optional.
         *
         * @param transferConfiguration the configuration to use
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #transferConfiguration(Consumer)
         */
        Builder transferConfiguration(S3TransferManagerOverrideConfiguration transferConfiguration);

        /**
         * Configuration settings for how {@link S3TransferManager} should process the request. The
         * {@link S3TransferManager} already provides sensible defaults. All values are optional.
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link S3TransferManagerOverrideConfiguration} builder
         * avoiding the need to create one manually via {@link S3TransferManagerOverrideConfiguration#builder()}.
         *
         * @param configuration the configuration to use
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #transferConfiguration(S3TransferManagerOverrideConfiguration)
         */
        default Builder transferConfiguration(Consumer<S3TransferManagerOverrideConfiguration.Builder> configuration) {
            Validate.paramNotNull(configuration, "configuration");
            S3TransferManagerOverrideConfiguration.Builder builder = S3TransferManagerOverrideConfiguration.builder();
            configuration.accept(builder);
            transferConfiguration(builder.build());
            return this;
        }

        /**
         * Build an instance of {@link S3TransferManager} based on the settings supplied to this builder
         *
         * @return an instance of {@link S3TransferManager}
         */
        S3TransferManager build();
    }
}
