/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

/**
 * An example test class to show the usage of
 * {@link TranscribeStreamingAsyncClient#startStreamTranscription(StartStreamTranscriptionRequest, Publisher,
 * StartStreamTranscriptionResponseHandler)} API.
 *
 * The audio files used in this class don't have voice, so there won't be any transcripted text would be empty
 */
@Ignore
public class TranscribeStreamingIntegrationTest {

    private static TranscribeStreamingAsyncClient client;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        client = TranscribeStreamingAsyncClient.builder()
                                               .region(Region.US_EAST_1)
                                               .credentialsProvider(getCredentials())
                                               .build();
    }

    @Test
    public void testFileWith16kRate() throws ExecutionException, InterruptedException, URISyntaxException {
        CompletableFuture<Void> result = client.startStreamTranscription(getRequest(16_000),
                                                                         new AudioStreamPublisher(
                                                                             getInputStream("silence_16kHz_s16le.wav")),
                                                                         getResponseHandler());

        // Blocking call to keep the main thread for shutting down
        result.get();
    }

    @Test
    public void testFileWith8kRate() throws ExecutionException, InterruptedException, URISyntaxException {
        CompletableFuture<Void> result = client.startStreamTranscription(getRequest(8_000),
                                                                         new AudioStreamPublisher(
                                                                             getInputStream("silence_8kHz_s16le.wav")),
                                                                         getResponseHandler());

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

    private StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                                                      .onResponse(r -> {
                                                          String idFromHeader = r.sdkHttpResponse()
                                                                                 .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                                                                 .orElse(null);
                                                          System.out.println("Received Initial response: " + idFromHeader);
                                                      })
                                                      .onError(e -> {
                                                          System.out.println("Error message: " + e.getMessage());
                                                      })
                                                      .onComplete(() -> {
                                                          System.out.println("All records stream successfully");
                                                      })
                                                      .subscriber(event -> {
                                                          System.out.println(((TranscriptEvent) event).transcript().results());
                                                      })
                                                      .build();
    }

    private class AudioStreamPublisher implements Publisher<AudioStream> {
        private final InputStream inputStream;

        private AudioStreamPublisher(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new SubscriptionImpl(s, inputStream));
        }
    }

    private class SubscriptionImpl implements Subscription {
        private static final int CHUNK_SIZE_IN_BYTES = 1024 * 1;
        private ExecutorService executor = Executors.newFixedThreadPool(1);
        private AtomicLong demand = new AtomicLong(0);

        private final Subscriber<? super AudioStream> subscriber;
        private final InputStream inputStream;

        private SubscriptionImpl(Subscriber<? super AudioStream> s, InputStream inputStream) {
            this.subscriber = s;
            this.inputStream = inputStream;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("Demand must be positive"));
            }

            demand.getAndAdd(n);

            executor.submit(() -> {
                try {
                    do {
                        ByteBuffer audioBuffer = getNextEvent();
                        if (audioBuffer.remaining() > 0) {
                            AudioEvent audioEvent = audioEventFromBuffer(audioBuffer);
                            subscriber.onNext(audioEvent);
                        } else {
                            subscriber.onComplete();
                            break;
                        }
                    } while (demand.decrementAndGet() > 0);
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            });
        }

        @Override
        public void cancel() {

        }

        private ByteBuffer getNextEvent() {
            ByteBuffer audioBuffer = null;
            byte[] audioBytes = new byte[CHUNK_SIZE_IN_BYTES];

            int len = 0;
            try {
                len = inputStream.read(audioBytes);

                if (len <= 0) {
                    audioBuffer = ByteBuffer.allocate(0);
                } else {
                    audioBuffer = ByteBuffer.wrap(audioBytes, 0, len);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return audioBuffer;
        }

        private AudioEvent audioEventFromBuffer(ByteBuffer bb) {
            return AudioEvent.builder()
                             .audioChunk(SdkBytes.fromByteBuffer(bb))
                             .build();
        }
    }
}