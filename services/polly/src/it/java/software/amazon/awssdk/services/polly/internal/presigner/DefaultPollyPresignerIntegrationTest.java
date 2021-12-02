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

package software.amazon.awssdk.services.polly.internal.presigner;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import javax.net.ssl.HttpsURLConnection;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.VoiceId;
import software.amazon.awssdk.services.polly.presigner.PollyPresigner;
import software.amazon.awssdk.services.polly.presigner.model.SynthesizeSpeechPresignRequest;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

/**
 * Integration tests for {@link DefaultPollyPresigner}.
 */
public class DefaultPollyPresignerIntegrationTest extends AwsIntegrationTestBase {

    private static PollyPresigner presigner;

    @BeforeClass
    public static void setup() {
        presigner = DefaultPollyPresigner.builder()
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Test
    public void presign_requestIsValid() throws IOException {
        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                .text("hello world!")
                .outputFormat(OutputFormat.PCM)
                .voiceId(VoiceId.SALLI)
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(45))
                .synthesizeSpeechRequest(request)
                .build();

        URL presignedUrl = presigner.presignSynthesizeSpeech(presignRequest).url();

        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = ((HttpsURLConnection) presignedUrl.openConnection());
            urlConnection.connect();
            assertThat(urlConnection.getResponseCode()).isEqualTo(200);
            assertThat(urlConnection.getHeaderField("Content-Type")).isEqualTo("audio/pcm");
        } finally {
            urlConnection.getInputStream().close();
        }
    }

}
