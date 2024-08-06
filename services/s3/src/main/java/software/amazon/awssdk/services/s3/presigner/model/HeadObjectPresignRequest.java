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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link HeadObjectRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignHeadObject(HeadObjectPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class HeadObjectPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<HeadObjectPresignRequest.Builder, HeadObjectPresignRequest> {
    private final HeadObjectRequest headObjectRequest;

    private HeadObjectPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.headObjectRequest = Validate.notNull(builder.headObjectRequest, "headObjectRequest");
    }

    /**
     * Create a builder that can be used to create a {@link HeadObjectPresignRequest}.
     *
     * @see S3Presigner#presignHeadObject(HeadObjectPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link HeadObjectRequest} that should be presigned.
     */
    public HeadObjectRequest headObjectRequest() {
        return headObjectRequest;
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

        HeadObjectPresignRequest that = (HeadObjectPresignRequest) o;

        return headObjectRequest.equals(that.headObjectRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + headObjectRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link HeadObjectPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
            CopyableBuilder<HeadObjectPresignRequest.Builder, HeadObjectPresignRequest> {
        /**
         * Configure the {@link HeadObjectRequest} that should be presigned.
         */
        Builder headObjectRequest(HeadObjectRequest headObjectRequest);

        /**
         * Configure the {@link HeadObjectRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #headObjectRequest(HeadObjectRequest)} without needing to invoke
         * {@code HeadObjectRequest.builder()} or {@code build()}.
         */
        default Builder headObjectRequest(Consumer<HeadObjectRequest.Builder> headObjectRequest) {
            HeadObjectRequest.Builder builder = HeadObjectRequest.builder();
            headObjectRequest.accept(builder);
            return headObjectRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        HeadObjectPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private HeadObjectRequest headObjectRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(HeadObjectPresignRequest request) {
            super(request);
            this.headObjectRequest = request.headObjectRequest;
        }

        @Override
        public Builder headObjectRequest(HeadObjectRequest headObjectRequest) {
            this.headObjectRequest = headObjectRequest;
            return this;
        }

        @Override
        public HeadObjectPresignRequest build() {
            return new HeadObjectPresignRequest(this);
        }
    }
}
