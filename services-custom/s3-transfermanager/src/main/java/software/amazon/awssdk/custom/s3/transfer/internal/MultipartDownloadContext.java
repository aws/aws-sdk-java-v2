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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.DownloadObjectSpecification;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;

/**
 * Context object for a multipart download.
 */
@SdkInternalApi
public final class MultipartDownloadContext {
    private DownloadRequest downloadRequest;
    private DownloadObjectSpecification partDownloadSpecification;
    private int partNumber;
    private long partOffset;
    private final long size;
    private boolean isLastPart;

    private MultipartDownloadContext(BuilderImpl builder) {
        this.downloadRequest = builder.downloadRequest;
        this.partDownloadSpecification = builder.partDownloadSpecification;
        this.partNumber = builder.partNumber;
        this.partOffset = builder.partOffset;
        this.size = builder.size;
        this.isLastPart = builder.isLastPart;
    }

    /**
     * @return The original download request given to the Transfer Manager.
     */
    public DownloadRequest downloadRequest() {
        return downloadRequest;
    }

    /**
     * @return The download specification used to get this part from S3.
     */
    public DownloadObjectSpecification partDownloadSpecification() {
        return partDownloadSpecification;
    }

    /**
     * @return The part number.
     */
    public int partNumber() {
        return partNumber;
    }

    /**
     * @return The offset from the beginning of the object where this part begins.
     */
    public long partOffset() {
        return partOffset;
    }

    /**
     * @return The size of the part requested.
     */
    public long size() {
        return size;
    }

    /**
     * @return Whether this is the last part of the object.
     */
    public boolean isLastPart() {
        return isLastPart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MultipartDownloadContext that = (MultipartDownloadContext) o;
        return partNumber == that.partNumber &&
                partOffset == that.partOffset &&
                size == that.size &&
                isLastPart == that.isLastPart &&
                Objects.equals(downloadRequest, that.downloadRequest) &&
                Objects.equals(partDownloadSpecification, that.partDownloadSpecification);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(downloadRequest);
        hashCode = 31 * hashCode + Objects.hashCode(partDownloadSpecification);
        hashCode = 31 * hashCode + Objects.hashCode(partNumber);
        hashCode = 31 * hashCode + Objects.hashCode(partOffset);
        hashCode = 31 * hashCode + Objects.hashCode(size);
        hashCode = 31 * hashCode + Objects.hashCode(isLastPart);
        return hashCode;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the original download request given to the Transfer Manager.
         *
         * @param downloadRequest The download request.
         * @return This object for method chaining.
         */
        Builder downloadRequest(DownloadRequest downloadRequest);

        /**
         * Set the download specification used to get this part from S3.
         *
         * @param partDownloadSpecification The download specification.
         * @return This object for method chaining.
         */
        Builder partDownloadSpecification(DownloadObjectSpecification partDownloadSpecification);

        /**
         * Set the part number.
         *
         * @param partNumber The part number.
         * @return This object for method chaining.
         */
        Builder partNumber(int partNumber);

        /**
         * Set the offset from the beginning of the object where this part
         * begins.
         *
         * @param partOffset The part offset.
         * @return This object for method chaining.
         */
        Builder partOffset(long partOffset);

        /**
         * Set the size of the part requested.
         *
         * @param size The size of the part.
         * @return This object for method chaining.
         */
        Builder size(long size);

        /**
         * Set whether this is the last part of the object.
         *
         * @param isLastPart Whether this is the last part.
         * @return This object for method chaining.
         */
        Builder isLastPart(boolean isLastPart);

        /**
         * @return The build context.
         */
        MultipartDownloadContext build();
    }

    private static class BuilderImpl implements Builder {
        private DownloadRequest downloadRequest;
        private DownloadObjectSpecification partDownloadSpecification;
        private int partNumber;
        private long partOffset;
        private long size;
        private boolean isLastPart;

        @Override
        public Builder downloadRequest(DownloadRequest downloadRequest) {
            this.downloadRequest = downloadRequest;
            return this;
        }

        @Override
        public Builder partDownloadSpecification(DownloadObjectSpecification partDownloadSpecification) {
            this.partDownloadSpecification = partDownloadSpecification;
            return this;
        }

        @Override
        public Builder partNumber(int partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        @Override
        public Builder partOffset(long partOffset) {
            this.partOffset = partOffset;
            return this;
        }

        @Override
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        @Override
        public Builder isLastPart(boolean isLastPart) {
            this.isLastPart = isLastPart;
            return this;
        }

        @Override
        public MultipartDownloadContext build() {
            return new MultipartDownloadContext(this);
        }
    }
}
