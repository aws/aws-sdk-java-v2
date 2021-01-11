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

import java.time.Duration;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.presigner.PresignRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A request to pre-sign a {@link software.amazon.awssdk.services.polly.presigner.PollyPresigner} so that it can be
 * executed at a later time without requiring additional signing or authentication.
 *
 * @see software.amazon.awssdk.services.polly.presigner.PollyPresigner#presignSynthesizeSpeech(SynthesizeSpeechPresignRequest)
 * @see #builder()
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class SynthesizeSpeechPresignRequest
        extends PresignRequest
        implements ToCopyableBuilder<SynthesizeSpeechPresignRequest.Builder, SynthesizeSpeechPresignRequest> {

    private final SynthesizeSpeechRequest synthesizeSpeechRequest;

    private SynthesizeSpeechPresignRequest(BuilderImpl builder) {
        super(builder);
        this.synthesizeSpeechRequest = builder.synthesizeSpeechRequest;
    }

    public SynthesizeSpeechRequest synthesizeSpeechRequest() {
        return synthesizeSpeechRequest;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends PresignRequest.Builder,
            CopyableBuilder<SynthesizeSpeechPresignRequest.Builder, SynthesizeSpeechPresignRequest> {

        Builder synthesizeSpeechRequest(SynthesizeSpeechRequest synthesizeSpeechRequest);

        Builder signatureDuration(Duration signatureDuration);

        /**
         * Build the presigned request, based on the configuration on this builder.
         */
        SynthesizeSpeechPresignRequest build();
    }

    private static class BuilderImpl extends PresignRequest.DefaultBuilder<BuilderImpl> implements Builder {
        private SynthesizeSpeechRequest synthesizeSpeechRequest;

        BuilderImpl() {
        }

        private BuilderImpl(SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest) {
            super(synthesizeSpeechPresignRequest);
            this.synthesizeSpeechRequest = synthesizeSpeechPresignRequest.synthesizeSpeechRequest();
        }

        @Override
        public Builder synthesizeSpeechRequest(SynthesizeSpeechRequest synthesizeSpeechRequest) {
            this.synthesizeSpeechRequest = synthesizeSpeechRequest;
            return this;
        }

        @Override
        public SynthesizeSpeechPresignRequest build() {
            return new SynthesizeSpeechPresignRequest(this);
        }
    }
}
