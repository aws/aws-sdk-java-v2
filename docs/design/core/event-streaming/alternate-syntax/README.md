**Design:** New Feature, **Status:** [Proposed](../../../README.md)

# Event Stream Alternate Syntax

Event streaming allows long-running bi-directional communication between
customers and AWS services over HTTP/2 connections.

The current syntax for event streaming APIs is adequate for power users,
but has a few disadvantages:

1. Customers must use reactive streams APIs, even for relatively simple
   use-cases. Reactive streams APIs are powerful, but difficult to use
   without external documentation and libraries. 
2. All response processing must be performed in a callback (the
   `ResponseHandler` abstraction), which makes it challenging to
   propagate information to the rest of the application.

This mini-proposal suggests an alternate syntax that customers would be
able to use for all event streaming operations.

## Proposal

A new method will be added to each event streaming operation: 
`Running{OPERATION} {OPERATION}({OPERATION}Request)` (and its
consumer-builder variant).

A new type will be created for each event streaming operation:
`Running{OPERATION}`:

```Java
interface Running{OPERATION} extends AutoCloseable {
    // A future that is completed when the entire operation completes.
    CompletableFuture<Void> completionFuture();

    /**
     * Methods enabling reading individual events asynchronously, as they are received.
     */
    
    CompletableFuture<Void> readAll(Consumer<{RESPONSE_EVENT_TYPE}> reader);
    CompletableFuture<Void> readAll({RESPONSE_EVENT_TYPE}Visitor responseVisitor);
    <T extends {RESPONSE_EVENT_TYPE}> CompletableFuture<Void> readAll(Class<T> type, Consumer<T> reader);

    CompletableFuture<Optional<{REQUEST_EVENT_TYPE}>> readNext();
    <T extends {RESPONSE_EVENT_TYPE}> CompletableFuture<Optional<T>> readNext(Class<T> type);

    /**
     * Methods enabling writing individual events asynchronously.
     */

    CompletableFuture<Void> writeAll(Publisher<? extends {REQUEST_EVENT_TYPE}> events);
    CompletableFuture<Void> writeAll(Iterable<? extends {REQUEST_EVENT_TYPE}> events);
    CompletableFuture<Void> write({REQUEST_EVENT_TYPE} event);

    /**
     * Reactive-streams methods for reading events and response messages, as they are received.
     */
    Publisher<{RESPONSE_EVENT_TYPE}> responseEventPublisher();
    Publisher<{OPERATION}Response> responsePublisher();

    /**
     * Java-8-streams methods for reading events and response messages, as they are received.
     */
     
    Stream<{RESPONSE_EVENT_TYPE}> blockingResponseEventStream();
    Stream<{OPERATION}Response> blockingResponseStream();

    @Override
    default void close() {
        completionFuture().cancel(false);
    }
}
```

This type enables customers to use the operation in either a
reactive-streams or a Java-8 usage pattern, depending on how they care
to manage their threads and back-pressure.

It's worth noting that every method on `Running{OPERATION}` is still
non-blocking and will never throw exceptions directly. Any method that
returns a type that itself contains blocking methods is prefixed with
`blocking`, e.g. `Stream<{RESPONSE_EVENT_TYPE}> 
blockingResponseEventStream()`.

**Example 1: Transcribe's `startStreamTranscription` with Reactive
Streams**

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create();
     // Create the connection to transcribe and send the initial request message
     RunningStartStreamTranscription transcription =
            client.startStreamTranscription(r -> r.languageCode(LanguageCode.EN_US)
                                                  .mediaEncoding(MediaEncoding.PCM)
                                                  .mediaSampleRateHertz(16_000))) {

    // Use RxJava to create the audio stream to be transcribed
    Publisher<AudioStream> audioPublisher =
            Bytes.from(audioFile)
                 .map(SdkBytes::fromByteArray)
                 .map(bytes -> AudioEvent.builder().audioChunk(bytes).build())
                 .cast(AudioStream.class);

    // Begin sending the audio data to transcribe, asynchronously
    transcription.writeAll(audioPublisher);

    // Get a publisher for the transcription
    Publisher<TranscriptResultStream> transcriptionPublisher = transcription.responseEventPublisher();

    // Use RxJava to log the transcription
    Flowable.fromPublisher(transcriptionPublisher)
            .filter(e -> e instanceof TranscriptEvent)
            .cast(TranscriptEvent.class)
            .forEach(e -> System.out.println(e.transcript().results()));

    // Wait for the operation to complete
    transcription.completionFuture().join();
}
```

**Example 2: Transcribe's `startStreamTranscription` without Reactive
Streams**

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create();
    // Create the connection to transcribe and send the initial request message
    RunningStartStreamTranscription transcription =
            client.startStreamTranscription(r -> r.languageCode(LanguageCode.EN_US)
                                                  .mediaEncoding(MediaEncoding.PCM)
                                                  .mediaSampleRateHertz(16_000))) {
    
    // Asynchronously log response transcription events, as we receive them
    transcription.readAll(TranscriptEvent.class, e -> System.out.println(e.transcript().results()));

    // Read from our audio file, 4 KB at a time
    try (InputStream reader = Files.newInputStream(audioFile)) {
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = reader.read(buffer)) != -1) {
            if (bytesRead > 0) {
                // Write the 4 KB we read to transcribe, and wait for the write to complete
                SdkBytes audioChunk = SdkBytes.fromByteBuffer(ByteBuffer.wrap(buffer, 0, bytesRead));
                CompletableFuture<Void> writeCompleteFuture =
                        transcription.write(AudioEvent.builder().audioChunk(audioChunk).build());
                writeCompleteFuture.join();
            }
        }
    }

    // Wait for the operation to complete
    transcription.completionFuture().join();
}
```

**Example 3: Kinesis's `subscribeToShard` with Java 8 Streams**

```Java
try (KinesisAsyncClient client = KinesisAsyncClient.create();
     // Create the connection to Kinesis and send the initial request message
     RunningSubscribeToShard transcription = client.subscribeToShard(r -> r.shardId("myShardId"))) {

    // Block this thread to log 5 Kinesis SubscribeToShardEvent messages
    transcription.blockingResponseEventStream()
                 .filter(SubscribeToShardEvent.class::isInstance)
                 .map(SubscribeToShardEvent.class::cast)
                 .limit(5)
                 .forEach(event -> System.out.println(event.records()));
}
```