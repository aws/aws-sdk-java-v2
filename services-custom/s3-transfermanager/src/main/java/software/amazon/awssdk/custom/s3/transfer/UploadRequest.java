/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Upload an object to S3 using {@link S3TransferManager}.
 */
@SdkPublicApi
public final class UploadRequest extends TransferRequest {
    private final UploadObjectSpecification uploadSpecification;
    private final Long size;

    private UploadRequest(BuilderImpl builder) {
        super(builder);
        this.uploadSpecification = builder.uploadSpecification;
        this.size = builder.size;
    }

    /**
     * @return The upload specification.
     */
    public UploadObjectSpecification uploadSpecification() {
        return uploadSpecification;
    }

    /**
     * @return The size of the object to be uploaded.
     */
    public Long size() {
        return size;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UploadRequest that = (UploadRequest) o;
        return Objects.equals(uploadSpecification, that.uploadSpecification) &&
                Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = hashCode * 31 + Objects.hashCode(uploadSpecification);
        hashCode = hashCode * 31 + Objects.hashCode(size);
        return hashCode;
    }

    @Override
    public TransferRequest.Builder toBuilder() {
        return null;
    }

    public interface Builder extends TransferRequest.Builder {

        /**
         * Set the upload specification.
         *
         * @param uploadSpecification The upload specification.
         * @return This object for method chaining.
         */
        Builder uploadSpecification(UploadObjectSpecification uploadSpecification);

        /**
         * Set the size of the object to be uploaded.
         *
         * @param size The object size.
         * @return This object for method chaining.
         */
        Builder size(Long size);

        @Override
        Builder overrideConfiguration(TransferOverrideConfiguration overrideConfiguration);

        @Override
        Builder progressListeners(Collection<TransferProgressListener> progressListeners);

        @Override
        Builder addProgressListener(TransferProgressListener progressListener);

        UploadRequest build();
    }

    private static class BuilderImpl extends TransferRequest.BuilderImpl implements Builder {
        private UploadObjectSpecification uploadSpecification;
        private Long size;

        @Override
        public Builder uploadSpecification(UploadObjectSpecification uploadSpecification) {
            this.uploadSpecification = uploadSpecification;
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
        public UploadRequest build() {
            return new UploadRequest(this);
        }
    }
}
