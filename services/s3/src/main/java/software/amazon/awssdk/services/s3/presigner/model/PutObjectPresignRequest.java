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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link PutObjectRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignPutObject(PutObjectPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class PutObjectPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<PutObjectPresignRequest.Builder, PutObjectPresignRequest> {
    private final PutObjectRequest putObjectRequest;

    private PutObjectPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.putObjectRequest = Validate.notNull(builder.putObjectRequest, "putObjectRequest");
    }

    /**
     * Create a builder that can be used to create a {@link PutObjectPresignRequest}.
     *
     * @see S3Presigner#presignPutObject(PutObjectPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link PutObjectRequest} that should be presigned.
     */
    public PutObjectRequest putObjectRequest() {
        return putObjectRequest;
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

        PutObjectPresignRequest that = (PutObjectPresignRequest) o;

        return putObjectRequest.equals(that.putObjectRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + putObjectRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link PutObjectPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
                                     CopyableBuilder<PutObjectPresignRequest.Builder, PutObjectPresignRequest> {
        /**
         * Configure the {@link PutObjectRequest} that should be presigned.
         */
        Builder putObjectRequest(PutObjectRequest putObjectRequest);

        /**
         * Configure the {@link PutObjectRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #putObjectRequest(PutObjectRequest)} without needing to invoke
         * {@code PutObjectRequest.builder()} or {@code build()}.
         */
        default Builder putObjectRequest(Consumer<PutObjectRequest.Builder> putObjectRequest) {
            PutObjectRequest.Builder builder = PutObjectRequest.builder();
            putObjectRequest.accept(builder);
            return putObjectRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        PutObjectPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private PutObjectRequest putObjectRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(PutObjectPresignRequest request) {
            super(request);
            this.putObjectRequest = request.putObjectRequest;
        }

        @Override
        public Builder putObjectRequest(PutObjectRequest putObjectRequest) {
            this.putObjectRequest = putObjectRequest;
            return this;
        }

        @Override
        public PutObjectPresignRequest build() {
            return new PutObjectPresignRequest(this);
        }
    }
}
