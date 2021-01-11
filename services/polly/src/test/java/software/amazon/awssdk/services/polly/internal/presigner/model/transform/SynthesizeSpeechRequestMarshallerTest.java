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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;

public class SynthesizeSpeechRequestMarshallerTest {
    private static Map<String, Object> properties;

    @BeforeClass
    public static void setup() {
        properties = new HashMap<>();

        properties.put("Engine", "engine");
        properties.put("LanguageCode", "languageCode");
        properties.put("LexiconNames", Arrays.asList("Lexicon1", "Lexicon2", "Lexicon3"));
        properties.put("OutputFormat", "outputFormat");
        properties.put("SampleRate", "sampleRate");
        properties.put("SpeechMarkTypes", Arrays.asList("SpeechMark1", "SpeechMark2"));
        properties.put("Text", "text");
        properties.put("TextType", "textType");
        properties.put("VoiceId", "voiceId");
    }

    // Test to ensure that we are testing with all the known properties for
    // this request. If this test fails, it means Polly added a new field to
    // SynthesizeSpeechRequest and the marshaller and/or properties map above
    // wasn't updated.
    @Test
    public void marshall_allPropertiesAccountedFor() {
        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder().build();
        request.sdkFields().forEach(f -> assertThat(properties.containsKey(f.locationName())).isTrue());
    }

    @Test
    public void marshall_allPropertiesMarshalled() {
        SynthesizeSpeechRequest.Builder builder = setAllProperties(SynthesizeSpeechRequest.builder());

        SdkHttpFullRequest.Builder marshalled = SynthesizeSpeechRequestMarshaller.getInstance().marshall(builder.build());

        Map<String, List<String>> queryParams = marshalled.rawQueryParameters();

        properties.keySet().forEach(k -> {
            Object expected = properties.get(k);
            List<String> actual = queryParams.get(k);
            if (expected instanceof List) {
                assertThat(actual).isEqualTo(expected);
            } else {
                assertThat(actual).containsExactly((String) expected);
            }
        });

        assertThat(marshalled.contentStreamProvider()).isNull();
    }

    @Test
    public void marshall_correctPath() {
        SdkHttpFullRequest.Builder marshalled = SynthesizeSpeechRequestMarshaller.getInstance()
                .marshall(SynthesizeSpeechRequest.builder().build());
        assertThat(marshalled.encodedPath()).isEqualTo("/v1/speech");
    }

    @Test
    public void marshall_correctMethod() {
        SdkHttpFullRequest.Builder marshalled = SynthesizeSpeechRequestMarshaller.getInstance()
                .marshall(SynthesizeSpeechRequest.builder().build());
        assertThat(marshalled.method()).isEqualTo(SdkHttpMethod.GET);
    }

    private SynthesizeSpeechRequest.Builder setAllProperties(SynthesizeSpeechRequest.Builder builder) {
        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder().build();
        List<SdkField<?>> sdkFields = request.sdkFields();
        sdkFields.forEach(f -> f.set(builder, properties.get(f.locationName())));
        return builder;
    }
}
