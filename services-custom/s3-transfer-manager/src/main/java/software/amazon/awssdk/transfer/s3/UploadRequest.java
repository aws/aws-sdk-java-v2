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

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
        this.putObjectRequest = paramNotNull(builder.putObjectRequest, "putObjectRequest");
        this.source = paramNotNull(builder.source, "source");
    }

    /**
     * @return The {@link PutObjectRequest} request that should be used for the upload
     */
    public PutObjectRequest putObjectRequest() {
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

    /**
     * Create a builder that can be used to create a {@link UploadRequest}.
     *
     * @see S3TransferManager#upload(UploadRequest)
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
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

        if (!Objects.equals(putObjectRequest, that.putObjectRequest)) {
            return false;
        }
        return Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        int result = putObjectRequest != null ? putObjectRequest.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }

    /**
     * A builder for a {@link UploadRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
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
         * Configure the {@link PutObjectRequest} that should be used for the upload
         *
         * @param putObjectRequestBuilder the putObjectRequest consumer builder
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        default Builder putObjectRequest(Consumer<PutObjectRequest.Builder> putObjectRequestBuilder) {
            return putObjectRequest(PutObjectRequest.builder()
                                                    .applyMutation(putObjectRequestBuilder)
                                                    .build());
        }

        /**
         * @return The built request.
         */
        @Override
        UploadRequest build();
    }

    private static class BuilderImpl implements Builder {
        private PutObjectRequest putObjectRequest;
        private Path source;

        @Override
        public Builder source(Path source) {
            this.source = source;
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
