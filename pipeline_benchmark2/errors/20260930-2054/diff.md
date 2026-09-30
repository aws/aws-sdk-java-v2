# Error behavior diff — `20260930-2054`

Baseline arm: `baseline`. Compared against: `bridge`.
23 cases, 92 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**1 behavioral difference(s).**

| fault | case | field | `baseline` | `bridge` |
|---|---|---|---|---|
| `malformed-body` | persistent | `cause` | java.io.UncheckedIOException | software.amazon.smithy.java.core.serde.SerializationException |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `internal-error` | transient | 95 | 211 |
| `unavailable` | persistent | 121 | 272 |
| `unavailable` | transient | 67 | 33 |
| `resource-not-found` | persistent | 35 | 3 |
| `resource-not-found` | transient | 4 | 2 |
| `conditional-check-failed` | persistent | 7 | 3 |
| `malformed-body` | persistent | 88 | 219 |
| `empty-500` | persistent | 205 | 33 |
| `connection-reset` | persistent | 120 | 25 |

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
| `conditional-check-failed` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `malformed-body` | persistent | `Unable_to_unmarshall_response_(software.amazon.awssdk.thirdparty.jackson.core.io.JsonEOFException:_Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_(software...` | `Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_REDACTED_(`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION`_disabled);_byte_offset:_#25]` |
| `empty-500` | persistent | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `truncated-stream` | persistent | `Unable_to_unmarshall_response_(org.apache.hc.core5.http.ConnectionClosedException:_Premature_end_of_Content-Length_delimited_message_body_(expected:_89;_receive...` | `org.apache.hc.core5.http.ConnectionClosedException:_Premature_end_of_Content-Length_delimited_message_body_(expected:_89;_received:_25)` |
| `connection-reset` | persistent | `Unable_to_execute_HTTP_request:_The_target_server_failed_to_respond_(SDK_Attempt_Count:_3)` | `The_target_server_failed_to_respond` |

## What each arm did

| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId | retryable | throttling | wall ms |
|---|---|---|---:|---|---|---|---:|---|---|---|---:|
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 210 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 181 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1630 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1865 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 1549 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2039 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1079 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1170 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2276 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2685 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 285 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 199 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 95 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 211 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 121 |
| `unavailable` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 272 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 67 |
| `unavailable` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 33 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2009 |
| `unavailable-retry-after` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2008 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2010 |
| `unavailable-retry-after` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2008 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 35 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 7 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 88 |
| `malformed-body` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 219 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 152 |
| `malformed-body` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 266 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 205 |
| `empty-500` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 33 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 129 |
| `empty-500` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 114 |
| `truncated-stream` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 102 |
| `truncated-stream` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 81 |
| `truncated-stream` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 301 |
| `truncated-stream` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 224 |
| `connection-reset` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 120 |
| `connection-reset` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 25 |
| `connection-reset` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 134 |
| `connection-reset` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 188 |

Median wall time per arm across all cases: `baseline` 144 ms, `bridge` 184 ms

