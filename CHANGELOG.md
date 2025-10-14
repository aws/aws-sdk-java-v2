 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.35.7__ __2025-10-14__
## __AWS Backup__
  - ### Features
    - The AWS Backup job attribute extension enhancement helps customers better understand the plan that initiated each job, and the properties of the resource each job creates.

## __AWS Transfer Family__
  - ### Features
    - SFTP connectors now support routing connections via customers' VPC. This enables connections to remote servers that are only accessible in a customer's VPC environment, and to servers that are accessible over the internet but need connections coming from an IP address in a customer VPC's CIDR range.

## __Amazon AppStream__
  - ### Features
    - This release introduces support for Microsoft license included applications streaming.

## __Amazon Connect Service__
  - ### Features
    - SDK release for TaskTemplateInfo in Contact for DescribeContact response.

## __Amazon DataZone__
  - ### Features
    - Support creating scoped and trustedIdentityPropagation enabled connections.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for creating instant, point-in-time copies of EBS volumes within the same Availability Zone

## __Amazon Transcribe Service__
  - ### Features
    - Move UntagResource API body member to query parameter

# __2.35.6__ __2025-10-13__
## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Updated http status code in control plane apis of agentcore runtime, tools and identity. Additional included provider types for AgentCore Identity

## __Amazon Bedrock AgentCore Data Plane Fronting Layer__
  - ### Features
    - Updated InvokeAgentRuntime API to accept account id optionally and added CompleteResourceTokenAuth API.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release Amazon EC2 c8i, c8i-flex, m8a, and r8gb

## __CloudWatch Observability Admin Service__
  - ### Features
    - CloudWatch Observability Admin adds the ability to enable Resource tags for telemetry in a customer account. The release introduces new APIs to enable, disable and describe the status of Resource tags for telemetry feature. This new capability simplifies monitoring AWS resources using tags.

# __2.35.5__ __2025-10-10__
## __AWS Glue__
  - ### Features
    - Addition of AuditContext in GetTable/GetTables Request

## __AWS Lambda__
  - ### Features
    - Add InvokedViaFunctionUrl context key to limit invocations to only FURL invokes.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Bedrock AgentCore release for Gateway, and Memory including Self-Managed Strategies support for Memory.

## __Amazon Bedrock AgentCore Data Plane Fronting Layer__
  - ### Features
    - Bedrock AgentCore release for Runtime, and Memory.

## __Amazon CloudFront__
  - ### Features
    - Added new viewer security policy, TLSv1.2_2025, for CloudFront.

## __Amazon Relational Database Service__
  - ### Features
    - Updated the text in the Important section of the ModifyDBClusterParameterGroup page.

## __odb__
  - ### Features
    - This release adds APIs that allow you to specify CIDR ranges in your ODB peering connection.

# __2.35.4__ __2025-10-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS WAFV2__
  - ### Features
    - This release adds the ability to throw WafLimitsExceededException when the maximum number of Application Load Balancer (ALB) associations per AWS WAF v2 WebACL is exceeded.

## __Amazon QuickSight__
  - ### Features
    - This release adds support for ActionConnector and Flow, which are new resources associated with Amazon Quick Suite. Additional updates include expanded Data Source options, further branding customization, and new capabilities that can be restricted by Admins.

## __Amazon S3__
  - ### Bugfixes
    - Skip Expect: 100-continue header for PutObject and UploadPart requests with zero content length

# __2.35.3__ __2025-10-08__
## __AWS License Manager User Subscriptions__
  - ### Features
    - Released support for IPv6 and dual-stack active directories

## __AWS Outposts__
  - ### Features
    - This release adds the new StartOutpostDecommission API, which starts the decommission process to return Outposts racks or servers.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Adding support for authorizer type AWS_IAM to AgentCore Control Gateway.

## __Service Quotas__
  - ### Features
    - introduces Service Quotas Automatic Management. Users can opt-in to monitoring and managing service quotas, receive notifications when quota usage reaches thresholds, configure notification channels, subscribe to EventBridge events for automation, and view notifications in the AWS Health dashboard.

# __2.35.2__ __2025-10-07__
## __AWS Proton__
  - ### Features
    - Deprecating APIs in AWS Proton namespace.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

# __2.35.1__ __2025-10-06__
## __AWS Backup__
  - ### Features
    - Adds optional MaxScheduledRunsPreview input to GetBackupPlan API to provide a preview of up to 10 next scheduled backup plan runs in the GetBackupPlan response.

## __AWS Glue__
  - ### Features
    - Adds labeling for DataQualityRuleResult for GetDataQualityResult and PublishDataQualityResult APIs

## __AWS MediaConnect__
  - ### Features
    - Enabling Tag-on-Create for AWS Elemental MediaConnect flow-based resource types

## __AWS Resource Explorer__
  - ### Features
    - Add new AWS Resource Explorer APIs

## __Amazon Bedrock Agent Core Control Plane Fronting Layer__
  - ### Features
    - Add support for VM lifecycle configuration parameters and A2A protocol

## __Amazon Bedrock AgentCore Data Plane Fronting Layer__
  - ### Features
    - Add support for batch memory management, agent card retrieval and session termination

## __Amazon MemoryDB__
  - ### Features
    - Support for DescribeMultiRegionParameterGroups and DescribeMultiRegionParameters API.

## __Amazon QuickSight__
  - ### Features
    - Documentation improvements for QuickSight API documentation to clarify that delete operation APIs are global.

## __Amazon Relational Database Service__
  - ### Features
    - Documentation updates to the CreateDBClusterMessage$PubliclyAccessible and CreateDBInstanceMessage$PubliclyAccessible properties.

# __2.35.0__ __2025-10-03__
## __AWS Clean Rooms Service__
  - ### Features
    - Added support for reading data sources across regions, and results delivery to allowedlisted regions.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive enables Mediapackage V2 users to configure ID3, KLV, Nielsen ID3, and Segment Length related parameters through the Mediapackage output group.

## __AWS SDK for Java v2__
  - ### Features
    - Adds business metrics tracking for credential providers.

## __Amazon Q Connect__
  - ### Features
    - Updated Amazon Q in Connect APIs to support Email Contact Recommendations.

## __Payment Cryptography Data Plane__
  - ### Features
    - Added a new API - translateKeyMaterial; allows keys wrapped by ECDH derived keys to be rewrapped under a static AES keyblock without first importing the key into the service.

