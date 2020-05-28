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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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

public class CurrentState {
    private File audioFile = new File(getClass().getClassLoader().getResource("silence_16kHz_s16le.wav").getFile());

    @Test
    public void demoCurrentState() throws FileNotFoundException {
        try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create()) {
            // Create the audio stream for transcription - we have to create a publisher that resumes where it left off.
            // If we don't, we'll replay the whole thing again on a reconnect.
            Publisher<AudioStream> audioStream =
                    Bytes.from(new FileInputStream(audioFile))
                         .map(SdkBytes::fromByteArray)
                         .map(bytes -> AudioEvent.builder().audioChunk(bytes).build())
                         .cast(AudioStream.class);

            CompletableFuture<Void> result = printAudio(client, audioStream, null, 3);
            result.join();
        }
    }

    private CompletableFuture<Void> printAudio(TranscribeStreamingAsyncClient client,
                                               Publisher<AudioStream> audioStream,
                                               String sessionId,
                                               int resumesRemaining) {
        if (resumesRemaining == 0) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalStateException("Failed to resume audio, because the maximum resumes " +
                                                                   "have been exceeded."));
            return result;
        }

        // Create the request for transcribe that includes the audio metadata
        StartStreamTranscriptionRequest audioMetadata =
                StartStreamTranscriptionRequest.builder()
                                               .languageCode(LanguageCode.EN_US)
                                               .mediaEncoding(MediaEncoding.PCM)
                                               .mediaSampleRateHertz(16_000)
                                               .sessionId(sessionId)
                                               .build();

        // Create the transcription handler
        AtomicReference<String> atomicSessionId = new AtomicReference<>(sessionId);
        Consumer<TranscriptResultStream> reader = event -> {
            if (event instanceof TranscriptEvent) {
                TranscriptEvent transcriptEvent = (TranscriptEvent) event;
                System.out.println(transcriptEvent.transcript().results());
            }
        };

        StartStreamTranscriptionResponseHandler responseHandler =
                StartStreamTranscriptionResponseHandler.builder()
                                                       .onResponse(r -> atomicSessionId.set(r.sessionId()))
                                                       .subscriber(reader)
                                                       .build();

        // Start talking with transcribe
        return client.startStreamTranscription(audioMetadata, audioStream, responseHandler)
                     .handle((x, error) -> resumePrintAudio(client, audioStream, atomicSessionId.get(), resumesRemaining, error))
                     .thenCompose(flatten -> flatten);
    }

    private CompletableFuture<Void> resumePrintAudio(TranscribeStreamingAsyncClient client,
                                                     Publisher<AudioStream> audioStream,
                                                     String sessionId,
                                                     int resumesRemaining,
                                                     Throwable error) {
        if (error == null) {
            return CompletableFuture.completedFuture(null);
        }

        System.out.print("Error happened. Reconnecting and trying again...");
        error.printStackTrace();

        if (sessionId == null) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(error);
            return result;
        }

        // If we failed, recursively call printAudio
        return printAudio(client, audioStream, sessionId, resumesRemaining - 1);
    }
}
