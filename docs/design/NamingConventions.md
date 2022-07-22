**Design:** Convention, **Status:** [Accepted](README.md)

## Naming Conventions

This page describes the naming conventions, nouns and common terms 

### Class Naming

#### General Rules
* Prefer singular class names: `SdkSystemSetting`, not `SdkSystemSettings`.
* Treat acronyms as a single word: `DynamoDbClient`, not `DynamoDBClient`.
  
#### Classes that instantiate other classes

* If the class's primary purpose is to return instances of another class:
  * If the "get" method has no parameters:
    * If the class implements `Supplier`: `{Noun}Supplier` (e.g. `CachedSupplier`)
    * If the class does not implement `Supplier`: `{Noun}Provider` (e.g. `AwsCredentialsProvider`)
  * If the "get" method has parameters: `{Noun}Factory` (e.g. `AwsJsonProtocolFactory`)

#### Service-specific classes

* If the class makes service calls:
  * If the class can be used to invoke *every* data-plane operation:
    * If the class is code generated:
      * If the class uses sync HTTP: `{ServiceName}Client` (e.g. `DynamoDbClient`)
      * If the class uses async HTTP: `{ServiceName}AsyncClient` (e.g. `DynamoDbAsyncClient`)
    * If the class is hand-written:
      * If the class uses sync HTTP: `{ServiceName}EnhancedClient` (e.g. `DynamoDbEnhancedClient`)
      * If the class uses async HTTP: `{ServiceName}EnhancedAsyncClient` (e.g. `DynamoDbEnhancedAsyncClient`)
  * If the class can be used to invoke only *some* data-plane operations:
    * If the class uses sync HTTP: `{ServiceName}{Noun}Manager` (e.g. `SqsBatchManager`)
    * If the class uses async HTTP: `{ServiceName}Async{Noun}Manager` (e.g. `SqsAsyncBatchManager`)
    * Note: If only the only implementation uses async HTTP, `Async` may be excluded. (e.g. `S3TransferManager`)
* If the class does not make service calls:
  * If the class creates presigned URLs: `{ServiceName}Presigner` (e.g. `S3Presigner`)
  * If the class is a collection of various unrelated "helper" methods: `{ServiceName}Utilities` (e.g. `S3Utilities`)

### Tests Naming

Test names SHOULD follow `methodToTest_when_expectedBehavior` (e.g. `close_withCustomExecutor_shouldNotCloseCustomExecutor`, `uploadDirectory_withDelimiter_filesSentCorrectly`)