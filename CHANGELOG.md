# __2.0.0-preview-6__ __2017-12-06__
## __AWS AppSync__
  - ### Features
    - Adding AWS AppSync based on customer request. [#318](https://github.com/aws/aws-sdk-java-v2/pull/318)

## __AWS Lambda__
  - ### Removals
    - Removed high-level utilities. [#247](https://github.com/aws/aws-sdk-java-v2/pull/247)

## __AWS SDK for Java v2__
  - ### Features
    - Add paginators-1.json file for some services [#298](https://github.com/aws/aws-sdk-java-v2/pull/298)
    - Added a primitive `Waiter` class for simplifying poll-until-condition-is-met behavior. [#300](https://github.com/aws/aws-sdk-java-v2/pull/300)
    - Adding Consumer<Builder> to overrideConfiguration on ClientBuilder [#291](https://github.com/aws/aws-sdk-java-v2/pull/291)
    - Adding helper to Either that allows construction from two possibly null values [#292](https://github.com/aws/aws-sdk-java-v2/pull/292)
    - Adding knownValues static to enum generation [#218](https://github.com/aws/aws-sdk-java-v2/pull/218)
    - Adding validation to Region class [#261](https://github.com/aws/aws-sdk-java-v2/pull/261)
    - Converted all wiremock tests to run as part of the build. [#260](https://github.com/aws/aws-sdk-java-v2/pull/260)
    - Enhanced pagination for synchronous clients[#207](https://github.com/aws/aws-sdk-java-v2/pull/207)
    - Implementing Consumer<Builder> fluent setter pattern on client operations [#280](https://github.com/aws/aws-sdk-java-v2/pull/280)
    - Implementing Consumer<Builder> fluent setters pattern on model builders. [#278](https://github.com/aws/aws-sdk-java-v2/pull/278)
    - Making it easier to supply async http configuration. [#274](https://github.com/aws/aws-sdk-java-v2/pull/274)
    - Refactoring retry logic out to separate class [#177](https://github.com/aws/aws-sdk-java-v2/pull/177)
    - Removing unnecessary javax.mail dependency [#312](https://github.com/aws/aws-sdk-java-v2/pull/312)
    - Replacing constructors with static factory methods [#284](https://github.com/aws/aws-sdk-java-v2/pull/284)
    - Retry policy refactor [#190](https://github.com/aws/aws-sdk-java-v2/pull/190)
    - Update latest models for existing services [#299](https://github.com/aws/aws-sdk-java-v2/pull/299)
    - Upgrade dependencies to support future migration to Java 9. [#271](https://github.com/aws/aws-sdk-java-v2/pull/271)
    - Upgraded dependencies:
      * javapoet 1.8.0 -> 1.9.0 [#311](https://github.com/aws/aws-sdk-java-v2/pull/311)
      * Apache HttpClient 4.5.2 -> 4.5.4 [#308](https://{github.com/aws/aws-sdk-java-v2/pull/308)
      * Jackson 2.9.1 -> 2.9.2 [#310](https://github.com/aws/aws-sdk-java-v2/pull/310)
      * Netty 4.1.13 -> 4.1.17 [#309](https://github.com/{aws/aws-sdk-java-v2/pull/309)
    - Use java.util.Objects to implement equals, hashCode [#294](https://github.com/aws/aws-sdk-java-v2/pull/294)

  - ### Bugfixes
    - Attempting to fix class-loader exception raised on gitter. [#216](https://github.com/aws/aws-sdk-java-v2/pull/216)
    - Call doClose in HttpClientDependencies#close method [#268](https://github.com/aws/aws-sdk-java-v2/pull/268)
    - Fixing bundle exports [#281](https://github.com/aws/aws-sdk-java-v2/pull/281)

  - ### Removals
    - Delete old jmespath AST script [#266](https://github.com/aws/aws-sdk-java-v2/pull/266)
    - Remove current waiter implementation. [#258](https://github.com/aws/aws-sdk-java-v2/pull/258)
    - Removed policy builder. [#259](https://github.com/aws/aws-sdk-java-v2/pull/259)
    - Removed progress listeners until they can be updated to V2 standards. [#285](https://github.com/aws/aws-sdk-java-v2/pull/285)

## __Amazon CloudFront__
  - ### Removals
    - Removed high-level cloudfront utilities. [#242](https://github.com/aws/aws-sdk-java-v2/pull/242)

## __Amazon DynamoDB__
  - ### Features
    - Adding some helpers for being able to create DyanmoDB AttributeValues. [#276](https://github.com/aws/aws-sdk-java-v2/pull/276)

  - ### Bugfixes
    - Fixed TableUtils that broke with enum change. [#235](https://github.com/aws/aws-sdk-java-v2/pull/235)

## __Amazon EC2__
  - ### Removals
    - Removed high-level utilities. [#244](https://github.com/aws/aws-sdk-java-v2/pull/244)

## __Amazon EMR__
  - ### Removals
    - Removed high-level utilities. [#245](https://github.com/aws/aws-sdk-java-v2/pull/245)

## __Amazon Glacier__
  - ### Removals
    - Removed high-level utilities. [#246](https://github.com/aws/aws-sdk-java-v2/pull/246)

## __Amazon Polly__
  - ### Removals
    - Removed polly presigners until they can be updated for V2. [#287](https://github.com/aws/aws-sdk-java-v2/pull/287)

## __Amazon S3__
  - ### Features
    - Adding utility that creates temporary bucket name using user-name  [#234](https://github.com/aws/aws-sdk-java-v2/pull/234)

## __Amazon SES__
  - ### Removals
    - Removed high-level utilities. [#248](https://github.com/aws/aws-sdk-java-v2/pull/248)

## __Amazon SNS__
  - ### Removals
    - Removed high-level utilities. [#255](https://github.com/aws/aws-sdk-java-v2/pull/255)

## __Amazon SQS__
  - ### Bugfixes
    - Porting SQS test to make use of async and hopefully resolve the bug [#240](https://github.com/aws/aws-sdk-java-v2/pull/240)

  - ### Removals
    - Removed high-level utilities and the interceptor that rewrites the endpoint based on the SQS queue. [#238](https://github.com/aws/aws-sdk-java-v2/pull/238)

## __Amazon SimpleDB__
  - ### Removals
    - Removed high-level utilities and unused response metadata handler. [#249](https://github.com/aws/aws-sdk-java-v2/pull/249)

## __Netty NIO Async HTTP Client__
  - ### Features
    - Adding socket resolver helper that will load the appropriate SocketChannel [#293](https://github.com/aws/aws-sdk-java-v2/pull/293)

  - ### Bugfixes
    - Netty spurious timeout error fix [#283](https://github.com/aws/aws-sdk-java-v2/pull/283)
    - Temporarily disable epoll [#254](https://github.com/aws/aws-sdk-java-v2/pull/254)

# __2.0.0-preview-5__ __2017-10-17__
## __AWS SDK for Java v2__
  - ### Features
    - Asynchronous request handler for strings `AsyncRequestProvider.fromString("hello world!!!")` [PR #183](https://github.com/aws/aws-sdk-java-v2/pull/183)
    - General HTTP core clean-up [PR #178](https://github.com/aws/aws-sdk-java-v2/pull/178)
    - Get value from request POJO using member model names `String bucketName = s3PutObjectResponse.getValueForField("Bucket", String.class);` [PR #144](https://github.com/aws/aws-sdk-java-v2/pull/144)
    - Model enums on service POJOs [PR #195](https://github.com/aws/aws-sdk-java-v2/pull/195)
    - Move `core` classes to their own package `software.amazon.awssdk.core` [PR #194](https://github.com/aws/aws-sdk-java-v2/pull/194)

  - ### Bugfixes
    - Resolve potential security issue handling DTD entities [PR #198](https://github.com/aws/aws-sdk-java-v2/pull/198)
    - Serialization/deserialization of complex model objects [PR #128](https://github.com/aws/aws-sdk-java-v2/pull/128) / [Issue #121](https://github.com/aws/aws-sdk-java-v2/issues/121)

## __Amazon S3__
  - ### Features
    - Handle 100-continue header for PUT object [PR #169](https://github.com/aws/aws-sdk-java-v2/pull/169)

## __Netty NIO Async HTTP Client__
  - ### Bugfixes
    - Better handling of event-loop selection for AWS Lambda container [PR #208](https://github.com/aws/aws-sdk-java-v2/pull/208)
    - Data corruption fix in streaming responses and stability fixes [PR #173](https://github.com/aws/aws-sdk-java-v2/pull/173)

# __2.0.0-preview-4__ __2017-09-19__
## __AWS SDK for Java v2__
  - ### Features
    - Added convenience methods for both sync and async streaming operations for file based uploads/downloads.
    - Added some convenience implementation of [AsyncResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/async/AsyncResponseHandler.java) to emit to a byte array or String.
    - Immutable objects can now be modified easily with a newly introduced [copy](https://github.com/aws/aws-sdk-java-v2/blob/master/utils/src/main/java/software/amazon/awssdk/utils/builder/ToCopyableBuilder.java#L42) method that applies a transformation on the builder for the object and returns a new immutable object.
    - Major refactor of RequestHandler interfaces. Newly introduced [ExecutionInterceptors](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/interceptor/ExecutionInterceptor.java) have a cleaner, more consistent API and are much more powerful.
    - S3's CreateBucket no longer requires the location constraint to be specified, it will be inferred from the client region if not present.
    - The [File](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L92) and [OutputStream](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L107) implementations of StreamingResponseHandler now return the POJO response in onComplete.

  - ### Bugfixes
    - Fixed a bug in default credential provider chain where it would erroneously abort at the ProfileCredentialsProvider. See [Issue #135](https://github.com/aws/aws-sdk-java-v2/issues/135)
    - Many improvments and fixes to the Netty NIO based transport.
    - Several fixes around S3's endpoint resolution, particularly with advanced options like path style addressing and accelerate mode. See [Issue #130](https://github.com/aws/aws-sdk-java-v2/issues/130)
    - Several fixes around serialization and deserialization of immutable objects. See [Issue #122](https://github.com/aws/aws-sdk-java-v2/issues/122)
    - Type parameters are now correctly included for [StreamingResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java) on the client interface.

  - ### Removals
    - Dependency on JodaTime has been dropped in favor of Java 8's APIS.
    - DynamoDBMapper and DynamoDB Document API have been removed.
    - Metrics subsystem has been removed.

# __2.0.0-preview-2__ __2017-07-21__
## __AWS SDK for Java v2__
  - ### Features
    - New pluggable HTTP implementation built on top of Java's HttpUrlConnection. Good choice for simple applications with low throughput requirements. Better cold start latency than the default Apache implementation.
    - Simple convenience methods have been added for operations that require no input parameters.
    - Substantial improvments to start up time and cold start latencies
    - The Netty NIO HTTP client now uses a shared event loop group for better resource management. More options for customizing the event loop group are now available.
    - Using java.time instead of the legacy java.util.Date in generated model classes.
    - Various improvements to the immutability of model POJOs. ByteBuffers are now copied and collections are returned as unmodifiable.

# __2.0.0-preview-1__ __2017-06-28__
## __AWS SDK for Java v2__
  - ### Features
    - Initial release of the AWS SDK for Java v2. See our [blog post](https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-0-developer-preview) for information about this new major veresion. This release is considered a developer preview and is not intended for production use cases.

