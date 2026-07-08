 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.47.2__ __2026-07-08__
## __AWS Common Runtime HTTP Client__
  - ### Bugfixes
    - Fix CRT connection pool exhaustion/leak when streams are cancelled due to API call timeouts.  Ensure concurrency/HTTP metrics are always published.

## __AWS IoT Wireless__
  - ### Features
    - Default session downlink transmission parameters have been added to the existing Multicast Group APIs. Explicit transmission parameters are no longer required when starting a multicast session during the FUOTA procedure.

## __AWS Resilience Hub V2__
  - ### Features
    - Next Generation Resilience Hub now supports filtering and sorting failure mode assessments, resource type filtering in ListResources, cross-region and cross-account topology edges, data recovery achievability status, and more granular dependency discovery progress tracking.

## __Amazon AppConfig__
  - ### Features
    - Update ExperimentRun APIs to support ConflictExceptions.

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - AgentCore Gateway now supports mapping allowed scopes to separate advertised scopes on the inbound authorizer.

## __Amazon EC2 Container Service__
  - ### Features
    - Amazon ECS now automatically detects the correct CPU architecture for Express Mode services.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Replace Root Volume now supports a VolumeId parameter. This allows the customer to pass in a pre-prepared volume as the target root volume for an RRV workflow.

## __Amazon Location Service Places V2__
  - ### Features
    - Added AddressNamesMode, AddressNameTranslations, MobilityMode, PostalCodeMode, SecondaryAddresses, and DriveThrough features across Places V2 APIs to support address name formatting,  multilingual translations, travel-aware search, multi-city postal codes, and unit-level address resolution.

# __2.47.1__ __2026-07-07__
## __AWS Config__
  - ### Features
    - Added support for connecting AWS Config to third-party cloud service providers. New APIs include PutConnector, GetConnector, DeleteConnector, and ListConnectors for managing connectors, and PutThirdPartyServiceLinkedConfigurationRecorder for creating third-party service-linked recorders.

## __AWS Lambda__
  - ### Features
    - AWS Lambda Durable Functions now supports customer managed KMS keys. This allows customers to configure a KMS key in Durable Config to have all their durable execution data encrypted.

## __AWS Marketplace Catalog Service__
  - ### Features
    - This release enhances the ListEntities API to support ResellerRole filter for ResaleAuthorization entity.

## __AWS SDK for Java v2__
  - ### Bugfixes
    - Re-resolve SSO access token on each credential refresh in the SSOCredentialsProvider instead of caching it at construction time ensuring that refreshed tokens (for example from running `aws sso login`) are always used.

## __AWS SecurityHub__
  - ### Features
    - release SecurityHub MultiCloud integration with Azure

## __AWSMarketplace Metering__
  - ### Features
    - The usage reporting window for the BatchMeterUsage API has been extended from 6 hours to 24 hours. Sellers can now submit usage records for up to 24 hours after a metered event occurs. The existing 6-hour grace period at the end of a billing cycle still applies.

## __Amazon Connect Service__
  - ### Features
    - Adds support for CreateAuthCode and DeleteSession APIs.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This launch surfaces the public SSM parameter associated with public AMIs in the AMI metadata.

## __Amazon Route 53 Global Resolver__
  - ### Features
    - Adds ListSharedDNSViews operation to list all DNS Views shared with caller using AWS Resource Access Manager. Also updates ListHostedZoneAssociations operation so that resource ARN param is optional, allowing caller to list all HostedZoneAssociations in account.

## __Amazon Simple Systems Manager (SSM)__
  - ### Features
    - Adding SSM Cloud Connector to support Azure Virtual Machines onboarding to AWS Systems Manager

## __Inspector2__
  - ### Features
    - This release extends vulnerability management to Azure VM, container registries and function apps. Adds support for per-member-account scan configuration settings.

## __Partner Central Revenue Measurement API__
  - ### Features
    - Add support for AWS Partner Central Revenue Measurement API for creating, managing, and tracking revenue attributions and marketplace revenue share allocations.

# __2.47.0__ __2026-07-06__
## __AWS Billing__
  - ### Features
    - Adds support for managing AWS account credits and billing preferences, including retrieving credit details, viewing per-month credit allocation history, redeeming promotional codes, and configuring credit sharing and billing preferences.

## __AWS SDK for Java v2__
  - ### Features
    - Moved auth scheme and endpoint resolution from per-service generated interceptors to shared pipeline stages, establishing clear separation between customer extension points and SDK internals. This also fixes a bug where credentials injected via `ExecutionInterceptor.modifyRequest()` were not being used for signing ([#6486](https://github.com/aws/aws-sdk-java-v2/issues/6486))
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Updating Lake Formation Access Grants Plugin version to 1.4.2
        - Contributed by: [@rajasbh-aws](https://github.com/rajasbh-aws)

## __Amazon CloudWatch Logs__
  - ### Features
    - Added PutStorageTierPolicy and GetStorageTierPolicy APIs to Amazon CloudWatch Logs. Customers can now configure account-level Intelligent Tiering to automatically optimize log storage costs by moving infrequently accessed data to lower-cost storage tiers.

## __Amazon OpenSearch Service__
  - ### Features
    - This release introduces Saved Object Migration APIs, enabling users to migrate dashboards, visualizations, index patterns, and other saved objects from a data source into an Amazon OpenSearch Service application workspace with configurable export filters and conflict resolution strategies.

## __MailManager__
  - ### Features
    - This release adds Smithy RPC v2 CBOR as an additional protocol alongside the existing AWS JSON 1.0. The SDK will prioritize its most performant protocol.

## __Contributors__
Special thanks to the following contributors to this release: 

[@rajasbh-aws](https://github.com/rajasbh-aws)
