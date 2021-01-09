## Design Documentation

### New Features

**In Development**

*Development on these features has started. No date can be shared for
when these features will be completed, and their end state may vary
significantly from the state proposed in these documents.*

* [S3 Transfer Manager](services/s3/transfermanager/README.md) -
  Simplifies uploading and downloading of objects to and from Amazon S3.
* [Dynamo DB Enhanced Client](services/dynamodb/high-level-library/README.md)
  \- Simplifies writing and reading objects to and from Amazon DynamoDB.

**Proposed**

*These features are being proposed for development. These features will
either transition to "In Development" when development begins, or
"Rejected" if it is decided that these features will not be implemented.
No date can be shared for when the fate of these features will be
decided.*

* [Event Streaming Alternate Syntax](core/event-streaming/alternate-syntax/README.md)
  \- Simplifies interacting with event streaming services for
  non-power-users.
* [Event Streaming Auto-Reconnect](core/event-streaming/reconnect/README.md)
  \- Automatically reconnects to an event streaming session when they are
  interrupted by a network error.

**Released**

*These features are considered "mostly implemented". Development on new features 
is never "done". These features are considered "done enough" that any remaining 
design elements or features can be implemented incrementally based on customer 
demand.*

* [Request Presigners](core/presigners/README.md) - Makes it possible to sign 
  requests to be executed at a later time. 

**Rejected**

None

### Conventions

**Proposed**

None
 
**Accepted**

* [Class Initialization](FavorStaticFactoryMethods.md) - Conventions
  used to initialize a class.
* [Naming Conventions](NamingConventions.md) - Conventions used for
  class naming.
* [Client Configuration](ClientConfiguration.md) - Conventions used for
  client configuration objects. 
* [Optional Usage](UseOfOptional.md) - Conventions governing the use of
  [java.util.Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html).
* [Completable Future Usage](UseOfCompletableFuture.java) - Conventions
  governing the use of
  [java.util.concurrent.CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html).

**Rejected**

None

