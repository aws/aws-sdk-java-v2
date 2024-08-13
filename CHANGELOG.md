 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.27.4__ __2024-08-13__
## __AWS Amplify__
  - ### Features
    - Add a new field "cacheConfig" that enables users to configure the CDN cache settings for an App

## __AWS Fault Injection Simulator__
  - ### Features
    - This release adds support for additional error information on experiment failure. It adds the error code, location, and account id on relevant failures to the GetExperiment and ListExperiment API responses.

## __AWS Glue__
  - ### Features
    - Add AttributesToGet parameter support for Glue GetTables

## __Amazon AppStream__
  - ### Features
    - This release includes following new APIs: CreateThemeForStack, DescribeThemeForStack, UpdateThemeForStack, DeleteThemeForStack to support custom branding programmatically.

## __Amazon Neptune Graph__
  - ### Features
    - Amazon Neptune Analytics provides a new option for customers to load data into a graph using the RDF (Resource Description Framework) NTRIPLES format. When loading NTRIPLES files, use the value `convertToIri` for the `blankNodeHandling` parameter.

# __2.27.3__ __2024-08-12__
## __AWS Compute Optimizer__
  - ### Features
    - Doc only update for Compute Optimizer that fixes several customer-reported issues relating to ECS finding classifications

## __AWS Config__
  - ### Features
    - Documentation update for the OrganizationConfigRuleName regex pattern.

## __AWS Elemental MediaLive__
  - ### Features
    - AWS Elemental MediaLive now supports now supports editing the PID values for a Multiplex.

## __AWS Ground Station__
  - ### Features
    - Updating documentation for OEMEphemeris to link to AWS Ground Station User Guide

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This release adds new capabilities to manage On-Demand Capacity Reservations including the ability to split your reservation, move capacity between reservations, and modify the instance eligibility of your reservation.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - Added support for new AL2023 GPU AMIs to the supported AMITypes.

## __Amazon SageMaker Service__
  - ### Features
    - Releasing large data support as part of CreateAutoMLJobV2 in SageMaker Autopilot and CreateDomain API for SageMaker Canvas.

# __2.27.2__ __2024-08-09__
## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue where invoking `abort` and then `close` on a `ResponseInputStream` would cause the `close` to fail.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Fixed a description of AdvancedSecurityAdditionalFlows in Amazon Cognito user pool configuration.

## __Amazon Connect Service__
  - ### Features
    - This release supports adding RoutingCriteria via UpdateContactRoutingData public API.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Systems Manager doc-only updates for August 2024.

# __2.27.1__ __2024-08-08__
## __AWS Glue__
  - ### Features
    - This release adds support to retrieve the validation status when creating or updating Glue Data Catalog Views. Also added is support for BasicCatalogTarget partition keys.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Update ResponseTransformer so download attempts to a directory that does not exist or does not have write permissions are not retried

## __AWS SDK for Java v2 Migration Tool__
  - ### Features
    - Introduce the preview release of the AWS SDK for Java v2 migration tool that automatically migrates applications from the AWS SDK for Java v1 to the AWS SDK for Java v2.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Added support for threat protection for custom authentication in Amazon Cognito user pools.

## __Amazon Connect Service__
  - ### Features
    - This release fixes a regression in number of access control tags that are allowed to be added to a security profile in Amazon Connect. You can now add up to four access control tags on a single security profile.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Launch of private IPv6 addressing for VPCs and Subnets. VPC IPAM supports the planning and monitoring of private IPv6 usage.

# __2.27.0__ __2024-08-07__
## __AWS Glue__
  - ### Features
    - Introducing AWS Glue Data Quality anomaly detection, a new functionality that uses ML-based solutions to detect data anomalies users have not explicitly defined rules for.

## __Amazon AppIntegrations Service__
  - ### Features
    - Updated CreateDataIntegration and CreateDataIntegrationAssociation API to support bulk data export from Amazon Connect Customer Profiles to the customer S3 bucket.

