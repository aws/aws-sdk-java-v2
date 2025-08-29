 #### 👋 _Looking for changelogs for older versions? You can find them in the [changelogs](./changelogs) directory._
# __2.33.0__ __2025-08-29__
## __AWS SDK for Java v2__
  - ### Features
    - Add `AsyncRequestBody#splitCloseable` API that returns a Publisher of `ClosableAsyncRequestBody`
    - Introduce BufferedSplittableAsyncRequestBody that enables splitting into retryable sub-request bodies.
    - Introduce CloseableAsyncRequestBody interface that extends both AsyncRequestBody and SdkAutoClosable interfaces
    - Updated endpoint and partition metadata.

  - ### Bugfixes
    - Improve Sigv4 signing performance, particularly for RPC protocols

  - ### Deprecations
    - Deprecate `AsyncRequestBody#split` in favor of `AsyncRequestBody#splitCloseable` that takes the same input but returns `SdkPublisher<CloseableAsyncRequestBody>`

## __AWS X-Ray__
  - ### Features
    - AWS X-Ray Features: Support Sampling Rate Boost On Anomaly

## __Amazon Bedrock Runtime__
  - ### Features
    - Fixed stop sequence limit for converse API.

## __Amazon Elastic Compute Cloud__
  - ### Features
    - Release shows new route types such as filtered and advertisement.

