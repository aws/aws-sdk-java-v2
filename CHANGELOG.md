 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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
