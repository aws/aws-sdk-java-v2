 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.52.0__ __2026-08-11__
## __AWS Clean Rooms Service__
  - ### Features
    - Adds support for exporting redacted query execution logs in AWS Clean Rooms

## __AWS Organizations__
  - ### Features
    - Documentation update for AWS Organizations that clarifies valid input values for the HandshakePartyType parameter in the InviteAccountToOrganization. API ORGANIZATION is valid in responses only. valid input values are ACCOUNT and EMAIL

## __AWS SDK for Java v2__
  - ### Features
    - Added support for the AWS_IGNORE_CONFIGURED_ENDPOINT_URLS setting to skip endpoint URLs from environment variables and config files.
    - Update Netty to 4.1.137

  - ### Bugfixes
    - Fixed the client-side rate limiting behavior of `AdaptiveRetryStrategy` under throttling conditions. Previously, the computed rate limit was overly aggressive, potentially allowing a request rate much lower than what the service allows, and was unstable, varying over time even when the service's allowed throughput was constant.
    - Make individual parts retryable when an in-memory `AsyncRequestBody` (for example one created with `AsyncRequestBody.fromBytes`, `fromByteBuffer`, or `fromString`) is split, such as during an S3 multipart upload. The body in these cases is entirely in memory already so no data is copied and callers no longer need to wrap the body in `BufferedSplittableAsyncRequestBody` to get retries.

## __Account Access__
  - ### Features
    - Adds SDK support for AWS IAM account access manager, a feature that enables mapping of IAM roles to the users and groups in AWS IAM Identity Center.

## __Amazon Bedrock AgentCore__
  - ### Features
    - Adding online eval arn as input for recommendation API

## __Amazon CloudDirectory__
  - ### Features
    - Added an end-of-support notice to Amazon Cloud Directory public CLI reference documentation.

## __Amazon Connect Service__
  - ### Features
    - Seven new APIs for managing custom metrics, including create, describe, update, and delete. Using Custom Metrics, customers of Amazon Connect Customer can tailor analytics dashboards to their needs by applying custom thresholds, filters, and calculations to one or more out of the box measurements.

## __Amazon DataZone__
  - ### Features
    - GetSubscriptionGrant now returns materialized asset scope name for mapping Lake Formation data cell filters or Redshift views to subscription grants.

## __Amazon Elastic Kubernetes Service__
  - ### Features
    - This feature would give customers the ability to selectively tune certain configurations of Kubernetes control plane components in an Amazon EKS cluster.

## __Amazon Textract__
  - ### Features
    - Amazon A2I entered maintenance mode in July 2026 and now rejects StartHumanLoop requests from accounts that it does not recognize as existing customers. This update adds a corresponding note to the HumanLoopConfig parameter documentation so that the API Reference and SDK docs explain this behavior.

## __Apache HTTP Client 5__
  - ### Features
    - Upgrade httpcomponents.client5 to 5.6.3 to address CVE-2026-64607

