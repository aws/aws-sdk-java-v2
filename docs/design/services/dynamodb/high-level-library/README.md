**Design:** New Feature, **Status:**
[In Development](../../../README.md)

# SDK Tenets (unless you know better ones)

1. Meeting customers in their problem space allows them to deliver value
   quickly.
2. Meeting customer expectations drives usability.
3. Discoverability drives usage.

# Project Tenets (unless you know better ones)

1. Providing a Java-focused experience for DynamoDB reduces the coding 
   effort required to integrate with DynamoDB.
2. Reusing the same nouns and verbs as the generated DynamoDB client
   meets customer expectations.
3. Optimizing for cold-start performance allows customers the
   convenience of using object mapping in a Lambda environment.

# Project Introduction

The enhanced DynamoDB client replaces the generated DynamoDB client with
one that is easier for a Java customer to use. It does this by
supporting conversions between Java objects and DynamoDB items, as well
as converting between Java built-in types (eg. java.time.Instant) and
DynamoDB attribute value types.

The enhanced DynamoDB client intentionally does not attempt to simplify
specific data access patterns, like relational or time-series data. It
is within the scope of future projects to provide data access
pattern-specific abstractions on top of the enhanced DynamoDB client
and/or other AWS services.

# Links

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

# FAQ

**Why not optimize for specific data access patterns, like relational
data?**

Some customers prefer to think about the DynamoDB-specific concepts of
tables, queries, conditions and global secondary indices. These
customers currently have trouble interacting with DynamoDB, because they
have to perform a large amount of conversion between their Java types
and DynamoDB types. 

If we were to optimize this specific project for a particular access
pattern, customers that have other access patterns would either be
forced to modify their access pattern into the one we've optimized for,
or they would need to use the generated client and handle their own Java
type conversion.

Instead, this project focuses on making integrating with DynamoDB easier
to do in Java, and leaves the problems of "making storing time series
data on AWS easy" or "making storing documents on AWS easy" to another
project.
