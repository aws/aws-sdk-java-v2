 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.27.7__ __2024-08-16__
## __AWS Batch__
  - ### Features
    - Improvements of integration between AWS Batch and EC2.

## __AWS SDK for Java v2__
  - ### Features
    - Add new spotbugs rule to detect blocking call in the async codepath
    - Updated endpoint and partition metadata.

## __Amazon QuickSight__
  - ### Features
    - Amazon QuickSight launches Customer Managed Key (CMK) encryption for Data Source metadata

## __Amazon SageMaker Service__
  - ### Features
    - Introduce Endpoint and EndpointConfig Arns in sagemaker:ListPipelineExecutionSteps API response

## __Amazon Simple Email Service__
  - ### Features
    - Marking use case description field of account details as deprecated.

## __Inspector2__
  - ### Features
    - Update the correct format of key and values for resource tags

# __2.27.6__ __2024-08-15__
## __AWS Identity and Access Management__
  - ### Features
    - Make the LastUsedDate field in the GetAccessKeyLastUsed response optional. This may break customers who only call the API for access keys with a valid LastUsedDate. This fixes a deserialization issue for access keys without a LastUsedDate, because the field was marked as required but could be null.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DocumentDB with MongoDB compatibility__
  - ### Features
    - This release adds Global Cluster Failover capability which enables you to change your global cluster's primary AWS region, the region that serves writes, during a regional outage. Performing a failover action preserves your Global Cluster setup.

## __Amazon EC2 Container Service__
  - ### Features
    - This release introduces a new ContainerDefinition configuration to support the customer-managed keys for ECS container restart feature.

## __Amazon Simple Storage Service__
  - ### Features
    - Amazon Simple Storage Service / Features : Adds support for pagination in the S3 ListBuckets API.

# __2.27.5__ __2024-08-14__
## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports using Secrets Manager to store git credentials and using multiple source credentials in a single project.

## __S3 Transfer Manager__
  - ### Features
    - This change enables multipart download for S3 Transfer Manager with the java-based Multipart S3 Async Client.

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

