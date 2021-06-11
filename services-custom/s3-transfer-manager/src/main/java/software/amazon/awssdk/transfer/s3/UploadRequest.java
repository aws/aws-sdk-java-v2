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

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Upload an object to S3 using {@link S3TransferManager}.
 */
@SdkPublicApi
public final class UploadRequest implements TransferRequest, ToCopyableBuilder<UploadRequest.Builder, UploadRequest> {
    private final PutObjectRequest putObjectRequest;
    private final Path source;

    private UploadRequest(BuilderImpl builder) {
        Validate.isTrue(bucketKeyPairProvided(builder) ^ apiRequestProvided(builder),
                "Exactly one of a bucket, key pair or API request must be provided.");

        if (bucketKeyPairProvided(builder)) {
            this.putObjectRequest = PutObjectRequest.builder()
                                                    .bucket(builder.bucket)
                                                    .key(builder.key)
                                                    .build();
        } else {
            putObjectRequest = builder.putObjectRequest;
        }
        this.source = builder.source;
    }

    @Override
    public String bucket() {
        return putObjectRequest.bucket();
    }

    @Override
    public String key() {
        return putObjectRequest.key();
    }

    public PutObjectRequest toPutObjectRequest() {
        return putObjectRequest;
    }

    /**
     * The {@link Path} to file containing data to send to the service.
     *
     * @return the source path
     */
    public Path source() {
        return source;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    private static boolean bucketKeyPairProvided(BuilderImpl builder) {
        return builder.bucket != null && builder.key != null;
    }

    private static boolean apiRequestProvided(BuilderImpl builder) {
        return builder.putObjectRequest != null;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }


    public interface Builder extends TransferRequest.Builder<UploadRequest, Builder>, CopyableBuilder<Builder, UploadRequest> {

        /**
         * The {@link Path} to file containing data to send to the service. File will be read entirely and may be read
         * multiple times in the event of a retry. If the file does not exist or the current user does not have
         * access to read it then an exception will be thrown.
         *
         * @param source the source path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder source(Path source);

        /**
         * Configure the {@link PutObjectRequest} that should be used for the upload
         *
         * @param putObjectRequest the putObjectRequest
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder putObjectRequest(PutObjectRequest putObjectRequest);

        /**
         * @return The built request.
         */
        UploadRequest build();
    }

    private static class BuilderImpl implements Builder {
        private String bucket;
        private String key;
        private PutObjectRequest putObjectRequest;
        private Path source;

        @Override
        public Builder source(Path source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        @Override
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        @Override
        public Builder putObjectRequest(PutObjectRequest putObjectRequest) {
            this.putObjectRequest = putObjectRequest;
            return this;
        }

        @Override
        public UploadRequest build() {
            return new UploadRequest(this);
        }
    }
}
