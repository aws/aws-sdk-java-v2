package software.amazon.awssdk.services.transcribestreaming;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

public class TestResponseHandlers {

    private static final Logger log = LoggerFactory.getLogger(TestResponseHandlers.class);

    /**
     * A simple consumer of events to subscribe
     */
    public static StartStreamTranscriptionResponseHandler responseHandlerBuilder_Consumer() {
        return StartStreamTranscriptionResponseHandler.builder()
                                                      .onResponse(r -> {
                                                          String idFromHeader = r.sdkHttpResponse()
                                                                                 .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                                                                 .orElse(null);
                                                          log.debug("Received Initial response: " + idFromHeader);
                                                      })
                                                      .onError(e -> {
                                                          log.error("Error message: " + e.getMessage(), e);
                                                      })
                                                      .onComplete(() -> {
                                                          log.debug("All records stream successfully");
                                                      })
                                                      .subscriber(event -> {
                                                          // Do nothing
                                                      })
                                                      .build();
    }


    /**
     * A classic way by implementing the interface and using helper method in {@link SdkPublisher}.
     */
    public static StartStreamTranscriptionResponseHandler responseHandlerBuilder_Classic() {
        return new StartStreamTranscriptionResponseHandler() {
            @Override
            public void responseReceived(StartStreamTranscriptionResponse response) {
                String idFromHeader = response.sdkHttpResponse()
                                              .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                              .orElse(null);
                log.debug("Received Initial response: " + idFromHeader);
            }

            @Override
            public void onEventStream(SdkPublisher<TranscriptResultStream> publisher) {
                publisher
                    // Filter to only SubscribeToShardEvents
                    .filter(TranscriptEvent.class)
                    // Flat map into a publisher of just records
                    // Using
                    .flatMapIterable(event -> event.transcript().results())
                    // TODO limit is broken. After limit is reached, app fails with SdkCancellationException
                    // instead of gracefully exiting. Limit to 1000 total records
                    //.limit(5)
                    // Batch records into lists of 25
                    .buffer(25)
                    // Print out each record batch
                    // You won't see any data printed as the audio files we use have no voice
                    .subscribe(batch ->  {
                        log.debug("Record Batch - " + batch);
                    });
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                log.error("Error message: " + throwable.getMessage(), throwable);
            }

            @Override
            public void complete() {
                log.debug("All records stream successfully");
            }
        };
    }
}
