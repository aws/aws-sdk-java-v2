 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.39.5__ __2025-11-26__
## __AWS Compute Optimizer__
  - ### Features
    - Compute Optimizer now identifies idle NAT Gateway resources for cost optimization based on traffic patterns and backup configuration analysis. Access recommendations via the GetIdleRecommendations API.

## __Amazon Bedrock Runtime__
  - ### Features
    - Bedrock Runtime Reserved Service Support

## __Apache5 HTTP Client (Preview)__
  - ### Bugfixes
    - Fix bug where Basic proxy authentication fails with credentials not found.
    - Fix bug where preemptive Basic authentication was not honored for proxies. Similar to fix for Apache 4.x in [#6333](https://github.com/aws/aws-sdk-java-v2/issues/6333).

## __Cost Optimization Hub__
  - ### Features
    - This release enables AWS Cost Optimization Hub to show cost optimization recommendations for NAT Gateway.

## __s3__
  - ### Features
    - Add CRT shouldStream config as CRT_MEMORY_BUFFER_DISABLED SDK advanced client option

# __2.39.4__ __2025-11-25__
## __AWS Network Firewall__
  - ### Features
    - Network Firewall release of the Proxy feature.

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the S3_POLICY and BEDROCK_POLICY policy type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Add support for GSI composite key to handle up to 4 partition and 4 sort keys

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support to view Network firewall proxy appliances attached to an existing NAT Gateway via DescribeNatGateways API NatGatewayAttachedAppliance structure.

## __Amazon Route 53__
  - ### Features
    - Adds support for new route53 feature: accelerated recovery.

# __2.39.3__ __2025-11-24__
## __Amazon CloudFront__
  - ### Features
    - Add TrustStore, ConnectionFunction APIs to CloudFront SDK

## __Amazon CloudWatch Logs__
  - ### Features
    - New CloudWatch Logs feature - LogGroup Deletion Protection, a capability that allows customers to safeguard their critical CloudWatch log groups from accidental or unintended deletion.

# __2.39.2__ __2025-11-21__
## __AWS CloudFormation__
  - ### Features
    - Adds the DependsOn field to the AutoDeployment configuration parameter for CreateStackSet, UpdateStackSet, and DescribeStackSet APIs, allowing users to set and read auto-deployment dependencies between StackSets

## __AWS Control Tower__
  - ### Features
    - The manifest field is now optional for the AWS Control Tower CreateLandingZone and UpdateLandingZone APIs for Landing Zone version 4.0

## __AWS Elemental MediaPackage v2__
  - ### Features
    - Adds support for excluding session key tags from HLS multivariant playlists

## __AWS Invoicing__
  - ### Features
    - Added the CreateProcurementPortalPreference, GetProcurementPortalPreference, PutProcurementPortalPreference, UpdateProcurementPortalPreferenceStatus, ListProcurementPortalPreferences and DeleteProcurementPortalPreference APIs for procurement portal preference management.

## __AWS Key Management Service__
  - ### Features
    - Support for on-demand rotation of AWS KMS Multi-Region keys with imported key material

## __AWS Lambda__
  - ### Features
    - Launching Enhanced Error Handling and ESM Grouping capabilities for Kafka ESMs

## __AWS Marketplace Entitlement Service__
  - ### Features
    - Endpoint update for new region

## __AWS Organizations__
  - ### Features
    - Add support for policy operations on the UPGRADE_ROLLOUT_POLICY policy type.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Transfer Family__
  - ### Features
    - Adds support for creating Webapps accessible from a VPC.

## __AWSMarketplace Metering__
  - ### Features
    - Endpoint update for new region

## __Amazon API Gateway__
  - ### Features
    - API Gateway supports VPC link V2 for REST APIs.

## __Amazon Athena__
  - ### Features
    - Introduces Spark workgroup features including log persistence, S3/CloudWatch delivery, UI and History Server APIs, and SparkConnect 3.5.6 support. Adds DPU usage limits at workgroup and query levels as well as DPU usage tracking for Capacity Reservation queries to optimize performance and costs.

## __Amazon Bedrock__
  - ### Features
    - Add support to automatically enforce safeguards across accounts within an AWS Organization.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Support for agentcore gateway interceptor configurations and NONE authorizer type

## __Amazon Bedrock Runtime__
  - ### Features
    - Add support to automatically enforce safeguards across accounts within an AWS Organization.

## __Amazon Connect Service__
  - ### Features
    - New APIs to support aliases and versions for ContactFlowModule. Updated ContactFlowModule APIs to support custom blocks.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds a new capability to create and manage interruptible EC2 Capacity Reservations.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for ECR managed signing

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Adds support for controlPlaneScalingConfig on EKS Clusters.

## __Amazon Kinesis Video Streams__
  - ### Features
    - This release adds support for Tiered Storage

## __Amazon Lex Model Building V2__
  - ### Features
    - Adds support for Intent Disambiguation, allowing resolution of ambiguous user inputs when multiple intents match by presenting clarifying questions to users. Also adds Speech Detection Sensitivity configuration for optimizing voice activity detection sensitivity levels in various noise environments.

## __Amazon Q Connect__
  - ### Features
    - This release introduces two new messaging channel subtypes: Push, WhatsApp, under MessageTemplate which is a resource in Amazon Q in Connect.

## __Amazon QuickSight__
  - ### Features
    - Amazon Quick Suite now supports QuickChat as an embedding type when calling the GenerateEmbedUrlForRegisteredUser API, enabling developers to embed conversational AI agents directly into their applications.

## __Amazon Redshift__
  - ### Features
    - Added support for Amazon Redshift Federated Permissions and AWS IAM Identity Center trusted identity propagation.

## __Amazon Relational Database Service__
  - ### Features
    - Add support for Upgrade Rollout Order

## __Amazon SageMaker Runtime HTTP2__
  - ### Features
    - Add support for bidirectional streaming invocations on SageMaker AI real-time endpoints

## __Amazon SageMaker Service__
  - ### Features
    - Enhanced SageMaker HyperPod instance groups with support for MinInstanceCount, CapacityRequirements (Spot/On-Demand), and KubernetesConfig (labels and taints). Also Added speculative decoding and MaxInstanceCount for model optimization jobs.

## __Amazon Simple Email Service__
  - ### Features
    - Added support for new SES regions - Asia Pacific (Malaysia) and Canada (Calgary)

## __Compute Optimizer Automation__
  - ### Features
    - Initial release of AWS Compute Optimizer Automation. Create automation rules to implement recommended actions on a recurring schedule based on your specified criteria. Supported actions include: snapshot and delete unattached EBS volumes and upgrade volume types to the latest generation.

## __Elastic Load Balancing__
  - ### Features
    - This release adds the health check log feature in ALB, allowing customers to send detailed target health check log data directly to their designated Amazon S3 bucket.

## __MailManager__
  - ### Features
    - Add support for resources in the aws-eusc partition.

## __Redshift Serverless__
  - ### Features
    - Added UpdateLakehouseConfiguration API to manage Amazon Redshift Federated Permissions and AWS IAM Identity Center trusted identity propagation for namespaces.

## __Runtime for Amazon Bedrock Data Automation__
  - ### Features
    - Adding new fields to GetDataAutomationStatus: jobSubmissionTime, jobCompletionTime, and jobDurationInSeconds

## __Security Incident Response__
  - ### Features
    - Add ListInvestigations and SendFeedback APIs to support SecurityIR AI agents

## __odb__
  - ### Features
    - Adds AssociateIamRoleToResource and DisassociateIamRoleFromResource APIs for managing IAM roles. Enhances CreateOdbNetwork and UpdateOdbNetwork APIs with KMS, STS, and cross-region S3 parameters. Adds OCI identity domain support to InitializeService API.

# __2.39.1__ __2025-11-20__
## __AWS Budgets__
  - ### Features
    - Add BillingViewHealthStatusException to DescribeBudgetPerformanceHistory and ServiceQuotaExceededException to UpdateBudget for improved error handling with Billing Views.

## __AWS CloudTrail__
  - ### Features
    - AWS launches CloudTrail aggregated events to simplify monitoring of data events at scale. This feature delivers both granular and summarized data events for resources like S3/Lambda, helping security teams identify patterns without custom aggregation logic.

## __AWS DataSync__
  - ### Features
    - The partition value "aws-eusc" is now permitted for ARN (Amazon Resource Name) fields.

## __AWS Database Migration Service__
  - ### Features
    - Added support for customer-managed KMS key (CMK) for encryption for import private key certificate. Additionally added Amazon SageMaker Lakehouse endpoint used for zero-ETL integrations with data warehouses.

## __AWS Device Farm__
  - ### Features
    - Add support for environment variables and an IAM execution role.

## __AWS Glue__
  - ### Features
    - Added FunctionType parameter to Glue GetuserDefinedFunctions.

## __AWS Lake Formation__
  - ### Features
    - Added ServiceIntegrations as a request parameter for CreateLakeFormationIdentityCenterConfigurationRequest and UpdateLakeFormationIdentityCenterConfigurationRequest and response parameter for DescribeLakeFormationIdentityCenterConfigurationResponse

## __AWS License Manager__
  - ### Features
    - Added cross-account resource aggregation via license asset groups and expiry tracking for Self-Managed Licenses. Extended Org-Wide View to Self-Managed Licenses, added reporting for license asset groups, and removed Athena/Glue dependencies for cross-account resource discovery in commercial regions.

## __AWS Network Manager__
  - ### Features
    - This release adds support for Cloud WAN Routing Policy providing customers sophisticated routing controls to better manage their global networks

## __AWS Organizations__
  - ### Features
    - Added new APIs for Billing Transfer, new policy type INSPECTOR_POLICY, and allow an account to transfer between organizations

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS SecurityHub__
  - ### Features
    - Release Findings and Resources Trends APIs- GetFindingsTrendsV2 and GetResourcesTrendsV2. This supports time-series aggregated counts with composite filtering for 1-year of historical data analysis of Findings and Resources.

## __AWS Signin__
  - ### Features
    - Add the LoginCredentialsProvider which allows use of AWS credentials vended by AWS Sign-In that correspond to an AWS Console session. AWS Sign-In credentials will be used automatically by the Credential resolution chain when `login_session` is set in the profile.

## __Amazon Aurora DSQL__
  - ### Features
    - Added clusterVpcEndpoint field to GetVpcEndpointServiceName API response, returning the VPC connection endpoint for the cluster

## __Amazon Bedrock AgentCore__
  - ### Features
    - Bedrock AgentCore Memory release for redriving memory extraction jobs (StartMemoryExtractionJob and ListMemoryExtractionJob)

## __Amazon CloudFront__
  - ### Features
    - This release adds support for bring your own IP (BYOIP) to CloudFront's CreateAnycastIpList API through an optional IpamCidrConfigs field.

## __Amazon CloudWatch Application Signals__
  - ### Features
    - Amazon CloudWatch Application Signals now supports un-instrumented services discovery, cross-account views, and change history, helping SRE and DevOps teams monitor and troubleshoot their large-scale distributed applications.

## __Amazon Connect Service__
  - ### Features
    - Add optional ability to exclude users from send notification actions for Contact Lens Rules.

## __Amazon EC2 Container Service__
  - ### Features
    - Launching Amazon ECS Express Mode - a new feature that enables developers to quickly launch highly available, scalable containerized applications with a single command.

## __Amazon EMR__
  - ### Features
    - Add support for configuring S3 destination for step logs on a per-step basis.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds support for multiple features including: VPC Encryption Control for the status of traffic flow; S2S VPN BGP Logging; TGW Flexible Costs; IPAM allocation of static IPs from IPAM pools to CF Anycast IP lists used on CloudFront distribution; and EBS Volume Integration with Recycle Bin

## __Amazon Kinesis__
  - ### Features
    - Kinesis Data Streams now supports up to 50 Enhance Fan-out consumers for On-demand Advantage Streams. On-demand Standard and Provisioned streams will continue with the existing limit of 20 consumers for Enhanced Fan-out.

## __Amazon QuickSight__
  - ### Features
    - Introducing comprehensive theme styling controls. New features include border customization (radius, width, color), flexible padding controls, background styling for cards and sheets, centralized typography management, and visual-level override support across layouts.

## __Amazon Recycle Bin__
  - ### Features
    - Add support for EBS volume in Recycle Bin

## __Amazon Relational Database Service__
  - ### Features
    - Add support for VPC Encryption Controls.

## __Amazon SageMaker Service__
  - ### Features
    - Added training plan support for inference endpoints. Added HyperPod task governance with accelerator partition-based quota allocation. Added BatchRebootClusterNodes and BatchReplaceClusterNodes APIs. Updated ListClusterNodes to include privateDnsHostName.

## __Amazon Simple Storage Service__
  - ### Features
    - Enable / Disable ABAC on a general purpose bucket.

## __Auto Scaling__
  - ### Features
    - This release adds support for three new features: 1) Image ID overrides in mixed instances policy, 2) Replace Root Volume - a new strategy for Instance Refresh, and 3) Instance Lifecycle Policy for enhanced instance lifecycle management.

