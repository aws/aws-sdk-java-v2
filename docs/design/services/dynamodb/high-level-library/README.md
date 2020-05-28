**Design:** New Feature, **Status:** [Public Preview](../../../../../services-custom/dynamodb-enhanced/README.md)

## Tenets (unless you know better ones)

1. Meeting customers in their problem space allows them to deliver value
   quickly.
2. Meeting customer expectations drives usability.
3. Discoverability drives usage.
4. Providing a Java-focused experience for DynamoDB reduces the coding
   effort required to integrate with DynamoDB. 
5. Reusing the same nouns and verbs as the generated DynamoDB client
   meets customer expectations. 
6. Optimizing for cold-start performance allows customers the
   convenience of using object mapping in a Lambda environment.

## Problem

Customers on the AWS SDK for Java 2.x currently use the `DynamoDbClient`
to communicate with DynamoDB. This client is generated from the model
provided by the DynamoDB team.

Because this client is generated, it does not provide an idiomatic Java
experience. For example: (1) the client represents numbers as `String`
instead of the more idiomatic `Number`, (2) customers must manually
convert common Java data types like `Instant` into types supported by
DynamoDB, (3) customers that represent their DynamoDB objects using Java
objects must manually convert these objects into the item representation
supported by DynamoDB.

## Existing Solutions

This problem is not currently addressed directly in the AWS SDK for Java
2.x by any known third-party tool. In 1.11.x, several solutions exist,
including AWS's own Document and Mapper Clients.

## Proposed Solution

The AWS SDK for Java will add a new "enhanced DynamoDB client" that
provides an alternative to the data-access portion of the generated
DynamoDB APIs. Only limited control-plane operations will be available,
specifically 'createTable'.

This enhanced client will make DynamoDB easier to use for Java customers
by:
1. Supporting conversions between Java objects and DynamoDB items
2. Directly supporting every data-plane operation of DynamoDB
3. Using the same verbs and nouns of DynamoDB
4. Support for tests, such as the ability to create tables using the
   same models as the data plane operations.

A fully functional public preview is available for this library. See
[DynamoDb Enhanced Public Preview
Library](../../../../../services-custom/dynamodb-enhanced/README.md).
   
## Appendix A: Requested Features

