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
package software.amazon.awssdk.services.transcribestreaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;

/**
 * An example test class to show the usage of
 * {@link TranscribeStreamingAsyncClient#startStreamTranscription(StartStreamTranscriptionRequest, Publisher,
 * StartStreamTranscriptionResponseHandler)} API.
 *
 * The audio files used in this class don't have voice, so there won't be any transcripted text would be empty
 */
public class TranscribeStreamingIntegrationTest {

    private static TranscribeStreamingAsyncClient client;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        client = TranscribeStreamingAsyncClient.builder()
                                               .region(Region.US_EAST_1)
                                               .overrideConfiguration(b -> b.addExecutionInterceptor(new VerifyHeaderInterceptor()))
                                               .credentialsProvider(getCredentials())
                                               .build();
    }

    @Test
    public void testFileWith16kRate() throws ExecutionException, InterruptedException, URISyntaxException {
        CompletableFuture<Void> result = client.startStreamTranscription(getRequest(16_000),
                                                                         new AudioStreamPublisher(
                                                                             getInputStream("silence_16kHz_s16le.wav")),
                                                                         TestResponseHandlers.responseHandlerBuilder_Classic());

        // Blocking call to keep the main thread for shutting down
        result.get();
    }

    @Test
    public void testFileWith8kRate() throws ExecutionException, InterruptedException, URISyntaxException {
        CompletableFuture<Void> result = client.startStreamTranscription(getRequest(8_000),
                                                                         new AudioStreamPublisher(
                                                                             getInputStream("silence_8kHz_s16le.wav")),
                                                                         TestResponseHandlers.responseHandlerBuilder_Consumer());

        result.get();
    }

    private static AwsCredentialsProvider getCredentials() {
        return DefaultCredentialsProvider.create();
    }

    private StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz) {
        return StartStreamTranscriptionRequest.builder()
                                              .languageCode(LanguageCode.EN_US.toString())
                                              .mediaEncoding(MediaEncoding.PCM)
                                              .mediaSampleRateHertz(mediaSampleRateHertz)
                                              .build();
    }

    private InputStream getInputStream(String audioFileName) {
        try {
            File inputFile = new File(getClass().getClassLoader().getResource(audioFileName).getFile());
            assertTrue(inputFile.exists());
            InputStream audioStream = new FileInputStream(inputFile);
            return audioStream;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private class AudioStreamPublisher implements Publisher<AudioStream> {
        private final InputStream inputStream;

        private AudioStreamPublisher(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new TestSubscription(s, inputStream));
        }
    }

    private static class VerifyHeaderInterceptor implements ExecutionInterceptor {

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            List<String> contentTypeHeader = context.httpRequest().headers().get(CONTENT_TYPE);
            assertThat(contentTypeHeader.size()).isEqualTo(1);
            assertThat(contentTypeHeader.get(0)).isEqualTo(Mimetype.MIMETYPE_EVENT_STREAM);
        }
    }
}
