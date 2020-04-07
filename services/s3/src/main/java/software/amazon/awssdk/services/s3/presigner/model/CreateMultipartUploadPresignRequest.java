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
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link CreateMultipartUploadRequest} so that it can be executed at a later time without requiring
 * additional signing or authentication.
 *
 * @see S3Presigner#presignCreateMultipartUpload(CreateMultipartUploadPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class CreateMultipartUploadPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<CreateMultipartUploadPresignRequest.Builder, CreateMultipartUploadPresignRequest> {
    private final CreateMultipartUploadRequest createMultipartUploadRequest;

    private CreateMultipartUploadPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.createMultipartUploadRequest = Validate.notNull(builder.createMultipartUploadRequest,
                                                             "createMultipartUploadRequest");
    }

    /**
     * Create a builder that can be used to create a {@link CreateMultipartUploadPresignRequest}.
     *
     * @see S3Presigner#presignCreateMultipartUpload(CreateMultipartUploadPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link CreateMultipartUploadRequest} that should be presigned.
     */
    public CreateMultipartUploadRequest createMultipartUploadRequest() {
        return createMultipartUploadRequest;
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

        CreateMultipartUploadPresignRequest that = (CreateMultipartUploadPresignRequest) o;

        return createMultipartUploadRequest.equals(that.createMultipartUploadRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + createMultipartUploadRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link CreateMultipartUploadPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder
        extends PresignRequest.Builder,
                CopyableBuilder<CreateMultipartUploadPresignRequest.Builder, CreateMultipartUploadPresignRequest> {
        /**
         * Configure the {@link CreateMultipartUploadRequest} that should be presigned.
         */
        Builder createMultipartUploadRequest(CreateMultipartUploadRequest createMultipartUploadRequest);

        /**
         * Configure the {@link CreateMultipartUploadRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #createMultipartUploadRequest(CreateMultipartUploadRequest)}
         * without needing to invoke {@code CreateMultipartUploadRequest.builder()} or {@code build()}.
         */
        default Builder createMultipartUploadRequest(
            Consumer<CreateMultipartUploadRequest.Builder> createMultipartUploadRequest) {
            CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder();
            createMultipartUploadRequest.accept(builder);
            return createMultipartUploadRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        CreateMultipartUploadPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private CreateMultipartUploadRequest createMultipartUploadRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CreateMultipartUploadPresignRequest request) {
            super(request);
            this.createMultipartUploadRequest = request.createMultipartUploadRequest;
        }

        @Override
        public Builder createMultipartUploadRequest(CreateMultipartUploadRequest createMultipartUploadRequest) {
            this.createMultipartUploadRequest = createMultipartUploadRequest;
            return this;
        }

        @Override
        public CreateMultipartUploadPresignRequest build() {
            return new CreateMultipartUploadPresignRequest(this);
        }
    }
}
