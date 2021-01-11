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

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * Request object to download an object from S3 using the Transfer Manager.
 */
@SdkPublicApi
public final class DownloadRequest extends TransferObjectRequest {
    private final DownloadObjectSpecification downloadSpecification;
    private final Long size;

    private DownloadRequest(BuilderImpl builder) {
        super(builder);
        this.downloadSpecification = Validate.notNull(builder.downloadSpecification, "downloadSpecification must not be null");
        this.size = builder.size;
    }

    /**
     * @return The download specification.
     */
    public DownloadObjectSpecification downloadSpecification() {
        return downloadSpecification;
    }

    /**
     * @return The known size of the object to be downloaded. If multipart
     * downloads are enabled, this allows the Transfer Manager to omit a call
     * to S3 to get the object size.
     */
    public Optional<Long> size() {
        return Optional.ofNullable(size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DownloadRequest that = (DownloadRequest) o;
        return downloadSpecification.equals(that.downloadSpecification) &&
                Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(downloadSpecification);
        hashCode = 31 * hashCode + Objects.hashCode(size);
        return hashCode;
    }

    /**
     * @return A builder for this request.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    public static DownloadRequest forBucketAndKey(String bucket, String key) {
        return DownloadRequest.builder()
                .downloadSpecification(DownloadObjectSpecification.fromApiRequest(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()
                ))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends TransferObjectRequest.Builder {
        /**
         * Set the download specification.
         *
         * @param downloadSpecification The specification.
         * @return This object for method chaining.
         */
        Builder downloadSpecification(DownloadObjectSpecification downloadSpecification);

        /**
         * Optionally set the known size of the object to be downloaded. If
         * multipart downloads are enabled, this allows the Transfer Manager to
         * omit a call to S3 to get the object size.
         *
         * @param size The object size.
         * @return This object for method chaining.
         */
        Builder size(Long size);

        @Override
        Builder overrideConfiguration(TransferOverrideConfiguration config);

        @Override
        Builder progressListeners(Collection<TransferProgressListener> progressListeners);

        @Override
        Builder addProgressListener(TransferProgressListener progressListener);

        /**
         * @return The built request.
         */
        DownloadRequest build();
    }

    private static final class BuilderImpl extends TransferObjectRequest.BuilderImpl implements Builder {
        private DownloadObjectSpecification downloadSpecification;
        private Long size;

        private BuilderImpl(DownloadRequest other) {
            super(other);
            this.downloadSpecification = other.downloadSpecification;
            this.size = other.size;
        }

        private BuilderImpl() {
        }

        @Override
        public Builder downloadSpecification(DownloadObjectSpecification downloadSpecification) {
            this.downloadSpecification = downloadSpecification;
            return this;
        }

        @Override
        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        @Override
        public Builder overrideConfiguration(TransferOverrideConfiguration config) {
            super.overrideConfiguration(config);
            return this;
        }

        @Override
        public Builder progressListeners(Collection<TransferProgressListener> progressListeners) {
            super.progressListeners(progressListeners);
            return this;
        }

        @Override
        public Builder addProgressListener(TransferProgressListener progressListener) {
            super.addProgressListener(progressListener);
            return this;
        }

        @Override
        public DownloadRequest build() {
            return new DownloadRequest(this);
        }
    }
}
