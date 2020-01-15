Here is the detailed list of metrics that SDK can collect. Each metric belongs to a category. If a category is enabled,
then all metrics belonging to that category will be collected by the SDK.

## Category

1) Default - All metrics under this category will be collected when the metrics are enabled
2) HttpClient - Additional information collected for http client. The metrics collected for each http client can vary
3) All - All metrics collected by the SDK comes under this category. This can be useful for debugging purposes.

Note: When metrics feature is enabled, only the `Default` category metrics are collected. Other categories should be
explicitly enabled.

## Information collected at application level (Category: Default)

| Metric Name                    | Meter         | Description      |
| ------------------             | -----------   | ---------------- |
| RequestCount                   | Counter       | Total number of requests (successful and failed) made from your code to AWS services
| SuccessRequestCount            | Counter       | Total number of requests from your code to AWS services that resulted in a successful response
| FailedRequestCount             | Counter       | Total number of requests from your code to AWS services that resulted in a failure. This can be expanded later to categorize the failures into buckets (like ClientErrorCount, ServiceErrorCount, ConnectionErrorCount etc)
                  
## Information collected for each request (ApiCall) (Category: Default)

| Metric Name                    | Meter         | Description      |
| ------------------             | -----------   | ---------------- |
| Service                        | ConstantGauge | Service ID of the AWS service that the API request is made against
| Api                            | ConstantGauge | The name of the AWS API the request is made to
| StreamingRequest               | ConstantGauge | True if the request has streaming payload
| StreamingResponse              | ConstantGauge | True if the response has streaming payload
| ApiCallStartTime               | Timer         | The start time of the request
| ApiCallEndTime                 | Timer         | The end time of the request
| ApiCallLatency                 | Timer         | The total time taken to finish a request (inclusive of all retries), ApiCallEndTime - ApiCallStartTime
| MarshallingLatency             | Timer         | The time taken to marshall the request
| ApiCallAttemptCount            | Counter       | Total number of attempts that were made by the service client to fulfill this request before succeeding or failing. (Value is 1 if there are no retries)

Each ApiCall can have multiple attempts before the call succeed or fail. The following metrics are collected for each ApiCall Attempt.

| Metric Name                    | Meter         | Description      |
| ------------------             | -----------   | ---------------- |
| ApiCallAttemptStartTime        | Timer         | The start time of each Api call attempt
| SigningLatency                 | Timer         | The time taken to sign the request in an Api Call Attempt
| HttpRequestRoundTripLatency    | Timer         | The time taken by the underlying http client to start the Api call attempt and return the response
| UnmarshallingLatency           | Timer         | The time taken to unmarshall the response (same metric for both successful and failed requests)
| ApiCallAttemptEndTime          | Timer         | The end time of a Api call attempt
| ApiCallAttemptLatency          | Timer         | The total time taken for an Api call attempt (exclusive of retries), ApiCallAttemptEndTime - ApiCallAttemptStartTime
| AwsRequestId                   | ConstantGauge      | The request Id for the request. Represented by `x-amz-request-id` header in response
| ExtendedRequestId              | ConstantGauge      | The extended request Id for the request. Represented by `x-amz-id-2` header in response
| HttpStatusCode                 | ConstantGauge      | The http status code returned in the response. Null if there is no response
| AwsException                   | ConstantGauge      | The Aws exception code returned by the service. This is included for each Api call attempt if the call results in a failure and caused by service
| SdkException                   | ConstantGauge      | The error name for any failure that is due to something other than an Aws exception. This is included for each API call attempt if the call results in a failure and is caused by something other than service

For each attempt, the following http client metrics are collected:

| Metric Name                    | Meter         | Description      |
| ------------------             | -----------   | ---------------- |
| HttpClientName                 | ConstantGauge      | Name of the underlying http client (Apache, Netty, UrlConnection)
| MaxConnections                 | Gauge         | Maximum number of connections allowed in the connection pool
| AvailableConnections           | Gauge         | The number of idle connections in the connection pool that are ready to serve a request
| LeasedConnections              | Gauge         | The number of connections in the connection pool that are busy serving requests
| PendingRequests                | Gauge         | The number of requests awaiting a free connection from the pool

## Additional Information collected for each http client (Category: HttpClient)

### ApacheHttpClient
HttpClientName - Apache

No additional metrics available for apache client currently

### UrlConnectionHttpClient
HttpClientName - UrlConnection

No additional metrics available for url connection client currently

### NettyNioAsyncHttpClient
HttpClientName - Netty

| Metric Name                    |    Meter      |    Description   |
| ------------------             | -----------   | ---------------- |
| FailedConnectionClose          | Counter       | Number of times a connection close has failed
| FailedPoolAcquire              | Counter       | Number of times a request failed to acquire a connection

For Http2 requests,

| Metric Name                    |    Meter      |    Description   |
| ------------------             | -----------   | ---------------- |
| ConnectionId                   | ConstantGauge      | The identifier for a connection
| MaxStreamCount                 | Gauge         | Maximum number of streams allowed on the connection
| CurrentStreamCount             | Gauge         | Number of active streams on the connection


## Information collected for event stream requests (Category: Default)

| Metric Name                    |    Meter      |    Description   |
| ------------------             | -----------   | ---------------- |
| RequestEventsReceivedCount     | Counter       | Number of events received from the client
| RequestEventsSentCount         | Counter       | Number of events sent to the service
| ResponseEventsReceivedCount    | Counter       | Number of events received from the service
| ResponseEventsDeliveredCount   | Counter       | Number of events delivered to the client
| RequestSubscriptionCreated     | Counter       | Number of request subscriptions created to deliver events from client to service (For event stream requests like startStreamTranscription API in Transcribe Streaming service)
| RequestSubscriptionCompleted   | Counter       | Number of request subscriptions completed 
| RequestSubscriptionCanceled    | Counter       | Number of request subscriptions canceled 
| ResponseSubscriptionCreated    | Counter       | Number of response subscriptions created to deliver events from service to client
| ResponseSubscriptionCompleted  | Counter       | Number of response subscriptions completed 
| ResponseSubscriptionCanceled   | Counter       | Number of response subscriptions canceled 


## FAQ
1) When is the end time calculated for async requests?  
   The end time is calculated when the future is completed (either successfully or exceptionally) as opposed to the time when future is returned from API
   
2) What errors are considered as throttling errors?
   The request was considered as throttled if one of the following conditions are met:
   1) The http status code is equal to: `429` or `503`
   2) The error code is equal to one of the following values:  
   SlowDown   
   SlowDownException  
   Throttling  
   ThrottlingException  
   Throttled  
   ThrottledException  
   ServiceUnavailable  
   ServiceUnavailableException  
   ServiceUnavailableError  
   ProvisionedThroughputExceededException  
   TooManyRequests  
   TooManyRequestsException  
   DescribeAttachmentLimitExceeded

           
## References
1) [V1 Metrics Description](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/metrics/package-summary.html)
