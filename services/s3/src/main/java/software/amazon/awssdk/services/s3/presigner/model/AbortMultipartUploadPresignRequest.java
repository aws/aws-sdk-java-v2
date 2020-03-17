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
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link AbortMultipartUploadRequest} so that it can be executed at a later time without requiring
 * additional signing or authentication.
 *
 * @see S3Presigner#presignAbortMultipartUpload(AbortMultipartUploadPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class AbortMultipartUploadPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<AbortMultipartUploadPresignRequest.Builder, AbortMultipartUploadPresignRequest> {
    private final AbortMultipartUploadRequest abortMultipartUploadRequest;

    private AbortMultipartUploadPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.abortMultipartUploadRequest = Validate.notNull(builder.abortMultipartUploadRequest, "abortMultipartUploadRequest");
    }

    /**
     * Create a builder that can be used to create a {@link AbortMultipartUploadPresignRequest}.
     *
     * @see S3Presigner#presignAbortMultipartUpload(AbortMultipartUploadPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link AbortMultipartUploadRequest} that should be presigned.
     */
    public AbortMultipartUploadRequest abortMultipartUploadRequest() {
        return abortMultipartUploadRequest;
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

        AbortMultipartUploadPresignRequest that = (AbortMultipartUploadPresignRequest) o;

        return abortMultipartUploadRequest.equals(that.abortMultipartUploadRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + abortMultipartUploadRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link AbortMultipartUploadPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder
        extends PresignRequest.Builder,
                CopyableBuilder<AbortMultipartUploadPresignRequest.Builder, AbortMultipartUploadPresignRequest> {
        /**
         * Configure the {@link AbortMultipartUploadRequest} that should be presigned.
         */
        Builder abortMultipartUploadRequest(AbortMultipartUploadRequest abortMultipartUploadRequest);

        /**
         * Configure the {@link AbortMultipartUploadRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #abortMultipartUploadRequest(AbortMultipartUploadRequest)}
         * without needing to invoke {@code AbortMultipartUploadRequest.builder()} or {@code build()}.
         */
        default Builder abortMultipartUploadRequest(Consumer<AbortMultipartUploadRequest.Builder> abortMultipartUploadRequest) {
            AbortMultipartUploadRequest.Builder builder = AbortMultipartUploadRequest.builder();
            abortMultipartUploadRequest.accept(builder);
            return abortMultipartUploadRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        AbortMultipartUploadPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private AbortMultipartUploadRequest abortMultipartUploadRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(AbortMultipartUploadPresignRequest request) {
            super(request);
            this.abortMultipartUploadRequest = request.abortMultipartUploadRequest;
        }

        @Override
        public Builder abortMultipartUploadRequest(AbortMultipartUploadRequest abortMultipartUploadRequest) {
            this.abortMultipartUploadRequest = abortMultipartUploadRequest;
            return this;
        }

        @Override
        public AbortMultipartUploadPresignRequest build() {
            return new AbortMultipartUploadPresignRequest(this);
        }
    }
}
