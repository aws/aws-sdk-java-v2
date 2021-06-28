# Github Design Document

# Design Document (Automatic Request Batching)

## Introduction

* * *
Some customers have described a need for batch write operations across multiple AWS services but the lack of these features either serve as blockers to adoption of the v2 SDK or limit SDK usability for customers. Specifically, this feature was implemented in v1 for the SQS service in the form of the `AmazonSQSBufferedAsyncClient` but equivalent functionality has not been ported over to v2.

However, since batch write operations are common across many AWS services, a general batching approach could be used to implement these features not only in SQS but in any service that might benefit from it. This document proposes how this general approach should be implemented in the Java SDK v2.

## Overview

* * *
The batch utility proposed in this document will work similarly to v1’s `AmazonSQSBufferedAsyncClient`. Calls made through the utility will first be buffered before being sent as a batch request to the respective service. Additional functionality will also be implemented in v2, such as the ability to automatically batch an array of items by the utility.

Client-side buffering will be implemented generically and allows up to the maximum requests for the respective service (ex. max 10 requests for SQS). Doing so will decrease the cost of using these AWS services by reducing the number of sent requests.

## Proposed APIs

* * *
The v2 SDK will support a batch utility for both sync and async clients that can leverage batch calls.

### Instantiation

**Option 1: Instantiating from an existing client**

```
// Sync utility
SqsClient sqs = SqsClient.create();
SqsBatchUtilities sqsBatch = sqs.batchUtilities();

// Async utility
SqsAsyncClient sqsAsync = SqsAsyncClient.create();
SqsAsyncBatchUtilities sqsAsyncBatch = sqsAsync.batchUtilities();
```

**Option 2: Instantiating from batch utility builder**

```
// Sync utility
SqsBatchUtilities sqsBatch = SqsBatchUtilities.builder()
                                          .client(client)
                                          .overrideConfiguration(newConfig)
                                          .build();

// Async utility
SqsAsyncBatchUtilities sqsBatch = SqsAsyncBatchUtilities.builder()
                                          .client(asyncClient)
                                          .overrideConfiguration(newConfig)
                                          .build();
```

### General Usage Examples:

```
// 1. Automatic Batching
SendMessageRequest request1 = ;
SendMessageRequest request2 = ;
SendMessageRequest[] messages = [request1, request2];

// Sync
SqsClient sqs = SqsClient.create();
SqsBatchUtilities sqsBatch = sqs.batchUtilities();
CompletableFuture<BatchResponse> response = sqsBatch.sendMessages(messages);

// Async
CompletableFuture<BatchResponses> responseFuture = 
 utilty.sendMessages(messages);
BatchResponses response = responseFuture.join();

// BatchResponse Methods
List<SendMesageBatchResultEntry> success = response.successful()
List<BatchResultErrorEntry> failed = response.failed()

// 2. Manual Flushing
utility.flush();
```



### `{Service}BatchUtilities` and `{Service}AsyncBatchUtilities`

For each service that can leverage batch features, two classes will be created: A {Service}BatchUtilities and {Service}AsyncBatchUtilities (ex. SqsBatchUtilities and SqsAsyncBatchUtilities for SQS). This follows the naming convention established in v2 like with {Service}Client and {Service}Utilities.

**Sync:**

```
/**
 * Batch utility class that implements batching features for a sync client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsBatchUtilities {
 
    /**
     * Buffers a variable number of messages on the client and sends them
     * as batch requests to the service. 
     *
     * If the number of messages passed in is greater than the maximum size of 
     * a batch request, the method also automatically chunks the messages into 
     * the appropriate batch sizes before sending them with batch requests. 
     * By default, messages are chunked according to a service's maximum size for a 
     * batch request. These settings can be customized via the configuration. 
     *
     * @param messages A variable number of SendMessageRequest items that represent
                       the messages to be passed to SQS.
     * @return {@link BatchResponses}
     */
    CompletableFuture<BatchResponses<SendMessageBatchResultEntry>> sendMessages(
                            SendMessageRequest... messages);
    
    /**
     * Manually flush the buffer for sendMessage requests.
     * The call returns successfully when all outstanding outbound requests 
     * submitted before the call are completed.
     * An exception is thrown otherwise.
     */
    CompletableFuture<Void> flush();
    
    /**
     * Option to flush a specific buffer/queue
     * Buffer: DeleteMessage buffer, sendMessage buffer, ...
     * Queue: User would provide queueUrl
     * Note: The option to flush a specific buffer is not implemented in v1 nor
     *       has it been requested.
     */
    CompletableFuture<Void> flush();
    
    // Other Batch Utility methods omitted
    // ...
    
    interface Builder {
    
        Builder client (SqsClient client);
        
        /** 
         * Method to override the default Batch utilities configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch utilities configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsBatchUtilities build();
    
    }
 }
```

**Async:**

