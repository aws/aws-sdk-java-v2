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

package software.amazon.awssdk.stability.tests.transcribestreaming;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.stability.tests.utils.TestEventStreamingResponseHandler;
import software.amazon.awssdk.stability.tests.utils.TestTranscribeStreamingSubscription;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

public class TranscribeStreamingStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(TranscribeStreamingStabilityTest.class.getSimpleName());
    public static final int CONCURRENCY = 2;
    public static final int TOTAL_RUNS = 1;
    private static TranscribeStreamingAsyncClient transcribeStreamingClient;
    private static InputStream audioFileInputStream;

    @BeforeAll
    public static void setup() {
        transcribeStreamingClient = TranscribeStreamingAsyncClient.builder()
                                                                  .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                                  .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                                            .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                                                                                            .maxConcurrency(CONCURRENCY))
                                                                  .build();

        audioFileInputStream = getInputStream();

        if (audioFileInputStream == null) {
            throw new RuntimeException("fail to get the audio input stream");
        }
    }

    @AfterAll
    public static void tearDown() {
        transcribeStreamingClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void startTranscription() {
        IntFunction<CompletableFuture<?>> futureIntFunction = i ->
            transcribeStreamingClient.startStreamTranscription(b -> b.mediaSampleRateHertz(8_000)
                                                                     .languageCode(LanguageCode.EN_US)
                                                                     .mediaEncoding(MediaEncoding.PCM),
                                                               new AudioStreamPublisher(),
                                                               new TestStartStreamTranscriptionResponseHandler());
        StabilityTestRunner.newRunner()
                           .futureFactory(futureIntFunction)
                           .totalRuns(TOTAL_RUNS)
                           .requestCountPerRun(CONCURRENCY)
                           .testName("TranscribeStreamingStabilityTest.startTranscription")
                           .run();
    }

    private static InputStream getInputStream() {
        return TranscribeStreamingStabilityTest.class.getResourceAsStream("silence_8kHz.wav");
    }

    private class AudioStreamPublisher implements Publisher<AudioStream> {

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new TestTranscribeStreamingSubscription(s, audioFileInputStream));
        }
    }

    private static class TestStartStreamTranscriptionResponseHandler extends TestEventStreamingResponseHandler<StartStreamTranscriptionResponse, TranscriptResultStream>
        implements StartStreamTranscriptionResponseHandler {

        @Override
        public void onEventStream(SdkPublisher<TranscriptResultStream> publisher) {
            publisher
                .filter(TranscriptEvent.class)
                .subscribe(result -> log.debug(() -> "Record Batch - " + result.transcript().results()));
        }
    }
}
