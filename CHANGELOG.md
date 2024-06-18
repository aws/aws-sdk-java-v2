 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.26.5__ __2024-06-18__
## __AWS CloudTrail__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Config__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Shield__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Bedrock Runtime__
  - ### Features
    - This release adds support for using Guardrails with the Converse and ConverseStream APIs.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This release adds support to surface async fargate customer errors from async path to customer through describe-fargate-profile API response.

## __Amazon Import/Export Snowball__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Lightsail__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Polly__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Rekognition__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon SageMaker Service__
  - ### Features
    - Launched a new feature in SageMaker to provide managed MLflow Tracking Servers for customers to track ML experiments. This release also adds a new capability of attaching additional storage to SageMaker HyperPod cluster instances.

# __2.26.4__ __2024-06-17__
## __AWS Batch__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Certificate Manager Private Certificate Authority__
  - ### Features
    - Doc-only update that adds name constraints as an allowed extension for ImportCertificateAuthorityCertificate.

## __AWS CodeBuild__
  - ### Features
    - AWS CodeBuild now supports global and organization GitHub webhooks

## __AWS Directory Service__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __AWS Elemental MediaConvert__
  - ### Features
    - This release includes support for creating I-frame only video segments for DASH trick play.

## __AWS Glue__
  - ### Features
    - This release introduces a new feature, Usage profiles. Usage profiles allow the AWS Glue admin to create different profiles for various classes of users within the account, enforcing limits and defaults for jobs and sessions.

## __AWS Key Management Service__
  - ### Features
    - Updating SDK example for KMS DeriveSharedSecret API.

## __AWS Secrets Manager__
  - ### Features
    - Doc only update for Secrets Manager

## __AWS WAF__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Cognito Identity Provider__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## __Amazon Elastic File System__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.3__ __2024-06-14__
## __AWS Elemental MediaConvert__
  - ### Features
    - This release adds the ability to search for historical job records within the management console using a search box and/or via the SDK/CLI with partial string matching search on input file name.

## __Amazon DataZone__
  - ### Features
    - This release introduces a new default service blueprint for custom environment creation.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Documentation updates for Amazon EC2.

## __Amazon Macie 2__
  - ### Features
    - This release adds support for managing the status of automated sensitive data discovery for individual accounts in an organization, and determining whether individual S3 buckets are included in the scope of the analyses.

## __Amazon Route 53 Domains__
  - ### Features
    - Add v2 smoke tests and smithy smokeTests trait for SDK testing.

# __2.26.2__ __2024-06-13__
## __AWS CloudHSM V2__
  - ### Features
    - Added support for hsm type hsm2m.medium. Added supported for creating a cluster in FIPS or NON_FIPS mode.

## __AWS Elemental MediaPackage v2__
  - ### Features
    - This release adds support for CMAF ingest (DASH-IF live media ingest protocol interface 1)

## __AWS Glue__
  - ### Features
    - This release adds support for configuration of evaluation method for composite rules in Glue Data Quality rulesets.

## __AWS IoT Wireless__
  - ### Features
    - Add RoamingDeviceSNR and RoamingDeviceRSSI to Customer Metrics.

## __AWS Key Management Service__
  - ### Features
    - This feature allows customers to use their keys stored in KMS to derive a shared secret which can then be used to establish a secured channel for communication, provide proof of possession, or establish trust with other parties.

## __Amazon S3__
  - ### Bugfixes
    - Fixes bug where Md5 validation is performed for S3 PutObject even if checksum value is supplied

# __2.26.1__ __2024-06-12__
## __AWS Mainframe Modernization Application Testing__
  - ### Features
    - AWS Mainframe Modernization Application Testing is an AWS Mainframe Modernization service feature that automates functional equivalence testing for mainframe application modernization and migration to AWS, and regression testing.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

## __AWS Secrets Manager__
  - ### Features
    - Introducing RotationToken parameter for PutSecretValue API

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Tagging support for Traffic Mirroring FilterRule resource

