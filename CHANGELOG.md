 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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

