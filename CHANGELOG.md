 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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

