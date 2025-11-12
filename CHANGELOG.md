 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.38.4__ __2025-11-11__
## __AWS Batch__
  - ### Features
    - Documentation-only update: update API and doc descriptions per EKS ImageType default value switch from AL2 to AL2023.

## __AWS Health Imaging__
  - ### Features
    - Added new fields in existing APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - AWS Site-to-Site VPN now supports VPN connections with up to 5 Gbps bandwidth per tunnel, a 4x improvement from existing limit of 1.25 Gbps.

## __Amazon S3__
  - ### Bugfixes
    - Fix NullPointerException when using AnonymousCredentials in a CredentialsChain with the S3 CRT Client.

## __Data Automation for Amazon Bedrock__
  - ### Features
    - Added support for Language Expansion feature for BDA Audio modality.

## __RTBFabric__
  - ### Features
    - Added LogSettings and LinkAttribute fields to external links

## __Security Incident Response__
  - ### Features
    - Added support for configuring communication preferences as well as clearly displaying case comment author identities.

# __2.38.3__ __2025-11-10__
## __AWS Backup__
  - ### Features
    - AWS Backup supports backups of Amazon EKS clusters, including Kubernetes cluster state and persistent storage attached to the EKS cluster via a persistent volume claim (EBS volumes, EFS file systems, and S3 buckets).

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Private Certificate Authority service now supports ML-DSA key algorithms.

## __AWS Identity and Access Management__
  - ### Features
    - Added CreateDelegationRequest API, which is not available for general use at this time.

## __AWS Invoicing__
  - ### Features
    - Added new invoicing get-invoice-pdf API Operation

## __AWS STS__
  - ### Bugfixes
    - Raise exceptions in resolveCredentials instead of creation for StsWebIdentityTokenFileCredentialsProvider

## __AWS Security Token Service__
  - ### Features
    - Added GetDelegatedAccessToken API, which is not available for general use at this time.

## __AWS WAFV2__
  - ### Features
    - AWS WAF now supports CLOUDWATCH_TELEMETRY_RULE_MANAGED as a LogScope option, enabling automated logging configuration through Amazon CloudWatch Logs for telemetry data collection and analysis.

## __Amazon AppStream__
  - ### Features
    - AWS Appstream support for IPv6

## __Amazon Aurora DSQL__
  - ### Features
    - Cluster endpoint added to CreateCluster and GetCluster API responses

## __Amazon DataZone__
  - ### Features
    - Remove trackingServerName from DataZone Connection MLflowProperties

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Amazon EC2 Fleet customers can now filter instance types based on encryption-in-transit support using Attribute-Based Instance Type Selection (ABIS), eliminating the manual effort of identifying and selecting compatible instance types for security-sensitive workloads.

## __Amazon GuardDuty__
  - ### Features
    - Include tags filed in CreatePublishingDestinationRequest and DescribePublishingDestinationResponse.

## __Amazon Verified Permissions__
  - ### Features
    - Amazon Verified Permissions / Features : Adds support for entity Cedar tags.

## __Braket__
  - ### Features
    - Adds ExperimentalCapabilities field to CreateQuantumTask request and GetQuantumTask response objects. Enables use of experimental software capabilities when creating quantum tasks.

## __Managed Streaming for Kafka__
  - ### Features
    - Amazon MSK now supports intelligent rebalancing for MSK Express brokers.

# __2.38.2__ __2025-11-07__
## __AWS Control Tower__
  - ### Features
    - Added Parent Identifier support to ListEnabledControls and GetEnabledControl API. Implemented RemediationType support for Landing Zone operations: CreateLandingZone, UpdateLandingZone and GetLandingZone APIs

## __AWS Key Management Service__
  - ### Features
    - Added support for new ECC_NIST_EDWARDS25519 AWS KMS key spec

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Consider outstanding demand in ByteBufferStoringSubscriber before requesting more - fixes OutOfMemoryIssues that may occur when using AWS CRT-based S3 client to upload a large object.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Adds PrivateDnsPreference and PrivateDnsSpecifiedDomains to control private DNS resolution for resource and service network VPC endpoints and IpamScopeExternalAuthorityConfiguration to integrate Amazon VPC IPAM with a third-party IPAM service

