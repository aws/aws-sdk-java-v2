**Design:** New Feature, **Status:** [Released](../../README.md)

# Waiters

"Waiters" are an abstraction used to poll a resource until a desired state is reached or until it is determined that
the resource will never enter into the desired state. This feature is supported in the AWS Java SDK 1.x and this document proposes 
how waiters should be implemented in the Java SDK 2.x. 

## Introduction

A waiter makes it easier for customers to wait for a resource to transition into a desired state. It comes handy when customers are
interacting with operations that are asynchronous on the service side.

For example, when you invoke `dynamodb#createTable`, the service immediately returns a response with a TableStatus of `CREATING`
and the table will not be available to perform write or read until the status has transitioned to `ACTIVE`. Waiters can be used to help 
you handle the task of waiting for the table to become available.

## Proposed APIs

The SDK 2.x will support both sync and async waiters for service clients that have waiter-eligible operations. It will also provide a generic `Waiter` class
which makes it possible for customers to customize polling function, define expected success, failure and retry conditions as well as configurations such as `maxAttempts`. 

### Usage Examples

#### Example 1: Using sync waiters

- instantiate a waiter object from an existing service client

```Java
DynamoDbClient client = DynamoDbClient.create();
DynamodbWaiter waiter = client.waiter();

WaiterResponse<DescribeTableResponse> response = waiter.waitUntilTableExists(b -> b.tableName("table"));
```

- instantiate a waiter object from builder

```java
DynamodbWaiter waiter = DynamoDbWaiter.builder()
                                      .client(client)
                                      .overrideConfiguration(p -> p.maxAttempts(10))
                                      .build();

WaiterResponse<DescribeTableResponse> response = waiter.waitUntilTableExists(b -> b.tableName("table"));

```

#### Example 2: Using async waiters

- instantiate a waiter object from an existing service client

```Java
DynamoDbAsyncClient asyncClient = DynamoDbAsyncClient.create();
DynamoDbAsyncWaiter waiter = asyncClient.waiter();

CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = waiter.waitUntilTableExists(b -> b.tableName("table"));

```

- instantiate a waiter object from builder

```java
DynamoDbAsyncWaiter waiter = DynamoDbAsyncWaiter.builder()
                                                .client(asyncClient)
                                                .overrideConfiguration(p -> p.maxAttempts(10))
                                                .build();

CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = waiter.waitUntilTableExists(b -> b.tableName("table"));
```


*FAQ Below: "Why not create waiter operations directly on the client?"*

#### Example 3: Using the generic waiter

```Java
Waiter<DescribeTableResponse> waiter =
   Waiter.builder(DescribeTableResponse.class)
         .addAcceptor(WaiterAcceptor.successAcceptor(r -> r.table().tableStatus().equals(TableStatus.ACTIVE)))
         .addAcceptor(WaiterAcceptor.retryAcceptor(t -> t instanceof ResourceNotFoundException))
         .addAcceptor(WaiterAcceptor.errorAcceptor(t -> t instanceof InternalServerErrorException))
         .overrideConfiguration(p -> p.maxAttemps(20).backoffStrategy(BackoffStrategy.defaultStrategy())
         .build();

// run synchronousely 
WaiterResponse<DescribeTableResponse> response = waiter.run(() -> client.describeTable(describeTableRequest));

// run asychronousely
CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture =
      waiter.runAsync(() -> asyncClient.describeTable(describeTableRequest));
```

### `{Service}Waiter` and `{Service}AsyncWaiter`

Two classes will be created for each waiter-eligible service: `{Service}Waiter` and `{Service}AsyncWaiter` (e.g. `DynamoDbWaiter`, `DynamoDbAsyncWaiter`). 
This follows the naming strategy established by the current `{Service}Client` and `{Service}Utilities` classes.

#### Example