## __Braket__
  - ### Features
    - Add support for Braket spending limits.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Added support for Synchronous project type and PII Detection and Redaction

## __EC2 Image Builder__
  - ### Features
    - EC2 Image Builder now enables the distribution of existing AMIs, retry distribution, and define distribution workflows. It also supports automatic versioning for recipes and components, allowing automatic version increments and dynamic referencing in pipelines.

## __Elastic Load Balancing__
  - ### Features
    - This release adds the target optimizer feature in ALB, enabling strict concurrency enforcement on targets.

## __Redshift Data API Service__
  - ### Features
    - Increasing the length limit of Statement Name from 500 to 2048.

## __Runtime for Amazon Bedrock Data Automation__
  - ### Features
    - Bedrock Data Automation Runtime Sync API

# __2.39.0__ __2025-11-19__
## __AWS Backup__
  - ### Features
    - Amazon GuardDuty Malware Protection now supports AWS Backup, extending malware detection capabilities to EC2, EBS, and S3 backups.

## __AWS Billing__
  - ### Features
    - Added name filtering support to ListBillingViews API through the new names parameter to efficiently filter billing views by name.

## __AWS Billing and Cost Management Pricing Calculator__
  - ### Features
    - Add GroupSharingPreference, CostCategoryGroupSharingPreferenceArn, and CostCategoryGroupSharingPreferenceEffectiveDate to Bill Estimate. Add GroupSharingPreference and CostCategoryGroupSharingPreferenceArn to Bill Scenario.

