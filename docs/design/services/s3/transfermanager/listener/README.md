**Design:** New Feature,
**Parent feature:** [TransferManager](..),
**Status:** [In Development](..)

# v2 Transfer Manager Progress Listeners

## Overview

Java SDK v1 supports the concept of a `ProgressListener` for its `TransferManager`-initiated uploads and downloads. The classic example use case of a `ProgressListener` is to allow a user to display a progress bar for tracking the progress of a long-running transfer. Java SDK v2 currently lacks equivalent functionality. We would like to offer users this functionality, while also taking advantage of the opportunity to rethink how we can best expose such behavior.

## Java SDK v1 Background

The v1 `ProgressListener` interface can be summarized as follows:

```
public interface ProgressListener {
    void progressChanged(ProgressEvent progressEvent);
}
```

```
@Value
public class ProgressEvent {
    ProgressEventType eventType;
    long bytes;
    long bytesTransferred;
}
```

```
public enum ProgressEventType {
    REQUEST_CONTENT_LENGTH_EVENT,
    RESPONSE_CONTENT_LENGTH_EVENT,
    REQUEST_BYTE_TRANSFER_EVENT,
    RESPONSE_BYTE_TRANSFER_EVENT,
    RESPONSE_BYTE_DISCARD_EVENT,
    CLIENT_REQUEST_STARTED_EVENT,
    HTTP_REQUEST_STARTED_EVENT,
    HTTP_REQUEST_COMPLETED_EVENT,
    HTTP_REQUEST_CONTENT_RESET_EVENT,
    CLIENT_REQUEST_RETRY_EVENT,
    HTTP_RESPONSE_STARTED_EVENT,
    HTTP_RESPONSE_COMPLETED_EVENT,
    HTTP_RESPONSE_CONTENT_RESET_EVENT,
    CLIENT_REQUEST_SUCCESS_EVENT,
    CLIENT_REQUEST_FAILED_EVENT,
    // TM-specific events:
    TRANSFER_PREPARING_EVENT,
    TRANSFER_STARTED_EVENT,
    TRANSFER_COMPLETED_EVENT,
    TRANSFER_FAILED_EVENT,
    TRANSFER_CANCELED_EVENT,
    TRANSFER_PART_STARTED_EVENT,
    TRANSFER_PART_COMPLETED_EVENT,
    TRANSFER_PART_FAILED_EVENT;
}
```

**Observations:**

1. There are *many* event types that can trigger a progress listener update.
2. Some events are generic and some are unique to `TransferManager`-related requests.
3. The fields exposed to the user do not vary between event types. It is always confined to `bytes` and `bytesTransferred`.
    1. The definition of these fields depends on the associated event type.
    2. For example, `{REQUEST_CONTENT_LENGTH_EVENT, bytes}` refers to the content-length bytes (the number of bytes we expect to send).
    3. While `{REQUEST_BYTE_TRANSFER_EVENT, bytes}` refers to the number of bytes that were just written.
    4. Other combinations return `0` when not applicable.
    5. It is impossible to convey any additional information that cannot be expressed in a simple `long`.
4. The `ProgressListener` interface does not expose a history or accumulation of past events. For example, a user is responsible for capturing and saving the content length, and a user is also responsible for continuously summing together the number of bytes written thus far.

