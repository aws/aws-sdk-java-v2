 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.45.1__ __2026-05-29__
## __AWS Ground Station__
  - ### Features
    - Adds support for Alpha-5 satellite number encoding in the Two-Line Element ephemeris format.

## __AWS RDS DataService__
  - ### Features
    - Service Release Notes

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Automated Reasoning checks - Added two build workflows for policies. Iterative Refine Policy uses AI to update policy definitions based on test results and feedback. Resolve Policy Ambiguities consolidates ambiguous variables in Automated Reasoning policies, a common source of ambiguous validation.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Service Release Notes

## __Amazon Omics__
  - ### Features
    - Add engineSettings to StartRun and GetRun. Add profiles and profileParameterTemplates to GetWorkflow and GetWorkflowVersion.

## __Amazon QuickSight__
  - ### Features
    - Adds support for creating, updating, describing, listing, and deleting an OAuthClientApplication resource, a new quicksight resource that allows customers to store OAuth configurations to connect to their databases via 3 Legged OAuth.

## __Amazon Route 53 Resolver__
  - ### Features
    - Added BatchCreateFirewallRule, BatchUpdateFirewallRule, BatchDeleteFirewallRule, and ListFirewallRuleTypes APIs. Added FirewallRuleType support to Firewall Rule APIs.

## __Amazon Simple Email Service__
  - ### Features
    - This release introduces support for Tenant Suppression Lists

## __Apache HTTP Client 5__
  - ### Features
    - Fail fast at Apache5HttpClient construction when SecurityManager is active and jdk.net.NetworkPermission setOption.TCP_KEEPIDLE, setOption.TCP_KEEPINTERVAL, setOption.TCP_KEEPCOUNT are not granted.

# __2.45.0__ __2026-05-28__
## __AWS Control Catalog__
  - ### Features
    - AWS Control Catalog - Added GovernedProviders response field and inclusion filter to GetControl and ListControls APIs to identify and filter by cloud provider. Added ParameterRequirementSummary response field indicating parameter requirements.

## __AWS IoT__
  - ### Features
    - Adds new connectivity-related fields to Fleet Indexing API requests and responses.

## __AWS IoT Data Plane__
  - ### Features
    - Service Release Notes

## __AWS Parallel Computing Service__
  - ### Features
    - This release adds support for configuring scaleDownIdleTimeInSeconds at the compute node group level, allowing customers to set different idle timeouts per node group. Previously this setting was only available at the cluster level.

## __AWS Resilience Hub V2__
  - ### Features
    - This is the initial SDK release for the next generation of Resilience Hub.

## __AWS S3 Control__
  - ### Features
    - Service Release Notes

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWSDeadlineCloud__
  - ### Features
    - Added support for persistent storage on Service-Managed Fleets, allowing customers to configure persistent storage that preserves data across worker sessions which reduces job startup times for workloads with large software installations or asset caches.

## __Amazon AppStream__
  - ### Features
    - Amazon WorkSpaces Applications now supports BYOL (Bring Your Own License). This enables customers to import their own WorkSpaces images and use them in WorkSpaces Applications.

## __Amazon Bedrock__
  - ### Features
    - Add support for ModelPackageArn in Bedrock's CreateCustomModel API

## __Amazon Bedrock AgentCore__
  - ### Features
    - Service Release Notes

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Service Release Notes

## __Amazon Bedrock Runtime__
  - ### Features
    - Service Release Notes

## __Amazon Connect Customer Profiles__
  - ### Features
    - Service Release Notes

## __Amazon S3__
  - ### Bugfixes
    - The multipart S3 client now honors MetadataDirective COPY by preserving source object metadata on the destination during multipart copy.

## __OpenSearch Service Serverless__
  - ### Features
    - Adds support for deletion protection on collections, ability to create NEXTGEN collection groups and autoscaling visibility for NEXTGEN collection groups

