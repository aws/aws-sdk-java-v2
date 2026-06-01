 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.46.0__ __2026-06-01__
## __AWS Marketplace Agreement Service__
  - ### Features
    - Adding Entitlements in SearchAgreements Response

## __AWS SDK for Java v2__
  - ### Features
    - This update replaces the default `apache-client` runtime dependency of service clients with the new `apache5-client`. This means that service clients will now use the `Apache5HttpClient` by default if no HTTP client is explicitly configured on the service client builder.\n Notable changes:\n - Apache 5 uses different logger names than Apache 4\n - Expect: 100-Continue is disabled by default\n - TCP keep-alive socket options require `jdk.net.NetworkPermission` when SecurityManager is active

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add support for multi-region replication, enabling synchronization of user data and configurations to a secondary user pool in a standby Region. Add support for customer managed keys (CMK) in AWS KMS for encrypting user pool data at rest.

## __Amazon DynamoDB Enhanced Client__
  - ### Features
    - Increase code coverage on dynamodb-enhanced module

## __Amazon QuickSight__
  - ### Features
    - This release adds public APIs for Amazon QuickSight Spaces, Agents, and Flows. Spaces APIs enable management of curated resource collections. Agents APIs provide lifecycle control over AI-powered agents that leverage Spaces. Flows APIs add CRUDL APIs for automated workflows.

## __Apache HTTP Client 5__
  - ### Features
    - Upgrade httpcomponents.client5 to 5.6.1

