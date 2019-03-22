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

package software.amazon.awssdk.custom.s3.transfer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * The context object for the upload of an object part to S3.
 */
@SdkInternalApi
public final class MultipartUploadContext {
    private final UploadRequest uploadRequest;
    private final CreateMultipartUploadRequest createMultipartRequest;
    private final UploadPartRequest uploadPartRequest;
    private final long partOffset;

    private MultipartUploadContext(BuilderImpl builder) {
        this.uploadRequest = builder.uploadRequest;
        this.createMultipartRequest = builder.createMultipartRequest;
        this.uploadPartRequest = builder.uploadPartRequest;
        this.partOffset = builder.partOffset;
    }

    /**
     * The original upload request given to the transfer manager.
     */
    public UploadRequest uploadRequest() {
        return uploadRequest;
    }

    /**
     * The request sent to S3 to initiate the multipart upload.
     */
    public CreateMultipartUploadRequest createMultipartRequest() {
        return createMultipartRequest;
    }

    /**
     * The upload request to be sent to S3 for this part.
     */
    public UploadPartRequest uploadPartRequest() {
        return uploadPartRequest;
    }

    /**
     * The offset from the beginning of the object where this part begins.
     */
    public long partOffset() {
        return partOffset;
    }

    public interface Builder {
        /**
         * Set the original upload request given to the transfer manager.
         *
         * @param uploadRequest The upload request.
         * @return This object for method chaining.
         */
        Builder uploadRequest(UploadRequest uploadRequest);

        /**
         * Set the request sent to S3 to initiate the multipart upload.
         *
         * @param createMultipartRequest The request.
         * @return This object for method chaining.
         */
        Builder createMultipartRequest(CreateMultipartUploadRequest createMultipartRequest);

        /**
         * Set the upload request to be sent to S3 for this part.
         *
         * @param uploadPartRequest The request.
         * @return This object for method chaining.
         */
        Builder uploadPartRequest(UploadPartRequest uploadPartRequest);

        /**
         * Set the offset from the beginning of the object where this part
         * begins.
         *
         * @param partOffset The offset.
         * @return This object for method chaining.
         */
        Builder partOffset(long partOffset);

        /**
         * @return the built context.
         */
        MultipartUploadContext build();
    }

    private static final class BuilderImpl implements Builder {
        private UploadRequest uploadRequest;
        private CreateMultipartUploadRequest createMultipartRequest;
        private UploadPartRequest uploadPartRequest;
        private long partOffset;

        @Override
        public Builder uploadRequest(UploadRequest uploadRequest) {
            this.uploadRequest = uploadRequest;
            return this;
        }

        @Override
        public Builder createMultipartRequest(CreateMultipartUploadRequest createMultipartRequest) {
            this.createMultipartRequest = createMultipartRequest;
            return this;
        }

        @Override
        public Builder uploadPartRequest(UploadPartRequest uploadPartRequest) {
            this.uploadPartRequest = uploadPartRequest;
            return this;
        }

        @Override
        public Builder partOffset(long partOffset) {
            this.partOffset = partOffset;
            return this;
        }

        @Override
        public MultipartUploadContext build() {
            return new MultipartUploadContext(this);
        }
    }
}
