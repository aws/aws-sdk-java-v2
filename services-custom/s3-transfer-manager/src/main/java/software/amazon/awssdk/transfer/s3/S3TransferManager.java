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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.internal.DefaultS3TransferManager;
import software.amazon.awssdk.utils.SdkAutoCloseable;

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
         * Build an instance of {@link S3TransferManager} based on the settings supplied to this builder
         *
         * @return an instance of {@link S3TransferManager}
         */
        S3TransferManager build();
    }
}
