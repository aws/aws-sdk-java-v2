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
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.stability.tests.utils.TestEventStreamingResponseHandler;
import software.amazon.awssdk.stability.tests.utils.TestTranscribeStreamingSubscription;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

/**
 * Base class for Transcribe Streaming stability tests.
 */
public abstract class TranscribeStreamingBaseStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(TranscribeStreamingBaseStabilityTest.class);
    protected static final int CONCURRENCY = 2;
    protected static final int TOTAL_RUNS = 1;

    protected static InputStream audioFileInputStream;

    protected static InputStream getInputStream() {
        return TranscribeStreamingBaseStabilityTest.class.getResourceAsStream("silence_8kHz.wav");
    }

    protected void runTranscriptionTest(TranscribeStreamingAsyncClient client, String testName) {
        IntFunction<CompletableFuture<?>> futureIntFunction = i ->
            client.startStreamTranscription(b -> b.mediaSampleRateHertz(8_000)
                                                  .languageCode(LanguageCode.EN_US)
                                                  .mediaEncoding(MediaEncoding.PCM),
                                            new AudioStreamPublisher(),
                                            new TestResponseHandler());
        StabilityTestRunner.newRunner()
                           .futureFactory(futureIntFunction)
                           .totalRuns(TOTAL_RUNS)
                           .requestCountPerRun(CONCURRENCY)
                           .testName(testName)
                           .run();
    }

    protected static class AudioStreamPublisher implements Publisher<AudioStream> {
        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new TestTranscribeStreamingSubscription(s, audioFileInputStream));
        }
    }

    protected static class TestResponseHandler
        extends TestEventStreamingResponseHandler<StartStreamTranscriptionResponse, TranscriptResultStream>
        implements StartStreamTranscriptionResponseHandler {

        @Override
        public void onEventStream(SdkPublisher<TranscriptResultStream> publisher) {
            publisher.filter(TranscriptEvent.class)
                     .subscribe(result -> log.debug(() -> "Record Batch - " + result.transcript().results()));
        }
    }
}
