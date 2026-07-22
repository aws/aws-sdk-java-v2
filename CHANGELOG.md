 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.49.1__ __2026-07-22__
## __ARC - Region switch__
  - ### Features
    - Adds support for a client token in StartPlanExecution to make plan execution requests idempotent for safe retries.

## __AWS CRT HTTP Client__
  - ### Bugfixes
    - Do not set the Content-Length header on a request that already carries Transfer-Encoding. Emitting both violates RFC 7230 and was rejected by the underlying CRT layer; this aligns the CRT client with the Netty client's existing behavior for chunked requests.

## __AWS Parallel Computing Service__
  - ### Features
    - AWS PCS Node Lifecycle Actions provides a structured way to run custom scripts at defined points in a compute node's lifecycle directly through the AWS PCS compute node group API.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon CloudWatch__
  - ### Features
    - Adds documented value constraints for CloudWatch Log Alarm scheduled query configuration fields, and makes LogGroupIdentifiers optional for log alarms.

## __Amazon GuardDuty__
  - ### Features
    - Amazon GuardDuty now returns filter lifecycle metadata in GetFilter responses. The response includes createdAt and updatedAt timestamps and a version number that increments on each update, giving you visibility into when a filter was created and last modified.

## __Amazon Prometheus Service__
  - ### Features
    - Add CloudWatch dataset destinations for Amazon Managed Service for Prometheus collectors.

## __Amazon S3__
  - ### Bugfixes
    - Fix a race condition in multipart upload with low maxInFlightParts where CompleteMultipartUpload could be initiated while the final part was still uploading.

## __Amazon Simple Email Service__
  - ### Features
    - Launching DEED and MREP in US GOV

## __CloudWatch Observability Admin Service__
  - ### Features
    - Enablement for ALB and Bedrock Knowledge Base logs via Observability Admin Telemetry Rule for account and organization level

## __Elastic Load Balancing__
  - ### Features
    - This adds CLI examples for the IpAddressType field on SourceIpConfig, enabling Network Load Balancer listener rules to match traffic based on whether the source IP is IPv4 or IPv6.

## __Partner Central Account API__
  - ### Features
    - Adds Qualifications Association APIs that enable partners to associate a subsidiary account's qualifications with a primary account. Once associated, qualifications are shared across all connected accounts and scorecards are consolidated. Partners can start and track association and disassociation.

# __2.49.0__ __2026-07-21__
## __AWS EntityResolution__
  - ### Features
    - Add support for real time matching with AWS Entity Resolution matching workflows with advanced rule sets.

## __AWS Invoicing__
  - ### Features
    - Added the SendProcurementPortalValidation and VerifyProcurementPortalValidation APIs. You can use the AWS SDKs to self-service activate your Procurement Portal Preferences created on the Billing Preferences page with a one-time-passcode (OTP) delivered to your portal.

## __AWS SDK for Java v2__
  - ### Features
    - Update Netty to 4.1.136

  - ### Bugfixes
    - Eliminate per-operation lambdas for endpoint and auth scheme resolution in generated clients, permanently fixing constant pool overflow for large services like EC2 Internal.

## __Amazon EMR Containers__
  - ### Features
    - Added support for the DeleteSecurityConfiguration API, which allows customers to delete security configurations in Amazon EMR on EKS. Also added authenticationConfiguration in securityConfigurationdata structure.

## __Amazon Redshift__
  - ### Features
    - Amazon Redshift - Added support for managing Query Editor V2 IAM Identity Center applications via new CreateQev2IdcApplication, DescribeQev2IdcApplications, ModifyQev2IdcApplication, and DeleteQev2IdcApplication API operations.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Added a WarningMessage field to Automation along with corresponding public documentation.

## __Inspector2__
  - ### Features
    - GA date - July 21st 2026, remove Tags field from ListCodeSecurityIntegration and ListCodeSecurityScanConfiguration.

## __Redshift Data API Service__
  - ### Features
    - update the workgroupArn to include EUSC partition, tests in THF Gamma and Prod no issue

## __Timestream InfluxDB__
  - ### Features
    - This release adds support for custom plugins in Amazon Timestream for InfluxDB. InfluxDB 3 Core and Enterprise DB parameter groups now accept a plugin repository URL and optional AWS Secrets Manager secret ARN, so the Processing Engine loads your Python plugins from a public or private repository.

