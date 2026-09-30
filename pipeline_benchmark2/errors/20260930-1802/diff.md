# Error behavior diff — `20260930-1802`

Baseline arm: `baseline`. Compared against: `bridge`.
23 cases, 92 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**19 behavioral difference(s).**

| fault | case | field | `baseline` | `bridge` |
|---|---|---|---|---|
| `throughput-exceeded` | persistent | `rawResponse` | present | absent |
| `throttling` | persistent | `rawResponse` | present | absent |
| `internal-error` | persistent | `rawResponse` | present | absent |
| `unavailable` | persistent | `rawResponse` | present | absent |
| `unavailable-retry-after` | persistent | `rawResponse` | present | absent |
| `resource-not-found` | persistent | `rawResponse` | present | absent |
| `resource-not-found` | transient | `rawResponse` | present | absent |
| `conditional-check-failed` | persistent | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | persistent | `rawResponse` | present | absent |
| `conditional-check-failed` | transient | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | transient | `rawResponse` | present | absent |
| `malformed-body` | persistent | `exception` | software.amazon.awssdk.core.exception.SdkClientException | software.amazon.smithy.java.core.serde.SerializationException |
| `malformed-body` | persistent | `cause` | java.io.UncheckedIOException | software.amazon.smithy.java.core.serde.SerializationException |
| `malformed-body` | persistent | `retryable` | false | - |
| `empty-500` | persistent | `rawResponse` | present | absent |
| `truncated-stream` | persistent | `exception` | software.amazon.awssdk.core.exception.SdkClientException | java.io.UncheckedIOException |
| `truncated-stream` | persistent | `cause` | java.io.UncheckedIOException | org.apache.hc.core5.http.ConnectionClosedException |
| `truncated-stream` | persistent | `retryable` | false | - |
| `connection-reset` | persistent | `cause` | org.apache.hc.core5.http.NoHttpResponseException | software.amazon.smithy.java.client.core.error.TransportException |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `throughput-exceeded` | transient | 852 | 2,448 |
| `throttling` | transient | 664 | 2,369 |
| `internal-error` | transient | 197 | 50 |
| `unavailable` | transient | 39 | 112 |
| `resource-not-found` | persistent | 23 | 4 |
| `conditional-check-failed` | persistent | 8 | 2 |
| `malformed-body` | transient | 56 | 197 |
| `empty-500` | persistent | 116 | 285 |
| `empty-500` | transient | 244 | 63 |
| `truncated-stream` | persistent | 156 | 51 |
| `connection-reset` | transient | 22 | 65 |

Message wording differences (not behavioral on their own):

| fault | case | `baseline` | `bridge` |
|---|---|---|---|
| `throughput-exceeded` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `throttling` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `internal-error` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `unavailable` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)` |
| `unavailable-retry-after` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)` |
| `resource-not-found` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `resource-not-found` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Service_returned_error_code_ConditionalCheckFailedException_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Service_returned_error_code_ConditionalCheckFailedException_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `malformed-body` | persistent | `Unable_to_unmarshall_response_(software.amazon.awssdk.thirdparty.jackson.core.io.JsonEOFException:_Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_(software...` | `Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_REDACTED_(`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION`_disabled);_byte_offset:_#25]` |
| `empty-500` | persistent | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `truncated-stream` | persistent | `Unable_to_unmarshall_response_(org.apache.hc.core5.http.ConnectionClosedException:_Premature_end_of_Content-Length_delimited_message_body_(expected:_89;_receive...` | `org.apache.hc.core5.http.ConnectionClosedException:_Premature_end_of_Content-Length_delimited_message_body_(expected:_89;_received:_25)` |
| `connection-reset` | persistent | `Unable_to_execute_HTTP_request:_The_target_server_failed_to_respond_(SDK_Attempt_Count:_3)` | `The_target_server_failed_to_respond` |

## What each arm did

| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId | retryable | throttling | wall ms |
|---|---|---|---:|---|---|---|---:|---|---|---|---:|
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 262 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 192 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1248 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 738 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 852 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2448 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1155 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 937 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 664 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2369 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 190 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 133 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 197 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 50 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 189 |
| `unavailable` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 282 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 39 |
| `unavailable` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 112 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2010 |
| `unavailable-retry-after` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2010 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2009 |
| `unavailable-retry-after` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2010 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 23 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 8 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 151 |
| `malformed-body` | persistent | `bridge` | 3 | throw | `SerializationException` | - | - | - | - | - | 201 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 56 |
| `malformed-body` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 197 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 116 |
| `empty-500` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 285 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 244 |
| `empty-500` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 63 |
| `truncated-stream` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 156 |
| `truncated-stream` | persistent | `bridge` | 3 | throw | `UncheckedIOException` | - | - | - | - | - | 51 |
| `truncated-stream` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 184 |
| `truncated-stream` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 224 |
| `connection-reset` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 120 |
| `connection-reset` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 132 |
| `connection-reset` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 22 |
| `connection-reset` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 65 |

Median wall time per arm across all cases: `baseline` 158 ms, `bridge` 181 ms

