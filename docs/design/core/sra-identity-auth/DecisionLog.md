# Decision Log for Smithy Reference Architecture Identity and Auth support 

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** (names)

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 3/31/23

**Source:** Meeting for API surface area review of Identity changes made as part of SRA.

**Attendees:** Anna-Karin, David, Debora, Dongie, Jay, John, Matt, Olivier, Zoe

**Closed Decisions:**

1. **Should the new interface `AwsCredentialsIdentity` provide `create()` methods?**
   1. Yes, to provide customers a way to easily create instances of this type without needing to depend on 
      `AwsBasicCredentials` from the `auth` module. Some duplication of code from `AwsBasicCredentials` is okay.
   2. The implementation of `create()` can use an anonymous inner class, instead of creating a new class with a name.
2. **How should `AwsCredentialsProviderChain` support the new Identity type `AwsCredentialsIdentity`?**
   1. `Builder.addCredentialsProvider()` can be overloaded to accept the new type.
   2. The varargs methods `of()` and `Builder.credentialsProviders()` can be overloaded to accept the new type. This
      would not be ambiguous when called with zero args, because according to
      https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2.5 the more specific method is chosen, 
      i.e., `AwsCredentialsProviderChain.of()` would invoke `of(AwsCredentialsProvider...)`.
   3. The `Builder.credentialsProviders()` method accepting a `Collection` cannot be overloaded because both methods
      would have the same erasure. So use a method with a different name - `credentialsIdentityProviders()`. We don't
      want to add `Identity` to the other methods (varargs, `add`, `of`) too, as it might mislead to thinking that's a 
      different "property" of the chain. So a separate method name is used only in this one-off.
3. **How should an `IdentityResolver` define which `IdentityProperty`s it supports?**
   1. An `IdentityResolver` should define a `public static` for each `IdentityProperty` it supports and document the
      behavior of how it uses it during `resolveIdentity`. This will help the caller determine how to construct an
      appropriate `ResolveIdentityRequest`. 
   2. Should there be stronger abstraction for any property?
      1. We discussed potential use cases like metrics collector / telemetry. If we have a compelling use case, we can
         add it later. Though it would have to be not AWS specific to be added to these generic interfaces.

**Open Decisions:**

None