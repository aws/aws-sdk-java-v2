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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.custom.s3.transfer.internal.DefaultS3TransferManager;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * The S3 Transfer Manager is a library that allows users to easily and
 * optimally upload and downloads to and from S3.
 * <p>
 * The list of features includes:
 * <ul>
 * <li>Parallel uploads and downloads</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 *
 * <pre>
 * {@code
 * // Create using all default configuration values
 * S3TransferManager tm = S3TranferManager.create();
 *
 * // TODO: update javadocs once we have more configuration
 * S3TransferManager tm = S3TransferManager.builder()
 *         .build();
 * }
 * </pre>
 */
@SdkPublicApi
public interface S3TransferManager extends SdkAutoCloseable {
    /**
     * Download an object identified by the bucket and key from S3 to the given
     * file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate the transfer
     * Download myDownload = tm.download(DownloadRequest.builder()
     *                                                  .bucket("mybucket")
     *                                                  .key("mykey")
     *                                                  .destination(Path.get("myFile.txt"))
     *                                                  .build());
     * // Wait for the transfer to complete
     * myDownload.completionFuture().join();
     * }
     * </pre>
     */
    default Download download(DownloadRequest downloadRequest) {
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
     * Download myDownload = tm.download(b -> b.bucket("mybucket")
     *                                         .key("mykey")
     *                                         .destination(Paths.get("myFile.txt")));
     * // Wait for the transfer to complete
     * myDownload.completionFuture().join();
     * }
     * </pre>
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
     * Upload myUpload = tm.upload(UploadRequest.bucket(myBucket)
     *                                          .key(myKey)
     *                                          .source(Paths.get("myFile.txt")));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(UploadRequest uploadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Upload a file to S3.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload myUpload = tm.upload(b -> b.bucket(myBucket)
     *                                   .key(myKey)
     *                                   .source(Paths.get("myFile.txt")));
     * myUpload.completionFuture().join();
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

    static S3TransferManager.Builder builder() {
        return DefaultS3TransferManager.builder();
    }

    interface Builder {

        Builder s3ClientConfiguration(S3ClientConfiguration configuration);

        default Builder s3ClientConfiguration(Consumer<S3ClientConfiguration.Builder> builderConsumer) {
            S3ClientConfiguration.Builder builder = S3ClientConfiguration.builder();
            builderConsumer.accept(builder);
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
