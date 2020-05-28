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

package software.amazon.awssdk.services.polly.presigner.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A pre-signed a {@link SynthesizeSpeechPresignRequest} that can be executed at a later time without requiring
 * additional signing or authentication.
 *
 * @see software.amazon.awssdk.services.polly.presigner.PollyPresigner#presignSynthesizeSpeech(SynthesizeSpeechPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class PresignedSynthesizeSpeechRequest
        extends PresignedRequest
        implements ToCopyableBuilder<PresignedSynthesizeSpeechRequest.Builder, PresignedSynthesizeSpeechRequest> {


    private PresignedSynthesizeSpeechRequest(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends PresignedRequest.Builder,
            CopyableBuilder<Builder, PresignedSynthesizeSpeechRequest> {

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
        PresignedSynthesizeSpeechRequest build();
    }

    private static class BuilderImpl extends PresignedRequest.DefaultBuilder<BuilderImpl> implements Builder {

        BuilderImpl() {
        }

        BuilderImpl(PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest) {
            super(presignedSynthesizeSpeechRequest);
        }

        @Override
        public PresignedSynthesizeSpeechRequest build() {
            return new PresignedSynthesizeSpeechRequest(this);
        }
    }
}
