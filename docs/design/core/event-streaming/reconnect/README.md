**Design:** New Feature, **Status:** [Proposed](../../../README.md)

# Event Stream Reconnects

Event streaming allows long-running bi-directional communication between
customers and AWS services over HTTP/2 connections.

Because a single request is intended to be long-running, services
usually provide a way for a client to "resume" an interrupted session on
a new TCP connection. In Kinesis's subscribe-to-shard API, each response
event includes a `continuationSequenceNumber` that can be specified in a
request message to pick up from where the disconnect occurred. In
Transcribe's streaming-transcription API, each response includes a
`sessionId` with similar semantics.

The current implementation requires the service to write a high-level
library for handling this logic (e.g. Kinesis's consumer library), or
for each customer to write this logic by hand.
[This hand-written code](CurrentState.java) is verbose and error prone.
 
This mini-design outlines API options for the SDK automatically
reconnecting when a network error occurs.

## [API Option 1: New Method](prototype/Option1.java)

This option adds a new method to each streaming operation that the
customer can use to enable automatic reconnects. The customer selects to
work with or without reconnects based on the method that they use.

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create()) {
    // ...
    // Current method (behavior is unchanged)
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    
    // New method that transparently reconnects on network errors (name to be bikeshed)
    client.startStreamTranscriptionWithReconnects(audioMetadata,
                                                  audioStream,
                                                  responseHandler);
    // ...
}
```

## [API Option 2: New Client Configuration](prototype/Option2.java)

This option adds a new setting on the client that the customer can use
to *disable* automatic reconnects. The customer gets automatic
reconnects by default, and would need to explicitly disable them if they
do not want them for their use-case.

```Java
// Current method is updated to transparently reconnect on network errors
try (TranscribeStreamingAsyncClient client = 
             TranscribeStreamingAsyncClient.create()) {
    // ...
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    // ...
}

// New client configuration option can be used to configure reconnect behavior
try (TranscribeStreamingAsyncClient client = 
             TranscribeStreamingAsyncClient.builder()
                                           .overrideConfiguration(c -> c.reconnectPolicy(ReconnectPolicy.none()))
                                           .build()) {
    // ...
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    // ...
}
```

## Comparison

| | Option 1 | Option 2 |
| --- | --- | --- |
| Discoverability | - | + |
| Configurability | - | + |
| Backwards Compatibility | + | - |