```
/**
 * Batch utility class that implements batching features for an async client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsAsyncBatchUtilities {
 
    /**
     * Buffers a variable number of messages on the client and sends them
     * as batch requests to the service. 
     *
     * If the number of messages passed in is greater than the maximum size of 
     * a batch request, the method also automatically chunks the messages into 
     * the appropriate batch sizes before sending them with batch requests. 
     * By default, messages are chunked according to the maximum size of a 
     * batch request as limited by the service. These settings can be customized
     * via the configuration. 
     *
     * @param messages A variable number of SendMessageRequest items that represent
                       the messages to be passed to SQS.
     * @return {@link BatchResponses}
     */
    CompletableFuture<BatchResponses<SendMessageBatchResultEntry>> sendMessages(
                            SendMessageRequest... messages);
    
    /**
     * Manually flush the buffer for sendMessage requests.
     * The call returns successfully when all outstanding outbound requests 
     * submitted before the call are completed.
     * An exception is thrown otherwise.
     */
    CompletableFuture<Void> flush();
    
    /**
     * Option to flush a specific buffer/queue
     * Buffer: DeleteMessage buffer, sendMessage buffer, ...
     * Queue: User would provide queueUrl
     * Note: The option to flush a specific buffer is not implemented in v1 nor
     *       has it been requested.
     */
    CompletableFuture<Void> flush();
    
    interface Builder {
    
        Builder client (SqsAsyncClient client);
        
        /** 
         * Method to override the default Batch utilities configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch utilities configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsAsyncBatchUtilities build();
    
    }
 }

```



### `BatchOverrideConfiguration`

```
/**
 * Configuration class to specify how the Batch Utilities will implement its
 * batching features. 
 */
public final class BatchOverrideConfiguration {
    
    private final int maxBatchSize;
    
    private final int maxBatchSizeInBytes;
    
    private final int maxBatchOpenInMs;
    
    // More fields and methods omitted
    // Focus on including configurable fields from v1
}
```



### `BatchUtilities<T> and AsyncBatchUtilities<T>`

For Discussion: Should this generic batch utility class be internal or be created as a public facing interface?

### `BatchResponses<T>`

```
/**
 * The response returned from a sendMessages operation
 * @param <T> the type of the response
 */
@SdkPublicApi
public interface BatchResponse<T, U> {

    /*
     * @return a list of successful responses of type T
     */ 
    List<T> successful();
    
    /*
     * @return a list of failed responses of type T
     */ 
    List<U> failed();
    
    /*
     * @return True if any of the batch responses return Failed.
     * False if none return Failed.
     */ 
    boolean hasFailed();

}
```

* * *

## FAQ

### **Which Services will we generate a batch utility?**

Services that can make use of batch requests in order to reduce cost for customers should be supported with a batch utility.

Note: In this document, we focus on implementing a batch utility for SQS to ensure the functionality of v1’s `AmazonSQSBufferedAsyncClient` is carried over to v2. Therefore the code snippets used mainly focus on methods and types  supported by the SQS client.


### **Why don’t we just implement batching features directly on the low level client?**

There are three options we discussed in implementing batching features:

1. Create batching features directly on the low level client
2. Create a separate high level library
3. Create a separate batch utility class

Using these three options would look like:

```
SqsAsyncClient sqsAsync = SqsAsyncClient.builder().build();

// Option 1
sqsAsync.automaticSendMessageBatch(messages);

// Option 2
BatchingClient batchClient = BatchingClient
                             .builder()
                             .client(sqsAsync)
                             .build()
batchClient.sendMessages(messages);

// Option 3
sqsAsync.batchUtilities().sendMessages(messages);
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


### **Why does `sendMessages` take a variable number of `SendMessageRequest` (as opposed to `SendMessageBatchRequest` or equivalent types for other services)?**

There are two main use cases for automatic request batching that we identified:

1. Sending one message at a time and having the client buffer requests before sending them as a batch request
2. Sending multiple messages at a time and having the client automatically chunk them into batch requests that do not exceed the batch limit for the service.

Accepting arguments like `SendMessageBatchRequest` would exclude customers who fall into the first use case since they would be utilizing `SendMessageRequests` for each message.

In order to simplify the automatic batching feature for customers, the `sendMessages` method should accept a variable number of `SendMessageRequests` arguments to encompass both use cases. All automatic batching and buffering wil thenl be handled by the SDK.


### **Why return a `BatchResponses`?**

The `sendMessages` method will work by buffering the methods on the client side, splicing the `SendMessageRequests` into `SendMessageBatchRequests`, and making requests to SQS via one or multiple `sendMessageBatch` calls. This in turn will return one or multiple responses of type `SendMessageBatchResponse`.

These responses could be exposed through a `List<SendMessageBatchResponse>`, however this would place the burden of manipulating and extracting the responses on the Customer. Instead, a `BatchResponse` wrapper class is created that will encompass every individual `sendMessageResponse` and include some functionality similar to the ones implemented in `SendMessageBatchResponse`.

Ex. `SendMessageBatchResponse` uses a `.``successful()` method that returns a list of `SendMessageBatchResultEntry` items that were successful. `BatchResponse` will also expose a `.successful()` method that returns a list of every successful `SendMessageBatchResultEntry` item in every `SendMessageBatchResponse`.

Note: The sync client returns a `BatchResponse` while an async client returns a `CompletableFuture<BatchResponse>`.


### **Why support Sync and Async?**

Supporting sync and async clients not only ensures that the APIs of both clients do not diverge, but would also have parity with the buffered client in v1. Furthermore, this support is just a matter of using the respective sync and async clients’ methods to make the requests and should both be simple for customers to understand, and for the SDK team to implement.


## References

* * *
Github feature requests for specific services:

* [SQS](https://github.com/aws/aws-sdk-java-v2/issues/165)
* [Kinesis](https://github.com/aws/aws-sdk-java/issues/1162)
* [Kinesis Firehose](https://github.com/aws/aws-sdk-java/issues/1343)
* [CloudWatch](https://github.com/aws/aws-sdk-java/issues/1109)
* [S3 batch style deletions](https://github.com/aws/aws-sdk-java/issues/1307)

