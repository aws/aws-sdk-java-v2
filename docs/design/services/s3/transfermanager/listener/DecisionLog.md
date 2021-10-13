# Decision Log for v2 Transfer Manager Progress Listeners

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** (names)

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 9/21/21

**Source:** Meeting to discuss implementing progress listeners for [TransferManager v2](https://github.com/aws/aws-sdk-java-v2/tree/master/docs/design/services/s3/transfermanager)

**Attendees:** Anna-Karin, Bennett, Dongie, John, Matt, Zoe

**Closed Decisions:**

1. **Should ExecutionListener be implemented (under the hood) as an ExecutionInterceptor?**
    1. We believe yes, but that would require ExecutionListener's API to inherently be a sub-set of ExecutionInterceptor,
       which may be a limitation. Where needed, we can consider *adding* methods to the Interceptor interface to support
       the Listener interface. Furthermore, we can consider migrating some parts of the metric implementation to be
       Listener-based.
1. **Should `TransferProgress` be mutable or immutable?**
    1. We want to strive for the consistent v2 design tenant of immutability, but `TransferProgress` is often used in a
    poll-based way to check the status of a transfer. We agreed that `TransferProgress` should be immutable, but we
    could implement this by taking a *snapshot* of an immutable object, i.e., `TransferProgressSnapshot`.
1. **What level of CRT support/integration is needed?**
    1. We believe that the CRT's [ResponseDataConsumer](https://github.com/awslabs/aws-crt-java/blob/main/src/main/java/s3NativeClient/com/amazonaws/s3/ResponseDataConsumer.java#L22)
    may be sufficient to implement the proposed TransferListener interface. Therefore we believe we can defer the
    implementation of ExecutionListeners until required.

**Open Decisions:**

1. **Do we need the functionality proposed in `ExecutionMode`?**
    1. Unknown. We may wish to reserve it only for async clients (which already have dedicated thread pools), and it may
       be cleaner to allow users to define their own offloading semantics. We also would need consistent with allowing
       users to provide async future completion, where Runnable::run would be analogous to the proposed "INLINE".
       Deferring this idea until we dive deeper.