# 1.11 to 2.0 Changelog

- [1. Clients](#1-clients)
    - [1.1. Client Creation Defaults](#11-client-creation-defaults)
    - [1.2. AWS Client Configuration: Custom Regions, Credentials and Endpoints](#12-aws-client-configuration-custom-regions-credentials-and-endpoints)
        - [1.2.1. Client Regions](#121-client-regions)
        - [1.2.2. Client Credentials](#122-client-credentials)
    - [1.3. SDK Client Configuration](#13-sdk-client-configuration)
        - [1.3.1. Client HTTP Configuration](#131-client-http-configuration)
        - [1.3.2. Client HTTP Proxy Configuration](#132-client-http-proxy-configuration)
        - [1.3.3. Client Override Configuration](#133-client-override-configuration)
        - [1.3.4. Client Override Retry Configuration](#134-client-override-retry-configuration)
        - [1.3.5. Async Configuration](#135-async-configuration)
        - [1.3.6. Other Options](#136-other-options)
- [2. Operations, Request and Response Changes](#2-operations-request-and-response-changes)
    - [2.1. Streaming Operations](#21-streaming-operations)
- [3. Exception Changes](#3-exception-changes)
- [4. Service Changes](#4-service-changes)
    - [4.1. S3 Changes](#41-s3-changes)
        - [4.1.1. S3 Operation Migration](#411-s3-operation-migration)
    - [4.2. SNS Changes](#42-sns-changes)
    - [4.3. SQS Changes](#43-sqs-changes)
- [5. Profile File Changes](#5-profile-file-changes)
- [6. Conversion Tables](#6-conversion-tables)
    - [6.1. Environment Variables and System Properties](#61-environment-variables-and-system-properties)
    - [6.2. Credential Providers](#62-credential-providers)
    - [6.3. Client Names](#63-client-names)
- [7. High-Level Libraries](#7-high-level-libraries)

# 1. Clients

Clients, like the `DynamoDbClient` are the most direct way of communicating with AWS services. See [7. High-Level Libraries](#7-high-level-libraries) for the status of high-level libraries like S3 Transfer Manager, the Dynamo DB Mapper, S3 Encryption Client and waiters.

In 2.0, the following changes have been made to the clients:

1. Clients can no longer be mutated.
2. Clients can no longer be created by their default constructor. The static `create` or `builder` methods must be used instead: `new AmazonDynamoDBClient` is now `DynamoDbClient.create` and `AmazonDynamoDBClient.builder` is now `DynamoDbClient.builder`.
3. Client builders no longer contain static methods. The static methods on the clients must be used: `AmazonDynamoDBClientBuilder.defaultClient` is now `DynamoDbClient.create` and `AmazonDynamoDBClientBuilder.standard` is now `DynamoDbClient.builder`.
4. Client classes have been renamed. See [6.3. Client Names](#63-client-names) for the 2.0-equivalent client names.
5. Async clients now use non-blocking IO.
6. Async operations now return `CompletableFuture`.
7. Async clients now use an internal executor only for calling `complete` on the `CompletableFuture` and retries.

## 1.1. Client Creation Defaults

In 2.0, the following changes have been made to the default client creation logic:

1. The default credential provider chain for S3 no longer includes anonymous credentials. Anonymous access to S3 must be specified manually using the `AnonymousCredentialsProvider`.
2. The following environment variables related to default client creation have been changed:
   1. `AWS_CBOR_DISABLED` is now `CBOR_ENABLED`
   2. `AWS_ION_BINARY_DISABLE` is now `BINARY_ION_ENABLED`
3. The following system properties related to default client creation have been changed:
   1. `com.amazonaws.sdk.disableEc2Metadata` is now `aws.disableEc2Metadata`.
   2. `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride` is now `aws.ec2MetadataServiceEndpoint`.
   3. `com.amazonaws.sdk.disableCbor` is now `aws.cborEnabled`.
   4. `com.amazonaws.sdk.disableIonBinary` is now `aws.binaryIonEnabled`.
   5. The following system properties no longer supported: `com.amazonaws.sdk.disableCertChecking`, `com.amazonaws.sdk.enableDefaultMetrics`, `com.amazonaws.sdk.enableThrottledRetry`, `com.amazonaws.regions.RegionUtils.fileOverride`, `com.amazonaws.regions.RegionUtils.disableRemote`, `com.amazonaws.services.s3.disableImplicitGlobalClients`, `com.amazonaws.sdk.enableInRegionOptimizedMode`
4. Loading region configuration from a custom `endpoints.json` file is no longer supported.
5. The default credentials logic has been modified. See `com.amazonaws.auth.DefaultAWSCredentialsProviderChain` changes below for more information.
6. Profile file format has changed to more closely match the CLI's behavior. See [5. Profile File Changes](#5-profile-file-changes).

## 1.2. AWS Client Configuration: Custom Regions, Credentials and Endpoints

In 2.0, regions, credentials and endpoints must be specified using the client builder.

| Setting | 1.11.x (Client) | 1.11.x (Builder) | 2.0 |
|---|---|---|---|
| Region | `new AmazonDynamoDBClient()`<br />`.withRegion(Regions.US_EAST_1)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withRegion(Regions.US_EAST_1)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.region(Region.US_EAST_1)`<br />`.build()` |
| Credentials | `new AmazonDynamoDBClient(credentials)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withCredentials(credentials)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.credentials(credentials)`<br />`.build()` |
| Endpoint | `new AmazonDynamoDBClient()`<br />`.withRegion(signingRegion)`<br />`.withEndpoint(endpoint)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion))`<br />`.build()` | `DynamoDbClient.builder()`<br />`.region(signingRegion)`<br />`.endpointOverride(endpoint)`<br />`.build()` |

### 1.2.1. Client Regions

In 2.0, the following changes have been made related to regions:

1. When using a service that does not currently have region-specific endpoints, you must use `Region.AWS_GLOBAL` or `Region.AWS_CN_GLOBAL` instead of a region-specific endpoint.
2. `com.amazonaws.regions.Regions` changes:
    1. This class has been replaced with `software.amazon.awssdk.regions.Region`.
    2. `Regions.fromName` is now `Region.of`.
    3. `Regions.getName` is now `Region.id`.
    4. The following `Regions` methods and fields are no longer supported: `DEFAULT_REGION`, `getDescription`, `getCurrentRegion`, `name`.
3. `com.amazonaws.regions.Region` changes:
    1. For region identification:
        1. This class has been replaced with `software.amazon.awssdk.regions.Region`, created with `Region.of`.
        3. `Region.getName` is now `Region.id`.
    2. For region metadata:
        1. This class has been replaced with `software.amazon.awssdk.regions.RegionMetadata`, created with `RegionMetadata.of`.
        2. `Region.getName` is now `RegionMetadata.name`.
        3. `Region.getDomain` is now `RegionMetadata.domain`.
        4. `Region.getPartition` is now `RegionMetadata.partition`.
    3. For service metadata:
        1. This class has been replaced with `software.amazon.awssdk.regions.ServiceMetadata`, created by calling `ServiceMetadata.of` (or the `serviceMetadata` method on any service client).
        2. `Region.getServiceEndpoint` is now `ServiceMetadata.endpointFor(Region)`.
        3. `Region.isServiceSupported` is now `ServiceMetadata.regions().contains(Region)`.
    4. The following `Region` methods are no longer supported: `hasHttpsEndpoint`, `hasHttpEndpoint`, `getAvailableEndpoints`, `createClient`.

### 1.2.2. Client Credentials

In 2.0, the following changes have been made related to the credentials providers:

1. `com.amazonaws.auth.AWSCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.AwsCredentialsProvider`.
    2. `AWSCredentialsProvider.getCredentials` is now `AwsCredentialsProvider.resolveCredentials`.
    3. The following `AWSCredentialsProvider` methods are no longer supported: `refresh`.
2. `com.amazonaws.auth.DefaultAWSCredentialsProviderChain` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider`.
    2. `new DefaultAWSCredentialsProviderChain` is now `DefaultCredentialsProvider.create`.
    3. System properties are treated as higher-priority than environment variables.
    4. See `EnvironmentVariableCredentialsProvider`, `SystemPropertiesCredentialsProvider`, `ProfileCredentialsProvider` and `EC2ContainerCredentialsProviderWrapper` below for more changes.
    5. The following `DefaultAWSCredentialsProviderChain` methods are no longer supported: `getInstance`.
3. `com.amazonaws.auth.AWSStaticCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.StaticCredentialsProvider`.
    2. `new AWSStaticCredentialsProvider` is now `StaticCredentialsProvider.create`.
4. `com.amazonaws.auth.EnvironmentVariableCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider`.
    2. `new EnvironmentVariableCredentialsProvider` is now `EnvironmentVariableCredentialsProvider.create`.
    3. The `AWS_ACCESS_KEY` environment variable is now `AWS_ACCESS_KEY_ID`.
    4. The `AWS_SECRET_KEY` environment variable is now `AWS_SECRET_ACCESS_KEY`.
5. `com.amazonaws.auth.SystemPropertiesCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider`.
    2. `new SystemPropertiesCredentialsProvider` is now `SystemPropertyCredentialsProvider.create`.
    3. The `aws.secretKey` system property is now `aws.secretAccessKey`.
6. `com.amazonaws.auth.profile.ProfileCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider`.
    2. `new ProfileCredentialsProvider` is now `ProfileCredentialsProvider.create`.
    3. Custom profile file paths are now specified via `ProfileCredentialsProvider.builder`.
    4. The `AWS_CREDENTIAL_PROFILES_FILE` environment variable is now `AWS_SHARED_CREDENTIALS_FILE`.
    5. Profile file format has changed to more closely match the CLI's behavior. See [5. Profile File Changes](#5-profile-file-changes).
7. `com.amazonaws.auth.ContainerCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider`.
    2. Async refresh is specified via `ContainerCredentialsProvider.builder`.
    3. `new ContainerCredentialsProvider` is now `ContainerCredentialsProvider.create`.
8. `com.amazonaws.auth.InstanceProfileCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider`.
    2. Async refresh is specified via `InstanceProfileCredentialsProvider.builder`.
    3. `new InstanceProfileCredentialsProvider` is now `InstanceProfileCredentialsProvider.create`.
    4. The `com.amazonaws.sdk.disableEc2Metadata` system property is now `aws.disableEc2Metadata`.
    5. The `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride` system property is now `aws.ec2MetadataServiceEndpoint`.
    6. The following `AWSCredentialsProvider` methods are no longer supported: `getInstance`.
9. `com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider`.
    2. Async refresh is no longer the default, but can be specified via `StsAssumeRoleCredentialsProvider.builder`.
    3. `new STSAssumeRoleSessionCredentialsProvider` and `new STSAssumeRoleSessionCredentialsProvider.Builder` are now `StsAssumeRoleCredentialsProvider.builder`.
    4. All builder configuration has been replaced in favor of specifying a `StsClient` and `AssumeRoleRequest` request.
10. `com.amazonaws.auth.STSSessionCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider`.
    2. Async refresh is no longer the default, but can be specified via `StsGetSessionTokenCredentialsProvider.builder`.
    3. `new STSAssumeRoleSessionCredentialsProvider` is now `StsGetSessionTokenCredentialsProvider.builder`.
    4. All constructor parameters have been replaced in favor of specifying a `StsClient` and `GetSessionTokenRequest` request in a builder.
11. `com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider` changes:
    1. This class has been replaced with `software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider`.
    2. Async refresh is no longer the default, but can be specified via `StsAssumeRoleWithWebIdentityCredentialsProvider.builder`.
    3. `new WebIdentityFederationSessionCredentialsProvider` is now `StsAssumeRoleWithWebIdentityCredentialsProvider.builder`.
    4. All constructor parameters have been replaced in favor of specifying a `StsClient` and `AssumeRoleWithWebIdentityRequest` request in a builder.
12. `com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper` has been removed in favor of `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` and `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider`.
13. `com.amazonaws.services.s3.S3CredentialsProviderChain` has been removed in favor of `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` and `software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider`.
14. `com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider` and `com.amazonaws.auth.PropertiesFileCredentialsProvider` have been removed.

## 1.3. SDK Client Configuration

In 1.11.x, SDK client configuration was modified by setting a `ClientConfiguration` instance on the client or client builder:

| 1.11.x (Client) | 1.11.x (Builder) |
|---|---|
| `new AmazonDynamoDBClient(clientConfiguration)` | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` |

In 2.0, SDK client configuration is split into separate configuration settings:

**Synchronous Configuration** 

```Java
ProxyConfiguration.Builder proxyConfig =
        ProxyConfiguration.builder();

ApacheHttpClient.Builder httpClientBuilder = 
        ApacheHttpClient.builder()
                        .proxyConfiguration(proxyConfig.build());

ClientOverrideConfiguration.Builder overrideConfig =
        ClientOverrideConfiguration.builder();

DynamoDbClient client = 
        DynamoDbClient.builder()
                      .httpClientBuilder(httpClientBuilder)
                      .overrideConfiguration(overrideConfig.build())
                      .build();
```

**Asynchronous Configuration**

```Java
NettyNioAsyncHttpClient.Builder httpClientBuilder = 
        NettyNioAsyncHttpClient.builder();

ClientOverrideConfiguration.Builder overrideConfig =
        ClientOverrideConfiguration.builder();

ClientAsyncConfiguration.Builder asyncConfig =
        ClientAsyncConfiguration.builder();

DynamoDbAsyncClient client = 
        DynamoDbAsyncClient.builder()
                           .httpClientBuilder(httpClientBuilder)
                           .overrideConfiguration(overrideConfig.build())
                           .asyncConfiguration(asyncConfig.build())
                           .build();
```

### 1.3.1. Client HTTP Configuration

1. It is now possible to change which HTTP client is used at runtime by specifying an implementation via `clientBuilder.httpClientBuilder`.
2. HTTP clients passed to `clientBuilder.httpClient` are not closed by default, allowing them to be shared between AWS clients.
3. HTTP for async clients now use non-blocking IO.
4. Some operations now utilize HTTP/2 for performance improvements.

| Setting | 1.11.x | 2.0 (Sync, Apache) | 2.0 (Async, Netty) |
|---|---|---|---|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ApacheHttpClient.Builder httpClientBuilder =`<br />`ApacheHttpClient.builder()` | `NettyNioAsyncHttpClient.Builder httpClientBuilder =`<br />`NettyNioAsyncHttpClient.builder()` |
| Max Connections | `clientConfig.setMaxConnections(...)`<br />`clientConfig.withMaxConnections(...)` | `httpClientBuilder.maxConnections(...)` | `httpClientBuilder.maxConcurrency(...)` |
| Connection Timeout | `clientConfig.setConnectionTimeout(...)`<br />`clientConfig.withConnectionTimeout(...)` | `httpClientBuilder.connectionTimeout(...)` | `httpClientBuilder.connectionTimeout(...)` |
| Socket Timeout | `clientConfig.setSocketTimeout(...)`<br />`clientConfig.withSocketTimeout(...)` | `httpClientBuilder.socketTimeout(...)` | `httpClientBuilder.writeTimeout(...)` <br /> `httpClientBuilder.readTimeout(...)` |
| Connection TTL | `clientConfig.setConnectionTTL(...)`<br />`clientConfig.withConnectionTTL(...)` | `httpClientBuilder.connectionTimeToLive(...)` | `httpClientBuilder.connectionTimeToLive(...)` |
| Connection Max Idle | `clientConfig.setConnectionMaxIdleMillis(...)`<br />`clientConfig.withConnectionMaxIdleMillis(...)` | `httpClientBuilder.connectionMaxIdleTime(...)` | `httpClientBuilder.connectionMaxIdleTime(...)` |
| Validate After Inactivity | `clientConfig.setValidateAfterInactivityMillis(...)`<br />`clientConfig.withValidateAfterInactivityMillis(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Local Address | `clientConfig.setLocalAddress(...)`<br />`clientConfig.withLocalAddress(...)` | `httpClientBuilder.localAddress(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/857) |
| Expect-Continue Enabled | `clientConfig.setUseExpectContinue(...)`<br />`clientConfig.withUseExpectContinue(...)` | `httpClientBuilder.expectContinueEnabled(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Connection Reaper | `clientConfig.setUseReaper(...)`<br />`clientConfig.withReaper(...)` | `httpClientBuilder.useIdleConnectionReaper(...)` | `httpClientBuilder.useIdleConnectionReaper(...)` |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` | `DynamoDbAsyncClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` |


### 1.3.2. Client HTTP Proxy Configuration

| Setting | 1.11.x | 2.0 (Sync, Apache) | 2.0 (Async, Netty) |
|---|---|---|---|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ProxyConfiguration.Builder proxyConfig =`<br />`ProxyConfiguration.builder()` | |
| Proxy Host | `clientConfig.setProxyHost(...)`<br />`clientConfig.withProxyHost(...)` | `proxyConfig.endpoint(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Port | `clientConfig.setProxyPort(...)`<br />`clientConfig.withProxyPort(...)` | `proxyConfig.endpoint(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Username | `clientConfig.setProxyUsername(...)`<br />`clientConfig.withProxyUsername(...)` | `proxyConfig.username(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Password | `clientConfig.setProxyPassword(...)`<br />`clientConfig.withProxyPassword(...)` | `proxyConfig.password(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Domain | `clientConfig.setProxyDomain(...)`<br />`clientConfig.withProxyDomain(...)` | `proxyConfig.ntlmDomain(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Workstation | `clientConfig.setProxyWorkspace(...)`<br />`clientConfig.withProxyWorkstation(...)` | `proxyConfig.ntlmWorkstation(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Proxy Authentication Methods | `clientConfig.setProxyAuthenticationMethods(...)`<br />`clientConfig.withProxyAuthenticationMethods(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Preemptive Basic Proxy Authentication | `clientConfig.setPreemptiveBasicProxyAuth(...)`<br />`clientConfig.withPreemptiveBasicProxyAuth(...)` | `proxyConfig.preemptiveBasicAuthenticationEnabled(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Non Proxy Hosts | `clientConfig.setNonProxyHosts(...)`<br />`clientConfig.withNonProxyHosts(...)` | `proxyConfig.nonProxyHosts(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/858) |
| Disable Socket Proxy | `clientConfig.setDisableSocketProxy(...)`<br />`clientConfig.withDisableSocketProxy(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `httpClientBuilder.proxyConfiguration(proxyConfig.build())` | |

### 1.3.3. Client Override Configuration

| Setting | 1.11.x | 2.0 |
|---|---|---|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `ClientOverrideConfiguration.Builder overrideConfig =`<br />`ClientOverrideConfiguration.builder()` 
| User Agent Prefix | `clientConfig.setUserAgentPrefix(...)`<br />`clientConfig.withUserAgentPrefix(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, ...)` |
| User Agent Suffix | `clientConfig.setUserAgentSuffix(...)`<br />`clientConfig.withUserAgentSuffix(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, ...)` |
| Signer | `clientConfig.setSignerOverride(...)`<br />`clientConfig.withSignerOverride(...)` | `overrideConfig.advancedOption(SdkAdvancedClientOption.SIGNER, ...)` |
| Additional Headers | `clientConfig.addHeader(...)`<br />`clientConfig.withHeader(...)` | `overrideConfig.putHeader(...)` |
| Request Timeout | `clientConfig.setRequestTimeout(...)`<br />`clientConfig.withRequestTimeout(...)` | `overrideConfig.apiCallAttemptTimeout(...)` |
| Client Execution Timeout | `clientConfig.setClientExecutionTimeout(...)`<br />`clientConfig.withClientExecutionTimeout(...)` | `overrideConfig.apiCallTimeout(...)` |
| Use Gzip | `clientConfig.setUseGzip(...)`<br />`clientConfig.withGzip(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Socket Buffer Size Hint | `clientConfig.setSocketBufferSizeHints(...)`<br />`clientConfig.withSocketBufferSizeHints(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Cache Response Metadata | `clientConfig.setCacheResponseMetadata(...)`<br />`clientConfig.withCacheResponseMetadata(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Response Metadata Cache Size | `clientConfig.setResponseMetadataCacheSize(...)`<br />`clientConfig.withResponseMetadataCacheSize(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| DNS Resolver | `clientConfig.setDnsResolver(...)`<br />`clientConfig.withDnsResolver(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| TCP Keepalive | `clientConfig.setUseTcpKeepAlive(...)`<br />`clientConfig.withTcpKeepAlive(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| Secure Random | `clientConfig.setSecureRandom(...)`<br />`clientConfig.withSecureRandom(...)` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `DynamoDbClient.builder()`<br />`.httpClientBuilder(httpClientBuilder)`<br />`.build()` |

### 1.3.4. Client Override Retry Configuration

Retry configuration has changed in 2.0 to be controlled entirely through the `RetryPolicy` in the `ClientOverrideConfiguration`.

| Setting | 1.11.x | 2.0 |
|---|---|---|
| | `ClientConfiguration clientConfig =`<br />`new ClientConfiguration()` | `RetryPolicy.Builder retryPolicy =`<br />`RetryPolicy.builder()` |
| Max Error Retry | `clientConfig.setMaxErrorRetry(...)`<br />`clientConfig.withMaxErrorRetry(...)` | `retryPolicy.numRetries(...)` |
| Use Throttled Retries | `clientConfig.setUseThrottleRetries(...)`<br />`clientConfig.withUseThrottleRetries(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/645) |
| Max Consecutive Retries Before Throttling | `clientConfig.setMaxConsecutiveRetriesBeforeThrottling(...)`<br />`clientConfig.withMaxConsecutiveRetriesBeforeThrottling(...)` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/645) |
| | `AmazonDynamoDBClientBuilder.standard()`<br />`.withClientConfiguration(clientConfiguration)`<br />`.build()` | `overrideConfig.retryPolicy(retryPolicy.build())` | |

### 1.3.5. Async Configuration

1. Async executors passed to `asyncConfig.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, ...)` must be shut down by the user.

| Setting | 1.11.x | 2.0 |
|---|---|---|
| | | `ClientAsyncConfiguration.Builder asyncConfig =`<br />`ClientAsyncConfiguration.builder()` |
| Executor | `AmazonDynamoDBAsyncClientBuilder.standard()`<br />`.withExecutorFactory(...)`<br />`.build()` | `asyncConfig.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, ...)` |
| | | `AmazonDynamoDBAsyncClientBuilder.standard()`<br />`.withExecutorFactory(...)`<br />`.build()` | `DynamoDbAsyncClient.builder()`<br />`.asyncConfiguration(asyncConfig.build())`<br />`.build()` |

### 1.3.6. Other Options

These `ClientConfiguration` options from 1.11.x have changed in 2.0 of the SDK and don't have direct equivalents.

| Setting | 1.11.x | 2.0 Equivalent |
|---|---|---|
| Protocol | `clientConfig.setProtocol(Protocol.HTTP)`<br />`clientConfig.withProtocol(Protocol.HTTP)` | The protocol is now HTTPS by default, and can only be modified by setting an HTTP endpoint on the client builder: `clientBuilder.endpointOverride(URI.create("http://..."))` |

# 2. Operations, Request and Response Changes

Requests, like `DynamoDbClient`'s `PutItemRequest` are passed to a client operation, like `DynamoDbClient.putItem`. These operations return a response from the AWS service, like a `PutItemResponse`.

In 2.0, the following changes have been made to the operations:

1. Operations with multiple response pages now have a `Paginator` method for automatically iterating over all items in the response.
2. Requests and responses can no longer be mutated.
3. Requests and responses can no longer be created by their default constructor. The static `builder` method must be used instead: `new PutItemRequest().withTableName(...)` is now `PutItemRequest.builder().tableName(...).build()`.
4. Operations and requests support a short-hand method of creating requests: `dynamoDbClient.putItem(request -> request.tableName(...))`.

## 2.1. Streaming Operations

Streaming operations, like `S3Client`'s `getObject` and `putObject` accept a stream of bytes or return a stream of bytes, without loading the entire payload into memory.

1. Streaming operation request objects no longer include the payload.
2. Sync streaming request methods now accept request payloads as a `RequestBody` that simplifies common loading logic: eg. `RequestBody.fromFile(...)`.
3. Async streaming request methods now accept request payloads as an `AsyncRequestBody` that simplifies common loading logic: eg. `AsyncRequestBody.fromFile(...)`.
4. Sync streaming response methods now specify response handling as a `ResponseTransformer` that simplifies common transformation logic: eg. `ResponseTransformer.toFile(...)`.
5. Async streaming response methods now specify response handling as an `AsyncResponseTransformer` that simplifies common transformation logic: eg. `AsyncResponseTransformer.toFile(...)`.
6. Streaming response operations now have an `AsBytes` method to load the response into memory and simplify common in-memory type conversions.

# 3. Exception Changes

In 2.0, the following changes have been made related to exceptions:

1. `com.amazonaws.SdkBaseException` and `com.amazonaws.AmazonClientException` changes:
    1. These classes have combined and replaced with `software.amazon.awssdk.core.exception.SdkException`.
    2. `AmazonClientException.isRetryable` is now `SdkException.retryable`.
2. `com.amazonaws.SdkClientException` changes:
    1. This class has been replaced with `software.amazon.awssdk.core.exception.SdkClientException`.
    2. This class now extends `software.amazon.awssdk.core.exception.SdkException`.
3. `com.amazonaws.AmazonServiceException` changes:
    1. This class has been replaced with `software.amazon.awssdk.awscore.exception.AwsServiceException`.
    2. This class now extends `software.amazon.awssdk.core.exception.SdkServiceException`, a new exception type that extends `software.amazon.awssdk.core.exception.SdkException`.
    3. `AmazonServiceException.getRequestId` is now `SdkServiceException.requestId`.
    4. `AmazonServiceException.getServiceName` is now `AwsServiceException.awsErrorDetails().serviceName`.
    5. `AmazonServiceException.getErrorCode` is now `AwsServiceException.awsErrorDetails().errorCode`.
    6. `AmazonServiceException.getErrorMessage` is now `AwsServiceException.awsErrorDetails().errorMessage`.
    7. `AmazonServiceException.getStatusCode` is now `AwsServiceException.awsErrorDetails().sdkHttpResponse().statusCode`.
    8. `AmazonServiceException.getHttpHeaders` is now `AwsServiceException.awsErrorDetails().sdkHttpResponse().headers`.
    9. `AmazonServiceException.rawResponse` is now `AwsServiceException.awsErrorDetails().rawResponse`.
    10. `AmazonServiceException.getErrorType` is no longer supported.

# 4. Service Changes

## 4.1. S3 Changes

The S3 client in 2.0 is drastically different from the client in 1.11, because it is now generated from models like every other service.

1. Cross-region access is no longer supported. A client may now only access buckets in the region with which the client has been configured.
2. Anonymous access is disabled by default and must be enabled using the `AnonymousCredentialsProvider`.

### 4.1.1. S3 Operation Migration

| 1.11.x Operation | 2.0 Operation |
|---|---|
| `abortMultipartUpload` | `abortMultipartUpload` |
| `changeObjectStorageClass` | `copyObject` |
| `completeMultipartUpload` | `completeMultipartUpload` |
| `copyObject` | `copyObject` |
| `copyPart` | `uploadPartCopy` |
| `createBucket` | `createBucket` |
| `deleteBucket` | `deleteBucket` |
| `deleteBucketAnalyticsConfiguration` | `deleteBucketAnalyticsConfiguration` |
| `deleteBucketCrossOriginConfiguration` | `deleteBucketCors` |
| `deleteBucketEncryption` | `deleteBucketEncryption` |
| `deleteBucketInventoryConfiguration` | `deleteBucketInventoryConfiguration` |
| `deleteBucketLifecycleConfiguration` | `deleteBucketLifecycle` |
| `deleteBucketMetricsConfiguration` | `deleteBucketMetricsConfiguration` |
| `deleteBucketPolicy` | `deleteBucketPolicy` |
| `deleteBucketReplicationConfiguration` | `deleteBucketReplication` |
| `deleteBucketTaggingConfiguration` | `deleteBucketTagging` |
| `deleteBucketWebsiteConfiguration` | `deleteBucketWebsite` |
| `deleteObject` | `deleteObject` |
| `deleteObjectTagging` | `deleteObjectTagging` |
| `deleteObjects` | `deleteObjects` |
| `deleteVersion` | `deleteObject` |
| `disableRequesterPays` | `putBucketRequestPayment` |
| `doesBucketExist` | `headBucket` |
| `doesBucketExistV2` | `headBucket` |
| `doesObjectExist` | `headObject` |
| `enableRequesterPays` | `putBucketRequestPayment` |
| `generatePresignedUrl` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/849) |
| `getBucketAccelerateConfiguration` | `getBucketAccelerateConfiguration` |
| `getBucketAcl` | `getBucketAcl` |
| `getBucketAnalyticsConfiguration` | `getBucketAnalyticsConfiguration` |
| `getBucketCrossOriginConfiguration` | `getBucketCors` |
| `getBucketEncryption` | `getBucketEncryption` |
| `getBucketInventoryConfiguration` | `getBucketInventoryConfiguration` |
| `getBucketLifecycleConfiguration` | `getBucketLifecycle` or `getBucketLifecycleConfiguration` |
| `getBucketLocation` | `getBucketLocation` |
| `getBucketLoggingConfiguration` | `getBucketLogging` |
| `getBucketMetricsConfiguration` | `getBucketMetricsConfiguration` |
| `getBucketNotificationConfiguration` | `getBucketNotification` or `getBucketNotificationConfiguration` |
| `getBucketPolicy` | `getBucketPolicy` |
| `getBucketReplicationConfiguration` | `getBucketReplication` |
| `getBucketTaggingConfiguration` | `getBucketTagging` |
| `getBucketVersioningConfiguration` | `getBucketVersioning` |
| `getBucketWebsiteConfiguration` | `getBucketWebsite` |
| `getObject` | `getObject` |
| `getObjectAcl` | `getObjectAcl` |
| `getObjectAsString` | `getObjectAsBytes().asUtf8String` |
| `getObjectMetadata` | `headObject` |
| `getObjectTagging` | `getObjectTagging` |
| `getResourceUrl` | [S3Utilities#getUrl](https://github.com/aws/aws-sdk-java-v2/blob/7428f629753c603f96dd700ca686a7b169fc4cd4/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3Utilities.java#L140) |
| `getS3AccountOwner` | `listBuckets` |
| `getUrl` | [S3Utilities#getUrl](https://github.com/aws/aws-sdk-java-v2/blob/7428f629753c603f96dd700ca686a7b169fc4cd4/services/s3/src/main/java/software/amazon/awssdk/services/s3/S3Utilities.java#L140) |
| `headBucket` | `headBucket` |
| `initiateMultipartUpload` | `createMultipartUpload` |
| `isRequesterPaysEnabled` | `getBucketRequestPayment` |
| `listBucketAnalyticsConfigurations` | `listBucketAnalyticsConfigurations` |
| `listBucketInventoryConfigurations` | `listBucketInventoryConfigurations` |
| `listBucketMetricsConfigurations` | `listBucketMetricsConfigurations` |
| `listBuckets` | `listBuckets` |
| `listMultipartUploads` | `listMultipartUploads` |
| `listNextBatchOfObjects` | `listObjectsV2Paginator` |
| `listNextBatchOfVersions` | `listObjectVersionsPaginator` |
| `listObjects` | `listObjects` |
| `listObjectsV2` | `listObjectsV2` |
| `listParts` | `listParts` |
| `listVersions` | `listObjectVersions` |
| `putObject` | `putObject` |
| `restoreObject` | `restoreObject` |
| `restoreObjectV2` | `restoreObject` |
| `selectObjectContent` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/859) |
| `setBucketAccelerateConfiguration` | `putBucketAccelerateConfiguration` |
| `setBucketAcl` | `putBucketAcl` |
| `setBucketAnalyticsConfiguration` | `putBucketAnalyticsConfiguration` |
| `setBucketCrossOriginConfiguration` | `putBucketCors` |
| `setBucketEncryption` | `putBucketEncryption` |
| `setBucketInventoryConfiguration` | `putBucketInventoryConfiguration` |
| `setBucketLifecycleConfiguration` | `putBucketLifecycle` or `putBucketLifecycleConfiguration` |
| `setBucketLoggingConfiguration` | `putBucketLogging` |
| `setBucketMetricsConfiguration` | `putBucketMetricsConfiguration` |
| `setBucketNotificationConfiguration` | `putBucketNotification` or `putBucketNotificationConfiguration` |
| `setBucketPolicy` | `putBucketPolicy` |
| `setBucketReplicationConfiguration` | `putBucketReplication` |
| `setBucketTaggingConfiguration` | `putBucketTagging` |
| `setBucketVersioningConfiguration` | `putBucketVersioning` |
| `setBucketWebsiteConfiguration` | `putBucketWebsite` |
| `setObjectAcl` | `putObjectAcl` |
| `setObjectRedirectLocation` | `copyObject` |
| `setObjectTagging` | `putObjectTagging` |
| `uploadPart` | `uploadPart` |

## 4.2. SNS Changes

1. An SNS client may no longer access SNS topics in regions different than the one with which the client was configured.

## 4.3. SQS Changes

1. An SQS client may no longer access SQS queues in regions different than the one with which the client was configured.

# 5. Profile File Changes

The parsing of the `~/.aws/config` and `~/.aws/credentials` has changed to more closely emulate that used by the AWS CLI.

1. A `~/` or `~` followed by the file system's default path separator at the start of the path is resolved by checking, in order, `$HOME`, `$USERPROFILE` (Windows only), `$HOMEDRIVE$HOMEPATH` (Windows only), and then the `user.home` system property.
2. The `AWS_CREDENTIAL_PROFILES_FILE` environment variable is now `AWS_SHARED_CREDENTIALS_FILE`.
3. Profile definitions in configuration files without a `profile` prefix are silently dropped.
4. Profile names that do not consist of alphanumeric, underscore or dash characters are silently dropped (after the `profile` prefix has been removed for configuration files).
5. Profiles duplicated within the same file have their properties merged.
6. Profiles duplicated in both the configuration and credentials files have their properties merged.
7. If both `[profile foo]` and `[foo]` are specified in the same file, their properties are NOT merged.
8. If both `[profile foo]` and `[foo]` are specified in the configuration file, `[profile foo]`'s properties are used.
9. Properties duplicated within the same file and profile use the later property in the file.
10. Both `;` and `#` are supported for defining a comment.
11. In profile definitions, `;` and `#` define a comment, even if they are adjacent to the closing bracket.
12. In property values, `;` and `#` define a comment only if they are preceded by whitespace.
13. In property values, `;` and `#` and all following content are included in the value if they are not preceded by whitespace.
14. Role-based credentials are the highest-priority credentials, and are always used if the user specifies the `role_arn` property.
15. Session-based credentials are the next-highest-priority credentials, and are always used if role-based credentials were not used and the user specifies the `aws_access_key_id` and `aws_session_token` properties.
16. Basic credentials are used if role-based and session-based credentials are not used and the user specified the `aws_access_key_id` property.

# 6. Conversion Tables

## 6.1. Environment Variables and System Properties

| 1.11.x Environment Variable | 1.11.x System Property | 2.0 Environment Variable | 2.0 System Property |
|---|---|---|---|
| `AWS_ACCESS_KEY_ID`<br />`AWS_ACCESS_KEY` | `aws.accessKeyId` | `AWS_ACCESS_KEY_ID` | `aws.accessKeyId` |
| `AWS_SECRET_KEY`<br />`AWS_SECRET_ACCESS_KEY` | `aws.secretKey` | `AWS_SECRET_ACCESS_KEY` | `aws.secretAccessKey` |
| `AWS_SESSION_TOKEN`| `aws.sessionToken` | `AWS_SESSION_TOKEN` | `aws.sessionToken` |
| `AWS_REGION` | `aws.region` | `AWS_REGION` | `aws.region` |
| `AWS_CONFIG_FILE` | | `AWS_CONFIG_FILE` | `aws.configFile` |
| `AWS_CREDENTIAL_PROFILES_FILE` | | `AWS_SHARED_CREDENTIALS_FILE` | `aws.sharedCredentialsFile` |
| `AWS_PROFILE` | `aws.profile` | `AWS_PROFILE` | `aws.profile` |
| `AWS_EC2_METADATA_DISABLED` | `com.amazonaws.sdk.disableEc2Metadata` | `AWS_EC2_METADATA_DISABLED` | `aws.disableEc2Metadata` |
| | `com.amazonaws.sdk.ec2MetadataServiceEndpointOverride` | `AWS_EC2_METADATA_SERVICE_ENDPOINT` | `aws.ec2MetadataServiceEndpoint` |
| `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI` | | `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI` | `aws.containerCredentialsPath` |
| `AWS_CONTAINER_CREDENTIALS_FULL_URI` | | `AWS_CONTAINER_CREDENTIALS_FULL_URI` | `aws.containerCredentialsFullUri` |
| `AWS_CONTAINER_AUTHORIZATION_TOKEN` | | `AWS_CONTAINER_AUTHORIZATION_TOKEN` | `aws.containerAuthorizationToken` |
| `AWS_CBOR_DISABLED` | `com.amazonaws.sdk.disableCbor` | `CBOR_ENABLED` | `aws.cborEnabled` |
| `AWS_ION_BINARY_DISABLE` | `com.amazonaws.sdk.disableIonBinary` | `BINARY_ION_ENABLED` | `aws.binaryIonEnabled` |
| `AWS_EXECUTION_ENV` | | `AWS_EXECUTION_ENV` | `aws.executionEnvironment` |
| | `com.amazonaws.sdk.disableCertChecking` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `com.amazonaws.sdk.enableDefaultMetrics` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/23) | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/23) |
| | `com.amazonaws.sdk.enableThrottledRetry` | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/645) | [Not Supported](https://github.com/aws/aws-sdk-java-v2/issues/645) |
| | `com.amazonaws.regions.RegionUtils.fileOverride` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `com.amazonaws.regions.RegionUtils.disableRemote` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `com.amazonaws.services.s3.disableImplicitGlobalClients` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| | `com.amazonaws.sdk.enableInRegionOptimizedMode` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |

## 6.2. Credential Providers

| 1.11.x Credential Provider | 2.0 Credential Provider |
|---|---|
| `com.amazonaws.auth.AWSCredentialsProvider` | `software.amazon.awssdk.auth.credentials.AwsCredentialsProvider` |
| `com.amazonaws.auth.DefaultAWSCredentialsProviderChain` | `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` |
| `com.amazonaws.auth.AWSStaticCredentialsProvider` | `software.amazon.awssdk.auth.credentials.StaticCredentialsProvider` |
| `com.amazonaws.auth.EnvironmentVariableCredentialsProvider` | `software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider` |
| `com.amazonaws.auth.SystemPropertiesCredentialsProvider` | `software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider` |
| `com.amazonaws.auth.profile.ProfileCredentialsProvider` | `software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider` |
| `com.amazonaws.auth.ContainerCredentialsProvider` | `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` |
| `com.amazonaws.auth.InstanceProfileCredentialsProvider` | `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider` |
| `com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider` | `software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider` |
| `com.amazonaws.auth.STSSessionCredentialsProvider` | `software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider` |
| `com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider` | `software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider` |
| `com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper` | `software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider` and `software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider` |
| `com.amazonaws.services.s3.S3CredentialsProviderChain` | `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider` and `software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider` |
| `com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| `com.amazonaws.auth.PropertiesFileCredentialsProvider` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |

## 6.3. Client Names

| 1.11.x Client | 2.0 Client |
|---|---|
| `amazonaws.services.acmpca.AWSACMPCAAsyncClient` | `software.amazon.awssdk.services.acm.AcmAsyncClient` |
| `amazonaws.services.acmpca.AWSACMPCAClient` | `software.amazon.awssdk.services.acm.AcmClient` |
| `amazonaws.services.alexaforbusiness.AmazonAlexaForBusinessAsyncClient` | `software.amazon.awssdk.services.alexaforbusiness.AlexaForBusinessAsyncClient` |
| `amazonaws.services.alexaforbusiness.AmazonAlexaForBusinessClient` | `software.amazon.awssdk.services.alexaforbusiness.AlexaForBusinessClient` |
| `amazonaws.services.apigateway.AmazonApiGatewayAsyncClient` | `software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient` |
| `amazonaws.services.apigateway.AmazonApiGatewayClient` | `software.amazon.awssdk.services.apigateway.ApiGatewayClient` |
| `amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingAsyncClient` | `software.amazon.awssdk.services.applicationautoscaling.ApplicationAutoScalingAsyncClient` |
| `amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClient` | `software.amazon.awssdk.services.applicationautoscaling.ApplicationAutoScalingClient` |
| `amazonaws.services.applicationdiscovery.AWSApplicationDiscoveryAsyncClient` | `software.amazon.awssdk.services.applicationdiscovery.ApplicationDiscoveryAsyncClient` |
| `amazonaws.services.applicationdiscovery.AWSApplicationDiscoveryClient` | `software.amazon.awssdk.services.applicationdiscovery.ApplicationDiscoveryClient` |
| `amazonaws.services.appstream.AmazonAppStreamAsyncClient` | `software.amazon.awssdk.services.appstream.AppStreamAsyncClient` |
| `amazonaws.services.appstream.AmazonAppStreamClient` | `software.amazon.awssdk.services.appstream.AppStreamClient` |
| `amazonaws.services.appsync.AWSAppSyncAsyncClient` | `software.amazon.awssdk.services.appsync.AppSyncAsyncClient` |
| `amazonaws.services.appsync.AWSAppSyncClient` | `software.amazon.awssdk.services.appsync.AppSyncClient` |
| `amazonaws.services.athena.AmazonAthenaAsyncClient` | `software.amazon.awssdk.services.athena.AthenaAsyncClient` |
| `amazonaws.services.athena.AmazonAthenaClient` | `software.amazon.awssdk.services.athena.AthenaClient` |
| `amazonaws.services.autoscaling.AmazonAutoScalingAsyncClient` | `software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient` |
| `amazonaws.services.autoscaling.AmazonAutoScalingClient` | `software.amazon.awssdk.services.autoscaling.AutoScalingClient` |
| `amazonaws.services.autoscalingplans.AWSAutoScalingPlansAsyncClient` | `software.amazon.awssdk.services.autoscalingplans.AutoScalingPlansAsyncClient` |
| `amazonaws.services.autoscalingplans.AWSAutoScalingPlansClient` | `software.amazon.awssdk.services.autoscalingplans.AutoScalingPlansClient` |
| `amazonaws.services.batch.AWSBatchAsyncClient` | `software.amazon.awssdk.services.batch.BatchAsyncClient` |
| `amazonaws.services.batch.AWSBatchClient` | `software.amazon.awssdk.services.batch.BatchClient` |
| `amazonaws.services.budgets.AWSBudgetsAsyncClient` | `software.amazon.awssdk.services.budgets.BudgetsAsyncClient` |
| `amazonaws.services.budgets.AWSBudgetsClient` | `software.amazon.awssdk.services.budgets.BudgetsClient` |
| `amazonaws.services.certificatemanager.AWSCertificateManagerAsyncClient` | `software.amazon.awssdk.services.acm.AcmAsyncClient` |
| `amazonaws.services.certificatemanager.AWSCertificateManagerClient` | `software.amazon.awssdk.services.acm.AcmClient` |
| `amazonaws.services.cloud9.AWSCloud9AsyncClient` | `software.amazon.awssdk.services.cloud9.Cloud9AsyncClient` |
| `amazonaws.services.cloud9.AWSCloud9Client` | `software.amazon.awssdk.services.cloud9.Cloud9Client` |
| `amazonaws.services.clouddirectory.AmazonCloudDirectoryAsyncClient` | `software.amazon.awssdk.services.clouddirectory.CloudDirectoryAsyncClient` |
| `amazonaws.services.clouddirectory.AmazonCloudDirectoryClient` | `software.amazon.awssdk.services.clouddirectory.CloudDirectoryClient` |
| `amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient` | `software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient` |
| `amazonaws.services.cloudformation.AmazonCloudFormationClient` | `software.amazon.awssdk.services.cloudformation.CloudFormationClient` |
| `amazonaws.services.cloudfront.AmazonCloudFrontAsyncClient` | `software.amazon.awssdk.services.cloudfront.CloudFrontAsyncClient` |
| `amazonaws.services.cloudfront.AmazonCloudFrontClient` | `software.amazon.awssdk.services.cloudfront.CloudFrontClient` |
| `amazonaws.services.cloudhsm.AWSCloudHSMAsyncClient` | `software.amazon.awssdk.services.cloudhsm.CloudHsmAsyncClient` |
| `amazonaws.services.cloudhsm.AWSCloudHSMClient` | `software.amazon.awssdk.services.cloudhsm.CloudHsmClient` |
| `amazonaws.services.cloudhsmv2.AWSCloudHSMV2AsyncClient` | `software.amazon.awssdk.services.cloudhsmv2.CloudHsmV2AsyncClient` |
| `amazonaws.services.cloudhsmv2.AWSCloudHSMV2Client` | `software.amazon.awssdk.services.cloudhsmv2.CloudHsmV2Client` |
| `amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainAsyncClient` | `software.amazon.awssdk.services.cloudsearchdomain.CloudSearchDomainAsyncClient` |
| `amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient` | `software.amazon.awssdk.services.cloudsearchdomain.CloudSearchDomainClient` |
| `amazonaws.services.cloudsearchv2.AmazonCloudSearchAsyncClient` | `software.amazon.awssdk.services.cloudsearch.CloudSearchAsyncClient` |
| `amazonaws.services.cloudsearchv2.AmazonCloudSearchClient` | `software.amazon.awssdk.services.cloudsearch.CloudSearchClient` |
| `amazonaws.services.cloudtrail.AWSCloudTrailAsyncClient` | `software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient` |
| `amazonaws.services.cloudtrail.AWSCloudTrailClient` | `software.amazon.awssdk.services.cloudtrail.CloudTrailClient` |
| `amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient` | `software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient` |
| `amazonaws.services.cloudwatch.AmazonCloudWatchClient` | `software.amazon.awssdk.services.cloudwatch.CloudWatchClient` |
| `amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsAsyncClient` | `software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsAsyncClient` |
| `amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient` | `software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient` |
| `amazonaws.services.codebuild.AWSCodeBuildAsyncClient` | `software.amazon.awssdk.services.codebuild.CodeBuildAsyncClient` |
| `amazonaws.services.codebuild.AWSCodeBuildClient` | `software.amazon.awssdk.services.codebuild.CodeBuildClient` |
| `amazonaws.services.codecommit.AWSCodeCommitAsyncClient` | `software.amazon.awssdk.services.codecommit.CodeCommitAsyncClient` |
| `amazonaws.services.codecommit.AWSCodeCommitClient` | `software.amazon.awssdk.services.codecommit.CodeCommitClient` |
| `amazonaws.services.codedeploy.AmazonCodeDeployAsyncClient` | `software.amazon.awssdk.services.codedeploy.CodeDeployAsyncClient` |
| `amazonaws.services.codedeploy.AmazonCodeDeployClient` | `software.amazon.awssdk.services.codedeploy.CodeDeployClient` |
| `amazonaws.services.codepipeline.AWSCodePipelineAsyncClient` | `software.amazon.awssdk.services.codepipeline.CodePipelineAsyncClient` |
| `amazonaws.services.codepipeline.AWSCodePipelineClient` | `software.amazon.awssdk.services.codepipeline.CodePipelineClient` |
| `amazonaws.services.codestar.AWSCodeStarAsyncClient` | `software.amazon.awssdk.services.codestar.CodeStarAsyncClient` |
| `amazonaws.services.codestar.AWSCodeStarClient` | `software.amazon.awssdk.services.codestar.CodeStarClient` |
| `amazonaws.services.cognitoidentity.AmazonCognitoIdentityAsyncClient` | `software.amazon.awssdk.services.cognitoidentity.CognitoIdentityAsyncClient` |
| `amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient` | `software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient` |
| `amazonaws.services.cognitoidp.AWSCognitoIdentityProviderAsyncClient` | `software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient` |
| `amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient` | `software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient` |
| `amazonaws.services.cognitosync.AmazonCognitoSyncAsyncClient` | `software.amazon.awssdk.services.cognitosync.CognitoSyncAsyncClient` |
| `amazonaws.services.cognitosync.AmazonCognitoSyncClient` | `software.amazon.awssdk.services.cognitosync.CognitoSyncClient` |
| `amazonaws.services.comprehend.AmazonComprehendAsyncClient` | `software.amazon.awssdk.services.comprehend.ComprehendAsyncClient` |
| `amazonaws.services.comprehend.AmazonComprehendClient` | `software.amazon.awssdk.services.comprehend.ComprehendClient` |
| `amazonaws.services.config.AmazonConfigAsyncClient` | `software.amazon.awssdk.services.config.ConfigAsyncClient` |
| `amazonaws.services.config.AmazonConfigClient` | `software.amazon.awssdk.services.config.ConfigClient` |
| `amazonaws.services.connect.AmazonConnectAsyncClient` | `software.amazon.awssdk.services.connect.ConnectAsyncClient` |
| `amazonaws.services.connect.AmazonConnectClient` | `software.amazon.awssdk.services.connect.ConnectClient` |
| `amazonaws.services.costandusagereport.AWSCostAndUsageReportAsyncClient` | `software.amazon.awssdk.services.costandusagereport.CostAndUsageReportAsyncClient` |
| `amazonaws.services.costandusagereport.AWSCostAndUsageReportClient` | `software.amazon.awssdk.services.costandusagereport.CostAndUsageReportClient` |
| `amazonaws.services.costexplorer.AWSCostExplorerAsyncClient` | `software.amazon.awssdk.services.costexplorer.CostExplorerAsyncClient` |
| `amazonaws.services.costexplorer.AWSCostExplorerClient` | `software.amazon.awssdk.services.costexplorer.CostExplorerClient` |
| `amazonaws.services.databasemigrationservice.AWSDatabaseMigrationServiceAsyncClient` | `software.amazon.awssdk.services.databasemigration.DatabaseMigrationAsyncClient` |
| `amazonaws.services.databasemigrationservice.AWSDatabaseMigrationServiceClient` | `software.amazon.awssdk.services.databasemigration.DatabaseMigrationClient` |
| `amazonaws.services.datapipeline.DataPipelineAsyncClient` | `software.amazon.awssdk.services.datapipeline.DataPipelineAsyncClient` |
| `amazonaws.services.datapipeline.DataPipelineClient` | `software.amazon.awssdk.services.datapipeline.DataPipelineAsyncClient` |
| `amazonaws.services.dax.AmazonDaxAsyncClient` | `software.amazon.awssdk.services.dax.DaxAsyncClient` |
| `amazonaws.services.dax.AmazonDaxClient` | `software.amazon.awssdk.services.dax.DaxClient` |
| `amazonaws.services.devicefarm.AWSDeviceFarmAsyncClient` | `software.amazon.awssdk.services.devicefarm.DeviceFarmAsyncClient` |
| `amazonaws.services.devicefarm.AWSDeviceFarmClient` | `software.amazon.awssdk.services.devicefarm.DeviceFarmClient` |
| `amazonaws.services.directconnect.AmazonDirectConnectAsyncClient` | `software.amazon.awssdk.services.directconnect.DirectConnectAsyncClient` |
| `amazonaws.services.directconnect.AmazonDirectConnectClient` | `software.amazon.awssdk.services.directconnect.DirectConnectClient` |
| `amazonaws.services.directory.AWSDirectoryServiceAsyncClient` | `software.amazon.awssdk.services.directory.DirectoryAsyncClient` |
| `amazonaws.services.directory.AWSDirectoryServiceClient` | `software.amazon.awssdk.services.directory.DirectoryClient` |
| `amazonaws.services.dlm.AmazonDLMAsyncClient` | `software.amazon.awssdk.services.dlm.DlmAsyncClient` |
| `amazonaws.services.dlm.AmazonDLMClient` | `software.amazon.awssdk.services.dlm.DlmClient` |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient` | `software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient` |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBClient` | `software.amazon.awssdk.services.dynamodb.DynamoDbClient` |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsAsyncClient` | `software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient` |
| `amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient` | `software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient` |
| `amazonaws.services.ec2.AmazonEC2AsyncClient` | `software.amazon.awssdk.services.ec2.Ec2AsyncClient` |
| `amazonaws.services.ec2.AmazonEC2Client` | `software.amazon.awssdk.services.ec2.Ec2Client` |
| `amazonaws.services.ecr.AmazonECRAsyncClient` | `software.amazon.awssdk.services.ecr.EcrAsyncClient` |
| `amazonaws.services.ecr.AmazonECRClient` | `software.amazon.awssdk.services.ecr.EcrClient` |
| `amazonaws.services.ecs.AmazonECSAsyncClient` | `software.amazon.awssdk.services.ecs.EcsAsyncClient` |
| `amazonaws.services.ecs.AmazonECSClient` | `software.amazon.awssdk.services.ecs.EcsClient` |
| `amazonaws.services.eks.AmazonEKSAsyncClient` | `software.amazon.awssdk.services.eks.EksAsyncClient` |
| `amazonaws.services.eks.AmazonEKSClient` | `software.amazon.awssdk.services.eks.EksClient` |
| `amazonaws.services.elasticache.AmazonElastiCacheAsyncClient` | `software.amazon.awssdk.services.elasticache.ElastiCacheAsyncClient` |
| `amazonaws.services.elasticache.AmazonElastiCacheClient` | `software.amazon.awssdk.services.elasticache.ElastiCacheClient` |
| `amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkAsyncClient` | `software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkAsyncClient` |
| `amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient` | `software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient` |
| `amazonaws.services.elasticfilesystem.AmazonElasticFileSystemAsyncClient` | `software.amazon.awssdk.services.efs.EfsAsyncClient` |
| `amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient` | `software.amazon.awssdk.services.efs.EfsClient` |
| `amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsyncClient` | `software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient` |
| `amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient` | `software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient` |
| `amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingAsyncClient` | `software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2AsyncClient` |
| `amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient` | `software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client` |
| `amazonaws.services.elasticmapreduce.AmazonElasticMapReduceAsyncClient` | `software.amazon.awssdk.services.emr.EmrAsyncClient` |
| `amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient` | `software.amazon.awssdk.services.emr.EmrClient` |
| `amazonaws.services.elasticsearch.AWSElasticsearchAsyncClient` | `software.amazon.awssdk.services.elasticsearch.ElasticsearchAsyncClient` |
| `amazonaws.services.elasticsearch.AWSElasticsearchClient` | `software.amazon.awssdk.services.elasticsearch.ElasticsearchClient` |
| `amazonaws.services.elastictranscoder.AmazonElasticTranscoderAsyncClient` | `software.amazon.awssdk.services.elastictranscoder.ElasticTranscoderAsyncClient` |
| `amazonaws.services.elastictranscoder.AmazonElasticTranscoderClient` | `software.amazon.awssdk.services.elastictranscoder.ElasticTranscoderClient` |
| `amazonaws.services.fms.AWSFMSAsyncClient` | `software.amazon.awssdk.services.fms.FmsAsyncClient` |
| `amazonaws.services.fms.AWSFMSClient` | `software.amazon.awssdk.services.fms.FmsClient` |
| `amazonaws.services.gamelift.AmazonGameLiftAsyncClient` | `software.amazon.awssdk.services.gamelift.GameLiftAsyncClient` |
| `amazonaws.services.gamelift.AmazonGameLiftClient` | `software.amazon.awssdk.services.gamelift.GameLiftClient` |
| `amazonaws.services.glacier.AmazonGlacierAsyncClient` | `software.amazon.awssdk.services.glacier.GlacierAsyncClient` |
| `amazonaws.services.glacier.AmazonGlacierClient` | `software.amazon.awssdk.services.glacier.GlacierClient` |
| `amazonaws.services.glue.AWSGlueAsyncClient` | `software.amazon.awssdk.services.glue.GlueAsyncClient` |
| `amazonaws.services.glue.AWSGlueClient` | `software.amazon.awssdk.services.glue.GlueClient` |
| `amazonaws.services.greengrass.AWSGreengrassAsyncClient` | `software.amazon.awssdk.services.greengrass.GreengrassAsyncClient` |
| `amazonaws.services.greengrass.AWSGreengrassClient` | `software.amazon.awssdk.services.greengrass.GreengrassClient` |
| `amazonaws.services.guardduty.AmazonGuardDutyAsyncClient` | `software.amazon.awssdk.services.guardduty.GuardDutyAsyncClient` |
| `amazonaws.services.guardduty.AmazonGuardDutyClient` | `software.amazon.awssdk.services.guardduty.GuardDutyClient` |
| `amazonaws.services.health.AWSHealthAsyncClient` | `software.amazon.awssdk.services.health.HealthAsyncClient` |
| `amazonaws.services.health.AWSHealthClient` | `software.amazon.awssdk.services.health.HealthClient` |
| `amazonaws.services.identitymanagement.AmazonIdentityManagementAsyncClient` | `software.amazon.awssdk.services.iam.IamAsyncClient` |
| `amazonaws.services.identitymanagement.AmazonIdentityManagementClient` | `software.amazon.awssdk.services.iam.IamClient` |
| `amazonaws.services.importexport.AmazonImportExportAsyncClient` | `software.amazon.awssdk.services.importexport.ImportExportAsyncClient` |
| `amazonaws.services.importexport.AmazonImportExportClient` | `software.amazon.awssdk.services.importexport.ImportExportClient` |
| `amazonaws.services.inspector.AmazonInspectorAsyncClient` | `software.amazon.awssdk.services.inspector.InspectorAsyncClient` |
| `amazonaws.services.inspector.AmazonInspectorClient` | `software.amazon.awssdk.services.inspector.InspectorClient` |
| `amazonaws.services.iot.AWSIotAsyncClient` | `software.amazon.awssdk.services.iot.IotAsyncClient` |
| `amazonaws.services.iot.AWSIotClient` | `software.amazon.awssdk.services.iot.IotClient` |
| `amazonaws.services.iot1clickdevices.AWSIoT1ClickDevicesAsyncClient` | `software.amazon.awssdk.services.iot1clickdevices.Iot1ClickDevicesAsyncClient` |
| `amazonaws.services.iot1clickdevices.AWSIoT1ClickDevicesClient` | `software.amazon.awssdk.services.iot1clickdevices.Iot1ClickDevicesClient` |
| `amazonaws.services.iot1clickprojects.AWSIoT1ClickProjectsAsyncClient` | `software.amazon.awssdk.services.iot1clickprojects.Iot1ClickProjectsAsyncClient` |
| `amazonaws.services.iot1clickprojects.AWSIoT1ClickProjectsClient` | `software.amazon.awssdk.services.iot1clickprojects.Iot1ClickProjectsClient` |
| `amazonaws.services.iotanalytics.AWSIoTAnalyticsAsyncClient` | `software.amazon.awssdk.services.iotanalytics.IotAnalyticsAsyncClient` |
| `amazonaws.services.iotanalytics.AWSIoTAnalyticsClient` | `software.amazon.awssdk.services.iotanalytics.IotAnalyticsClient` |
| `amazonaws.services.iotdata.AWSIotDataAsyncClient` | `software.amazon.awssdk.services.iotdata.IotDataAsyncClient` |
| `amazonaws.services.iotdata.AWSIotDataClient` | `software.amazon.awssdk.services.iotdata.IotDataClient` |
| `amazonaws.services.iotjobsdataplane.AWSIoTJobsDataPlaneAsyncClient` | `software.amazon.awssdk.services.iotdataplane.IotDataPlaneAsyncClient` |
| `amazonaws.services.iotjobsdataplane.AWSIoTJobsDataPlaneClient` | `software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient` |
| `amazonaws.services.kinesis.AmazonKinesisAsyncClient` | `software.amazon.awssdk.services.kinesis.KinesisAsyncClient` |
| `amazonaws.services.kinesis.AmazonKinesisClient` | `software.amazon.awssdk.services.kinesis.KinesisClient` |
| `amazonaws.services.kinesisanalytics.AmazonKinesisAnalyticsAsyncClient` | `software.amazon.awssdk.services.kinesisanalytics.KinesisAnalyticsAsyncClient` |
| `amazonaws.services.kinesisanalytics.AmazonKinesisAnalyticsClient` | `software.amazon.awssdk.services.kinesisanalytics.KinesisAnalyticsClient` |
| `amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClient` | `software.amazon.awssdk.services.firehose.FirehoseAsyncClient` |
| `amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClient` | `software.amazon.awssdk.services.firehose.FirehoseClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaAsyncClient` | `software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaAsyncClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoArchivedMediaClient` | `software.amazon.awssdk.services.kinesisvideoarchivedmedia.KinesisVideoArchivedMediaClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoAsyncClient` | `software.amazon.awssdk.services.kinesisvideo.KinesisVideoAsyncClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoClient` | `software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoMediaAsyncClient` | `software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaAsyncClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoMediaClient` | `software.amazon.awssdk.services.kinesisvideomedia.KinesisVideoMediaClient` |
| `amazonaws.services.kinesisvideo.AmazonKinesisVideoPutMediaClient` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| `amazonaws.services.kms.AWSKMSAsyncClient` | `software.amazon.awssdk.services.kms.KmsAsyncClient` |
| `amazonaws.services.kms.AWSKMSClient` | `software.amazon.awssdk.services.kms.KmsClient` |
| `amazonaws.services.lambda.AWSLambdaAsyncClient` | `software.amazon.awssdk.services.lambda.LambdaAsyncClient` |
| `amazonaws.services.lambda.AWSLambdaClient` | `software.amazon.awssdk.services.lambda.LambdaClient` |
| `amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingAsyncClient` | `software.amazon.awssdk.services.lexmodelbuilding.LexModelBuildingAsyncClient` |
| `amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClient` | `software.amazon.awssdk.services.lexmodelbuilding.LexModelBuildingClient` |
| `amazonaws.services.lexruntime.AmazonLexRuntimeAsyncClient` | `software.amazon.awssdk.services.lexruntime.LexRuntimeAsyncClient` |
| `amazonaws.services.lexruntime.AmazonLexRuntimeClient` | `software.amazon.awssdk.services.lexruntime.LexRuntimeClient` |
| `amazonaws.services.lightsail.AmazonLightsailAsyncClient` | `software.amazon.awssdk.services.lightsail.LightsailAsyncClient` |
| `amazonaws.services.lightsail.AmazonLightsailClient` | `software.amazon.awssdk.services.lightsail.LightsailClient` |
| `amazonaws.services.logs.AWSLogsAsyncClient` | `software.amazon.awssdk.services.logs.LogsAsyncClient` |
| `amazonaws.services.logs.AWSLogsClient` | `software.amazon.awssdk.services.logs.LogsClient` |
| `amazonaws.services.machinelearning.AmazonMachineLearningAsyncClient` | `software.amazon.awssdk.services.machinelearning.MachineLearningAsyncClient` |
| `amazonaws.services.machinelearning.AmazonMachineLearningClient` | `software.amazon.awssdk.services.machinelearning.MachineLearningClient` |
| `amazonaws.services.macie.AmazonMacieAsyncClient` | `software.amazon.awssdk.services.macie.MacieAsyncClient` |
| `amazonaws.services.macie.AmazonMacieClient` | `software.amazon.awssdk.services.macie.MacieClient` |
| `amazonaws.services.marketplacecommerceanalytics.AWSMarketplaceCommerceAnalyticsAsyncClient` | `software.amazon.awssdk.services.marketplacecommerceanalytics.MarketplaceCommerceAnalyticsAsyncClient` |
| `amazonaws.services.marketplacecommerceanalytics.AWSMarketplaceCommerceAnalyticsClient` | `software.amazon.awssdk.services.marketplacecommerceanalytics.MarketplaceCommerceAnalyticsClient` |
| `amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementAsyncClient` | `software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementAsyncClient` |
| `amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementClient` | `software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementClient` |
| `amazonaws.services.marketplacemetering.AWSMarketplaceMeteringAsyncClient` | `software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringAsyncClient` |
| `amazonaws.services.marketplacemetering.AWSMarketplaceMeteringClient` | `software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringClient` |
| `amazonaws.services.mediaconvert.AWSMediaConvertAsyncClient` | `software.amazon.awssdk.services.mediaconvert.MediaConvertAsyncClient` |
| `amazonaws.services.mediaconvert.AWSMediaConvertClient` | `software.amazon.awssdk.services.mediaconvert.MediaConvertClient` |
| `amazonaws.services.medialive.AWSMediaLiveAsyncClient` | `software.amazon.awssdk.services.medialive.MediaLiveAsyncClient` |
| `amazonaws.services.medialive.AWSMediaLiveClient` | `software.amazon.awssdk.services.medialive.MediaLiveClient` |
| `amazonaws.services.mediapackage.AWSMediaPackageAsyncClient` | `software.amazon.awssdk.services.mediapackage.MediaPackageAsyncClient` |
| `amazonaws.services.mediapackage.AWSMediaPackageClient` | `software.amazon.awssdk.services.mediapackage.MediaPackageClient` |
| `amazonaws.services.mediastore.AWSMediaStoreAsyncClient` | `software.amazon.awssdk.services.mediastore.MediaStoreAsyncClient` |
| `amazonaws.services.mediastore.AWSMediaStoreClient` | `software.amazon.awssdk.services.mediastore.MediaStoreClient` |
| `amazonaws.services.mediastoredata.AWSMediaStoreDataAsyncClient` | `software.amazon.awssdk.services.mediastoredata.MediaStoreDataAsyncClient` |
| `amazonaws.services.mediastoredata.AWSMediaStoreDataClient` | `software.amazon.awssdk.services.mediastoredata.MediaStoreDataClient` |
| `amazonaws.services.mediatailor.AWSMediaTailorAsyncClient` | `software.amazon.awssdk.services.mediatailor.MediaTailorAsyncClient` |
| `amazonaws.services.mediatailor.AWSMediaTailorClient` | `software.amazon.awssdk.services.mediatailor.MediaTailorClient` |
| `amazonaws.services.migrationhub.AWSMigrationHubAsyncClient` | `software.amazon.awssdk.services.migrationhub.MigrationHubAsyncClient` |
| `amazonaws.services.migrationhub.AWSMigrationHubClient` | `software.amazon.awssdk.services.migrationhub.MigrationHubClient` |
| `amazonaws.services.mobile.AWSMobileAsyncClient` | `software.amazon.awssdk.services.mobile.MobileAsyncClient` |
| `amazonaws.services.mobile.AWSMobileClient` | `software.amazon.awssdk.services.mobile.MobileClient` |
| `amazonaws.services.mq.AmazonMQAsyncClient` | `software.amazon.awssdk.services.mq.MqAsyncClient` |
| `amazonaws.services.mq.AmazonMQClient` | `software.amazon.awssdk.services.mq.MqClient` |
| `amazonaws.services.mturk.AmazonMTurkAsyncClient` | `software.amazon.awssdk.services.mturk.MTurkAsyncClient` |
| `amazonaws.services.mturk.AmazonMTurkClient` | `software.amazon.awssdk.services.mturk.MTurkClient` |
| `amazonaws.services.neptune.AmazonNeptuneAsyncClient` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| `amazonaws.services.neptune.AmazonNeptuneClient` | Not Supported ([Request Feature](https://github.com/aws/aws-sdk-java-v2/issues/new)) |
| `amazonaws.services.opsworks.AWSOpsWorksAsyncClient` | `software.amazon.awssdk.services.opsworks.OpsWorksAsyncClient` |
| `amazonaws.services.opsworks.AWSOpsWorksClient` | `software.amazon.awssdk.services.opsworks.OpsWorksClient` |
| `amazonaws.services.opsworkscm.AWSOpsWorksCMAsyncClient` | `software.amazon.awssdk.services.opsworkscm.OpsWorksCmAsyncClient` |
| `amazonaws.services.opsworkscm.AWSOpsWorksCMClient` | `software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient` |
| `amazonaws.services.organizations.AWSOrganizationsAsyncClient` | `software.amazon.awssdk.services.organizations.OrganizationsAsyncClient` |
| `amazonaws.services.organizations.AWSOrganizationsClient` | `software.amazon.awssdk.services.organizations.OrganizationsClient` |
| `amazonaws.services.pi.AWSPIAsyncClient` | `software.amazon.awssdk.services.pi.PiAsyncClient` |
| `amazonaws.services.pi.AWSPIClient` | `software.amazon.awssdk.services.pi.PiClient` |
| `amazonaws.services.pinpoint.AmazonPinpointAsyncClient` | `software.amazon.awssdk.services.pinpoint.PinpointAsyncClient` |
| `amazonaws.services.pinpoint.AmazonPinpointClient` | `software.amazon.awssdk.services.pinpoint.PinpointClient` |
| `amazonaws.services.polly.AmazonPollyAsyncClient` | `software.amazon.awssdk.services.polly.PollyAsyncClient` |
| `amazonaws.services.polly.AmazonPollyClient` | `software.amazon.awssdk.services.polly.PollyClient` |
| `amazonaws.services.pricing.AWSPricingAsyncClient` | `software.amazon.awssdk.services.pricing.PricingAsyncClient` |
| `amazonaws.services.pricing.AWSPricingClient` | `software.amazon.awssdk.services.pricing.PricingClient` |
| `amazonaws.services.rds.AmazonRDSAsyncClient` | `software.amazon.awssdk.services.rds.RdsAsyncClient` |
| `amazonaws.services.rds.AmazonRDSClient` | `software.amazon.awssdk.services.rds.RdsClient` |
| `amazonaws.services.redshift.AmazonRedshiftAsyncClient` | `software.amazon.awssdk.services.redshift.RedshiftAsyncClient` |
| `amazonaws.services.redshift.AmazonRedshiftClient` | `software.amazon.awssdk.services.redshift.RedshiftClient` |
| `amazonaws.services.rekognition.AmazonRekognitionAsyncClient` | `software.amazon.awssdk.services.rekognition.RekognitionAsyncClient` |
| `amazonaws.services.rekognition.AmazonRekognitionClient` | `software.amazon.awssdk.services.rekognition.RekognitionClient` |
| `amazonaws.services.resourcegroups.AWSResourceGroupsAsyncClient` | `software.amazon.awssdk.services.resourcegroups.ResourceGroupsAsyncClient` |
| `amazonaws.services.resourcegroups.AWSResourceGroupsClient` | `software.amazon.awssdk.services.resourcegroups.ResourceGroupsClient` |
| `amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIAsyncClient` | `software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiAsyncClient` |
| `amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClient` | `software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient` |
| `amazonaws.services.route53.AmazonRoute53AsyncClient` | `software.amazon.awssdk.services.route53.Route53AsyncClient` |
| `amazonaws.services.route53.AmazonRoute53Client` | `software.amazon.awssdk.services.route53.Route53Client` |
| `amazonaws.services.route53domains.AmazonRoute53DomainsAsyncClient` | `software.amazon.awssdk.services.route53domains.Route53DomainsAsyncClient` |
| `amazonaws.services.route53domains.AmazonRoute53DomainsClient` | `software.amazon.awssdk.services.route53domains.Route53DomainsClient` |
| `amazonaws.services.s3.AmazonS3Client` | `software.amazon.awssdk.services.s3.S3Client` |
| `amazonaws.services.sagemaker.AmazonSageMakerAsyncClient` | `software.amazon.awssdk.services.sagemaker.SageMakerAsyncClient` |
| `amazonaws.services.sagemaker.AmazonSageMakerClient` | `software.amazon.awssdk.services.sagemaker.SageMakerClient` |
| `amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeAsyncClient` | `software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeAsyncClient` |
| `amazonaws.services.sagemakerruntime.AmazonSageMakerRuntimeClient` | `software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient` |
| `amazonaws.services.secretsmanager.AWSSecretsManagerAsyncClient` | `software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient` |
| `amazonaws.services.secretsmanager.AWSSecretsManagerClient` | `software.amazon.awssdk.services.secretsmanager.SecretsManagerClient` |
| `amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClient` | `software.amazon.awssdk.services.sts.StsAsyncClient` |
| `amazonaws.services.securitytoken.AWSSecurityTokenServiceClient` | `software.amazon.awssdk.services.sts.StsClient` |
| `amazonaws.services.serverlessapplicationrepository.AWSServerlessApplicationRepositoryAsyncClient` | `software.amazon.awssdk.services.serverlessapplicationrepository.ServerlessApplicationRepositoryAsyncClient` |
| `amazonaws.services.serverlessapplicationrepository.AWSServerlessApplicationRepositoryClient` | `software.amazon.awssdk.services.serverlessapplicationrepository.ServerlessApplicationRepositoryClient` |
| `amazonaws.services.servermigration.AWSServerMigrationAsyncClient` | `software.amazon.awssdk.services.sms.SmsAsyncClient` |
| `amazonaws.services.servermigration.AWSServerMigrationClient` | `software.amazon.awssdk.services.sms.SmsClient` |
| `amazonaws.services.servicecatalog.AWSServiceCatalogAsyncClient` | `software.amazon.awssdk.services.servicecatalog.ServiceCatalogAsyncClient` |
| `amazonaws.services.servicecatalog.AWSServiceCatalogClient` | `software.amazon.awssdk.services.servicecatalog.ServiceCatalogClient` |
| `amazonaws.services.servicediscovery.AWSServiceDiscoveryAsyncClient` | `software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryAsyncClient` |
| `amazonaws.services.servicediscovery.AWSServiceDiscoveryClient` | `software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient` |
| `amazonaws.services.shield.AWSShieldAsyncClient` | `software.amazon.awssdk.services.shield.ShieldAsyncClient` |
| `amazonaws.services.shield.AWSShieldClient` | `software.amazon.awssdk.services.shield.ShieldClient` |
| `amazonaws.services.simpledb.AmazonSimpleDBAsyncClient` | `software.amazon.awssdk.services.simpledb.SimpleDbAsyncClient` |
| `amazonaws.services.simpledb.AmazonSimpleDBClient` | `software.amazon.awssdk.services.simpledb.SimpleDbClient` |
| `amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient` | `software.amazon.awssdk.services.ses.SesAsyncClient` |
| `amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient` | `software.amazon.awssdk.services.ses.SesClient` |
| `amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementAsyncClient` | `software.amazon.awssdk.services.ssm.SsmAsyncClient` |
| `amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient` | `software.amazon.awssdk.services.ssm.SsmClient` |
| `amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsyncClient` | `software.amazon.awssdk.services.swf.SwfAsyncClient` |
| `amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient` | `software.amazon.awssdk.services.swf.SwfClient` |
| `amazonaws.services.snowball.AmazonSnowballAsyncClient` | `software.amazon.awssdk.services.snowball.SnowballAsyncClient` |
| `amazonaws.services.snowball.AmazonSnowballClient` | `software.amazon.awssdk.services.snowball.SnowballClient` |
| `amazonaws.services.sns.AmazonSNSAsyncClient` | `software.amazon.awssdk.services.sns.SnsAsyncClient` |
| `amazonaws.services.sns.AmazonSNSClient` | `software.amazon.awssdk.services.sns.SnsClient` |
| `amazonaws.services.sqs.AmazonSQSAsyncClient` | `software.amazon.awssdk.services.sqs.SqsAsyncClient` |
| `amazonaws.services.sqs.AmazonSQSClient` | `software.amazon.awssdk.services.sqs.SqsClient` |
| `amazonaws.services.stepfunctions.AWSStepFunctionsAsyncClient` | `software.amazon.awssdk.services.sfn.SfnAsyncClient` |
| `amazonaws.services.stepfunctions.AWSStepFunctionsClient` | `software.amazon.awssdk.services.sfn.SfnClient` |
| `amazonaws.services.storagegateway.AWSStorageGatewayAsyncClient` | `software.amazon.awssdk.services.storagegateway.StorageGatewayAsyncClient` |
| `amazonaws.services.storagegateway.AWSStorageGatewayClient` | `software.amazon.awssdk.services.storagegateway.StorageGatewayClient` |
| `amazonaws.services.support.AWSSupportAsyncClient` | `software.amazon.awssdk.services.support.SupportAsyncClient` |
| `amazonaws.services.support.AWSSupportClient` | `software.amazon.awssdk.services.support.SupportClient` |
| `amazonaws.services.transcribe.AmazonTranscribeAsyncClient` | `software.amazon.awssdk.services.transcribe.TranscribeAsyncClient` |
| `amazonaws.services.transcribe.AmazonTranscribeClient` | `software.amazon.awssdk.services.transcribe.TranscribeClient` |
| `amazonaws.services.translate.AmazonTranslateAsyncClient` | `software.amazon.awssdk.services.translate.TranslateAsyncClient` |
| `amazonaws.services.translate.AmazonTranslateClient` | `software.amazon.awssdk.services.translate.TranslateClient` |
| `amazonaws.services.waf.AWSWAFAsyncClient` | `software.amazon.awssdk.services.waf.WafAsyncClient` |
| `amazonaws.services.waf.AWSWAFClient` | `software.amazon.awssdk.services.waf.WafClient` |
| `amazonaws.services.waf.AWSWAFRegionalAsyncClient` | `software.amazon.awssdk.services.waf.regional.WafRegionalAsyncClient` |
| `amazonaws.services.waf.AWSWAFRegionalClient` | `software.amazon.awssdk.services.waf.regional.WafRegionalClient` |
| `amazonaws.services.workdocs.AmazonWorkDocsAsyncClient` | `software.amazon.awssdk.services.workdocs.WorkDocsAsyncClient` |
| `amazonaws.services.workdocs.AmazonWorkDocsClient` | `software.amazon.awssdk.services.workdocs.WorkDocsClient` |
| `amazonaws.services.workmail.AmazonWorkMailAsyncClient` | `software.amazon.awssdk.services.workmail.WorkMailAsyncClient` |
| `amazonaws.services.workmail.AmazonWorkMailClient` | `software.amazon.awssdk.services.workmail.WorkMailClient` |
| `amazonaws.services.workspaces.AmazonWorkspacesAsyncClient` | `software.amazon.awssdk.services.workspaces.WorkSpacesAsyncClient` |
| `amazonaws.services.workspaces.AmazonWorkspacesClient` | `software.amazon.awssdk.services.workspaces.WorkSpacesClient` |
| `amazonaws.services.xray.AWSXRayAsyncClient` | `software.amazon.awssdk.services.xray.XRayAsyncClient` |
| `amazonaws.services.xray.AWSXRayClient` | `software.amazon.awssdk.services.xray.XRayClient` |

# 7. High-Level Libraries

1. All high-level libraries have been removed.
2. High-level libraries will be re-designed to match the 2.0 programming model and re-introduced over time.
