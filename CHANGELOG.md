 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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

