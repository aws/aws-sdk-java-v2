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

package software.amazon.awssdk.services.s3.presigner.model;

import java.time.Duration;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link UploadPartRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignUploadPart(UploadPartPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class UploadPartPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<UploadPartPresignRequest.Builder, UploadPartPresignRequest> {
    private final UploadPartRequest uploadPartRequest;

    private UploadPartPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.uploadPartRequest = Validate.notNull(builder.uploadPartRequest, "uploadPartRequest");
    }

    /**
     * Create a builder that can be used to create a {@link UploadPartPresignRequest}.
     *
     * @see S3Presigner#presignUploadPart(UploadPartPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link UploadPartRequest} that should be presigned.
     */
    public UploadPartRequest uploadPartRequest() {
        return uploadPartRequest;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        UploadPartPresignRequest that = (UploadPartPresignRequest) o;

        return uploadPartRequest.equals(that.uploadPartRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + uploadPartRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link UploadPartPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
                                     CopyableBuilder<UploadPartPresignRequest.Builder, UploadPartPresignRequest> {
        /**
         * Configure the {@link UploadPartRequest} that should be presigned.
         */
        Builder uploadPartRequest(UploadPartRequest uploadPartRequest);

        /**
         * Configure the {@link UploadPartRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #uploadPartRequest(UploadPartRequest)} without needing to invoke
         * {@code UploadPartRequest.builder()} or {@code build()}.
         */
        default Builder uploadPartRequest(Consumer<UploadPartRequest.Builder> uploadPartRequest) {
            UploadPartRequest.Builder builder = UploadPartRequest.builder();
            uploadPartRequest.accept(builder);
            return uploadPartRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        UploadPartPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private UploadPartRequest uploadPartRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(UploadPartPresignRequest request) {
            super(request);
            this.uploadPartRequest = request.uploadPartRequest;
        }

        @Override
        public Builder uploadPartRequest(UploadPartRequest uploadPartRequest) {
            this.uploadPartRequest = uploadPartRequest;
            return this;
        }

        @Override
        public UploadPartPresignRequest build() {
            return new UploadPartPresignRequest(this);
        }
    }
}
