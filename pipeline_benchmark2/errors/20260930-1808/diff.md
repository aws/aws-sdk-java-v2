# Error behavior diff — `20260930-1808`

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
| `throttling` | persistent | 2,104 | 797 |
| `throttling` | transient | 334 | 1,889 |
| `resource-not-found` | persistent | 14 | 3 |
| `conditional-check-failed` | persistent | 9 | 3 |
| `malformed-body` | transient | 205 | 102 |
| `empty-500` | transient | 264 | 104 |
| `truncated-stream` | persistent | 201 | 99 |

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
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 207 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 229 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1267 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1725 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2028 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 1973 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 2104 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 797 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 334 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 1889 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 54 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 106 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 155 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 111 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 136 |
| `unavailable` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 142 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 161 |
| `unavailable` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 176 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2010 |
| `unavailable-retry-after` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2009 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2010 |
| `unavailable-retry-after` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2009 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 14 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 9 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 250 |
| `malformed-body` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 207 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 205 |
| `malformed-body` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 102 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 124 |
| `empty-500` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 183 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 264 |
| `empty-500` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 104 |
| `truncated-stream` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 201 |
| `truncated-stream` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 99 |
| `truncated-stream` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 64 |
| `truncated-stream` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 91 |
| `connection-reset` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 156 |
| `connection-reset` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 278 |
| `connection-reset` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 114 |
| `connection-reset` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 86 |

Median wall time per arm across all cases: `baseline` 159 ms, `bridge` 154 ms

