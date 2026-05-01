 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.44.0__ __2026-05-01__
## __AWS EntityResolution__
  - ### Features
    - Add support for transitive matching in AWS Entity Resolution rule-based matching workflows. When enabled, records that match through different rules are grouped together into the same match group, allowing related records to be connected across rule levels.

## __AWS Identity and Access Management__
  - ### Features
    - Added guidance for CreateOpenIDConnectProvider to include multiple thumbprints when OIDC discovery and JWKS endpoints use different hosts or certificates

## __AWS IoT__
  - ### Features
    - AWS IoT HTTP rule actions now support cross-topic batching, combining messages from different MQTT topics into single HTTP requests.

## __AWS SDK for Java v2__
  - ### Features
    - Added support for v2.1 retry behavior behind the `AWS_NEW_RETRIES_2026` feature gate. When enabled via environment variable `AWS_NEW_RETRIES_2026=true` or system property `-Daws.newRetries2026=true`, the SDK applies the following changes:

      - Uses `STANDARD` as the default retry mode
      - Reduces base backoff delay from 100ms to 50ms
      - Differentiates token costs between transient and throttling errors
      - Honors the `max_attempts` profile property
      - Uses the `x-amz-retry-after` response header for server-suggested delays
      - Retries on `LimitExceededException` as a throttling error
      - Retries on STS `IdpCommunicationErrorException`
      - Reduces DynamoDB default max attempts from 9 to 4
      - Backs off before failing long-polling operations (e.g., SQS `ReceiveMessage`) when the retry token bucket is exhausted, instead of failing immediately

      Example usage:
      ```java
      System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
      DynamoDbClient ddb = DynamoDbClient.create();
      ```
    - Updated endpoint and partition metadata.

## __Amazon AppStream__
  - ### Features
    - Amazon WorkSpaces Applications now enables AI agents to securely operate desktop applications. Administrators configure stacks to provide agents access to WorkSpaces. Agents can click, type, and take screenshots. Agents authenticate with AWS IAM credentials with activity logged in AWS CloudTrail.

## __Amazon CloudWatch__
  - ### Features
    - This release adds tag support for CloudWatch Dashboards. The PutDashboard API now accepts a Tags parameter, allowing you to tag dashboards at creation time. Additionally, the TagResource, UntagResource, and ListTagsForResource APIs now support dashboard ARNs as resources.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adds support for filtering log groups by tags in the ListLogGroups API via the new logGroupTags parameter.

## __Amazon Q Connect__
  - ### Features
    - Added reasoning details, statusDescription, and timeToFirstTokenMs fields to the ListSpans response in Amazon Q in Connect to provide visibility into model thinking, error diagnostics, and inference latency metrics.

## __Amazon QuickSight__
  - ### Features
    - Add IdentityProviderCACertificatesBundleS3Uri for private CA certs with OAuth datasources. 256-char limit for FontFamily in themes. ControlTitleFormatText on all 13 filters. ControlTitleFontConfiguration. ContextRegion for cross-region identity context. Story,scenario in CreateCustomCapability API.

