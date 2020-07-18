# Collected Metrics

 This document lists the metrics collected by various components of the SDK,
 namely the core libraries, and HTTP clients.

 A note on collector path: The path is assuming a `MetricCollection` tree rooted at an API call.

## Core Metrics

The set of core metrics includes all metrics collected by the core components
of the SDK. This includes components like SDK service clients,
request/resposne marshallers and unmarshallers, and signers.

All the in code constants associated with metric below can be found in the
[`software.amazon.awssdk.core.metrics.CoreMetric`](https://github.com/aws/aws-sdk-java-v2/blob/8c192e3b04892987bf0872f76ba4f65167f3a872/core/sdk-core/src/main/java/software/amazon/awssdk/core/metrics/CoreMetric.java#L24)
class within `sdk-core`.

| Name                          | Type          | Description |
|-------------------------------|---------------|-------------|
| ServiceId                     | `String`      | The unique ID for the service. This is present for all API call metrics.|
| OperationName                 | `String`      | The name of the service operation being invoked. This is present for all API call metrics.|
| ApiCallDuration               | `Duration`    | The duration of the API call. This includes all call attempts made.|
| ApiCallSuccessful             | `Boolean`     | True if the API call succeeded, false otherwise. |
| BackoffDelayDuration          | `Duration`    | The duration of time that the SDK has waited before this API call attempt, based on the retry policy. |
| MarshallingDuration           | `Duration`    | The duration of time taken to marshall the SDK request to an HTTP request.|
| CredentialsFetchDuration      | `Duration`    | The duration of time taken to fetch signing credentials for the request.|
| SigningDuration               | `Duration`    | The duration of time taken to sign the HTTP request.|
| AwsRequestId                  | `String`      | The request ID of the service request.|
| AwsExtendedRequestId          | `String`      | The extended request ID of the service request.|
| UnmarshallingDuration         | `Duration`    | The duration of time taken to unmarshall the HTTP response to an SDK response. |
| ServiceCallDuration           | `Duration`    | The duration of time  taken to connect to the service (or acquire a connection from the connection pool), send the serialized request and receive the initial response (e.g. HTTP status code and headers). This DOES NOT include the time taken to read the entire response from the service. |
| `RetryCount`                  | `Integer`    | The number of retries that the SDK performed in the execution of the request. 0 implies that the request worked the first  time, and no retries were attempted. |

## HTTP Metrics

The set of HTTP metrics below are collected by components that implement the [HTTP SPI](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/http-client-spi). Which metrics are collected depends on the specific HTTP library used to implement the SPI; not all libraries will allow the collection of every metric below.

Note that in the context of an SDK client API call, all `HttpClient` collectors are children of `ApiCallAttept`; i.e. the full path to HTTP client metrics for an individual API call attempt is `ApiCall` > `ApiCallAttept` > `HttpClient`.

### Common HTTP Metrics

Below are the metrics common to both HTTP/1.1 and HTTP/2 operations.

The constants are located in `software.amazon.awssdk.http.HttpMetric` class in the `http-spi` module.

| Name                          | Type      | Description | 
|-------------------------------|-----------|-------------|
| HttpClientName                | `String`  |  The name of the HTTP client. |
| MaxConcurrency                | `Integer` | For HTTP/1.1 operations, this is equal to the maximum number of TCP connections that can be be pooled by the HTTP client. For HTTP/2 operations, this is equal to the maximum number of streams that can be pooled by the HTTP client.
| LeasedConcurrency             | `Integer` | The number of requests that are currently being executed by the HTTP client. |
| PendingConcurrencyAcquires    | `Integer` | The number of requests that are awaiting concurrency to be made available from the HTTP client. |
| HttpStatusCode                | `Integer` | The status code of the HTTP response. |

### HTTP/2 Metrics

Below are the metrics specific to HTTP/2 operations.

|  Name                    | Type      | Description  |
|--------------------------|-----------|--------------|
| LocalStreamWindowSize    | `Integer` | The local HTTP/2 window size in bytes for the stream that this request was executed on. |
| RemoteStreamWindowSize   | `Integer` | The remote HTTP/2 window size in bytes for the stream that this request was executed on. |
