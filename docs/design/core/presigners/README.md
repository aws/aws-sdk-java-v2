**Design:** New Feature, **Status:** [Released](../../../README.md)

# Request Presigners

"Presigned URLs" are a generic term usually used for an AWS request that has been signed using
[SigV4's query parameter signing](https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-query-string-auth.html) so that it can be
invoked by a browser, within a certain time period.

The 1.x family of SDKs is able to generate presigned requests of multiple types, with S3's GetObject being the most
frequently-used flavor of presigned URL. Customers have been [very](https://github.com/aws/aws-sdk-java-v2/issues/203)
[vocal](https://dzone.com/articles/s3-and-the-aws-java-sdk-20-look-before-you-leap) about this feature not yet being included
in the 2.x SDK. This document proposes how presigned URLs should be supported by the Java SDK 2.x.

**What is request presigning?**

Request presigning allows a **signature creator** to use their secret **signing credentials** to generate an AWS request. This
**presigned request** can be executed by a separate **signature user** within a fixed time period, without any additional
authentication required.

For example, a support engineer for a backend service may temporarily share a service log with a customer by: (1) uploading
the logs to S3, (2) presigning an S3 GetObject request for the logs, (3) sending the presigned request to the customer. The
customer can then download the logs using the presigned request. The presigned request would remain valid until it "expires" at
a time specified by the support engineer when the request was signed.

**What is a presigned URL?**

Colloquially, most people wouldn't consider every presigned request to be a "presigned URL". For example, a presigned DynamoDb
PutItem can be executed by a signature user, but it would require the signature creator to share the headers and payload that
were included when the signature was generated, so that the signature user can send them along with the request.

For the purposes of this document:
1. A **presigned request** is any request signed using query parameter signing with the express purpose of another entity
executing the presigned request at a later time.
2. A **presigned URL** is a presigned request that: (1) does not include a payload, (2) does not include content-type or x-amz-*
headers, and (3) uses the GET HTTP method.

This distinction is useful, because a presigned URL can be trivially executed by a browser.

*Example*

Under this definition, a presigned S3 GetObjectRequest is a presigned URL if and only if it does not include one of the
following fields:

1. sseCustomerAlgorithm (Header: x-amz-server-side-encryption-customer-algorithm)
2. sseCustomerKey (Header: x-amz-server-side-encryption-customer-key)
3. sseCustomerKeyMD5 (Header: x-amz-server-side-encryption-customer-key-MD5)
4. requesterPays (Header: x-amz-request-payer)

If these fields were included when the presigned request was generated and the URL was opened in a browser, the signature user
will get a "signature mismatch" error. This is because these headers are included in the signature, but these values are not
sent by a browser.



## Proposed APIs

The SDK 2.x will support both presigned requests and presigned URLs. The API will make it easy to distinguish between the two,
and make it possible to support executing presigned requests using HTTP clients that implement the AWS SDK HTTP client blocking
or non-blocking SPI.

*FAQ Below: "What about execution of presigned requests?"*

To more quickly address a very vocal desire for presigned URLs, the first iteration will only support generating presigned S3
GetObject requests. Later milestones will add other operations and services as well as code-generated presigners.

*Section Below: "Milestones"*

### Usage Examples

#### Example 1: Generating presigned requests

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest =
        s3Presigner.presignGetObject(r -> r.getObject(get -> get.bucket("bucket").key("key"))
                                           .signatureDuration(Duration.ofMinutes(15)));
URL URL = presignedRequest.url();
```

#### Example 2: Determining whether the presigned request will work in a browser

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(...);

Validate.isTrue(presignedRequest.isBrowserCompatible());

System.out.println("Click the following link to download the object: " + presignedRequest.url());
```

#### Example 3: Executing the presigned request using the URL connection HTTP client.

```Java
S3Presigner s3Presigner = S3Presigner.create();
PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(...);

try (SdkHttpClient httpClient = UrlConnectionHttpClient.create()) {
    ContentStreamProvider payload = presignedRequest.payload()
                                                    .map(SdkBytes::asInputStream)
                                                    .map(is -> () -> is)
                                                    .orElseNull();

    HttpExecuteRequest executeRequest =
            HttpExecuteRequest.builder()
                              .request(presignedRequest.httpRequest())
                              .contentStreamProvider(payload)
                              .build();

    HttpExecuteResponse httpRequest = client.prepareRequest(executeRequest).call();

    Validate.isTrue(httpRequest.httpResponse().isSuccessful());
}
```

### `{Service}Presigner`

A new class will be created for each service: `{Service}Presigner` (e.g. `S3Presigner`). This follows the naming strategy
established by the current `{Service}Client` and `{Service}Utilities` classes.

#### Example

```Java
/**
 * Allows generating presigned URLs for supported S3 operations.
 */
public interface S3Presigner {
    static S3Presigner create();
    static S3Presigner.Builder builder();

    /**
     * Presign a `GetObjectRequest` so that it can be invoked directly via an HTTP client.
     */
    PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request);
    PresignedGetObjectRequest presignGetObject(Consumer<GetObjectPresignRequest.Builder> request);

    interface Builder {
        Builder region(Region region);
        Builder credentialsProvider(AwsCredentialsProvider credentials);
        Builder endpointOverride(URL endpointOverride);
        // ...
        S3Presigner build();
    }
}
```

#### Instantiation

This class can be instantiated in a few ways:

**Create method**

Uses the default region / credential chain, similar to `S3Client.create()`.

```Java
S3Presigner s3Presigner = S3Presigner.create();
```

**Builder**

Uses configurable region / credentials, similar to `S3Client.builder().build()`.

```Java
S3Presigner s3Presigner = S3Presigner.builder().region(Region.US_WEST_2).build();
```

**From an existing S3Client**

Uses the region / credentials from an existing `S3Client` instance, similar to `s3.utilities()`.

```Java
S3Client s3 = S3Client.create();
S3Presigner s3Presigner = s3.presigner();
```

**From the S3 gateway class**

(Implementation date: TBD) A discoverable alias for the `create()` and `builder()` methods.

```Java
S3Presigner s3Presigner = S3.presigner();
S3Presigner s3Presigner = S3.presignerBuilder().build();
```

#### Methods

A method will be generated for each operation: `Presigned{Operation}Request presign{Operation}({Operation}PresignRequest)`
(e.g. `PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest)`).

*FAQ Below: "Why a different input shape per operation?" and "Why a different output shape per operation?"*.

#### Inner-Class: `{Service}Presigner.Builder`

An inner-class will be created for each service presigner: `{Service}Presigner.Builder` (e.g. `S3Presigner.Builder`).
This follows the naming strategy established by the current `{Service}Utilities` classes.

##### Methods

The presigner builder will have at least the following configuration:

1. `region(Region)`: The region that should be used when generating the presigned URLs.
2. `endpointOverride(URI)`: An endpoint that should be used in place of the one derived from the region.
3. `credentialsProvider(AwsCredentialsProvider)`: The credentials that should be used when signing the request.

Additional configuration will be added later after more investigation (e.g. signer, service-specific configuration).

### `{Operation}PresignRequest`

A new input class will be generated for each operation that supports presigning. These requests
will extend a common base, allowing for common code to be used to configure the request.

*FAQ Below: "Why a different input shape per operation?"*.

#### Example

```Java
/**
 * A request to generate presigned GetObjectRequest, passed to S3Presigner#getObject.
 */
public interface GetObjectPresignRequest extends PresignRequest {
    /**
     * The GetObjectRequest that should be presigned.
     */
    GetObjectRequest getObject();
    // Plus builder boilerplate
}

public interface PresignRequest {
    /**
     * The duration for which this presigned request should be valid. After this time has expird,
     * attempting to use the presigned request will fail.
     */
    Duration signatureDuration();
    // Plus builder boilerplate
}
```

### `Presigned{Operation}Request`

A new output class will be generated for each operation that supports presigning. These presigned requests
will extend a common base, allowing for common code to be used to process the response.

*FAQ Below: "Why a different output shape per operation?"*.

#### Example

```Java
/**
 * A presigned GetObjectRequest, returned by S3Presigner#getObject.
 */
public interface PresignedGetObjectRequest extends PresignedRequest {
    // Builder boilerplate
}

/**
 * A generic presigned request. The isBrowserCompatible method can be used to determine whether this request
 * can be executed by a web browser.
 */
public interface PresignedRequest {
    /**
     * The URL that the presigned request will execute against. The isBrowserCompatible method can be used to
     * determine whether this request will work in a browser.
     */
    URL url();

    /**
     * The exact SERVICE time that the request will expire. After this time, attempting to execute the request
     * will fail.
     *
     * This may differ from the local clock, based on the skew between the local and AWS service clocks.
     */
    Instant expiration();

    /**
     * Returns true if the url returned by the url method can be executed in a browser.
     *
     * This is true when the HTTP request method is GET, and hasSignedHeaders and hasSignedPayload are false.
     * 
     * TODO: This isn't a universally-agreed-upon-good method name. We should iterate on it before GA.
     */
    boolean isBrowserCompatible();

    /**
     * Returns true if there are signed headers in the request. Requests with signed headers must have those
     * headers sent along with the request to prevent a "signature mismatch" error from the service.
     */
    boolean hasSignedHeaders();

    /**
     * Returns the subset of headers that were signed, and MUST be included in the presigned request to prevent
     * the request from failing.
     */
    Map<String, List<String>> signedHeaders();

    /**
     * Returns true if there is a signed payload in the request. Requests with signed payloads must have those
     * payloads sent along with the request to prevent a "signature mismatch" error from the service.
     */
    boolean hasSignedPayload();

    /**
     * Returns the payload that was signed, or Optional.empty() if hasSignedPayload is false.
     */
    Optional<SdkBytes> signedPayload();

    /**
     * The entire SigV4 query-parameter signed request (minus the payload), that can be transmitted as-is to a
     * service using any HTTP client that implement the SDK's HTTP client SPI.
     *
     * This request includes signed AND unsigned headers.
     */
    SdkHttpRequest httpRequest();

    // Plus builder boilerplate
}
```



## Milestones

### M1: Hand-Written S3 GetObject Presigner

**Done When:** Customers can use an SDK-provided S3 presigner to generate presigned S3 GetObject requests.

**Expected Tasks:**

1. Hand-write relevant interfaces and class definitions as described in this document.
2. DO NOT create the `S3Client#presigner` method, yet.
3. Implement the `presignGetObject` method using minimum refactoring of core classes.

### M2: Hand-Written S3 PutObject Presigner

**Done When:** Customers can use an SDK-provided S3 presigner to generate presigned S3 PutObject requests.

**Expected Tasks:**

1. Hand-write relevant interfaces and class definitions as described in this document.
2. Implement the `presignPutObject` method using minimum refactoring of core classes.

### M3: Hand-Written Polly SynthesizeSpeech Presigner

**Done When:** Customers can use an SDK-provided Polly presigner to generate presigned Polly SynthesizeSpeech requests.

**Expected Tasks:**

1. Hand-write relevant interfaces and class definitions as described in this document.
2. Hand-create a `SynthesizeSpeech` marshaller that generates browser-compatible HTTP requests.
3. Implement the `presignSynthesizeSpeech` method using minimum refactoring of core classes. (Considering whether
or not the browser-compatible HTTP request marshaller should be the default).

### M4: Generated Presigners

**Done When:** The existing presigners are generated, and customers do not have to make any code changes.

**Expected Tasks:**

1. Refactor core classes to remove unnecessary duplication between `presignGetObject`, `presignPutObject`,
and `presignSynthesizeSpeech`.
2. Implement customization for allowing use of the browser-compatible `SynthesizeSpeech` marshaller.
3. Update hand-written `presign*` methods to use the refactored model.
4. Update code generation to generate the `presign*` inputs/outputs.
5. Update code generation to generate the `{Service}Presigner` classes.

### M5: Generate Existing 1.11.x Presigners

**Done When:** Any operation presigners that exist in 1.11.x are available in 2.x.

**Expected Tasks:**

1. Generate EC2 Presigners
2. Generate RDS Presigners

### M6: Generate All Presigners

**Done When:** All operations (that can support presigning) support presigning.

*FAQ Below: "For which operations will we generate a URL presigner?"*

**Expected Tasks:**

1. Testing generated presigners for a representative sample of operations

### M7: Instantiation and Discoverability Simplifications

**Done When:** All clients contain a `{Service}Client#presigner()` method.

**Expected Tasks:**

1. Determine which pieces of configuration need to be inherited from the service clients, and how their inheritance will work
   (e.g. how will execution interceptors work?).
2. Determine whether async clients will have a separate presigner interface, to be forward-compatible if we add blocking
   credential/region providers.
3. Update the generated clients to support inheriting this configuration
   in a sane way.
4. Generate this method in the client, for all supported services.



## FAQ

### For which Services will we generate URL presigners?

We will generate a `{Service}Presigner` class if the service has any operations that need presigner support.

### For which operations will we generate a URL presigner?

The support operations vary based on the implementation milestone (see Milestones above).

The set of supported operations assumed by this document is ALL operations, except ones with signed, streaming
payloads. Signed, streaming payloads require additional modeling (e.g. of chunked encoded payloads or event
streaming) and can be  implemented at a later time after additional design, if there is sufficient customer
demand.

### Why a different input shape per operation?

The input shape must be aware of the operation inputs, so the options are: (1) a different, generated input shape per operation,
or (2) a common "core" input shape that is parameterized with the input shape.

The following compares Option 1 to Option 2, in the interest of illustrating why Option 1 was chosen.

**Option 1:** `presignGetObject(GetObjectPresignRequest)` and `presignGetObject(Consumer<GetObjectPresignRequest.Builder>)`:
Generated `GetObjectPresignRequest`

```Java
s3.presignGetObject(GetObjectPresignRequest.builder()
                                           .getObject(GetObjectRequest.builder().bucket("bucket").key("key").build())
                                           .signatureDuration(Duration.ofMinutes(15))
                                           .build());
```

```Java
s3.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(15))
                          .getObject(go -> go.bucket("bucket").key("key")));
```

**Option 2:** `presignGetObject(PresignRequest<GetObject>)` and
`presignGetObject(GetObject, Consumer<PresignRequest.Builder<GetObject>>)`

```Java
s3.presignGetObject(PresignRequest.builder(GetObjectRequest.builder().bucket("bucket").key("key").build())
                                  .signatureDuration(Duration.ofMinutes(15))
                                  .build());
```

```Java
s3.presignGetObject(GetObjectRequest.builder().bucket("bucket").key("key").build(),
                    r -> r.signatureDuration(Duration.ofMinutes(15)));
```

**Option 1 Pros:**

1. More readable to entry-level developers
2. Closer to `S3Client` signature
3. Simpler construction (operation input is not needed to instantiate builder)
4. Much simpler `Consumer<Builder>` method variant

**Option 2 Pros:**

1. Smaller jar size

### Why a different output shape per operation?

This decision is much less obvious. The output shape does not technically need to understand the input shape,
so the options are: (1) a different, generated output shape per operation, (2) return the `PresignedRequest`
directly.

The following compares Option 1 and Option 2, in the interest of illustrating why Option 1 was chosen.

**Option 1:** `PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest)`

```Java
PresignedGetObjectRequest presignedRequest = s3.presignGetObject(...);
URL presignedUrl = presignedRequest.getUrl();
```

**Option 2:** `PresignedRequest presignGetObject(GetObjectPresignRequest)``

```Java
PresignedRequest presignedRequest = s3.presignGetObject(...);
URL presignedUrl = presignedRequest.getUrl();
```

**Option 1 Pros:**

1. Makes type-safe execution of presigned requests possible.
2. Closest to `S3Client` method signature

**Option 2 Pros:**

1. Smaller jar size

**Decision:** Option 1 will be used, because the cost of an empty interface is very small, and it enables
support for type-safe execution of presigned requests in the future.

*FAQ Below: "What about execution of presigned requests?"*

### What about execution of presigned requests?

The design proposed above makes it possible to execute the signed requests using any HTTP client that implements
the AWS SDK's HTTP client SPI.

In the future, it would be beneficial to allow a signature user to **execute** a presigned URL using the entire
SDK stack, instead of just the HTTP client portion. This would enable:

1. Automatic retries, in the case of network or service outages
2. Response shape unmarshalling, in the case of modeled responses
3. SDK metric integration (once implemented)

As an example (this is not a design proposal), if the `DynamoDbClient` supported executing a presigned URL, it would be
beneficial to make sure that the request was for the correct operation, so that the retries and response processing are
appropriate for the service/operation.

```Java
DynamoDbClient dynamo = DynamoDbClient.create();
PresignedPutItemRequest presignedRequest = dynamo.presigner().presignPutItem(...);
PutItemResponse response = dynamo.putItem(presignedRequest);
```

### What about non-blocking request presigning?

The proposal above does not distinguish between blocking or non-blocking request presigning. This is because the
SDK currently only distinguishes between blocking and non-blocking, when it comes to an HTTP client implementation.

The generated presigned request can be executed by a blocking OR non-blocking HTTP client.

In the future, the SDK could implement non-blocking region providers and non-blocking credential providers, at which
time it could become relevant to distinguish between blocking or non-blocking URL presigners.

For this reason, we will need to decide whether the `presigner()` method to get to a pre-configured URL presigner will
only be included on the blocking `{Service}Client` (with a separate non-blocking `{Service}Presigner` async class for the 
`{Service}AsyncClient`s), or whether we should use the same `{Service}Presigner` for sync and async clients.
