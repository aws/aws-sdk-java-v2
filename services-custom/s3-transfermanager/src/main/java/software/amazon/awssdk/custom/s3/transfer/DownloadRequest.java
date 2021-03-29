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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object to download an object from S3 using the Transfer Manager.
 */
@SdkPublicApi
public final class DownloadRequest implements TransferRequest, ToCopyableBuilder<DownloadRequest.Builder, DownloadRequest> {
    private final Path destination;
    private final GetObjectRequest getObjectRequest;

    private DownloadRequest(BuilderImpl builder) {
        Validate.isTrue((builder.bucket != null && builder.key != null) ^ builder.getObjectRequest != null,
                        "Exactly one of a bucket, key pair or API request must be provided.");
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.getObjectRequest = builder.getObjectRequest == null ? GetObjectRequest.builder()
                                                                                   .bucket(builder.bucket)
                                                                                   .key(builder.key)
                                                                                   .build() : builder.getObjectRequest;
    }

    /**
     * @return A builder for this request.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    @Override
    public String bucket() {
        return getObjectRequest.bucket();
    }

    @Override
    public String key() {
        return getObjectRequest.key();
    }

    /**
     * The {@link Path} to file that response contents will be written to. The file must not exist or this method
     * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *
     * @return the destination path
     */
    public Path destination() {
        return destination;
    }

    public GetObjectRequest toApiRequest() {
        return getObjectRequest;
    }

    public interface Builder extends TransferRequest.Builder<DownloadRequest, Builder>, CopyableBuilder<Builder,
        DownloadRequest> {

        /**
         * The {@link Path} to file that response contents will be written to. The file must not exist or this method
         * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
         *
         * @param destination the destination path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder destination(Path destination);


        /**
         * The {@link GetObjectRequest} request
         *
         * @param getObjectRequest the getObject request
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder apiRequest(GetObjectRequest getObjectRequest);

        /**
         * @return The built request.
         */
        DownloadRequest build();
    }

    private static final class BuilderImpl implements Builder {
        private String bucket;
        private String key;
        private Path destination;
        private GetObjectRequest getObjectRequest;

        private BuilderImpl() {
        }

        @Override
        public Builder destination(Path destination) {
            this.destination = destination;
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
        public Builder apiRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        @Override
        public DownloadRequest build() {
            return new DownloadRequest(this);
        }
    }
}
