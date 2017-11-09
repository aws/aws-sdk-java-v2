# __2.0.0-preview-5__ __2017-10-17__
## __AWS SDK for Java v2__
  - ### New Features ###
    - Asynchronous request handler for strings `AsyncRequestProvider.fromString("hello world!!!")` [PR #183](https://github.com/aws/aws-sdk-java-v2/pull/183)
    - Get value from request POJO using member model names `String bucketName = s3PutObjectResponse.getValueForField("Bucket", String.class);` [PR #144](https://github.com/aws/aws-sdk-java-v2/pull/144)
    - Model enums on service POJOs [PR #195](https://github.com/aws/aws-sdk-java-v2/pull/195)
  - ### Bug Fixes ###
    - Serialization/deserialization of complex model objects [PR #128](https://github.com/aws/aws-sdk-java-v2/pull/128) / [Issue #121](https://github.com/aws/aws-sdk-java-v2/issues/121)
    - Resolve potential security issue handling DTD entities [PR #198](https://github.com/aws/aws-sdk-java-v2/pull/198)
  - ### Refactors / Moves ###
    - General HTTP core clean-up [PR #178](https://github.com/aws/aws-sdk-java-v2/pull/178)
    - Move `core` classes to their own package `software.amazon.awssdk.core` [PR #194](https://github.com/aws/aws-sdk-java-v2/pull/194)
    
## Amazon S3
  - ### New Features ###
    - Handle 100-continue header for PUT object [PR #169](https://github.com/aws/aws-sdk-java-v2/pull/169)
    
## Netty NIO Async HTTP Client
  - ### Bug Fixes ###
    - Data corruption fix in streaming responses and stability fixes [PR #173](https://github.com/aws/aws-sdk-java-v2/pull/173)
    - Better handling of event-loop selection for AWS Lambda container [PR #208](https://github.com/aws/aws-sdk-java-v2/pull/208)
    
# __2.0.0-preview-4__ __2017-09-19__
## __AWS SDK for Java v2__
  - ### New Features ###
    - The [File](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L92) and [OutputStream](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java#L107) implementations of StreamingResponseHandler now return the POJO response in onComplete.
    - Added convenience methods for both sync and async streaming operations for file based uploads/downloads.
    - Major refactor of RequestHandler interfaces. Newly introduced [ExecutionInterceptors](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/interceptor/ExecutionInterceptor.java) have a cleaner, more consistent API and are much more powerful.
    - Immutable objects can now be modified easily with a newly introduced [copy](https://github.com/aws/aws-sdk-java-v2/blob/master/utils/src/main/java/software/amazon/awssdk/utils/builder/ToCopyableBuilder.java#L42) method that applies a transformation on the builder for the object and returns a new immutable object.
    - S3's CreateBucket no longer requires the location constraint to be specified, it will be inferred from the client region if not present.
    - Added some convenience implementation of [AsyncResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/async/AsyncResponseHandler.java) to emit to a byte array or String.

  - ### Bug fixes ###
    - Many improvments and fixes to the Netty NIO based transport.
    - Type parameters are now correctly included for [StreamingResponseHandler](https://github.com/aws/aws-sdk-java-v2/blob/master/core/src/main/java/software/amazon/awssdk/sync/StreamingResponseHandler.java) on the client interface.
    - Several fixes around S3's endpoint resolution, particularly with advanced options like path style addressing and accelerate mode. See [Issue #130](https://github.com/aws/aws-sdk-java-v2/issues/130)
    - Fixed a bug in default credential provider chain where it would erroneously abort at the ProfileCredentialsProvider. See [Issue #135](https://github.com/aws/aws-sdk-java-v2/issues/135)
    - Several fixes around serialization and deserialization of immutable objects. See [Issue #122](https://github.com/aws/aws-sdk-java-v2/issues/122)

  - ### Removals ###
    - Dependency on JodaTime has been dropped in favor of Java 8's APIS.
    - Metrics subsystem has been removed.
    - DynamoDBMapper and DynamoDB Document API have been removed.

# __2.0.0-preview-3__ __2017-09-18__
Invalid partial release. Use 2.0.0-preview-4 instead.

# __2.0.0-preview-2__ __2017-07-21__
## __AWS SDK for Java v2__
  - ### New Features ###
    - Substantial improvments to start up time and cold start latencies
    - New pluggable HTTP implementation built on top of Java's HttpUrlConnection. Good choice for simple applications with low throughput requirements. Better cold start latency than the default Apache implementation.
    - The Netty NIO HTTP client now uses a shared event loop group for better resource management. More options for customizing the event loop group are now available.
    - Simple convenience methods have been added for operations that require no input parameters.
    - Using java.time instead of the legacy java.util.Date in generated model classes.
    - Various improvements to the immutability of model POJOs. ByteBuffers are now copied and collections are returned as unmodifiable.

# __2.0.0-preview-1__ __2017-06-28__
## __AWS SDK for Java v2__
  - ### Features ###
    - Initial release of the AWS SDK for Java v2. See our [blog post](https://aws.amazon.com/blogs/developer/aws-sdk-for-java-2-0-developer-preview) for information about this new major veresion. This release is considered a developer preview and is not intended for production use cases.