* * *
*Note: The [v1 Public docs](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-transfermanager.html#transfermanager-get-status-and-progress) currently states that overall progress can be calculated with a ProgressListener as follows:*

```
@Override
public void progressChanged(ProgressEvent e) {
    double pct = e.getBytesTransferred() * 100.0 / e.getBytes();
    eraseProgressBar();
    printProgressBar(pct);
}
```

*This is incorrect. A single ProgressEvent does not track a past-seen content length, nor does it accumulate total number of bytes processed so far. This is evidence of the multiple, overloaded meanings of these parameters resulting in a confusing user experience.*
* * *
Separate from the v1 `ProgressListener` is the v1 `TransferProgress` class. This is a stateful class that *does* track the total number of bytes processed so far. Every `TransferManager`-initiated transfer is associated with a `TransferProgress`, which can be queried as follows:

```
TransferProgress progress = transfer.getProgress();
progress.getBytesTransferred();
progress.getTotalBytesToTransfer();
progress.getPercentTransferred();
```

Internally, `TransferManager` uses its own implementation of a `ProgressListener` to record updates to its paired `TransferProgress`. An ideal & correct event-driven user implementation would probably implement the `ProgressListener` interface and, within, query the corresponding `TransferProgress`. (Unfortunately, `ProgressListener` does not expose any direct references to `Transfer` or `TransferProgress`, which makes this cumbersome to declare before a transfer has been initiated.)

## Java SDK v2 Background

While v2 does not currently support the concept of a `ProgressListener`, it does make extensive use of `ExecutionInterceptor`s, which offers some similar functionality. The `ExecutionInterceptor` interface is an implementation of the [intercepting filter design pattern](https://en.wikipedia.org/wiki/Intercepting_filter_pattern), which offers a composable way to respond to different events and apply resulting transformations. The key difference here is that `ExecutionInterceptor`s tend to be targeted towards mutating operations, and the interface method names further reinforce this, e.g.: `modifyRequest`, `modifyHttpContent`, etc.

The v2 `ExecutionInterceptor` interface can be summarized as follows:

```
public interface ExecutionInterceptor {
    // context = SdkRequest
    default void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {}
    default SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {return context.request();}
    default void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {}
    // context += SdkHttpRequest, RequestBody (contentLength)
    default void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {}
    default SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.httpRequest();}
    default Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.requestBody();}
    default Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.asyncRequestBody();}
    default void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {}
    // context += SdkHttpResponse, responseBody
    default void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {}
    default SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.httpResponse();}
    default Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.responsePublisher();}
    default Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.responseBody();}
    default void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {}
    // context += SdkResponse
    default void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {}
    default SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {return context.response();}
    default void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {}
    
    // Separate (optional) context attributes
    default Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {return context.exception();}
    default void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {}
}
```

**Observations:**

1. Each context object *extends* the previous context, adding more and more parameters available, based on the current lifecycle of the request.
2. Compared to the v1 interface, rather than having one method with many enum types, we have many methods. This allows us to more strongly bind a given event type to its available parameters (rather than trying to redefine & multipurpose the meaning of a single `long` parameter).
3. Where a v1 implementation may perform a *switch* statement on an enum, a similar v2 implementation would selectively choose which methods to override. In some cases this may be cleaner, in other cases it may result in more duplicate code (or missed events).
4. We have the ability to transform bodies & input/output streams, but we lack an easy way to measure the progress of bodies (outside of wrapping those streams with metered implementations).
5. As mentioned before, this interface is explicitly targeting mutating use cases, which may seem intimidating or excessive to customers simply desiring to listen-in on events.

## v2 Progress Listener Goals

1. Create an intuitive interface for users to be able to track the progress of `TranserManager`-initiated uploads
2. Facilitate the logic that is most commonly associated with tracking the progress of uploads & downloads (i.e., progress bars)
3. Design for **reuse**: while our design is targeted at satisfying the TM use case, we should look for ways to allow this functionality to benefit other use cases.
4. Design for **extensibility**: we want to be able to satisfy the TM requirements without requiring a perfect solution in the general case. We should be able to easily & safely add functionality in the future (i.e., backwards compatible).
5. Be mindful of potential overlap with `ExecutionInterceptor`. Where possible, avoid duplication of effort. Be explicit about any intended overlap.
6. Performance impact should be minimal, as well as not impact existing library usage that doesn’t leverage the new functionality.

## Proposal

1. Create a new `ExecutionListener` interface (or alternative name TBD) in `sdk-core`:
    1. We initially design this interface to be leveraged by `TranserManager`, but we can also create a new user-friendly public API in the process.
    2. `ExecutionListener` closely matches the functionality of the `ExecutionInterceptor` interface but which is intended strictly for non-mutating & non-control-flow-altering operations.
    3. All methods return `void`. Any exceptions thrown are suppressed (with a warning log). This reinforces that listeners are not to be used for control flow.
    4. A `Listener` can be declared anywhere an `Interceptor` can be declared today (i.e., client-level, request-level).
    5. The `Listener` interface will expose *fewer* methods than `Interceptor` in most scenarios, i.e., there is no need to expose both `before` and after `methods`, we would just expose the equivalent of `after`.
    6. The `Listener` interface may expose *more* methods in some scenarios, i.e., there may be more interesting events that occur during the lifecycle of a request/response that cannot necessarily be modified and thus are not relevant to an `Interceptor`.
    7. Similar to the `Interceptor`, we will daisy-chain context interfaces from one event to another. Where relevant, we may store a history of critical events within the context, like the total number of bytes written or received thus far, therefore minimizing the state-tracking needed by user implementations.
    8. The `Listener` interface becomes our recommended way for customers to instrument more fine-grained logging, custom metrics, etc.
    9. Optional optimization: `Listener`s can declare whether they are *blocking* or *non-blocking*. Blocking listeners are executed within a separate thread pool (similar to future completions), non-blocking listeners are executed in-line. By default, we assume all listeners are blocking.
2. Create a new `TransferListener` interface that is specific to `TransferManager`.
    1. The `TransferListener` interface offers transfer-specific callbacks for the lifecycle of a transfer.
    2. Unlike v1, `TransferListener` is distinctly separate from `ExecutionListener`. We do not conflate the two.
        1. `TransferListener` will not attempt to be 1:1 compatible with `ExecutionListener` because a single transfer may consist of multiple requests (i.e., multiple multi-part uploads or multiple byte-ranged GETs).
    3. A `TransferListener` can be declared as part of creating a `TransferManager` instance or an individual `TransferRequest`.
    4. A `TransferListener` *cannot* be declared as part of a `PutObjectRequest` or `GetObjectRequest` (these only accept `ExecutionListener`s).
3. Extend the concept of the v1 `TransferProgress` interface to a comparable v2 `TransferProgress`.
    1. Unlike v1, `TransferProgress` is directly accessible from the `TransferListener` callbacks.
    2. Unlike v1, `TransferProgress` is immutable.
    3. Like v1, `TransferProgress` is also directly accessible from the existing `Transfer` interface (`Upload`/`Download`).
    4. Like v1, `TransferProgress` facilitates poll-like use cases for checking a transfer.
    5. Unlike v1, `TransferProgress` also offers a convenient (immutable) view from within a given listener update.
    6. Improved from v1, `TransferProgress` offers first-class support for querying the progress of a transfer, such as percentage elapsed, time elapsed, average transfer rate, etc. This significantly improves the developer experience for our primary use case.

## Proposed public interfaces (draft)

```
public interface ExecutionModeAware {
     enum ExecutionMode {
        INLINE, // executed in EventLoop thread, use w/ caution, non-blocking only
        OFFLOAD // executed in separate thread pool, slower but safer, blocking okay
    }
    
    default ExecutionMode executionMode() {
        return ExecutionMode.OFFLOAD;
    }
}
```

```
// Generic, request-level listener
public interface ExecutionListener extends ExecutionModeAware {
    // context = SdkRequest
    // (called 1x for a "logical" request)
    default void requestCalled(Context.RequestSent context) {}
    
    // context += SdkHttpRequest, RequestBody (contentLength)
    // (called 1x for a "logical" request)
    default void requestPrepared(Context.RequestSent context) {}
    
    // (called for each "physical" request (incl. retry-attempt))
    default void requestSent(Context.RequestSent context) {}
    
    // context += requestBytesSent
    default void requestBytesSent(Context.RequestBytesSent context) {}
    
    // context += SdkResponse, SdkHttpResponse, responseBody
    default void responseReceived(Context.ResponseReceived context) {}
    
    // context += responseBytesReceived
    default void responseBytesReceived(Context.ResponseBytesReceived context) {}
    
    // Separate (optional) context attributes
    default void executionFailure(Context.ExecutionFailure context) {}
    
    // TODO: Consider other callbacks for more advanced retry notifications
    // TODO: Consider more low-level callbacks, see v1 events for reference
}
```

```
// TM-specific listener
public interface TransferListener extends ExecutionModeAware {
    // context = TransferRequest (UploadRequest/DownloadRequest), TransferProgress
    default void transferInitiated(Context.TransferInitiated context) {}  
      
    default void bytesTransferred(Context.BytesTransferred context) {}
    
    // context += CompletedTransfer (CompletedUpload/CompletedDownload)
    default void transferComplete(Context.TransferComplete context) {}

    // Separate (optional) attributes
    default void executionFailure(Context.ExecutionFailure context) {}
    
    // TODO: Consider other callbacks for more advanced retry notifications
    
    // We may wish to warn that num bytes transferred may temporarily
    // decrease in the event of a retry.
}
```

```
public interface TransferProgress {
     // Attributes
    Instant startTime();
    long totalBytesTransferred();
    OptionalLong totalTransferSize();
    
    // Convenience methods (optional methods depend on a known transferSize)
    Duration elapsedTime();
    double averageBytesPer(TimeUnit timeUnit);
    OptionalDouble percentageTransferred();
    OptionalLong bytesRemaining();
    Optional<Duration> estimatedTimeRemaining(); // uses avg, users may want EWMA
}
```

Furthermore, the existing `Transfer` interface is extended to incorporate a `TransferProgress` attribute:

```
  public interface Transfer {
      CompletableFuture<? extends CompletedTransfer> completionFuture();
+     TransferProgress progress();
  }
```

## Example

Now the previous (incorrect) documentation example of calculating a progress percentage with a `ProgressListener` is trivially possible with a `TransferListener` as follows:

```
@Override
public void bytesTransferred(Context.BytesTransferred context) {
     context.transferProgress().percentageTransferred().ifPresent(this::updateProgressBar);
}
```

## Extension & backwards compatibility

It is backwards-compatible to add new events to the `ExecutionListener` interface:

* In the case of daisy-chained context interfaces `A → B → C`, adding `A → B → B′ → C` would be backwards safe since `C` still extends `B`.
* Furthermore, by design all event methods return `void`. We can safely add `default` implementations that are no-ops.

It is not backwards-compatible to re-order events or to move parameters from one context interface to another. In the same vein, it is not backwards-compatible to break one event into multiple events. Extra care is needed to ensure we get the right level of granularity the first time.

## Implementation

The `ExecutionListener` interface is highly comparable to the `ExecutionInterceptor` interface and is implemented in a similar fashion. That is, almost everywhere in the codebase that accepts or invokes an `Interceptor` is updated to also support `Listener`s. Implementation-wise, `Listener`s are either implemented in parallel to `Interceptor`s, or potentially even implemented as an `Interceptor` itself (as long as the `Listener` interface is a subset of the `Interceptor` interface).

`TransferManager` invokes its registered `TransferListener`s by declaring its own implementation of `ExecutionListener` for every sub-request. That is, upon detecting byte progression by any of its `ExecutionListener`s, it creates a new immutable `TransferProgress` and invokes all of its `TransferListener`s accordingly. The internal `ExecutionListener` can be safely declared as `ExecutionMode.INLINE` since we have full control of the implementation. However, this implementation would still have to respect the `ExecutionMode` of its own listeners.

An optimization that both implementations should consider is to first ensure their registered listener list is non-empty before doing any work, like creating a new `TransferProgress` or a new context object. This may help minimize allocations when no listeners are actually configured.

## Miscellaneous comments/questions

Java v1 TM also supports the concept of [MultipleFileUpload](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/MultipleFileUpload.html) & Download interfaces for multi-file transfers, which supports a reference to get its sub-transfers. The `MultipleFileUpload` interface also extends the `Transfer` interface, which means it has its own `ProgressListener`s. We should implement the same sub-transfer & listener logic for v2.

Java v1 TM supports *adding* listeners to existing & in-progress transfers. This is consistent with the API of something like Java’s `CompletableFuture` and therefore may be more user-friendly for some users. We need to determine if we wish to support this use case for v2, or if we wish it to be more immutable by design. We could, theoretically, support the dynamic adding of `TransferListener`s without requiring dynamic adding of `ExecutionListener`s.

Should the proposed `ExecutionListener` interface be broken down into more steps (e.g., `requestExecuted`, `requestMarshalled`, `requestSent`, etc.)? Unsure if these individual steps offer value since there is no networking delay between them. The primary advantage would be if an error occurred during one of them, we would have more visibility.

Alternative names to `ExecutionListener`? `ProgressListener`, `CallListener`, `RequestListener`, etc.
