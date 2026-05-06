 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.44.2__ __2026-05-05__
## __AWS Clean Rooms ML__
  - ### Features
    - Increase max configurable output limits in the Clean Rooms ML configured model algorithm association resource.

## __AWS Health Imaging__
  - ### Features
    - Add support for DICOM Json Metadata Override features in startDICOMImportJob API

## __AWS Marketplace Agreement Service__
  - ### Features
    - With this release, Agreements API provides a programmatic way to generate quotes, accept offers, track charges and entitlements, manage renewals and cancellations, and streamline operations entirely through APIs without navigating to the AWS Marketplace website or AWS Management Console.

## __AWS MediaTailor__
  - ### Features
    - Added support for Monetization Functions. Monetization Functions let you enrich ad requests with external data and transform session parameters using JSONata expressions, without deploying custom infrastructure.

## __AWS SDK for Java v2__
  - ### Features
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Fix an issue in the async SDK clients where a retry can lead to a `NullPointerException` if the exception that the SDK encountered did not originate from the service, such as a connection exception.

## __Amazon CloudFront__
  - ### Features
    - Adds support for tagging CloudFront Functions and KeyValueStores resources.

## __Amazon OpenSearch Service__
  - ### Features
    - Amazon OpenSearch Service now supports VPC egress, enabling outbound traffic from your OpenSearch domain to route privately through your VPC instead of the public internet.

## __Amazon Route 53 Domains__
  - ### Features
    - This release adds the TLDInMaintenance exception.

## __Amazon SageMaker Service__
  - ### Features
    - Adds support for ml.p5.4xlarge instance type for SageMaker Studio JupyterLab and CodeEditor apps for IAD (us-east-1), NRT (ap-northeast-1), BOM (ap-south-1), CGK (ap-southeast-3), GRU (sa-east-1), PDX (us-west-2), CMH (us-east-2).

# __2.44.1__ __2026-05-04__
## __AWS Elemental MediaLive__
  - ### Features
    - Updates the type of the MediaLiveRouterOutputConnectionMap.

## __AWS SDK for Java v2__
  - ### Features
    - Optimized JSON marshalling performance for JSON RPC, REST JSON and RPCv2 Cbor protocols.

## __AWS Security Agent__
  - ### Features
    - AWS Security Agent is adding a new target domain verification method for private VPC penetration testing. Additionally, the target domain resource will now have a verification status reason field to surface additional details about domain verification

## __Amazon Bedrock AgentCore Control__
  - ### Features
    - Amazon Bedrock AgentCore gateways now support MCP Sessions and response streaming from MCP targets. Session timeouts can be set between 15 minutes and 8 hours, and response streaming enables forwarding stream events sent by MCP targets to gateway users.

## __Amazon CloudWatch Logs__
  - ### Features
    - Adding an additional optional deliverySourceConfiguration field to PutDeliverySource API. This enables customers to pass service-specific configurations through IngestionHub such as tracing enablement or sampling rates that will be propagated to the source resource.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - This feature allows customers to change the tunnel bandwidth on existing VPN connections using the ModifyVpnConnectionOptions API

## __Amazon Lex Model Building Service__
  - ### Features
    - Lex V1 is deprecated, use Lex V2 instead

## __Amazon Location Service Routes V2__
  - ### Features
    - Added support for TravelTimeExceedsDriverWorkHours, ViolatedBlockedRoad, and ViolatedVehicleRestriction notice codes to the CalculateRoutes API response.

## __Amazon VPC Lattice__
  - ### Features
    - Amazon VPC Lattice now supports privately resolvable DNS resources

## __S3 Transfer Manager__
  - ### Bugfixes
    - Fix S3 Transfer Manager progress tracking overshoot when a multipart download part-get is retried after partial data delivery

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

