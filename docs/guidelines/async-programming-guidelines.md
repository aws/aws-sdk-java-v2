# Asynchronous Programming Guidelines for AWS SDK v2

## CompletableFuture Guidelines

### Best Practices for CompletableFuture

- **Read the documentation**: Always read the [CompletionStage Javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) to understand the nuances of CompletableFuture
- **Prefer Non-Blocking Methods for Getting Results**:

```java
    // Avoid when possible - blocks the current thread
    String result = future.get();

    // Better - use callbacks
    future.thenAccept(result -> processResult(result));
```
- **Add stacktrace to exceptions**: When using `CompletableFuture#join`, use `CompletableFutureUtils#joinLikeSync` to preserve stacktraces
- **Don't ignore results**: Never ignore the result of a new `CompletionStage` if the parent stage can fail (unless you're absolutely sure it's safe to)
- **Don't make thread assumptions**: Never make assumptions about which thread completes the future
  - CompletableFuture callbacks may execute on:
    - The thread that completed the future
    - A thread from the common ForkJoinPool (for async methods without explicit executor)
    - A thread from a provided executor
  - Thread behavior can vary based on:
    - Whether the future is already completed when a callback is added
    - Whether an async or non-async method is used (e.g., `thenApply` vs `thenApplyAsync`)
    - The specific JVM implementation and platform
  - Always ensure thread safety when:
    - Accessing shared state in callbacks
    - Modifying UI components (use the appropriate UI thread)
    - Working with thread-affinity resources like ThreadLocals
  - Example of incorrect assumption:
  ```java
  // INCORRECT: Assuming the callback runs on a specific thread
  ThreadLocal<Context> contextHolder = new ThreadLocal<>();
  
  public void processAsync(CompletableFuture<Response> responseFuture) {
      Context context = new Context();
      contextHolder.set(context);  // Set in current thread
      
      responseFuture.thenApply(response -> {
          // WRONG: Assuming contextHolder still has the context
          Context ctx = contextHolder.get();  // May be null if running on different thread!
          return processWithContext(response, ctx);
      });
  }
  ```
  - Correct approach:
  ```java
  // CORRECT: Explicitly passing context to callback
  public void processAsync(CompletableFuture<Response> responseFuture) {
      Context context = new Context();
      
      responseFuture.thenApply(response -> {
          // Explicitly use the context passed from the outer scope
          return processWithContext(response, context);
      });
  }
  ```
- **Always provide custom executors**: Don't use `CompletableFuture#xxAsync` methods (like `runAsync` or `thenComposeAsync`) without providing a custom executor, as the default `ForkJoinPool.commonPool()` behavior can vary by platform
- **Handle cancellation properly**: CompletableFuture does not support automatic cancellation propagation, so use `CompletableFutureUtils#forwardExceptionTo` to manually propagate cancellation
- **Avoid chaining multiple API calls**: This can lead to cancellation issues without proper handling
- **Avoid blocking operations**: Never use `get()` or `join()` inside a CompletableFuture chain as it defeats the purpose of asynchronous execution
- **Handle exceptions properly**: Always include exception handling with `exceptionally()` or `handle()` methods
```java
   CompletableFuture.supplyAsync(() -> fetchData())
       .exceptionally(ex -> {
           logger.error("Error processing data", ex);
           return fallbackData();
       }, executor);
```
- **Use appropriate completion methods**:
  - whenComplete() - when no transformation is needed, but you need to perform cleanup or side effects with access to both result and exception
  - handle() - when you need to transform both successful results and exceptions into a new result
  - exceptionally() - when you only need to handle exceptional cases
  - thenApply() - when transforming a result
  - thenAccept() - when consuming a result without returning anything
  - thenRun() - when executing code regardless of the result
  - thenCompose() - when the next step returns a CompletableFuture
- **Test asynchronous code properly**:
  - Use `CompletableFuture.join()` in tests to wait for completion
  - Set appropriate timeouts for tests

## Related Guidelines

For reactive streams implementation guidelines, see [Reactive Streams Implementation Guidelines](reactive-streams-guidelines.md).

## References

- [CompletableFuture JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- [CompletionStage JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)