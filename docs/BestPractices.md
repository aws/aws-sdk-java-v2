##  AWS Java SDK 2.x Best Practices

Here are the best practices of using AWS Java SDK 2.x.

### Reuse SDK client if possible

Each SDK client maintains its own HTTP connection pool so that connections can be reused by a new request 
to cut down the time to establish a new connection. It is recommended to share a single instance of the client to 
avoid the overhead of having too many connection pools that are not being utilized effectively. All SDK clients are thread safe.

If it is not desirable to share a client instance, invoke `client.close()` to release the resources once the client is not needed.

### Close input streams from client operations

For streaming operations such as `S3Client#getObject`,  if you are working with `ResponseInputStream` directly, we have the following recommendations:

- Read all the data from the input stream as soon as possible
- Close the input stream as soon as possible

This is because the input stream is a direct stream of data from the HTTP connection and the underlying HTTP connection cannot be
reused until all data from the stream has been read and the stream is closed. If these rules are not followed, the client can 
run out of resources by allocating too many open, but unused, HTTP connections.

### Close input streams of responseBody() from Custom Client based on SdkHttpClient
While implementing custom clients from  `SdkHttpClient` interface
- Close the [responseBody](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/http/HttpExecuteResponse.html#responseBody()) Input Stream: Always close the "responseBody" input stream to release the HTTP Connection, even when handling error responses.
- SDK Requirement: The SDK requires the closing of the input stream to manage HTTP connections effectively.
- Handling Error Responses: Even in the case of error responses, the SDK client creates an input stream for reading error data.
- Consistent Practice: Ensure that consumers close the input stream in the "responseBody" attribute for both success and error cases.

### Tune HTTP configurations based on performance tests

The SDK provides a set of [default http configurations] that apply to general use cases. Customers are recommended to tune the configurations for their applications based on their use cases.

### Use OpenSSL for Netty async client

By default, `NettyNioAsyncHttpClient` uses JDK as the SslProvider. In our local tests, we found that using `OpenSSL`
is more performant than JDK. Using OpenSSL is also the recommended approach by Netty community, see [Netty TLS with OpenSSL].
Note that you need to add `netty-tcnative` to your dependency in order to use OpenSSL, see [Configuring netty-tcnative]

Once you have `netty-tcnative` configured correctly, `NettyAsyncHttpClient` will select OpenSSL automatically, or you can set the
`SslProvider` explicitly on the NettyNioAsyncHttpClient builder.

```
   NettyNioAsyncHttpClient.builder()
                          .sslProvider(SslProvider.OPENSSL)
                          .build();
```

### Utilize timeout configurations

The SDK provides timeout configurations on requests out of box and they can be configured via `ClientOverrideConfiguration#apiCallAttemptTimeout` and `ClientOverrideConfiguration#ApiCallTimeout`.

```java
  S3Client.builder()
          .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(API_CALL_TIMEOUT))
                                       .apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
          .build();
```

- `ApiCallAttemptTimeout` tracks the amount of time for a single http attempt and the request can be retried if timed out on api call attempt. 
- `ApiCallTimeout` configures the amount of time for the entire execution including all retry attempts. 

By default, timeouts are disabled. Using them together is helpful to set a hard limit on total time spent on all attempts across retries and each individual HTTP request to fail fast on one slow request.

### Use HTTPS endpoints

The SDK uses HTTPS endpoints by default. Configuring an `http://` endpoint through `endpointOverride` disables TLS, so request
and response headers and bodies are not encrypted in transit. Use plaintext HTTP only for local emulators and trusted private
networks.

Over plaintext HTTP, SigV4 payload signing differs between the sync and async clients:

- Sync clients (for example `S3Client`) always sign the request payload over HTTP. The `x-amz-content-sha256` header is set to
  `STREAMING-AWS4-HMAC-SHA256-PAYLOAD` (or `-PAYLOAD-TRAILER` with a checksum), and each chunk carries a signature. Setting
  `AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED` to `false` has no effect over HTTP.
- Async clients (for example `S3AsyncClient` and `S3AsyncClient.crtBuilder()`) do not sign the request payload over HTTP when
  chunked encoding is enabled, which is the default for `PutObject` and `UploadPart`. The `x-amz-content-sha256` header is set
  to `UNSIGNED-PAYLOAD` or `STREAMING-UNSIGNED-PAYLOAD-TRAILER`, and the signature covers the request headers only. The
  Java-based async client logs a warning once per JVM when this happens; the CRT-based client does not.

Flexible checksums (`CRC32`, `CRC32C`, `CRC64NVME`, `SHA1`, `SHA256`) do not replace payload signing over HTTP. When the checksum
is sent as a trailer, which is the default for streaming uploads, the trailer is not covered by the signature.

If payload integrity matters, use HTTPS.

[default http configurations]: https://github.com/aws/aws-sdk-java-v2/blob/master/http-client-spi/src/main/java/software/amazon/awssdk/http/SdkHttpConfigurationOption.java
[Netty TLS with OpenSSL]: https://netty.io/wiki/requirements-for-4.x.html#tls-with-openssl
[Configuring netty-tcnative]: https://netty.io/wiki/forked-tomcat-native.html