```Java
/**
 * Waiter utility class that waits for a resource to transition to the desired state.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface DynamoDbWaiter extends SdkAutoCloseable {

    /**
     * Poller method that waits for the table status to transition to <code>ACTIVE</code> by
     * invoking {@link DynamoDbClient#describeTable}. It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param describeTableRequest Represents the input of a <code>DescribeTable</code> operation.
     * @return {@link DescribeTableResponse}
     */
    default WaiterResponse<DescribeTableResponse> waitUntilTableExists(DescribeTableRequest describeTableRequest) {
        throw new UnsupportedOperationException();
    }

    default WaiterResponse<DescribeTableResponse> waitUntilTableExists(Consumer<DescribeTableRequest.Builder> describeTableRequest) {
        return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build());
    }

     /**
      * Polls {@link DynamoDbAsyncClient#describeTable} API until the desired condition {@code TableExists} is met, or
      * until it is determined that the resource will never enter into the desired state
      *
      * @param describeTableRequest
      *        The request to be used for polling
      * @param overrideConfig
      *        Per request override configuration for waiters
      * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
      *         condition
      */
     default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(
             DescribeTableRequest describeTableRequest, WaiterOverrideConfiguration overrideConfig) {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Polls {@link DynamoDbAsyncClient#describeTable} API until the desired condition {@code TableExists} is met, or
      * until it is determined that the resource will never enter into the desired state.
      * <p>
      * This is a convenience method to create an instance of the request builder and instance of the override config
      * builder
      *
      * @param describeTableRequest
      *        The consumer that will configure the request to be used for polling
      * @param overrideConfig
      *        The consumer that will configure the per request override configuration for waiters
      * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
      *         condition
      */
     default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(
             Consumer<DescribeTableRequest.Builder> describeTableRequest,
             Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
         return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build(),
                 WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
     }

    // other waiter operations omitted
    // ...


    interface Builder {
    
        Builder client(DynamoDbClient client);
        
        /**
         * Defines overrides to the default SDK waiter configuration that should be used for waiters created from this
         * builder
         *
         * @param overrideConfiguration
         *        the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);

        DynamoDbWaiter build();
    }
}

/**
 * Waiter utility class that waits for a resource to transition to the desired state asynchronously.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface DynamoDbAsyncWaiter extends SdkAutoCloseable {

    /**
     * Poller method that waits for the table status to transition to <code>ACTIVE</code> by
     * invoking {@link DynamoDbClient#describeTable}. It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param describeTableRequest Represents the input of a <code>DescribeTable</code> operation.
     * @return A CompletableFuture containing the result of the DescribeTable operation returned by the service. It completes
     * successfully when the resource enters into a desired state or it completes exceptionally when it is determined that the
     * resource will never enter into the desired state.
     */
    default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(DescribeTableRequest describeTableRequest) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(Consumer<DescribeTableRequest.Builder> describeTableRequest) {
        return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build());
    }

    // other waiter operations omitted
    // ...


    interface Builder {
        
        Builder client(DynamoDbAsyncClient client);

        Builder scheduledExecutorService(ScheduledExecutorService executorService);

        Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);

        DynamoDbAsyncWaiter build();
    }

}
```

*FAQ Below: "Why returning a WaiterResponse wrapper class"*.

#### Instantiation

This class can be instantiated from an existing service client or builder

- from an existing service client

```Java
// sync waiter
DynamoDbClient dynamo = DynamoDbClient.create();
DynamoDbWaiter dynamoWaiter = dynamo.waiter();

// async waiter
DynamoDbClient dynamoAsync = DynamoDbAsyncClient.create();
DynamoDbAsyncWaiter dynamoAsyncWaiter = dynamoAsync.waiter();
```

- from waiter builder

```java
// sync waiter
DynamodbWaiter waiter = DynamoDbWaiter.builder()
              .client(client)
              .overrideConfiguration(p -> p.maxAttempts(10))
              .build();


// async waiter
DynamoDbAsyncWaiter asyncWaiter = DynamoDbAsyncWaiter.builder()
                           .client(asyncClient)
                           .overrideConfiguration(p -> p.maxAttempts(10))
                           .build();


```

#### Methods

A method will be generated for each operation that needs waiter support. There are two categories depending on the expected success state.

  - sync: `WaiterResponse<{Operation}Response> waitUntil{DesiredState}({Operation}Request)`
    ```java
    WaiterResponse<DescribeTableResponse> waitUntilTableExists(DescribeTableRequest describeTableRequest)
    ```
  - async: `CompletableFuture<WaiterResponse<{Operation}Response>> waitUntil{DesiredState}({Operation}Request)`
    ```java
    CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(DescribeTableRequest describeTableRequest)
    ```