## __AWS CloudTrail__
  - ### Features
    - AWS CloudTrail now supports Insights for data events, expanding beyond management events to automatically detect unusual activity on data plane operations.

## __AWS Cost Explorer Service__
  - ### Features
    - Add support for COST_CATEGORY, TAG, and LINKED_ACCOUNT AWS managed cost anomaly detection monitors

## __AWS Elemental MediaLive__
  - ### Features
    - MediaLive is adding support for MediaConnect Router by supporting a new input type called MEDIACONNECT_ROUTER. This new input type will provide seamless encrypted transport between MediaConnect Router and your MediaLive channel.

## __AWS Health APIs and Notifications__
  - ### Features
    - Adds actionability and personas properties to Health events exposed through DescribeEvents, DescribeEventsForOrganization, DescribeEventDetails, and DescribeEventTypes APIs. Adds filtering by actionabilities and personas in EventFilter, OrganizationEventFilter, EventTypeFilter.

## __AWS Identity and Access Management__
  - ### Features
    - Added the EnableOutboundWebIdentityFederation, DisableOutboundWebIdentityFederation and GetOutboundWebIdentityFederationInfo APIs for the IAM outbound federation feature.

## __AWS Invoicing__
  - ### Features
    - Add support for adding Billing transfers in Invoice configuration

