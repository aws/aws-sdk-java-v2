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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A pre-signed a {@link PutObjectRequest} that can be executed at a later time without requiring additional signing or
 * authentication.
 *
 * @see S3Presigner#presignPutObject(PutObjectPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public class PresignedPutObjectRequest
        extends PresignedRequest
        implements ToCopyableBuilder<PresignedPutObjectRequest.Builder, PresignedPutObjectRequest> {
    private PresignedPutObjectRequest(DefaultBuilder builder) {
        super(builder);
    }

    /**
     * Create a builder that can be used to create a {@link PresignedPutObjectRequest}.
     *
     * @see S3Presigner#presignPutObject(PutObjectPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * A builder for a {@link PresignedPutObjectRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignedRequest.Builder,
                                     CopyableBuilder<PresignedPutObjectRequest.Builder, PresignedPutObjectRequest> {
        @Override
        Builder expiration(Instant expiration);

        @Override
        Builder isBrowserExecutable(Boolean isBrowserExecutable);

        @Override
        Builder signedHeaders(Map<String, List<String>> signedHeaders);

        @Override
        Builder signedPayload(SdkBytes signedPayload);

        @Override
        Builder httpRequest(SdkHttpRequest httpRequest);

        @Override
        PresignedPutObjectRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder
            extends PresignedRequest.DefaultBuilder<DefaultBuilder>
            implements Builder {
        private DefaultBuilder() {
        }

        private DefaultBuilder(PresignedPutObjectRequest request) {
            super(request);
        }

        @Override
        public PresignedPutObjectRequest build() {
            return new PresignedPutObjectRequest(this);
        }
    }
}