## __Amazon OpenSearch Ingestion__
  - ### Features
    - SDK changes for self-managed vpc endpoint to OpenSearch ingestion pipelines.

## __Amazon Redshift__
  - ### Features
    - Updates to remove DC1 and DS2 node types.

## __Amazon Security Lake__
  - ### Features
    - This release updates request validation regex to account for non-commercial aws partitions.

## __Amazon Simple Email Service__
  - ### Features
    - This release adds support for Amazon EventBridge as an email sending events destination.

# __2.26.0__ __2024-06-11__
## __"AWS SDK for Java v2"__
  - ### Features
    - This release contains a major internal refactor of retries and is part
      of moving the SDK to a standardized AWS SDK architecture. It
      introduces the interface `RetryStrategy` and three subclasses
      `StandardRetryStrategy`, `LegacyRetryStrategy` , and
      `AdaptiveRetryStrategy`. The new interfaces live in the `retry-spi`
      module, and the implementation classes live in the `retries` module.

      Note 1) This change marks RetryPolicy as as deprecated and we
      encourage users to migrate to its replacement, RetryStrategy. However,
      retry policies are, and will for the foreseeable future be fully
      supported. Clients configured to use retry policies will not need any
      code changes and won’t see any behavioral change with this release.

      Note 2) The original implementation of adaptive mode (see
      [#2658](https://github.com/aws/aws-sdk-java-v2/pull/2658)) that was
      released with the retry policy API contains a bug in its rate-limiter
      logic which prevents it from remembering state across requests. In
      this release of the retry strategy API, we introduce
      `RetryMode.ADAPTIVE_V2`, which implements the correct adaptive
      behavior. `RetryMode.ADAPTIVE` is still present in order to maintain
      backwards compatibility, but is now marked as deprecated.

      Note 3) When configuring retry mode through system settings or
      environment variables, users can only choose adaptive mode. This
      setting will map to `RetryMode.ADAPTIVE_V2` instead of
      `RetryMode.ADAPTIVE` with this release, giving users the correct
      behavior and still keeping the settings consistent across all
      SDKs. The list of configuration options are: profile file `retry_mode`
      setting, the `aws.retryMode` system property and the `AWS_RETRY_MODE`
      environment variable.
        - Contributed by: [@sugmanue](https://github.com/sugmanue)

## __AWS Network Manager__
  - ### Features
    - This is model changes & documentation update for Service Insertion feature for AWS Cloud WAN. This feature allows insertion of AWS/3rd party security services on Cloud WAN. This allows to steer inter/intra segment traffic via security appliances and provide visibility to the route updates.

## __AWS SDK for Java v2__
  - ### Features
    - Adds the new module retries API module
        - Contributed by: [@sugmanue](https://github.com/sugmanue)
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fixed an issue in async client where the future would get stuck if there is a server error and the server fails to return response body that matches with the content length specified in the response header. See [#4354](https://github.com/aws/aws-sdk-java-v2/issues/4354)

## __Access Analyzer__
  - ### Features
    - IAM Access Analyzer now provides policy recommendations to help resolve unused permissions for IAM roles and users. Additionally, IAM Access Analyzer now extends its custom policy checks to detect when IAM policies grant public access or access to critical resources ahead of deployments.

## __Amazon GuardDuty__
  - ### Features
    - Added API support for GuardDuty Malware Protection for S3.

## __Amazon SageMaker Service__
  - ### Features
    - Introduced Scope and AuthenticationRequestExtraParams to SageMaker Workforce OIDC configuration; this allows customers to modify these options for their private Workforce IdP integration. Model Registry Cross-account model package groups are discoverable.

## __Private CA Connector for SCEP__
  - ### Features
    - Connector for SCEP allows you to use a managed, cloud CA to enroll mobile devices and networking gear. SCEP is a widely-adopted protocol used by mobile device management (MDM) solutions for enrolling mobile devices. With the connector, you can use AWS Private CA with popular MDM solutions.

## __Contributors__
Special thanks to the following contributors to this release: 

[@sugmanue](https://github.com/sugmanue)
