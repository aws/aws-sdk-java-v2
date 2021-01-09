**Design:** Convention, **Status:** [Accepted](README.md)

## Use of CompletableFuture

Operations on the asynchronous clients return [`CompleteableFuture<T>`][1] where `T` is the response type for the operation. This is somewhat curious in that [`CompleteableFuture`][1] is a concrete implementation rather than an interface. The alternative to returning a [`CompleteableFuture`][1] would be to return a [`CompletionStage`][2], an interface intended to allow chaining of asynchronous operations.

The key advantage of [`CompleteableFuture`][1] is that it implements both the [`CompletionStage`][2] and [`Future`][3] interfaces - giving users of the SDK maximum flexibility when it comes to handling responses from their asynchronous calls.

Currently [`CompleteableFuture`][1] is the only implementation of [`CompletionStage`][2] that ships with the JDK. Whilst it's possible that future implementations will be added  [`CompletionStage`][2] will always be tied to [`CompleteableFuture`][1] via the  [`#toCompletableFuture`][4] method. Additionally, [`CompleteableFuture`][1] is not a `final` class and thus could be extended if there was a requirement to do so.

One of the perceived risks with exposing [`CompleteableFuture`][1] rather than [`CompletionStage`][2] is that a user of the SDK may spuriously call [`#complete`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#complete-T-) or [`#completeExceptionally`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#completeExceptionally-java.lang.Throwable-) which could cause unintended consequences in their application and the SDK itself. However this risk is also present on the [`CompletionStage`][2] via the [`#toCompletableFuture`][4] method.

If the [`CompletionStage`][2] interface did not have a [`#toCompletableFuture`][4] method the argument for using it would be a lot stronger, however as it stands the interface and its implementation are tightly coupled.

Using [`CompleteableFuture`][1] gives users the best bang for their buck without much additional risk.

[1]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
[2]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html
[3]: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html
[4]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#toCompletableFuture--
