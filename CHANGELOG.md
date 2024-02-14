 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.24.3__ __2024-02-14__
## __AWS Control Tower__
  - ### Features
    - Adds support for new Baseline and EnabledBaseline APIs for automating multi-account governance.

## __AWS SDK for Java v2__
  - ### Features
    - Switching half of the AWS service clients onto the new SRA (Smithy Reference Architecture) identity and auth logic that was released in v2.21.0. For a list of individual services affected, please check the committed files.
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue where NPE would be thrown if there was an empty event in the input for an event streaming operation.

## __Amazon Lookout for Equipment__
  - ### Features
    - This feature allows customers to see pointwise model diagnostics results for their models.

## __Amazon Simple Storage Service__
  - ### Bugfixes
    - Fix for Issue [#4912](https://github.com/aws/aws-sdk-java-v2/issues/4912) where client region with AWS_GLOBAL calls failed for cross region access.

## __QBusiness__
  - ### Features
    - This release adds the metadata-boosting feature, which allows customers to easily fine-tune the underlying ranking of retrieved RAG passages in order to optimize Q&A answer relevance. It also adds new feedback reasons for the PutFeedback API.

# __2.24.2__ __2024-02-13__
## __AWS Marketplace Catalog Service__
  - ### Features
    - AWS Marketplace Catalog API now supports setting intent on requests

## __AWS Resource Explorer__
  - ### Features
    - Resource Explorer now uses newly supported IPv4 'amazonaws.com' endpoints by default.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __Amazon DynamoDB__
  - ### Features
    - Add additional logical operator ('and' and 'or') methods to DynamoDB Expression
        - Contributed by: [@akiesler](https://github.com/akiesler)

## __Amazon Lightsail__
  - ### Features
    - This release adds support to upgrade the major version of a database.

## __Amazon S3__
  - ### Features
    - Automatically trim object metadata keys of whitespace for `PutObject` and `CreateMultipartUpload`.

## __Amazon Security Lake__
  - ### Features
    - Documentation updates for Security Lake

## __URL Connection Client__
  - ### Bugfixes
    - Fix a bug where headers with multiple values don't have all values for that header sent on the wire. This leads to signature mismatch exceptions.

      Fixes [#4746](https://github.com/aws/aws-sdk-java-v2/issues/4746).

## __Contributors__
Special thanks to the following contributors to this release: 

[@akiesler](https://github.com/akiesler)
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

