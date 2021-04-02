# Decision Log for SDK V2 RequestBody Content-Length

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** Anna-Karin, Dongie, Nico, John, Zoe

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 4/8/2021

**Source:** Meeting to discuss how to address the deprecated content-length getter after adding a new `Optional<Long>` getter in sync RequestBody: https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/sync/RequestBody.java#L66-L71

**Attendees:** Anna-Karin, Dongie, Nico, John, Zoe

**Closed Decisions:**

1. Should we replace the old content-length getter with a new optional getter? No, because that's going to be a breaking change, adding a Deprecated annotation is more acceptable.
2. Should we use a negative value to denote the null content-length? Yes, because a negative value can be distinguished from normal zero content-length.
3. Should we throw an exception in the deprecated getter when the content-length is negative? Yes, because with an exception the customers using this deprecated method would be notified that this method is deprecated and start to use the `Optional<Long>` getter. On the other hand, since the content-length was always greater or equal to zero before this change, throwing an exception here wouldn't break any existed customers.
   
**Open Decisions:**

None
