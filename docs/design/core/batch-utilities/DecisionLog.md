# Decision Log for SDK V2 Batch Utility

## Log Entry Template

* * *
**Source**: (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees**: Anna-Karin, Irene, Dongie, Matt, Vinod, John, Zoe, Debora, Bennett, Michael

**Closed Decisions:**

1. Question? Decision. Justification

**Open Decisions:**

1. (Old/Reopened/new) Question?

## 6/29/21

* * *
**Source:** Meeting to discuss initial design for Batch Utility: https://github.com/aws/aws-sdk-java-v2/pull/2563

**Attendees:** Dongie, Matt, Vinod, John, Zoe, Debora, Bennett, Michael

**Closed Decisions:**

1. Should we implement batching methods directly on the client or in a separate utility as proposed in the design document? Separate utility. This would be consistent with other APIs (like Waiters). If we want to change this, we would have to change all APIs and we might as well do it all together.
2. Should we use a `batch.json` or `batcher.json` to store default values and service-specific batch methods? Yes. We do not want to place the burden of providing these values on the customer, and this will be consistent with other APIs like Waiters. We will consider implementing this across the SDK.
3. Should we create a wrapper class for `CompletableFuture<BatchResponse<SendMessageBatchResultEntry>>`? Yes. We should create a wrapper class like `SQSBatchResponse` in order to avoid nesting generics and make it easier to understand for Customers.
4. Should we include a manual flush and the option to flush a specific buffer? Yes. This will have parity with v1 and give Customers additional functionality that they may need.
5. Should batch retries be handled by the client? Yes. Retries should be handled by the client as is done throughout the SDK. These retries could possibly be batched as well.
6. Should we have separate sync and async interfaces if they look so similar? Yes. We expect the interfaces to diverge as more features are added. The builders are also different.
7. Should async client handle throttling exceptions by just sending requests one after another? No. This would defeat the purpose of an async client. Instead, the number of requests sent will only be limited by the maximum number of connections allowed by the client. Additionally, the low level clients already provide throttling support.

**Open Decisions:**

1. (New) Should the batch utility only use a `sendMessage()` method that only sends one request for a 1:1 correlation between request and response?
    1. If not, how will Customers using `sendMessages()` correlate the request message with the response messages?
2. (New) Should we accept streams or iterators to `sendMessages()`?
    1. Note: A decision on this runs counter to open decision #1. i.e. If we decide on a 1:1 correlation between request and response, we will not accept streams or iterators and vice versa.
3. (New) What should we name the batching utility to make it more discoverable and easy to understand?

