 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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

