 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.42.7__ __2026-03-05__
## __AWS Multi-party Approval__
  - ### Features
    - Updates to multi-party approval (MPA) service to add support for approval team baseline operations.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed a thread leak in ResponseInputStream and ResponsePublisher where the internal timeout scheduler thread persisted for the lifetime of the JVM, even when no streams were active. The thread now terminates after being idle for 60 seconds.

## __AWS Savings Plans__
  - ### Features
    - Added support for OpenSearch and Neptune Analytics to Database Savings Plans.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Added metadata field to CapacityAllocation.

## __Amazon GuardDuty__
  - ### Features
    - Added MALICIOUS FILE to IndicatorType enum in MDC Sequence

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for S3 Bucket Ownership validation for SageMaker Managed MLflow.

## __Connect Health__
  - ### Features
    - Connect-Health SDK is AWS's unified SDK for the Amazon Connect Health offering. It allows healthcare developers to integrate purpose-built agents - such as patient insights, ambient documentation, and medical coding - into their existing applications, including EHRs, telehealth, and revenue cycle.

# __2.42.6__ __2026-03-04__
## __AWS Elastic Beanstalk__
  - ### Features
    - As part of this release, Beanstalk introduce a new info type - analyze for request environment info and retrieve environment info operations. When customers request an Al analysis, Elastic Beanstalk runs a script on an instance in their environment and returns an analysis of events, health and logs.

## __Amazon Connect Service__
  - ### Features
    - Added support for configuring additional email addresses on queues in Amazon Connect. Agents can now select an outbound email address and associate additional email addresses for replying to or initiating emails.

## __Amazon Elasticsearch Service__
  - ### Features
    - Adds support for DeploymentStrategyOptions.

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers now offers DDoS protection for Linux-based EC2 and Container Fleets on SDKv5. The player gateway proxy relay network provides traffic validation, per-player rate limiting, and game server IP address obfuscation all with negligible added latency and no additional cost.

## __Amazon OpenSearch Service__
  - ### Features
    - Adding support for DeploymentStrategyOptions

## __Amazon QuickSight__
  - ### Features
    - Added several new values for Capabilities, increased visual limit per sheet from previous limit to 75, renamed Quick Suite to Quick in several places.

# __2.42.5__ __2026-03-03__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Support for AgentCore Policy GA

## __Amazon CloudWatch Logs__
  - ### Features
    - CloudWatch Logs updates- Added support for the PutBearerTokenAuthentication API to enable or disable bearer token authentication on a log group. For more information, see CloudWatch Logs API documentation.

## __Amazon DataZone__
  - ### Features
    - Adding QueryGraph operation to DataZone SDK

## __Amazon SageMaker Service__
  - ### Features
    - This release adds b300 and g7e instance types for SageMaker inference endpoints.

## __Partner Central Channel API__
  - ### Features
    - Adds the Resold Unified Operations support plan and removes the Resold Business support plan in the CreateRelationship and UpdateRelationship APIs

# __2.42.4__ __2026-02-27__
## __ARC - Region switch__
  - ### Features
    - Post-Recovery Workflows enable customers to maintain comprehensive disaster recovery automation. This allows customer SREs and leadership to have complete recovery orchestration from failover through post-recovery preparation, ensuring Regions remain ready for subsequent recovery events.

## __AWS Batch__
  - ### Features
    - This feature allows customers to specify the minimum time (in minutes) that AWS Batch keeps instances running in a compute environment after all jobs on the instance complete

## __AWS Health APIs and Notifications__
  - ### Features
    - Updates the regex for validating availabilityZone strings used in the describe events filters.

## __AWS Resource Access Manager__
  - ### Features
    - Resource owners can now specify ResourceShareConfiguration request parameter for CreateResourceShare API including RetainSharingOnAccountLeaveOrganization boolean parameter

## __Amazon Bedrock__
  - ### Features
    - Added four new model lifecycle date fields, startOfLifeTime, endOfLifeTime, legacyTime, and publicExtendedAccessTime. Adds support for using the Converse API with Bedrock Batch inference jobs.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Cognito is introducing a two-secret rotation model for app clients, enabling seamless credential rotation without downtime. Dedicated APIs support passing in a custom secret. Custom secrets need to be at least 24 characters. This eliminates reconfiguration needs and reduces security risks.

## __Amazon Connect Customer Profiles__
  - ### Features
    - This release introduces an optional SourcePriority parameter to the ProfileObjectType APIs, allowing you to control the precedence of object types when ingesting data from multiple sources. Additionally, WebAnalytics and Device have been added as new StandardIdentifier values.

## __Amazon Connect Service__
  - ### Features
    - Deprecate EvaluationReviewMetadata's CreatedBy and CreatedTime, add EvaluationReviewMetadata's RequestedBy and RequestedTime

## __Amazon Keyspaces Streams__
  - ### Features
    - Added support for Change Data Capture (CDC) streams with Duration DataType.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - AWS Transcribe Streaming now supports specifying a resumption window for the stream through the SessionResumeWindow parameter, allowing customers to reconnect to their streams for a longer duration beyond stream start time.

## __odb__
  - ### Features
    - ODB Networking Route Management is a feature improvement which allows for implicit creation and deletion of EC2 Routes in the Peer Network Route Table designated by the customer via new optional input. This feature release is combined with Multiple App-VPC functionality for ODB Network Peering(s).

