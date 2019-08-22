**Design:** New Feature, **Status:** [In Development](../../../README.md)

# Project Goals

1. Capturing progress events should have minimal impact on the application performance.

2. Progress events and listeners should be extensible.

# Introduction

This project provides Progress Listeners feature, which allows customers to track the progress of executions for streaming operations and execute callbacks
on different progress events. 

# Progress Event

A progress event contains the progress type and progress data.

```
@SdkPublicApi
public interface ProgressEvent {

    /**
     * @return the type of the progress event
     */
    ProgressEventType eventType();

    /**
     * @return the optional event data
     */
    ProgressEventData eventData();
}
```

## Progress Event Types

Currently, there are two core ProgressEventTypes and one TransferManager event type

`ByteCountEvent`: The Byte count events indicating the number of bytes in the execution of a single http request-response.

`RequestCycleEvent`: The request cycle events related to the execution of a single http request-response.

`S3TransferEvent`: The events related to a S3 {@link Transfer}.

# Configuration

Customers can provide `ProgressEventListener`s on client level as well as request level and the provided progress listeners will be merged.

A `ProgressEventListener` can be created by implementing the interface or from the `ProgressEventListener#builder`.

## Low level client

```java

        ProgressEventListener eventListener = new ProgressEventListener() {
            @Override
            public CompletableFuture<? extends ProgressEventResult> onByteCountEvent(ByteCountEvent progressEvent) {
                System.out.println("received ByteCountEvent");
                return CompletableFuture.completedFuture(null);
            }
        };

        // Client level progress listener
        S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                                                   .overrideConfiguration(b -> b.progressListeners(Collections.singletonList(eventListener)))
                                                   .build();

        // Request level progress listener
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket("bucket")
                                                            .key("key")
                                                            .overrideConfiguration(o -> o.addProgressListener(eventListener))
                                                            .build();

```


## High level libraries

```java
        transferManager = S3TransferManager.builder()
                                           .s3client(s3Async)
                                           // Implement interface
                                           .addProgressListener(new S3TransferProgressEventListener() {
                                               @Override
                                               public CompletableFuture<? extends ProgressEventResult> onTransferEvent(S3TransferEvent progressEvent) {
                                                   System.out.println("received S3TransferEvent");
                                                   return CompletableFuture.completedFuture(null);
                                               }

                                               @Override
                                               public CompletableFuture<? extends ProgressEventResult> onRequestLifeCycleEvent(RequestCycleEvent progressEvent) {
                                                   System.out.println("received RequestLifeCycleEvent");
                                                   return CompletableFuture.completedFuture(null);
                                               }

                                               @Override
                                               public CompletableFuture<? extends ProgressEventResult> onByteCountEvent(ByteCountEvent progressEvent) {
                                                   System.out.println("received ByteCountEvent");
                                                   return CompletableFuture.completedFuture(null);
                                               }
                                           })
                                           // Using builder
                                           .addProgressListener(S3TransferProgressEventListener.builder()
                                                                                               .onTransferEvent(t -> CompletableFuture.completedFuture(null))
                                                                                               .onByteCountEvent(t -> CompletableFuture.completedFuture(null))
                                                                                               .onTransferEvent(t -> CompletableFuture.completedFuture(null))
                                                                                               .build())
        
            
```



