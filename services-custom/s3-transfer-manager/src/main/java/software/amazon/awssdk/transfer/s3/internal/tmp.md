**This is a temporary file for tracking development notes. It will be deleted before merging to the master branch.**

1. Handle retries in reactive stream delegation logic (and add corresponding tests)
1. Do we have existing, reusable support for delegating reactive streams? `EventListeningSubscriber` offers similar support but was missing some features. See if we can consolidate.
1. Also provide option to declare listeners at the TM/client level?
1. Look to minimize work if no listeners are declared
1. Include timestamps on snapshot? Can provide some helpful utility, but may be expensive to call `Instant.now()` too frequently. Also, things like "elapsed time" and "estimated time remaining" may be misleading if a snapshot is referenced some time after it was created.