## __Amazon OpenSearch Service__
  - ### Features
    - This release introduces the Default Application feature, allowing users to set, change, or unset a preferred OpenSearch UI application on a per-region basis for a streamlined and consistent user experience.

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports custom domain name for resource configurations

# __2.38.1__ __2025-11-06__
## __AWS Backup__
  - ### Features
    - AWS Backup now supports customer-managed keys (CMK) for logically air-gapped vaults, enabling customers to maintain full control over their encryption key lifecycle. This feature helps organizations meet specific internal governance requirements or external regulatory compliance standards.

## __AWS IoT__
  - ### Features
    - Unsuppress the following operations that were previously suppressed:

       - AttachPrincipalPolicy
       - DetachPrincipalPolicy
       - ListPolicyPrincipals
       - ListPrincipalPolicies

      These operations were previously supporessed because they were deprecated. However, this may be a blocker for customers moving from V1 to V2, so make these available for those customers.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Increased the default chunk size from 16KB to 128KB for async trailer-based checksum data transfers when using a custom legacy signer

## __AWS SSO Identity Store__
  - ### Features
    - IdentityStore API: added new KMSExceptionReason fields to the Exception object; added multiple new fields to the User APIs - UserStatus, Birthdate, Website and Photos; added multiple new metadata fields for User, Groups and Membership APIs - CreatedAt, CreatedBy, UpdatedAt and UpdatedBy.

## __Access Analyzer__
  - ### Features
    - New field totalActiveErrors added to getFindingsStatistics response.

## __Amazon Connect Service__
  - ### Features
    - Added support for Conditional Questions in Evaluation Forms. Introduced Auto Evaluation capability for Evaluation Forms and Contact Evaluations. Added new API operations: SearchEvaluationForms and SearchContactEvaluations.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Add Amazon EC2 R8a instance types

## __Amazon GameLift__
  - ### Features
    - Amazon GameLift Servers now supports game builds that use the Windows 2022 operating system.

## __Amazon QuickSight__
  - ### Features
    - Support for New Data Prep Experience

## __Amazon S3 Tables__
  - ### Features
    - Adds support for tagging APIs for S3 Tables

## __Amazon S3 Vectors__
  - ### Features
    - Amazon S3 Vectors provides cost-effective, elastic, and durable vector storage for queries based on semantic meaning and similarity.

## __Amazon SageMaker Service__
  - ### Features
    - Added NodeProvisioningMode parameter to UpdateCluster API to determine how instance provisioning is handled during cluster operations; in Continuous mode. Added VpcId field in UpdateDomain request for SageMaker Unified Studio domains with no VPC to add a customer VPC.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Provides NoLongerSupportedException error message

# __2.38.0__ __2025-11-05__
## __AWS Ground Station__
  - ### Features
    - Introduce CreateDataflowEndpointGroupV2 action

## __AWS SDK for Java v2__
  - ### Features
    - Add support for signing async payloads in the default `AwsV4aHttpSigner`.

## __Amazon CloudFront__
  - ### Features
    - This release adds new and updated API operations. You can now use the IpAddressType field to specify either ipv4 or dualstack for your Anycast static IP list. You can also enable cross-account resource sharing to share your VPC origins with other AWS accounts

## __Amazon DataZone__
  - ### Features
    - Added support for Project Resource Tags

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Optimize implementation of the `EnhancedDocument#toJson()` and `EnhancedDocument#getJson()`.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds AvailabilityZoneId support for DescribeFastSnapshotRestores, DisableFastSnapshotRestores, and EnableFastSnapshotRestores APIs.

## __Amazon FSx__
  - ### Features
    - Amazon FSx now enables secure management of Active Directory credentials through AWS Secrets Manager integration. Customers can use Secret ARNs instead of direct credentials when joining resources to Active Directory domains.

## __Amazon SageMaker Service__
  - ### Features
    - Add new fields in SageMaker Hyperpod DescribeCluster API response: TargetStateCount, SoftwareUpdateStatus and ActiveSoftwareDeploymentConfig to provide AMI update progress visibility .

## __Amazon Simple Storage Service__
  - ### Features
    - Launch IPv6 dual-stack support for S3 Express

