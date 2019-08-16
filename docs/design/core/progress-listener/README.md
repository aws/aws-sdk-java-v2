**Design:** New Feature, **Status:** [In Development](../../../README.md)

# Project Tenets (unless you know better ones)

1. Recording progress events should have minimal impact on the application performance.

2. The supported progress event types should have minimum overlaps with existing execution interceptors. [To discuss]

3. Progress events and publisher should be extensible.

# Introduction

This project provides Progress Listeners feature, which allows customers to track the progress of executions for streaming operations and execute callbacks
on different progress events. 

# Default Progress Event Types

- Byte Counting Events

```java

        /**
         * Event of the content length to be sent in a request.
         */
        REQUEST_CONTENT_LENGTH_EVENT,
        /**
         * Event of the content length received in a response.
         */
        RESPONSE_CONTENT_LENGTH_EVENT,

        /**
         * Used to indicate the number of bytes to be sent to the services.
         */
        REQUEST_BYTE_TRANSFER_EVENT,

        /**
         * Used to indicate the number of bytes received from services.
         */
        RESPONSE_BYTE_TRANSFER_EVENT,
    
 ```   

- Request/Response Events

```java 


        ////////////////////////////////////////
        // Byte counting progress events
        ////////////////////////////////////////
        /**
         * Used to indicate the request body has been cancelled.
         */
        REQUEST_BODY_CANCEL_EVENT,

        /**
         * Used to indicate the request body is complete.
         */
        REQUEST_BODY_COMPLETE_EVENT,

        /**
         * Used to indicate the request has been reset.
         */
        REQUEST_BODY_RESET_EVENT,

        RESPONSE_BODY_CANCEL_EVENT,

        RESPONSE_BODY_COMPLETE_EVENT,

        RESPONSE_BODY_RESET_EVENT

```

# Configuration
```java

        ProgressEventListener eventListener = progressEvent -> {
            System.out.println(progressEvent);
            return CompletableFuture.completedFuture(null);
        };

        S3AsyncClient.builder()
                     .overrideConfiguration(b -> b.addProgressListener(eventListener))
                     //.overrideConfiguration(b -> b.progressListeners(Collections.singletonList(eventListener)))
                     .build();

    }
```