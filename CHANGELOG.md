 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
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
