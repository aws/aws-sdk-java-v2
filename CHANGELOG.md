 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.45.1__ __2026-05-29__
## __AWS Ground Station__
  - ### Features
    - Adds support for Alpha-5 satellite number encoding in the Two-Line Element ephemeris format.

## __AWS RDS DataService__
  - ### Features
    - RDS Data API arrays (longValues, doubleValues, stringValues, booleanValues) in ExecuteStatement responses now correctly support null elements. Runtime change for JS v3 and .NET. Compile-time change for C plus plus, .NET, Kotlin, Rust. No impact for Java, Python, Ruby, PHP, Go.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock__
  - ### Features
    - Automated Reasoning checks - Added two build workflows for policies. Iterative Refine Policy uses AI to update policy definitions based on test results and feedback. Resolve Policy Ambiguities consolidates ambiguous variables in Automated Reasoning policies, a common source of ambiguous validation.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Reference your own AWS Secrets Manager secrets when configuring credential providers, giving you control over encryption, rotation, and access policies instead of using service-managed secrets.

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
    - Adding GetConnection, ListSubscriptions, and SendDirectMessage APIs to IoT Data Plane

## __AWS Parallel Computing Service__
  - ### Features
    - This release adds support for configuring scaleDownIdleTimeInSeconds at the compute node group level, allowing customers to set different idle timeouts per node group. Previously this setting was only available at the cluster level.

## __AWS Resilience Hub V2__
  - ### Features
    - This is the initial SDK release for the next generation of Resilience Hub.

## __AWS S3 Control__
  - ### Features
    - Update the minimum value of MinStorageBytesPercentage in StorageLensPrefixLevel.SelectionCriteria from 0.1 to 1, aligning the model with the documented contract.

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
    - Added Harness support for LiteLLM model configuration for third-party model providers. Added S3 and Git skill source types. Added Responses API format for OpenAI and Bedrock models. Added runtimeUserId and runtimeClientError to InvokeHarness.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Added Harness support for LiteLLM model configuration for third-party model providers. Added S3 and Git skill source types. Added Responses API format for OpenAI and Bedrock models. Added runtimeUserId parameter to InvokeHarness for end-user identification.

## __Amazon Bedrock Runtime__
  - ### Features
    - Support system role in message

## __Amazon Connect Customer Profiles__
  - ### Features
    - BatchPutProfileObject API adds multiple profile objects to a domain of a given ObjectType in a single API call.

## __Amazon S3__
  - ### Bugfixes
    - The multipart S3 client now honors MetadataDirective COPY by preserving source object metadata on the destination during multipart copy.

## __OpenSearch Service Serverless__
  - ### Features
    - Adds support for deletion protection on collections, ability to create NEXTGEN collection groups and autoscaling visibility for NEXTGEN collection groups

