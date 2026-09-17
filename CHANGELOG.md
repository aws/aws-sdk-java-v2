 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.55.0__ __2026-09-17__
## __AWS CRT HTTP Client__
  - ### Features
    - Added an opt-in read/write (socket) inactivity timeout for the CRT-based HTTP clients (`AwsCrtHttpClient` and `AwsCrtAsyncHttpClient`). It is not enabled unless you set the `AWS_ENABLE_DEFAULT_SOCKET_TIMEOUT_2026` environment variable or the `aws.enableDefaultSocketTimeout2026` system property. Once enabled, an SDK-managed CRT HTTP client that has no explicit `connectionHealthConfiguration` set receives a per-service read/write timeout; a connection that makes no read or write progress within that window is closed and the in-flight request fails with a retryable `IOException`. The timeout is not applied to an HTTP client you build and supply yourself via `httpClient(...)`.

## __AWS End User Messaging Social__
  - ### Features
    - Add support for WhatsApp Calling APIs.

## __AWS IoT Wireless__
  - ### Features
    - Adds Multi-frame GNSS support to the AWS IoT Core Device Location GetPositionEstimate API. The new GnssMultiFrame measurement type improves location accuracy by combining multiple GNSS signal captures (2, 4, 8, 16, or 32) from the same device to estimate its position.

## __AWS User Notifications__
  - ### Features
    - Added support for attachments on managed notification events. Added support to access and subscribe sensitive managed notification events.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Batch evaluation now supports evaluating specific traces within a session. Each session can specify up to 100 trace IDs to evaluate.

## __Amazon Connect Service__
  - ### Features
    - Made the replicaAlias attribute optional in the ReplicateInstance API to support Global routing for Amazon Connect Global Resiliency (ACGR) instances. This change maintains backward compatibility. When onboarding to ACGR without Global routing, you must specify a custom replicaAlias in your API call

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adding support for "Tunnel" VPC Endpoint

## __Amazon GuardDuty__
  - ### Features
    - This change surfaces AI Protection resources on existing public IAM attack sequences. Customers will now see which model was accessed and whether a guardrail intervened as part of the credential-compromise sequence.

## __Amazon Simple Email Service__
  - ### Features
    - Added support to query the tenant name for BatchGetMetricData and CreateExportJob APIs to filter metrics and messages at the tenant level.

## __Amazon Simple Notification Service__
  - ### Features
    - SNS API reference documentation update

## __Amazon VPC Lattice__
  - ### Features
    - Adding support for CIDR Resource Configuration

