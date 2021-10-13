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
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a completed upload directory transfer to Amazon S3. It can be used to track
 * failed single file uploads.
 *
 * @see S3TransferManager#uploadDirectory(UploadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class CompletedUploadDirectory implements CompletedTransfer {
    private final Collection<FailedFileUpload> failedUploads;

    private CompletedUploadDirectory(DefaultBuilder builder) {
        this.failedUploads = Collections.unmodifiableCollection(Validate.paramNotNull(builder.failedUploads, "failedUploads"));
    }

    /**
     * An immutable collection of failed uploads with error details, request metadata about each file that is failed to
     * upload.
     *
     * <p>
     * Failed single file uploads can be retried by calling {@link S3TransferManager#upload(UploadRequest)}
     *
     * <pre>
     * {@code
     * // Retrying failed uploads if the exception is retryable
     * List<CompletableFuture<CompletedUpload>> futures =
     *     completedUploadDirectory.failedUploads()
     *                             .stream()
     *                             .filter(failedSingleFileUpload -> isRetryable(failedSingleFileUpload.exception()))
     *                             .map(failedSingleFileUpload ->
     *                                  tm.upload(failedSingleFileUpload.request()).completionFuture())
     *                             .collect(Collectors.toList());
     * CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
     * }
     * </pre>
     *
     * @return a list of failed uploads
     */
    public Collection<FailedFileUpload> failedUploads() {
        return failedUploads;
    }

    /**
     * Creates a default builder for {@link CompletedUploadDirectory}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompletedUploadDirectory that = (CompletedUploadDirectory) o;

        return failedUploads.equals(that.failedUploads);
    }

    @Override
    public int hashCode() {
        return failedUploads.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("CompletedUploadDirectory")
                       .add("failedUploads", failedUploads)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    public interface Builder {

        /**
         * Sets a collection of {@link FailedFileUpload}s
         *
         * @param failedUploads failed uploads
         * @return This builder for method chaining.
         */
        Builder failedUploads(Collection<FailedFileUpload> failedUploads);

        /**
         * Builds a {@link CompletedUploadDirectory} based on the properties supplied to this builder
         * @return An initialized {@link CompletedUploadDirectory}
         */
        CompletedUploadDirectory build();
    }

    private static final class DefaultBuilder implements Builder {
        private Collection<FailedFileUpload> failedUploads = Collections.emptyList();

        private DefaultBuilder() {
        }

        @Override
        public Builder failedUploads(Collection<FailedFileUpload> failedUploads) {
            this.failedUploads = failedUploads;
            return this;
        }

        public Collection<FailedFileUpload> getFailedUploads() {
            return failedUploads;
        }

        public void setFailedUploads(Collection<FailedFileUpload> failedUploads) {
            failedUploads(failedUploads);
        }

        @Override
        public CompletedUploadDirectory build() {
            return new CompletedUploadDirectory(this);
        }
    }
}
