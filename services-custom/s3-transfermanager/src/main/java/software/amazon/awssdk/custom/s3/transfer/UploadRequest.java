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

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Upload an object to S3 using {@link S3TransferManager}.
 */
@SdkPublicApi
public final class UploadRequest implements TransferRequest, ToCopyableBuilder<UploadRequest.Builder, UploadRequest> {
    private final String bucket;
    private final String key;
    private final Path source;

    private UploadRequest(BuilderImpl builder) {
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.source = builder.source;
    }

    @Override
    public String bucket() {
        return bucket;
    }

    @Override
    public String key() {
        return key;
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

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }


    public interface Builder extends TransferRequest.Builder, CopyableBuilder<Builder, UploadRequest> {

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
         * @return The built request.
         */
        UploadRequest build();
    }

    private static class BuilderImpl implements Builder {
        private String bucket;
        private String key;
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
        public UploadRequest build() {
            return new UploadRequest(this);
        }
    }
}