## __AWS Lambda__
  - ### Features
    - Added support for creating and invoking Tenant Isolated functions in AWS Lambda APIs.

## __AWS MediaConnect__
  - ### Features
    - This release adds support for global routing in AWS Elemental MediaConnect. You can now use router inputs and router outputs to manage global video and audio routing workflows both within the AWS-Cloud and over the public internet.

## __AWS Network Firewall__
  - ### Features
    - Partner Managed Rulegroup feature support

## __AWS Secrets Manager__
  - ### Features
    - Adds support to create, update, retrieve, rotate, and delete managed external secrets.

## __AWS Security Token Service__
  - ### Features
    - IAM now supports outbound identity federation via the STS GetWebIdentityToken API, enabling AWS workloads to securely authenticate with external services using short-lived JSON Web Tokens.

## __AWS Sign-In Service__
  - ### Features
    - AWS Sign-In manages authentication for AWS services. This service provides secure authentication flows for accessing AWS resources from the console and developer tools. This release adds the CreateOAuth2Token API, which can be used to fetch OAuth2 access tokens and refresh tokens from Sign-In.

## __AWS Step Functions__
  - ### Features
    - Adds support to TestState for mocked results and exceptions, along with additional inspection data.

## __AWSBillingConductor__
  - ### Features
    - This release adds support for Billing Transfers, enabling management of billing transfers with billing groups on AWS Billing Conductor.

