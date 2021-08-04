**Design:** New Feature, **Status:** [In Development](../../../README.md)

# Design Document (Automatic Request Batching)

## Introduction

* * *
Some customers have described a need for batch write operations across multiple AWS services but the lack of these features either serve as blockers to adoption of the v2 SDK or limit SDK usability for customers. Specifically, this feature was implemented in v1 for the SQS service in the form of the `AmazonSQSBufferedAsyncClient` but equivalent functionality has not been ported over to v2.

However, since batch write operations are already included in many AWS services, a general automatic batching solution could not only be implemented in SQS but in any service that might benefit from it. On top of features included in v1, additional simplifications and abstractions can also be included in the batch manager to simplify how customers interact with batching throughout the SDK. Therefore the batching manager hopes to benefit customers by reducing cost, improving performance, and/or simplifying implementation.

This document proposes how this general approach should be implemented in the Java SDK v2.


## Design Review

* * *
Look at decision log here: https://github.com/aws/aws-sdk-java-v2/blob/master/docs/design/core/batch-utilities/DecisionLog.md

The Java SDK team has decided to implement a separate batch manager for the time being. Further discussion is required surrounding separate utilities vs implementing directly on the client.

## Overview

* * *
The batch manager proposed in this document will work similarly to v1’s `AmazonSQSBufferedAsyncClient`. Calls made through the manager will first be buffered before being sent as a batch request to the respective service. Additional functionality will also be implemented in v2, such as the ability to automatically batch an array of items by the manager.

Client-side buffering will be implemented generically and allows up to the maximum requests for the respective service (ex. max 10 requests for SQS). Doing so will decrease the cost of using these AWS services by reducing the number of sent requests.

## Proposed APIs

* * *
The v2 SDK will support a batch manager for both sync and async clients that can leverage batch calls.

### Instantiation

**Option 1: Instantiating from an existing client**

```
// Sync Batch Manager
SqsClient sqs = SqsClient.create();
SqsBatchManager sqsBatch = sqs.batchManager();

// Async Batch Manager
SqsAsyncClient sqsAsync = SqsAsyncClient.create();
SqsAsyncBatchManager sqsAsyncBatch = sqsAsync.batchManager();
```

**Option 2: Instantiating from batch manager builder**

```
// Sync Batch Manager
SqsBatchManager sqsBatch = SqsBatchManager.builder()
                                          .client(client)
                                          .overrideConfiguration(newConfig)
                                          .build();

// Async Batch Manager
SqsAsyncBatchManager sqsBatch = SqsAsyncBatchManager.builder()
                                          .client(asyncClient)
                                          .overrideConfiguration(newConfig)
                                          .build();
```

### General Usage Examples:

Note: Focusing on automatic batching and manual flushing for the scope of the internship.

```
// 1. Automatic Batching
SendMessageRequest request1 = SendMessageRequest.builder()
                                                .messageBody("1")
                                                .build();
SendMessageRequest request2 = SendMessageRequest.builder()
                                                .messageBody("2")
                                                .build();

// Sync
SqsClient sqs = SqsClient.create();
SqsBatchManager sqsBatch = sqs.batchManager();
CompletableFuture<SendMessageResponse> response1 = sqsBatch.sendMessage(request1);
CompletableFuture<SendMessageResponse> response2 = sqsBatch.sendMessage(request2);

// Async
CompletableFuture<SendMessageResponse> response1 = sqsBatch.sendMessage(request1);
CompletableFuture<SendMessageResponse> response2 = sqsBatch.sendMessage(request2);

// 2. Manual Flushing
sqsBatch.flush();
```



### `{Service}BatchManager` and `{Service}AsyncBatchManager`

For each service that can leverage batch features, two classes will be created: A {Service}BatchManager and {Service}AsyncBatchManager (ex. SqsBatchManager and SqsAsyncBatchManager for SQS). This follows the naming convention established in v2 like with {Service}Client and {Service}Manager.

**Sync:**

```
/**
 * Batch Manager class that implements batching features for a sync client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsBatchManager {
 
    /**
     * Buffers outgoing requests on the client and sends them as batch requests to the service. 
     * Requests are batched together according to a batchKey and are sent periodically to the 
     * service as determined by {@link #maxBatchOpenInMs}. If the number of requests for a 
     * batchKey reaches or exceeds {@link #maxBatchItems}, then the requests are immediately 
     * flushed and the timeout on the periodic flush is reset.
     * By default, messages are batched according to a service's maximum size for a batch request. 
     * These settings can be customized via the configuration.
     *
     * @param request the outgoing request.
     * @return a CompletableFuture of the corresponding response.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);
    
    /**
     * Manually flush the buffer for sendMessage requests. Completes when requests
     * are sent. An exception is returned otherwise.
     */
    CompletableFuture<Void> flush();
    
    // Other Batch Manager methods omitted
    // ...
    
    interface Builder {
    
        Builder client (SqsClient client);
        
        /** 
         * Method to override the default Batch Manager configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch Manager configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsBatchManager build();
    
    }
 }
```

**Async:**

