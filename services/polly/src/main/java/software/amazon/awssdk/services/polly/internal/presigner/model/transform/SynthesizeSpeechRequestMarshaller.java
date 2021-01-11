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

package software.amazon.awssdk.services.polly.internal.presigner.model.transform;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;

/**
 * Marshaller for {@link SynthesizeSpeechRequest} that marshalls the request into a form that can be presigned.
 */
@SdkInternalApi
public final class SynthesizeSpeechRequestMarshaller {
    private static final SynthesizeSpeechRequestMarshaller INSTANCE = new SynthesizeSpeechRequestMarshaller();

    public SdkHttpFullRequest.Builder marshall(SynthesizeSpeechRequest synthesizeSpeechRequest) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .encodedPath("/v1/speech");

        if (synthesizeSpeechRequest.text() != null) {
            builder.putRawQueryParameter("Text", synthesizeSpeechRequest.text());
        }

        if (synthesizeSpeechRequest.textType() != null) {
            builder.putRawQueryParameter("TextType", synthesizeSpeechRequest.textTypeAsString());
        }

        if (synthesizeSpeechRequest.voiceId() != null) {
            builder.putRawQueryParameter("VoiceId", synthesizeSpeechRequest.voiceIdAsString());
        }

        if (synthesizeSpeechRequest.sampleRate() != null) {
            builder.putRawQueryParameter("SampleRate", synthesizeSpeechRequest.sampleRate());
        }

        if (synthesizeSpeechRequest.outputFormat() != null) {
            builder.putRawQueryParameter("OutputFormat", synthesizeSpeechRequest.outputFormatAsString());
        }

        if (synthesizeSpeechRequest.lexiconNames() != null) {
            builder.putRawQueryParameter("LexiconNames", synthesizeSpeechRequest.lexiconNames());
        }

        if (synthesizeSpeechRequest.speechMarkTypes() != null) {
            builder.putRawQueryParameter("SpeechMarkTypes", synthesizeSpeechRequest.speechMarkTypesAsStrings());
        }

        if (synthesizeSpeechRequest.languageCode() != null) {
            builder.putRawQueryParameter("LanguageCode", synthesizeSpeechRequest.languageCodeAsString());
        }

        if (synthesizeSpeechRequest.engine() != null) {
            builder.putRawQueryParameter("Engine", synthesizeSpeechRequest.engineAsString());
        }

        return builder;
    }

    public static SynthesizeSpeechRequestMarshaller getInstance() {
        return INSTANCE;
    }
}
