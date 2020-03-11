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
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A pre-signed {@link UploadPartRequest} that can be executed at a later time without requiring additional signing or
 * authentication.
 *
 * @see S3Presigner#presignUploadPart(UploadPartPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public class PresignedUploadPartRequest
        extends PresignedRequest
        implements ToCopyableBuilder<PresignedUploadPartRequest.Builder, PresignedUploadPartRequest> {
    private PresignedUploadPartRequest(DefaultBuilder builder) {
        super(builder);
    }

    /**
     * Create a builder that can be used to create a {@link PresignedUploadPartRequest}.
     *
     * @see S3Presigner#presignUploadPart(UploadPartPresignRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * A builder for a {@link PresignedUploadPartRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends PresignedRequest.Builder,
                                     CopyableBuilder<PresignedUploadPartRequest.Builder, PresignedUploadPartRequest> {
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
        PresignedUploadPartRequest build();
    }

    @SdkInternalApi
    private static final class DefaultBuilder
            extends PresignedRequest.DefaultBuilder<DefaultBuilder>
            implements Builder {
        private DefaultBuilder() {
        }

        private DefaultBuilder(PresignedUploadPartRequest request) {
            super(request);
        }

        @Override
        public PresignedUploadPartRequest build() {
            return new PresignedUploadPartRequest(this);
        }
    }
}
