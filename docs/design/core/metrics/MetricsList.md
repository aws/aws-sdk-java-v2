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

| Name                          | Type          | Collector Path                 | Description |
|-------------------------------|---------------|--------------------------------|-------------|
| ServiceId                     | `String`      | `ApiCall`                      | The unique ID for the service. This is present for all API call metrics.|
| OperationName                 | `String`      | `ApiCall`                      | The name of the service operation being invoked. This is present for all API call metrics.|
| ApiCallDuration               | `Duration`    | `ApiCall`                      | The duration of the API call. This includes all call attempts made.|
| MarshallingDuration           | `Duration`    | `ApiCall`                      | The duration of time taken to marshall the SDK request to an HTTP request.|
| CredentialsFetchDuration      | `Duration`    | `ApiCall`                      | The duration of time taken to fetch signing credentials for the request.|
| SigningDuration               | `Duration`    | `ApiCall` > `ApiCallAttempt`   | The duration fo time taken to sign the HTTP request.|
| HttpRequestRoundTripTime      | `Duration`    | `ApiCall` > `ApiCallAttempt`   | The total time take to send a HTTP request and receive the response.|
| HttpStatusCode                | `Integer`     | `ApiCall` > `ApiCallAttempt`   | The status code of the HTTP response.|
| AwsRequestId                  | `String`      | `ApiCall` > `ApiCallAttempt`   | The extended request ID of the service request.|
| AwsExtendedRequestId          | `String`      | `ApiCall` > `ApiCallAttempt`   | The extended request ID of the service request.|
| Exception                     | `Throwable`   | `ApiCall` > `ApiCallAttempt`   | The exception thrown during request execution. Note this may be a service error that has been unmarshalled, or a client side exception.|

## HTTP Metrics

The set of HTTP metrics below are collected by components that implement the [HTTP SPI](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/http-client-spi). Which metrics are collected depends on the specific HTTP library used to implement the SPI; not all libraries will allow the collection of every metric below.

Note that in the context of an SDK client API call, all `HttpClient` collectors are children of `ApiCallAttept`; i.e. the full path to HTTP client metrics for an individual API call attempt is `ApiCall` > `ApiCallAttept` > `HttpClient`.

### Common HTTP Metrics

Below are the metrics common to both HTTP/1.1 and HTTP/2 operations.

The constants are located in `software.amazon.awssdk.http.HttpMetric` class in the `http-spi` module.

| Name                          | Type      | Collector Path | Description | 
|-------------------------------|-----------|----------------|-------------|
| HttpClientName                | `String`  | `HttpClient`   |  The name of the HTTP client. |
| MaxConcurrency                | `Integer` | `HttpClient`   | For HTTP/1.1 operations, this is equal to the maximum number of TCP connections that can be be pooled by the HTTP client. For HTTP/2 operations, this is equal to the maximum number of streams that can be pooled by the HTTP client.
| LeasedConcurrency             | `Integer` | `HttpClient`   | The number of requests that are currently being executed by the HTTP client. |
| PendingConcurrencyAcquires    | `Integer` | `HttpClient`   | The number of requests that are awaiting concurrency to be made available from the HTTP client. |

### HTTP/2 Metrics

Below are the metrics specific to HTTP/2 operations.

|  Name                    | Type      | Collector Path  | Description  |
|--------------------------|-----------|---------------- |--------------|
| LocalStreamWindowSize    | `Integer` | `HttpClient`    | The local HTTP/2 window size in bytes for the stream that this request was executed on. |
| RemoteStreamWindowSize   | `Integer` | `HttpClient`    | The remote HTTP/2 window size in bytes for the stream that this request was executed on. |
