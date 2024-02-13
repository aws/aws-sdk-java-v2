 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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
    - Updated internal core logic for signing properties with non-default values to be codegen based instead of set at runtime.

## __Amazon EC2 Container Service__
  - ### Features
    - Documentation only update for Amazon ECS.

## __Amazon Prometheus Service__
  - ### Features
    - Overall documentation updates.

## __Amazon S3__
  - ### Features
    - Overriding signer properties for S3 through the deprecated non-public execution attributes in S3SignerExecutionAttribute no longer works with this release. The recommended approach is to use plugins in order to change these settings.

  - ### Bugfixes
    - Fix bug where PUT fails when using SSE-C with Checksum when using S3AsyncClient with multipart enabled. Enable CRC32 for putObject when using multipart client if checksum validation is not disabled and checksum is not set by user

## __Braket__
  - ### Features
    - Creating a job will result in DeviceOfflineException when using an offline device, and DeviceRetiredException when using a retired device.

## __Cost Optimization Hub__
  - ### Features
    - Adding includeMemberAccounts field to the response of ListEnrollmentStatuses API.

