# Decision Log for SDK V2 Waiters

Note: The decision log process was implemented late in this project, so decisions earlier than 9/22/20 are not included 
below.

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** Anna-Karin, Ben, Dongie, Irene, Matt, Nico, Vinod, John, Zoe

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 9/22/20

**Source:** Meeting to review API surface-area of the waiters implementation https://github.com/aws/aws-sdk-java-v2/tree/waiters-development

**Attendees:** Anna-Karin, Ben, Dongie, John, Matt, Nico, Vinod, Zoe

**Closed Decisions:**

1. Should we change the default `ScheduledExecutorService` core number? Yes, it should be changed to 1 because it's only used to schedule attempts and 5 seems to be a lot.
2. Should we relax the validation of `DynamodbEnhancedClient#Builder` and update `DynamodbEnhancedClient.builder().build()` to create a default SDK client? Yes, because `DynamodbEnhancedClient.builder().build` should be equivalent to `DynamodbEnhancedClient.create()`
3. Should we make the generic `Waiter` non-public? No, because it is essentially a protected API and has to be backwards-compatible any way. In addition, customers can benefit from it
when service waiters are not available.
4. Should we create a union type `ResponseOrException`? Yes, because it clearly indicates response and exception are mutually exclusive and only one is present. We should keep the naming `ResponseOrException` because it's intuitive and descriptive. 
5. Should we rename `WaiterResponse#responseOrExecption`? Yes, we should rename it to `WaiterResponse#matched` because it has the most votes compared with other options: `matchedResponse`, `matchedResult`, `matchedValue`
6. Should we rename `{Service}Waiter.Builder#executorService`? Yes, we should rename it to `{Service}Waiter.Builder#scheduledExecutorService` to be more clear it's a `ScheduledExecutorService`
7. Should we rename `Waiter.Builder#pollingStrategy`? Yes, we should rename it to `Waiter.Builder#waiterOverrideConfiguration` so that we can easily add more configurations if needed. All configs under `WaiterOverrideConfiguration` should have default values to be consistent with `ClientOverrideConfiguration`
8. Should we support per request waiter configuration override? Yes, it's a reasonable feature that we should support by creating an overloaded method which takes a `WaiterOverrideConfiguration` parameter to allow request-level config override for every waiter operation, eg: `DynamodbWaiter#waitUntilTableExists(DescribeTableRequest, WaiterOverrideConfiguration)`? Yes, 
   
**Open Decisions:**

None