## __Amazon API Gateway__
  - ### Features
    - API Gateway now supports response streaming and new security policies for REST APIs and custom domain names.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release includes support for Search Results.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adding support for ocsf version 1.5, add optional parameter MappingVersion

## __Amazon DataZone__
  - ### Features
    - Amazon DataZone now supports business metadata (readme and metadata forms) at the individual attribute (column) level, a new rule type for glossary terms, and the ability to update the owner of the root domain unit.

## __Amazon DynamoDB__
  - ### Features
    - Extended Global Secondary Index (GSI) composite keys to support up to 8 attributes.

## __Amazon EC2 Container Service__
  - ### Features
    - Added support for Amazon ECS Managed Instances infrastructure optimization configuration.

## __Amazon EMR__
  - ### Features
    - Add CloudWatch Logs integration for Spark driver, executor and step logs

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This launch adds support for two new features: Regional NAT Gateway and IPAM Policies. IPAM policies offers customers central control for public IPv4 assignments across AWS services. Regional NAT is a single NAT Gateway that automatically expands across AZs in a VPC to maintain high availability.

## __Amazon Elastic Container Registry__
  - ### Features
    - Add support for ECR archival storage class and Inspector org policy for scanning

## __Amazon FSx__
  - ### Features
    - Adding File Server Resource Manager configuration to FSx Windows

## __Amazon GuardDuty__
  - ### Features
    - Add support for scanning and viewing scan results for backup resource types

## __Amazon Route 53__
  - ### Features
    - Add dual-stack endpoint support for Route53

## __Amazon SageMaker Service__
  - ### Features
    - Added support for enhanced metrics for SageMaker AI Endpoints. This features provides Utilization Metrics at instance and container granularity and also provides easy configuration of metric publish frequency from 10 sec -> 5 mins

## __Amazon Simple Storage Service__
  - ### Features
    - Adds support for blocking SSE-C writes to general purpose buckets.

## __Amazon Transcribe Streaming Service__
  - ### Features
    - This release adds support for additional locales in AWS transcribe streaming.

## __AmazonApiGatewayV2__
  - ### Features
    - Support for API Gateway portals and portal products.

## __AmazonConnectCampaignServiceV2__
  - ### Features
    - This release added support for ring timer configuration for campaign calls.

## __CloudWatch RUM__
  - ### Features
    - CloudWatch RUM now supports mobile application monitoring for Android and iOS platforms

## __Cost Optimization Hub__
  - ### Features
    - Release ListEfficiencyMetrics API

## __Inspector2__
  - ### Features
    - This release introduces BLOCKED_BY_ORGANIZATION_POLICY error code and IMAGE_ARCHIVED scanStatusReason. BLOCKED_BY_ORGANIZATION_POLICY error code is returned when an operation is blocked by an AWS Organizations policy. IMAGE_ARCHIVED scanStatusReason is returned when an Image is archived in ECR.

## __Network Flow Monitor__
  - ### Features
    - Added new enum value (AWS::EKS::Cluster) for type field under MonitorLocalResource

## __Partner Central Channel API__
  - ### Features
    - Initial GA launch of Partner Central Channel

