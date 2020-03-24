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
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link CompleteMultipartUploadRequest} so that it can be executed at a later time without requiring
 * additional signing or authentication.
 *
 * @see S3Presigner#presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class CompleteMultipartUploadPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<CompleteMultipartUploadPresignRequest.Builder, CompleteMultipartUploadPresignRequest> {
    private final CompleteMultipartUploadRequest completeMultipartUploadRequest;

    private CompleteMultipartUploadPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.completeMultipartUploadRequest = Validate.notNull(builder.completeMultipartUploadRequest,
                                                               "completeMultipartUploadRequest");
    }

    /**
     * Create a builder that can be used to create a {@link CompleteMultipartUploadPresignRequest}.
     *
     * @see S3Presigner#presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link CompleteMultipartUploadRequest} that should be presigned.
     */
    public CompleteMultipartUploadRequest completeMultipartUploadRequest() {
        return completeMultipartUploadRequest;
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

        CompleteMultipartUploadPresignRequest that = (CompleteMultipartUploadPresignRequest) o;

        return completeMultipartUploadRequest.equals(that.completeMultipartUploadRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + completeMultipartUploadRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link CompleteMultipartUploadPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder
        extends PresignRequest.Builder,
                CopyableBuilder<CompleteMultipartUploadPresignRequest.Builder, CompleteMultipartUploadPresignRequest> {
        /**
         * Configure the {@link CompleteMultipartUploadRequest} that should be presigned.
         */
        Builder completeMultipartUploadRequest(CompleteMultipartUploadRequest completeMultipartUploadRequest);

        /**
         * Configure the {@link CompleteMultipartUploadRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #completeMultipartUploadRequest(CompleteMultipartUploadRequest)}
         * without needing to invoke {@code CompleteMultipartUploadRequest.builder()} or {@code build()}.
         */
        default Builder completeMultipartUploadRequest(
            Consumer<CompleteMultipartUploadRequest.Builder> completeMultipartUploadRequest) {
            CompleteMultipartUploadRequest.Builder builder = CompleteMultipartUploadRequest.builder();
            completeMultipartUploadRequest.accept(builder);
            return completeMultipartUploadRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        CompleteMultipartUploadPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private CompleteMultipartUploadRequest completeMultipartUploadRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CompleteMultipartUploadPresignRequest request) {
            super(request);
            this.completeMultipartUploadRequest = request.completeMultipartUploadRequest;
        }

        @Override
        public Builder completeMultipartUploadRequest(CompleteMultipartUploadRequest completeMultipartUploadRequest) {
            this.completeMultipartUploadRequest = completeMultipartUploadRequest;
            return this;
        }

        @Override
        public CompleteMultipartUploadPresignRequest build() {
            return new CompleteMultipartUploadPresignRequest(this);
        }
    }
}
