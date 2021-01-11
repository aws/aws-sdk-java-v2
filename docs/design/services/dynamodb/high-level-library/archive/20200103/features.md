# Launch Features

See [Prototype 1](prototype/option-1/sync/Prototype.java) for an early
prototype of the customer-facing APIs.

* A `software.amazon.aws:dynamodb-all` module, including a gateway class
  that can create generated DynamoDB and enhanced DynamoDB clients.
* A `software.amazon.awssdk:dynamodb-enhanced` module, including all of
  the DynamoDB enhanced client classes.
* Support for the following DynamoDB control-plane resources: tables,
  global tables, global secondary indexes.
* Support for the following DynamoDB data-plane operations: get, put,
  query, scan, delete, update, batchGet, batchWrite, transactGet,
  transactWrite.
* Support for object projections.
* An Item abstraction that replaces the Map<String, AttributeValue>
  abstraction of the generated client.
* An ItemAttributeValue abstraction that replaces the AttributeValue
  abstraction of the generated client.
* Converters between built-in Java types and ItemAttributeValues.
* Converters between custom Java beans and ItemAttributeValues.
* The ability to register custom type converters.
* Exposure of low-level client metadata, like consumed capacity and
  metrics.

# Post-Launch Features

* Support for inheritance of custom Java beans.
* Converters between custom POJOs that do not match the Java bean
  standard and ItemAttributeValues.
* Support for the following DynamoDB control-plane resources: backups,
  continuous backups, limits, TTLs, tags.
  
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

# FAQ

**Why is dirty data tracking and persistence in the load-modify-save
scenario not on the feature list?**

From an implementation standpoint, this requires the SDK to track which
fields have been modified. This complexity is better left to a (future)
higher level of abstraction, while this library focuses entirely on the
problem of type conversions.

