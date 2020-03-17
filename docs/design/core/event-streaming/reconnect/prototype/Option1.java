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

import com.github.davidmoten.rx2.Bytes;
import io.reactivex.Flowable;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

/**
 * Option 1: Add a new method to hide: (1) the need for non-replayable publishers, (2) the reconnect boilerplate.
 */
public class Option1 {
    private File audioFile = new File(getClass().getClassLoader().getResource("silence_16kHz_s16le.wav").getFile());

    @Test
    public void option1() {
        try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create()) {
            // Create the request for transcribe that includes the audio metadata
            StartStreamTranscriptionRequest audioMetadata =
                    StartStreamTranscriptionRequest.builder()
                                                   .languageCode(LanguageCode.EN_US)
                                                   .mediaEncoding(MediaEncoding.PCM)
                                                   .mediaSampleRateHertz(16_000)
                                                   .build();

            // Create the audio stream for transcription
            Publisher<AudioStream> audioStream =
                    Bytes.from(audioFile)
                         .map(SdkBytes::fromByteArray)
                         .map(bytes -> AudioEvent.builder().audioChunk(bytes).build())
                         .cast(AudioStream.class);

            // Create the visitor that handles the transcriptions from transcribe
            Consumer<TranscriptResultStream> reader = event -> {
                if (event instanceof TranscriptEvent) {
                    TranscriptEvent transcriptEvent = (TranscriptEvent) event;
                    System.out.println(transcriptEvent.transcript().results());
                }
            };

            StartStreamTranscriptionResponseHandler responseHandler = StartStreamTranscriptionResponseHandler.builder()
                                                                                                             .subscriber(reader)
                                                                                                             .build();

            // Start talking with transcribe using a new auto-reconnect method (method name to be bikeshed)
            CompletableFuture<Void> result = client.startStreamTranscriptionWithAutoReconnect(audioMetadata,
                                                                                              audioStream,
                                                                                              responseHandler);
            result.join();
        }
    }

}