### `WaiterResponse<T>`
```java
/**
 * The response returned from a waiter operation
 * @param <T> the type of the response
 */
@SdkPublicApi
public interface WaiterResponse<T> {

    /**
     * @return the ResponseOrException union received that has matched with the waiter success condition
     */
    ResponseOrException<T> matched();

    /**
     * @return the number of attempts executed
     */
    int attemptsExecuted();

}
```

*FAQ Below: "Why making response and exception optional"*.
    
### `Waiter<T>`

The generic `Waiter` class enables users to customize waiter configurations and provide their own `WaiterAcceptor`s which define the expected states and controls the terminal state of the waiter.

#### Methods

```java
@SdkPublicApi
public interface Waiter<T> {

    /**
     * It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param pollingFunction the polling function
     * @return the {@link WaiterResponse} containing either a response or an exception that has matched with the
     * waiter success condition
     */
    default WaiterResponse<T> run(Supplier<T> pollingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param pollingFunction the polling function
     * @param overrideConfig per request override configuration
     * @return the {@link WaiterResponse} containing either a response or an exception that has matched with the
     * waiter success condition
     */
    default WaiterResponse<T> run(Supplier<T> pollingFunction, WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }
    
    default WaiterResponse<T> run(Supplier<T> pollingFunction, Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return run(pollingFunction, WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }

    /**
     * Creates a newly initialized builder for the waiter object.
     *
     * @param responseClass the response class
     * @param <T> the type of the response
     * @return a Waiter builder
     */
    static <T> Builder<T> builder(Class<? extends T> responseClass) {
        return DefaultWaiter.builder();
    }
}
```
#### Inner-Class: `Waiter.Builder`

```java
    public interface Builder<T> {

        /**
         * Defines a list of {@link WaiterAcceptor}s to check if an expected state has met after executing an operation.
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> acceptors(List<WaiterAcceptor<T>> waiterAcceptors);

        /**
         * Add a {@link WaiterAcceptor}s
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> addAcceptor(WaiterAcceptor<T> waiterAcceptors);

        /**
        * Defines overrides to the default SDK waiter configuration that should be used
        * for waiters created by this builder.
        *
        * @param overrideConfiguration the override configuration
        * @return a reference to this object so that method calls can be chained together.
        */
        Builder<T> overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);
    }
```
### `AsyncWaiter<T>`

#### Methods
```java
@SdkPublicApi
public interface AsyncWaiter<T> {

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     *  @param overrideConfig per request override configuration
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return runAsync(asyncPollingFunction, WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }
}
```

#### Inner-Class: `AsyncWaiter.Builder`
#### Methods
```java
    public interface Builder<T> {

        /**
         * Defines a list of {@link WaiterAcceptor}s to check if an expected state has met after executing an operation.
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> acceptors(List<WaiterAcceptor<T>> waiterAcceptors);

        /**
         * Add a {@link WaiterAcceptor}s
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> addAcceptor(WaiterAcceptor<T> waiterAcceptors);

        /**
        * Defines overrides to the default SDK waiter configuration that should be used
        * for waiters created by this builder.
        *
        * @param overrideConfiguration the override configuration
        * @return a reference to this object so that method calls can be chained together.
        */
        Builder<T> overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);
        
        /**
         * Define the {@link ScheduledExecutorService} used to schedule async attempts
         *
         * @param scheduledExecutorService the schedule executor service
         * @return the chained builder
         */
        Builder<T> scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);
    }
```

#### `WaiterOverrideConfiguration`

WaiterOverrideConfiguration specifies how the waiter polls the resources.

```java
public final class WaiterOverrideConfiguration {
   //...

    /**
     * @return the optional maximum number of attempts that should be used when polling the resource
     */
    public Optional<Integer> maxAttempts() {
        return Optional.ofNullable(maxAttempts);
    }

    /**
     * @return the optional {@link BackoffStrategy} that should be used when polling the resource
     */
    public Optional<BackoffStrategy> backoffStrategy() {
        return Optional.ofNullable(backoffStrategy);
    }

    /**
     * @return the optional amount of time to wait that should be used when polling the resource
     *
     */
    public Optional<Duration> waitTimeout() {
        return Optional.ofNullable(waitTimeout);
    }
}

```

