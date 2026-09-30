# Error behavior diff — `20260930-1804`

Baseline arm: `baseline`. Compared against: `bridge`.
23 cases, 92 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**14 behavioral difference(s).**

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
| `malformed-body` | persistent | `cause` | java.io.UncheckedIOException | software.amazon.smithy.java.core.serde.SerializationException |
| `empty-500` | persistent | `rawResponse` | present | absent |
| `connection-reset` | persistent | `cause` | org.apache.hc.core5.http.NoHttpResponseException | software.amazon.smithy.java.client.core.error.TransportException |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `throttling` | transient | 1,968 | 884 |
| `internal-error` | persistent | 208 | 102 |
| `unavailable` | persistent | 195 | 76 |
| `resource-not-found` | persistent | 15 | 4 |
| `conditional-check-failed` | persistent | 6 | 2 |
| `malformed-body` | transient | 203 | 47 |
| `empty-500` | transient | 257 | 43 |
| `connection-reset` | persistent | 249 | 100 |
| `connection-reset` | transient | 102 | 218 |

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
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 193 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 224 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1845 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1199 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 1265 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 1388 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1946 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1006 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 1968 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 884 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 208 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 102 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 140 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 133 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 195 |
| `unavailable` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 76 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 120 |
| `unavailable` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 219 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2009 |
| `unavailable-retry-after` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2009 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2034 |
| `unavailable-retry-after` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2010 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 15 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 6 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 149 |
| `malformed-body` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 262 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 203 |
| `malformed-body` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 47 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 166 |
| `empty-500` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 214 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 257 |
| `empty-500` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 43 |
| `truncated-stream` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 117 |
| `truncated-stream` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 83 |
| `truncated-stream` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 182 |
| `truncated-stream` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 190 |
| `connection-reset` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 249 |
| `connection-reset` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 100 |
| `connection-reset` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 102 |
| `connection-reset` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 218 |

Median wall time per arm across all cases: `baseline` 180 ms, `bridge` 165 ms

