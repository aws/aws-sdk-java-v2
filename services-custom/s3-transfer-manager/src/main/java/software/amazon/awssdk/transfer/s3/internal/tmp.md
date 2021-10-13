**This is a temporary file for tracking development notes. It will be deleted before merging to the master branch.**

1. Handle retries in reactive stream delegation logic (and add corresponding tests)
1. Do we have existing, reusable support for delegating reactive streams? `EventListeningSubscriber` offers similar support but was missing some features. See if we can consolidate.
1. Do we want listener interface to be generic for `Upload`/`Download`, or should it just contain a `Transfer`?
1. Should `TransferFailed` extend `TransferInitiated`? Will we always consider a failure at least "initiated"?
1. Should `TransferProgress` be a class or interface? (currently: interface, so we can "hide" update method)
1. Should `TransferProgressSnapshot` be a class or interface? (currently: class)
1. What should `percentageTransferred()` return for a 0 byte object? 0%, 100%, `Optional.empty()`?
1. Should we call it `totalTransferSize`, `transferSize`, or `contentLength`?
1. `totalBytesTransferred` vs `bytesTransferred`?
1. Are we correct to distinguish between `ratioTransferred` vs `percentageTransferred`? Will the return value of `percentageTransferred` surprise any users?
1. Should the `Context` wrapper class reside inside `TransferListener` or outside? Outside is consistent w/ `ExecutionInterceptor`'s `Context`, but prevents having 2 `Context` objects in the same package.
1. Should we have a callback for when the `content-length` is discovered on a Get?
1. Also provide option to declare listeners at the TM/client level?
1. Look to minimize work if no listeners are declared
1. Include timestamps on snapshot? Can provide some helpful utility, but may be expensive to call `Instant.now()` too frequently. Also, things like "elapsed time" and "estimated time remaining" may be misleading if a snapshot is referenced some time after it was created.