### `WaiterState`

`WaiterState` is an enum that defines possible states of a waiter to be transitioned to if a condition is met

```java
public enum WaiterState {
    /**
     * Indicates the waiter succeeded and must no longer continue waiting.
     */
    SUCCESS,

    /**
     * Indicates the waiter failed and must not continue waiting.
     */
    FAILURE,

    /**
     * Indicates that the waiter encountered an expected failure case and should retry if possible.
     */
    RETRY
}
```

### `WaiterAcceptor`

`WaiterAcceptor` is a class that inspects the response or error returned from the operation and determines whether an expected condition
is met and indicates the next state that the waiter should be transitioned to if there is a match.

```java
@SdkPublicApi
public interface WaiterAcceptor<T> {

    /**
     * @return the next {@link WaiterState} that the waiter should be transitioned to if this acceptor matches with the response or error
     */
    WaiterState waiterState();

    /**
     * Check to see if the response matches with the expected state defined by the acceptor
     *
     * @param response the response to inspect
     * @return whether it accepts the response
     */
    default boolean matches(T response) {
        return false;
    }
```

## FAQ

### For which services will we generate waiters?

We will generate a `{Service}Waiter` class if the service has any operations that need waiter support.

### Why not create waiter operations directly on the client?

The options are: (1) create separate waiter utility classes or (2) create waiter operations on the client

The following compares Option 1 to Option 2, in the interest of illustrating why Option 1 was chosen.

**Option 1:** create separate waiter utility classes

```Java
dynamodb.waiter().untilUntilTableExists(describeTableRequest)
```

**Option 2:** create waiter operations on each service client

```Java
dynamodb.waitUntilTableExists(describeTableRequest)
```

**Option 1 Pros:**

1. consistent with existing s3 utilities and presigner method approach, eg: s3Client.utilities()
2. similar api to v1 waiter, and it might be easier for customers who are already using v1 waiter to migrate to v2.

**Option 2 Pros:**

1. slightly better discoverability

**Decision:** Option 1 will be used, because it is consistent with existing features and option2 might bloat the size
of the client, making it more difficult to use.

### Why returning `WaiterResponse`?

For waiter operations that awaits a resource to be created, the last successful response sometimes contains important metadata such as resourceId, which is often required for customers to perform other actions with the resource. Without returning the response, customers will have to send an extra request to retrieve the response. This is a [feature request](https://github.com/aws/aws-sdk-java/issues/815) from v1 waiter implementation.

For waiter operations that treats a specific exception as the success state, some customers might still want to access the exception to retrieve the requestId or raw response.

A `WaiterResposne` wrapper class is created to provide either the response or exception depending on what triggers the waiter to reach the desired state. It also provides flexibility to add more metadata such as `attemptExecuted` in the future if needed.


### Why making response and exception optional in `WaiterResponse`?

Per the SDK's style guideline `UseOfOptional`,

> `Optional` should be used when it isn't obvious to a caller whether a result will be null.

we make `response` and `exception` optional in `WaiterResponse` because only one of them can be present and it cannot be determined which is present at compile time.

The following example shows how to retrieve a response from `WaiterResponse`

```java
waiterResponse.matched.response().ifPresent(r -> ...);

```

Another approach is to create a flag field, say `isResponseAvailable`, to indicate if the response is null or not. Customers can check this before accessing `response` to avoid NPE.

```java
if (waiterResponse.isResponseAvailable()) {
   DescribeTableResponse response = waiterResponse.response();
   ...
}

```

The issue with this approach is that `isResponseAvailable` might not be discovered by customers when they access `WaiterResponse` and they'll have to add null pointer check, otherwise they will end up getting NPEs. It also violates our guideline for the use of optional.

## References

Github feature request links: 
- [Waiters](https://github.com/aws/aws-sdk-java-v2/issues/24)
- [Async requests that complete when the operation is complete](https://github.com/aws/aws-sdk-java-v2/issues/286) 

