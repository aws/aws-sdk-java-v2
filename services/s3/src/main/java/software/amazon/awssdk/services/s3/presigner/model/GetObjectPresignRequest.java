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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link GetObjectRequest} so that it can be executed at a later time without requiring additional
 * signing or authentication.
 *
 * @see S3Presigner#presignGetObject(GetObjectPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class GetObjectPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<GetObjectPresignRequest.Builder, GetObjectPresignRequest> {
    private final GetObjectRequest getObjectRequest;

    private GetObjectPresignRequest(DefaultBuilder builder) {
        super(builder);
        this.getObjectRequest = Validate.notNull(builder.getObjectRequest, "getObjectRequest");
    }

    /**
     * Create a builder that can be used to create a {@link GetObjectPresignRequest}.
     *
     * @see S3Presigner#presignGetObject(GetObjectPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Retrieve the {@link GetObjectRequest} that should be presigned.
     */
    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
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

        GetObjectPresignRequest that = (GetObjectPresignRequest) o;

        return getObjectRequest.equals(that.getObjectRequest);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getObjectRequest.hashCode();
        return result;
    }

    /**
     * A builder for a {@link GetObjectPresignRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignRequest.Builder,
                                     CopyableBuilder<GetObjectPresignRequest.Builder, GetObjectPresignRequest> {
        /**
         * Configure the {@link GetObjectRequest} that should be presigned.
         */
        Builder getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * Configure the {@link GetObjectRequest} that should be presigned.
         * <p/>
         * This is a convenience method for invoking {@link #getObjectRequest(GetObjectRequest)} without needing to invoke
         * {@code GetObjectRequest.builder()} or {@code build()}.
         */
        default Builder getObjectRequest(Consumer<GetObjectRequest.Builder> getObjectRequest) {
            GetObjectRequest.Builder builder = GetObjectRequest.builder();
            getObjectRequest.accept(builder);
            return getObjectRequest(builder.build());
        }

        @Override
        Builder signatureDuration(Duration signatureDuration);

        @Override
        GetObjectPresignRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder extends PresignRequest.DefaultBuilder<DefaultBuilder> implements Builder {
        private GetObjectRequest getObjectRequest;

        private DefaultBuilder() {
        }

        private DefaultBuilder(GetObjectPresignRequest request) {
            super(request);
            this.getObjectRequest = request.getObjectRequest;
        }

        @Override
        public Builder getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        @Override
        public GetObjectPresignRequest build() {
            return new GetObjectPresignRequest(this);
        }
    }
}
