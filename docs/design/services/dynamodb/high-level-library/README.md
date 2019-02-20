# Project Tenets (unless you know better ones)

1. Meeting customers in their problem space allows them to deliver value
   quickly.
2. Meeting customer expectations drives usability.
3. Discoverability drives usage.

# Introduction

This project provides a Dynamo DB experience that is easier to use and
understand than can be provided by the generated Dynamo DB client. It
provides object-oriented features, like persisting and updating Java
data objects, as well as expressive data retrieval operations.

# Project Goals

1. The high-level library should be the ideal way to integrate with
   Dynamo DB.
2. The nouns and verbs used by the high-level library should match those
   in Dynamo DB.
3. The high-level library should use and build on the idiomatic norms in
   the rest of the 2.x Java SDK.
4. The high-level library should perform well in cold JVMs.
5. Using the low-level Dynamo DB client should be a conscious choice by
   the customer, not an accidental one.
   
# Customer-Requested Changes from 1.11.x

* All built-in Java types should be supported without type converters.
* There should be just one method of type conversion that can be
  extended to support any reasonable, foreseeable customer use-case.
* The high-level library should support immutable object representations
  from the customer.
* The high-level library should support object inheritance.
* The high-level library should support transactional writes.
* The high-level library should support non-blocking access patterns.
* The high-level library should expose low-level client metadata, like
  consumed capacity and metrics.