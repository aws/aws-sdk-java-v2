 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.24.1__ __2024-02-12__
## __AWS AppSync__
  - ### Features
    - Adds support for new options on GraphqlAPIs, Resolvers and Data Sources for emitting Amazon CloudWatch metrics for enhanced monitoring of AppSync APIs.

## __Amazon CloudWatch__
  - ### Features
    - This release enables PutMetricData API request payload compression by default.

## __Amazon Neptune Graph__
  - ### Features
    - Adding a new option "parameters" for data plane api ExecuteQuery to support running parameterized query via SDK.

## __Amazon Route 53 Domains__
  - ### Features
    - This release adds bill contact support for RegisterDomain, TransferDomain, UpdateDomainContact and GetDomainDetail API.

# __2.24.0__ __2024-02-09__
## __AWS Batch__
  - ### Features
    - This feature allows Batch to support configuration of repository credentials for jobs running on ECS

## __AWS IoT__
  - ### Features
    - This release allows AWS IoT Core users to enable Online Certificate Status Protocol (OCSP) Stapling for TLS X.509 Server Certificates when creating and updating AWS IoT Domain Configurations with Custom Domain.

## __AWS Price List Service__
  - ### Features
    - Add Throttling Exception to all APIs.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only update for Amazon ECS.

## __Amazon Prometheus Service__
  - ### Features
    - Overall documentation updates.

## __Amazon S3__
  - ### Bugfixes
    - Fix bug where PUT fails when using SSE-C with Checksum when using S3AsyncClient with multipart enabled. Enable CRC32 for putObject when using multipart client if checksum validation is not disabled and checksum is not set by user

## __Braket__
  - ### Features
    - Creating a job will result in DeviceOfflineException when using an offline device, and DeviceRetiredException when using a retired device.

## __Cost Optimization Hub__
  - ### Features
    - Adding includeMemberAccounts field to the response of ListEnrollmentStatuses API.