* [Immutable classes](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-315049138)
* [Getter/setter-less fields](https://github.com/aws/aws-sdk-java/issues/547)
* [Replace `PaginatedList` with `Stream`](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-318051305)
* [Allow 'setters' and 'getters' to support different types](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-318792534)
* [Have 'scan' respect the table's read throughput](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-329007523)
* [Allow creating a table with an LSI that projects all attributes](https://github.com/aws/aws-sdk-java/issues/214#issue-31304615)
* [Projection expressions in 'load' and 'batchLoad'](https://github.com/aws/aws-sdk-java/issues/527)
* [New condition expressions](https://github.com/aws/aws-sdk-java/issues/534)
* [Accessing un-modeled/dynamic attributes in a POJO](https://github.com/aws/aws-sdk-java/issues/674)
* [Inheritance](https://github.com/aws/aws-sdk-java/issues/832)
* [Service-side metrics](https://github.com/aws/aws-sdk-java/issues/953)
  ([1](https://github.com/aws/aws-sdk-java/issues/1170),
  [2](https://github.com/aws/aws-sdk-java-v2/issues/703),
  [3](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-417656448))
* [Merging DynamoDB mapper configurations](https://github.com/aws/aws-sdk-java/issues/1201)
* [Cache merged DynamoDB mapper configurations](https://github.com/aws/aws-sdk-java/issues/1235)
* [Create one single type converter interface](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-330616648)
* [Support `@DynamoDBGeneratedUuid` in objects nested within lists](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332958299)
* [Allow annotating fields in addition to methods](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332968651)
* [Non-string keys in maps](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332974427)
* [Multiple conditions on the same attribute, for save/delete](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-342586344)
* [Persisting public getters from package-private classes](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-343006566)
* [Return modified attributes when doing a save](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-417656448)
* [More direct exposure of scan or filter expressions](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-430993224)
* [Transactions support](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-443308198)
* [Creating an Item from JSON (and vice-versa)](https://github.com/aws/aws-sdk-java-v2/issues/1240)
* Straight-forward support for multiple classes in a single table (as
  per
  [here](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-general-nosql-design.html))
  (from email)
* Support for `Optional` (from email)
* Support for `Publisher` for async paginated responses (from email)
* Create a table with partial projections (from email)
* Better integration with DynamoDB streams (from email)
* Configuring table auto-scaling when a table is created (from email)
* Request-level credentials (from email)
* Wrappers for transactional isolation (from email)
* Dynamic attributes - ones with different types depending on the value
  of other attributes, or attributes with names that are generated at
  runtime (from email)
* Structure versioning (from email)

## Appendix B: Alternative Solutions

### Alternative Solution 1: Level 3 Storage Library

A "Level 2" high-level library is a service-specific library built on
top of the "Level 1" generated client. The solution proposed above is a
Level 2 high-level library for DynamoDB.

A "Level 3" high-level library focuses on a specific customer problem
instead of a specific AWS service. For example, customers frequently use
DynamoDB to store time series data. An alternate to the proposed
solution above, would be to build multiple Level 3 libraries, each
focusing on a specific customer problem: a document database library, a
time series database library, etc. These libraries would support
DynamoDB as one of many backing data stores.

Instead of using traditional DynamoDB nouns and verbs (e.g. Item), a
Level 3 library would use words more aligned to the problem domain (e.g.
Document for document databases or Entry for time-series data). They
would also expose operations more constrained to the problem domain they
were trying to solve, instead of trying to expose every piece of
DynamoDB functionality.

This solution would be better for customers that are more familiar with
the problem they are trying to solve and less familiar with DynamoDB.
This solution would be worse for customers that are familiar with
DynamoDB and want to be "closer" to the service.

**Customer Feedback**

The Java SDK team collected customer feedback internally and
[externally](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-468435660),
comparing this alternate solution against the proposed solution.
Customers were presented with the following option comparison:

> Option 1: A DynamoDB-specific client that combines the functionality
> of 1.11.x's Documents APIs and DynamoDB Mapper APIs in a
> straight-forward manner. 

> Option 2: A generic document database client that creates an
> abstraction over all document databases, like DynamoDB and MongoDB.
> This would simplify using multiple document databases in the same
> application, and make it easier to migrate between the two.
> Unfortunately as a result, it also wouldn't be a direct DynamoDB
> experience.

We requested that customers review these two options as well as a
[prototype of option 1](prototype/option-1/sync/Prototype.java) and a
[prototype of option 2](prototype/option-2/sync/Prototype.java), to let
us know which they prefer.

The following anecdotes are from this customer feedback:

> If \[Amazon] can make something like https://serverless.com/ or
> https://onnx.ai/ which free customers from vendor lock-in, that would
> be a great Think Big & Customer Obsession idea. If \[Amazon] cannot,
> I feel that somebody who is more vendor-neutral can make a better
> mapper than \[Amazon].

> Have you thought about contributing to projects which already exist,
> like Spring Data? https://github.com/derjust/spring-data-dynamodb 

> Both options would work well for us.

> I think \[doing option 1 and then creating a Spring Data plugin] might
> get adoption from a broader audience than option 2. It could be used
> as a stepping stone to move to DynamoDB.

> I believe Option 2 does not make much sense. It would make sense to me
> to go for Option 1 and start a bounty program to implement a module to
> popular data access abstraction libraries such as spring-data
> mentioned above or GORM.

> Maybe you could implement/support JNOSQL spec http://www.jnosql.org/

**Decision**

Based on customer feedback, it was decided to temporarily reject
alternative solution 1, and build the proposed solution. At a later
time, the SDK may build a Level 3 abstraction for DynamoDB or integrate
with existing Java Level 3 abstractions like Spring Data, Hibernate OGM,
and/or JNoSQL. This Level 3 abstraction will possibly leverage the Level
2 solution "under the hood".

## Links

**[Features](features.md)** - The features intended for inclusion during
and after the launch of the enhanced DynamoDB client.

**Prototypes**

During the design of the project, two prototype interfaces were created
to solicit feedback from customers on potential design directions.

* [Prototype 1](prototype/option-1/sync/Prototype.java) - A DynamoDB
  specific API that focuses on making DynamoDB easy to use from Java.
* [Prototype 2](prototype/option-2/sync/Prototype.java) - A
  DynamoDB-agnostic API that focuses on creating a generic document
  database abstraction, that could be backed by DynamoDB or other
  document databases.
  
**Feedback**

* [DynamoDB Mapper Feature Request](https://github.com/aws/aws-sdk-java-v2/issues/35)
  \- A github issue for tracking customer feature requests and feedback
  for DynamoDB mapper-equivalent functionality in 2.x.
* [DynamoDB Document API Feature Request](https://github.com/aws/aws-sdk-java-v2/issues/36)
  \- A github issue for tracking customer feature requests and feedback
  for DynamoDB document API-equivalent functionality in 2.x.