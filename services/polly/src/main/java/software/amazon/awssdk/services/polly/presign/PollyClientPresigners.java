/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.polly.presign;

import static java.time.ZoneOffset.UTC;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.SdkClock;
import software.amazon.awssdk.auth.presign.PresignerFacade;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Presigning extensions methods for {@link software.amazon.awssdk.services.polly.PollyClient}.
 */
public final class PollyClientPresigners {

    private static final int SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES = 15;

    private final URI endpoint;
    private final PresignerFacade presignerFacade;
    private final SdkClock clock;

    @SdkInternalApi
    public PollyClientPresigners(PresignerParams presignerParams) {
        this.endpoint = presignerParams.endpoint();
        this.presignerFacade = new PresignerFacade(presignerParams);
        this.clock = presignerParams.clock();
    }

    /**
     * Presign a {@link SynthesizeSpeechRequest} to be vended to consumers. The expiration time of the presigned URL is
     * {@value #SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} from generation time.
     */
    @ReviewBeforeRelease("Refactor as part of siging changes.")
    public URL getPresignedSynthesizeSpeechUrl(SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest) {
        SdkHttpFullRequest.Builder request =
                SdkHttpFullRequest.builder()
                                  .protocol(endpoint.getScheme())
                                  .host(endpoint.getHost())
                                  .port(endpoint.getPort())
                                  .encodedPath(SdkHttpUtils.appendUri(endpoint.getPath(), "/v1/speech"))
                                  .method(SdkHttpMethod.GET);
        marshallIntoRequest(synthesizeSpeechPresignRequest, request);
        Date expirationDate = synthesizeSpeechPresignRequest.getExpirationDate() == null ?
                              getDefaultExpirationDate() : synthesizeSpeechPresignRequest.getExpirationDate();
        return presignerFacade.presign(synthesizeSpeechPresignRequest, request.build(), RequestConfig.empty(), expirationDate);
    }

    private void marshallIntoRequest(SynthesizeSpeechPresignRequest synthesizeSpeechRequest, SdkHttpFullRequest.Builder request) {
        if (synthesizeSpeechRequest.getText() != null) {
            request.rawQueryParameter("Text", synthesizeSpeechRequest.getText());
        }

        if (synthesizeSpeechRequest.getTextType() != null) {
            request.rawQueryParameter("TextType", synthesizeSpeechRequest.getTextType());
        }

        if (synthesizeSpeechRequest.getVoiceId() != null) {
            request.rawQueryParameter("VoiceId", synthesizeSpeechRequest.getVoiceId());
        }

        if (synthesizeSpeechRequest.getSampleRate() != null) {
            request.rawQueryParameter("SampleRate", synthesizeSpeechRequest.getSampleRate());
        }

        if (synthesizeSpeechRequest.getOutputFormat() != null) {
            request.rawQueryParameter("OutputFormat", synthesizeSpeechRequest.getOutputFormat());
        }

        if (synthesizeSpeechRequest.getLexiconNames() != null) {
            request.rawQueryParameter("LexiconNames", synthesizeSpeechRequest.getLexiconNames());
        }

        if (synthesizeSpeechRequest.getSpeechMarkTypes() != null) {
            request.rawQueryParameter("SpeechMarkTypes", synthesizeSpeechRequest.getSpeechMarkTypes());
        }
    }

    private Date getDefaultExpirationDate() {
        return new Date(ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.currentTimeMillis()), UTC)
                        .plusMinutes(SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES)
                        .toInstant().toEpochMilli());
    }

}
