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
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link HeadBucketRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignHeadBucket(HeadBucketPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class HeadBucketPresignRequest
    extends PresignRequest
    implements ToCopyableBuilder<HeadBucketPresignRequest.Builder, HeadBucketPresignRequest> {
    private final HeadBucketRequest headBucketRequest;

    private HeadBucketPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.headBucketRequest = Validate.notNull(builder.headBucketRequest, "headBucketRequest");
    }

    /**
     * Create a builder that can be used to create a {@link HeadBucketPresignRequest}.
     *
     * @see S3Presigner#presignHeadBucket(HeadBucketPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link HeadBucketRequest} that should be presigned.
     */
    public HeadBucketRequest headBucketRequest() {
        return headBucketRequest;
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

        HeadBucketPresignRequest that = (HeadBucketPresignRequest) o;

        return headBucketRequest.equals(that.headBucketRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + headBucketRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link HeadBucketPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
                                     CopyableBuilder<HeadBucketPresignRequest.Builder, HeadBucketPresignRequest> {
        /**
         * Configure the {@link HeadBucketRequest} that should be presigned.
         */
        Builder headBucketRequest(HeadBucketRequest headBucketRequest);

        /**
         * Configure the {@link HeadBucketRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #headBucketRequest(HeadBucketRequest)} without needing to invoke
         * {@code HeadBucketRequest.builder()} or {@code build()}.
         */
        default Builder headBucketRequest(Consumer<HeadBucketRequest.Builder> headBucketRequest) {
            HeadBucketRequest.Builder builder = HeadBucketRequest.builder();
            headBucketRequest.accept(builder);
            return headBucketRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        HeadBucketPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private HeadBucketRequest headBucketRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(HeadBucketPresignRequest request) {
            super(request);
            this.headBucketRequest = request.headBucketRequest;
        }

        @Override
        public Builder headBucketRequest(HeadBucketRequest headBucketRequest) {
            this.headBucketRequest = headBucketRequest;
            return this;
        }

        @Override
        public HeadBucketPresignRequest build() {
            return new HeadBucketPresignRequest(this);
        }
    }
}