# __2.42.3__ __2026-02-26__
## __AWS Backup Gateway__
  - ### Features
    - This release updates GetGateway API to include deprecationDate and softwareVersion in the response, enabling customers to track gateway software versions and upcoming deprecation dates.

## __AWS Marketplace Entitlement Service__
  - ### Features
    - Added License Arn as a new optional filter for GetEntitlements and LicenseArn field in each entitlement in the response.

## __AWS SecurityHub__
  - ### Features
    - Security Hub added EXTENDED PLAN integration type to DescribeProductsV2 and added metadata.product.vendor name GroupBy support to GetFindingStatisticsV2

## __AWSMarketplace Metering__
  - ### Features
    - Added LicenseArn to ResolveCustomer response and BatchMeterUsage usage records. BatchMeterUsage now accepts LicenseArn in each UsageRecord to report usage at the license level. Added InvalidLicenseException error response for invalid license parameters.

## __Amazon EC2 Container Service__
  - ### Features
    - Adding support for Capacity Reservations for ECS Managed Instances by introducing a new "capacityOptionType" value of "RESERVED" and new field "capacityReservations" for CreateCapacityProvider and UpdateCapacityProvider APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add c8id, m8id and hpc8a instance types.

## __Apache 5 HTTP Client__
  - ### Features
    - Update `httpcore5` to `5.4.1`.

# __2.42.2__ __2026-02-25__
## __AWS Batch__
  - ### Features
    - AWS Batch documentation update for service job capacity units.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports GetTopPathStatisticsByTraffic that provides aggregated statistics on the top URI paths accessed by bot traffic. Use this operation to see which paths receive the most bot traffic, identify the specific bots accessing them, and filter by category, organization, or bot name.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add support for EC2 Capacity Blocks in Local Zones.

## __Amazon Elastic Container Registry__
  - ### Features
    - Update repository name regex to comply with OCI Distribution Specification

## __Amazon Neptune__
  - ### Features
    - Neptune global clusters now supports tags

# __2.42.1__ __2026-02-24__
## __AWS Elemental Inference__
  - ### Features
    - Initial GA launch for AWS Elemental Inference including capabilities of Smart Crop and Live Event Clipping

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive - Added support for Elemental Inference for Smart Cropping and Clipping features for MediaLive.

## __Amazon CloudWatch__
  - ### Features
    - This release adds the APIs (PutAlarmMuteRule, ListAlarmMuteRules, GetAlarmMuteRule and DeleteAlarmMuteRule) to manage a new Cloudwatch resource, AlarmMuteRules. AlarmMuteRules allow customers to temporarily mute alarm notifications during expected downtime periods.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds httpTokensEnforced property to ModifyInstanceMetadataDefaults API. Set per account or manage organization-wide using declarative policies to prevent IMDSv1-enabled instance launch and block attempts to enable IMDSv1 on existing IMDSv2-only instances.

## __Amazon Elasticsearch Service__
  - ### Features
    - Fixed HTTP binding for DescribeDomainAutoTunes API to correctly pass request parameters as query parameters in the HTTP request.

## __Amazon OpenSearch Service__
  - ### Features
    - Fixed HTTP binding for DescribeDomainAutoTunes API to correctly pass request parameters as query parameters in the HTTP request.

## __CloudWatch Observability Admin Service__
  - ### Features
    - Adding a new field in the CreateCentralizationRuleForOrganization, UpdateCentralizationRuleForOrganization API and updating the GetCentralizationRuleForOrganization API response to include the new field

## __Partner Central Selling API__
  - ### Features
    - Added support for filtering opportunities by target close date in the ListOpportunities API. You can now filter results to return opportunities with a target close date before or after a specified date, enabling more precise opportunity searches based on expected closure timelines.

# __2.42.0__ __2026-02-23__
## __AWS Control Catalog__
  - ### Features
    - Updated ExemptedPrincipalArns parameter documentation for improved accuracy

## __AWS MediaTailor__
  - ### Features
    - Updated endpoint rule set for dualstack endpoints. Added a new opt-in option to log raw ad decision server requests for Playback Configurations.

## __AWS SDK for Java v2__
  - ### Features
    - Add support for additional checksum algorithms: XXHASH64, XXHASH3, XXHASH128, SHA512.
    - Updated endpoint and partition metadata.

## __AWS Wickr Admin API__
  - ### Features
    - AWS Wickr now provides APIs to manage your Wickr OpenTDF integration. These APIs enable you to test and save your OpenTDF configuration allowing you to manage rooms based on Trusted Data Format attributes.

## __Amazon Bedrock__
  - ### Features
    - Automated Reasoning checks in Amazon Bedrock Guardrails now support fidelity report generation. The new workflow type assesses policy coverage and accuracy against customer documents. The GetAutomatedReasoningPolicyBuildWorkflowResultAssets API adds support for the three new asset types.

## __Amazon Connect Cases__
  - ### Features
    - SearchCases API can now accept 25 fields in the request and response as opposed to the previous limit of 10. DeleteField's hard limit of 100 fields per domain has been lifted.

## __Amazon DataZone__
  - ### Features
    - Add workflow properties support to connections APIs

## __Amazon DynamoDB__
  - ### Features
    - This change supports the creation of multi-account global tables. It adds one new arguments to UpdateTable, GlobalTableSettingsReplicationMode.

## __Amazon QuickSight__
  - ### Features
    - Adds support for SEMISTRUCT to InputColumn Type

