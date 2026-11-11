 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.58.0__ __2026-11-11__
## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Cloud Map__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS CloudHSM V2__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS CodeBuild__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS CodeDeploy__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS CodePipeline__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Config__
  - ### Features
    - AWS Config now supports connectors for Google Cloud. PutConnector accepts a Google Cloud connector configuration, and ListConnectors returns Google Cloud connector details.

## __AWS DataSync__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Direct Connect__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Directory Service__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds the BandwidthReportingMode setting to DASH manifest configurations, which lets you choose whether the bandwidth attribute of video Representations in the DASH manifest reports the average (default) or peak bitrate of the video track.

## __AWS Network Firewall__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __AWS Resource Groups Tagging API__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS SDK for Java v2__
  - ### Features
    - The default retry mode is now `standard`, and the `standard` and `adaptive` retry modes now use retry behavior 2.1. The `AWS_NEW_RETRIES_2026` setting is ignored. Retry behavior 2.1 retries sooner, uses fewer attempts, and spends more of the retry quota on each retry, so bursts of failures can exhaust it faster. To keep the previous behavior, set the retry mode to `legacy`, for example with `AWS_RETRY_MODE=legacy`.
    - Updated endpoint and partition metadata.

## __AWS SSO Identity Store__
  - ### Features
    - Updated ApiRestrictSourceVpcs in NetworkConfiguration to accept VPC ARNs in addition to VPC IDs.

## __AWS Service Catalog__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWS Shield__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __AWSMarketplace Metering__
  - ### Features
    - AWS Marketplace Metering Service adds decimal quantity support to the MeterUsage and BatchMeterUsage APIs. This includes new request fields UsageQuantityAsDecimal (MeterUsage), QuantityAsDecimal (BatchMeterUsage), and AllocatedUsageQuantityAsDecimal (UsageAllocation for both APIs).

## __Amazon Bedrock AgentCore__
  - ### Features
    - Support SigV4 request signing (cryptographically signed) in the Bedrock AgentCore Harness MCP connector tool.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Adds private certificate authority (CA) support for AgentCore Identity custom OAuth2.0 credential providers and custom JWT authorizers, enabling connections to IdPs with private CAs. Support SigV4 request signing (cryptographically signed) in the Bedrock AgentCore Harness MCP connector tool.

## __Amazon Cognito Identity Provider__
  - ### Features
    - This release adds session management operations that let user pool administrators and users list and revoke active sessions, and configure session tracking, per-user session limits, and SSO session validity per app client.

## __Amazon Comprehend__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Connect Cases__
  - ### Features
    - This release adds APIs that allow customers to merge cases to consolidate case history, and get the status of an ongoing case merge operation.

## __Amazon Connect Service__
  - ### Features
    - Added support for 96 new languages for AI-powered automated evaluations. Chat interactions support all 96 languages. Voice interactions support 57 languages, those with transcription available through post-call conversational analytics.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds support for AWS Cloud WAN Flow Logs. The CreateFlowLogs API now accepts CoreNetwork, CoreNetworkEdge, and CoreNetworkAttachment resource types, enabling customers to capture information about IP traffic traversing their Cloud WAN core networks.

## __Amazon Kinesis Analytics__
  - ### Features
    - This release adds support for Enhanced Snapshots in Amazon Managed Service for Apache Flink, enabling application state to be managed as a standalone resource that customers can export, import, and reuse across applications.

## __Amazon MemoryDB__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Route 53 Domains__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Textract__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon Transcribe Service__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __Amazon WorkSpaces__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.1. The SDK will prioritize its most performant protocol.

## __EC2 Image Builder__
  - ### Features
    - Update list operations to support wildcard name filtering and page sizes up to 100 results via MaxResults

## __Netty NIO HTTP Client__
  - ### Bugfixes
    - Fixed a connection pool leak with `ProtocolNegotiation.ALPN`. A TLS handshake failure on a new connection never returned its pool slot. Once all slots leaked, every request failed with `Acquire operation took longer than the configured maximum time`. The request now fails with an `IOException` and returns the slot.