```
/**
 * Batch Manager class that implements batching features for an async client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsAsyncBatchManager {
 
    /**
     * Buffers outgoing requests on the client and sends them as batch requests to the service. 
     * Requests are batched together according to a batchKey and are sent periodically to the 
     * service as determined by {@link #maxBatchOpenInMs}. If the number of requests for a 
     * batchKey reaches or exceeds {@link #maxBatchItems}, then the requests are immediately 
     * flushed and the timeout on the periodic flush is reset.
     * By default, messages are batched according to a service's maximum size for a batch request. 
     * These settings can be customized via the configuration.
     *
     * @param request the outgoing request.
     * @return a CompletableFuture of the corresponding response.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);
    
    /**
     * Manually flush the buffer for sendMessage requests. Completes when requests
     * are sent. An exception is returned otherwise.
     */
    CompletableFuture<Void> flush();
    
    // Other Batch Manager methods omitted
    // ...
    
    interface Builder {
    
        Builder client (SqsAsyncClient client);
        
        /** 
         * Method to override the default Batch Manager configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch Manager configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsAsyncBatchManager build();
    
    }
 }

```



### `BatchOverrideConfiguration`

```
/**
 * Configuration class to specify how the Batch Manager will implement its
 * batching features. 
 */
public final class BatchOverrideConfiguration {
    
    private final int maxBatchItems;
    
    private final long maxBatchSizeInBytes;
    
    private final Duration maxBatchOpenInMs;
    
    // More fields and methods omitted
    // Focus on including configurable fields from v1
}
```

* * *

## FAQ

### **Which Services will we generate a Batch Manager?**

Services that already support batch requests (ex. SQS with sendMessageBatch, Kinesis with putRecords) in order to reduce cost for customers should be supported with a batch manager.

Note: In this document, we focus on implementing a batch manager for SQS to ensure the functionality of v1’s `AmazonSQSBufferedAsyncClient` is carried over to v2. Therefore the code snippets used mainly focus on methods and types  supported by the SQS client.

### **Why don’t we just implement batching features directly on the low level client?**

There are three options we discussed in implementing batching features:

1. Create batching features directly on the low level client
2. Create a separate high level library
3. Create a separate batch manager class

Using these three options would look like:

```
SqsAsyncClient sqsAsync = SqsAsyncClient.builder().build();

// Option 1
sqsAsync.automaticSendMessageBatch(message1);
sqsAsync.automaticSendMessageBatch(message1);

// Option 2
 SqsAsyncBatchManager batchManager = SqsAsyncBatchManager
                                        .builder()
                                        .client(sqsAsync)
                                        .build()
batchManager.sendMessage(message1);
batchManager.sendMessage(message2);

// Option 3
SqsAsyncBatchManager batchManager = SqsAsyncBatchManager.batchManager()
batchManager.sendMessage(message1);
batchManager.sendMessage(message2);
```


**Option 1 Pros:**

1. Automatic batching features are slightly more discoverable.

**Option 2 Pros:**

1. Hand written library can be more user friendly than generated utility methods.
2. Works very similarly to the v1 `AmazonSQSBufferedAsyncClient`, so migration from v1 to v2 should require minimal changes.

**Option 3 Pros:**

1. All batch related features for a service would be self-contained in the client’s respective utility class.
2. Works very similarly to the v1 `AmazonSQSBufferedAsyncClient`, so migration from v1 to v2 should require minimal changes.
3. Consistent with existing utilities such as the Waiters utility class.
4. Easily configurable and scalable to incorporate many services.

**Decision:** Option 3 will be used since it closely follows the style used throughout v2 (especially similar to how the waiters abstraction is used). Furthermore, it provides the most flexibility to scale across multiple services without becoming too complicated to use.

Look at [decision log](./DecisionLog.md) for reasoning made on 6/29/2021.

### Why do we only support sending one message at a time instead of a list of messages or streams?

Supporting a singular sendMessage method makes it easier and simpler for customers to correlate request messages with the respective responses than an implementation that receives streams or lists of messages (ex. sending a SendMessageRequest in SQS returns a SendMessageResponse as opposed to a batch response wrapper class).

Sending multiple messages or a stream of messages can be as simple as looping through each of the messages and calling the sendMessage method. Therefore, if needed, adding support for sending streams or a list of messages can easily be done as long as the sendMessage method is supported.

### **Why support Sync and Async?**

Supporting sync and async clients not only ensures that the APIs of both clients do not diverge, but would also have parity with the buffered client in v1. Furthermore, this support is just a matter of using the respective sync and async clients’ methods to make the requests and should both be simple for customers to understand, and for the SDK team to implement.


### **Why does `sendMessage` return a CompletableFuture for both the sync and async client?**

The sendMessage method automatically buffers each sendMessage request until the buffer is full or a timeout occurs. Therefore, the sync client’s sendMessage would block until the entire batchRequest is sent and received, which could take as long as the timeout specified. To reduce blocking for this extended period of time, the sendMessage returns a CompletableFuture in both the sync and async client which completes when the underlying batchRequest is sent and a response is received.

Therefore, as mentioned above, the distinction between the sync and async client lies in the use of the respective clients’ methods (ex. the sync batch manager leverages the sync client’s sendMessageBatch under the hood while the async batch manager uses the async client’s sendMessageBatch).


## References

* * *
Github feature requests for specific services:

* [SQS](https://github.com/aws/aws-sdk-java-v2/issues/165)
* [Kinesis](https://github.com/aws/aws-sdk-java/issues/1162)
* [Kinesis Firehose](https://github.com/aws/aws-sdk-java/issues/1343)
* [CloudWatch](https://github.com/aws/aws-sdk-java/issues/1109)
* [S3 batch style deletions](https://github.com/aws/aws-sdk-java/issues/1307)

