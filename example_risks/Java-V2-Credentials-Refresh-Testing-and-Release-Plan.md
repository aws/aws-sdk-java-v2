# Java V2 - Credentials Refresh - Testing and Release Plan

This doc describes the release plan for the Java SDK V2 implementation of the [Credentials Refresh SEP](https://quip-amazon.com/tlmsA1Wlwgq9/Credential-Refresh-Behavior) which standardizes credential refresh windows, expands static-stability and adds `IdentityProvider.invalidate`.   Note that the Java SDK uses the terms `stale` and `prefetch` for the SEP's `mandatory` and `advisory` windows.  This document generally uses the Java SDK terminology.

The designs/surface area api reviews:

- [Java SDK IdentityProvider.invalidate design](https://chorus.aws.dev/doc/nKC6Ve2L7Y6s)
- [Credentials Refresh Surface Area API review](https://pippin.amazon.dev/reviews/jr7tvVL9OsJH/artifacts/TaSnDi23DTgC)
- [Java v2 API Surface Area Review - IdentityProvider.invalidate](https://chorus.aws.dev/doc/3MXniF13VRNa)

The Java SDK Changes are implemented in the feature branch: [feature/master/credential_cache](https://github.com/aws/aws-sdk-java-v2/tree/feature/master/credential_cache)

- PRs:

  - [PR 1: CachedSupplier ALLOW engine](https://github.com/aws/aws-sdk-java-v2/pull/7028)
  - [PR 2: Enable static stability for STS, Container, SSO, and Login credential providers](https://github.com/aws/aws-sdk-java-v2/pull/7031)
  - [PR 3: Standardize refresh window configuration across all credential providers](https://github.com/aws/aws-sdk-java-v2/pull/7053)
  - [PR 4: Add dynamic advisory refresh window and update prefetch behavior](https://github.com/aws/aws-sdk-java-v2/pull/7093)
  - [PR 5: Add IdentityProvider.invalidate](https://github.com/aws/aws-sdk-java-v2/pull/7108)
  - [Related PR: Re-resolve SSO access token on each credential refresh instead of caching it at construction time](https://github.com/aws/aws-sdk-java-v2/pull/7097) - This PR has been released, but its fix is required for the new `invalidate` behavior and the nonRecoverableError behavior.

## Summary of the Changes and Risks

The overall change can be broken down into 3 areas, with distinct risks.  Each risk is labeled with an id (referenced below in rollout and testing plans) and a risk impact level: low, medium and high.  The impact level is based only on potential customer impact and NOT the probability of hitting the case.

### 1. Changes to default stale/prefetch (aka mandatory/advisory) credential refresh windows

The [Credentials Refresh SEP](https://quip-amazon.com/tlmsA1Wlwgq9/Credential-Refresh-Behavior)  standardizes credential refresh windows for all caching credential providers to 1 minute for the stale (aka mandatory) and a dynamic value based on credential expiration for the prefetch (aka advisory) window.  For the exact changes in the Java SDK see the "Provider Change Summary Table" in the [Credentials Refresh Surface Area API review. ](https://pippin.amazon.dev/reviews/jr7tvVL9OsJH/artifacts/TaSnDi23DTgC)

#### **Risks:**

- We are changing how often and when different sorts of credentials are refreshed by the SDK.  This may impact how often credentials are refreshed (in some cases more, some cases less) and how long they're valid for in the case of outages. 

  - [1.1][medium] IMDS prefetch moves from ~3 hours to ~5 hours into a 6-hour credential lifetime - This reduces IMDS load, but means that if IMDS has a sustained outage during the final hour, we're more likely to hit expired credentials.  For IMDS this is mitigated by static stability.
  - [1.2][low] IMDS staleTime moves from 1 second to 1 minute before expiry.  In general this should be a safer buffer and reduce the chance of failure of requests that expire during transit (ie, have expired by the time they reach the service).  This possibly could increase the chance of hitting blocking refreshes under a rare/edge case (failed advisory refreshes during the 59 minutes between advisory window start and mandatory window start). 
  - [1.3][med] ProcessCredentialsProvider staleTime changes from 0 (at expiry) to 1 minute before expiry - similar to the increase in IMDS stale time, this should reduce the chance of failed requests that expire during transit but comes at a cost of calling arbitrary process credential providers more frequently.
  - [1.4][med] ProcessCredentialsProvider prefetch changes from 15 seconds to dynamic (5-60 minutes) - This can result in more calls to the process, especially for short lived credentials.  This may have unknown side effects (billing, logging, rate limits),but this all depends on what process is being used.
  - [1.5][low] STS/Login/SSO advisory/prefetch window increased from 5 min to 15+minutes - In all cases the default prefetch/advisory window increases from 5 minutes to at least 15 minutes.  This may slightly increase the number of calls made

### 2. Expanding Refresh Resilience/Static-Stability behavior beyond IMDS

We are enabling Refresh Resilience (aka Static Stability) in the STS, Container, SSO, and Login providers as well as changing the backoff semantics for the IMDS provider.  Refresh resilience allows the SDK to continue to use stale/expired credentials after exceptions when refreshing them (eg, during an outage).   At release, only IMDS and EKS Credentials (ContainerCredentialsProvider) will have backend credential support for static stability.  For other credentials, requests will continue to fail with expired credentials during an outage, but the SDK will back off on requests, reducing the load on the service and allowing for faster recovery.

#### **Risks:**

- [2.1][high] Slower recovery for customers after outage for non IMDS/EKS credentials: For credential providers like STS where the credential backend/service does not support static stability (ie, will not accept the expired credentials), this could lead to a slower recovery from credential outages for individual customers (even while it improves the outage duration for everyone by reducing thundering herd traffic). 
- [2.2][high] Silent suppression of credential refresh failures (STS, Container, SSO, Login) - Previously operation calls would fail with exceptions raised directly from the credential provider when credentials refresh failed.  However, these will now only be logged and customers will instead get service side exceptions for expired credentials.  
- [2.3][medium][Risk to AWS Services] Services see an increase in calls with expired credentials during a credentials service outage (for credential sources that don't support static stability) - Previously during an STS outage, customers using STS credentials would fail with an exception and not make requests using expired credentials.  However, with the new refresh resilience, the SDK will now send those requests, causing the service to receive more requests that it must reject due to the expired credentials.  SDKs generally won't retry those requests, but that behavior is left as unspecified in the new retries SEP.  The Java SDK does NOT retry expired credentials related exceptions, so this won't lead to runaway retries, but it is still a change in call patterns. 
- [2.4][medium] IMDS Backoff change from exponential 100ms→10s to uniform 5-10 minutes - In all cases this increases the amount of time that stale/expired credentials are used.  While this reduces load on IMDS, it means that the SDK may continue to use expired credentials for up to almost 10 minutes longer than previously.  If the static stability window from HOSM doesn't cover this, it will lead to failed requests from the SDK that would have previously succeeded. 



### 3. Implementing IdentityProvider.invalidate

When the SDK receives an authentication error that indicates credentials may be invalid (ExpiredToken/InvalidToken) we call `invalidate` on the selected IdentityProvider.  For the AwsCredentialsChainProvider we call invalidate on all of its chained providers.  For caching credential providers we call invalidate on the CachedSupplier and mark the credentials as stale if they match the invalid identity meaning the next request will be in the mandatory/blocking refresh window unless back off is active.  The Java SDK does not consider these authentication errors as retryable, so the first request will still fail and raise the same exception as before, but the next request will do a blocking credentials refresh and hopefully succeed. 

#### Risks:

- [3.1][low] False-positive invalidation on legitimate ExpiredToken during clock skew - The SDK already implements clock skew detection, but if it hasn't been applied yet (this is the first exception), then we may call invalidate on still-valid credentials.  When this happens the SDK will call refresh earlier than expected for the credentials, which should still return new, valid credentials.  This generally wouldn't result in new failing or invalid requests, but might lead to unexpected extra calls to credential services or process providers. 
- [3.2][low] Invalidate false positives - Our check for invalidation directly checks for the ExpiredToken/InvalidToken error codes.  Its possible, though unlikely, that some services may use these for other cases, resulting in invalidate being called on valid credentials.  Since we don't retry these errors this wouldn't result in an increase in retries of failing requests, but might result in increased or unexpected calls to credential services like STS or credential providers. 
- [3.3][low] Invalidate false negatives from service-specific error codes - Services may use error codes other than ExpiredToken/InvalidToken to indicate that credentials are invalid (eg, AuthFailure in EC2 is used for both authorization and authentication failures).  In these cases invalidate will not be called on invalid credentials and requests will continue to fail.  This is the current behavior so is not a new breaking change/risk to customers. 

## Release and Testing Strategies

### Internal release with Release Staging

[Release Staging](https://docs.hub.amazon.dev/msu/user-guide/howto-release-staging/) is a new builderhub feature that helps safely release changes to `live` by testing them against a diverse sample of consumer pipelines first. Instead of making changes available to all consumers simultaneously, Release Staging deploys changes in waves of increasing size so we can detect issues early and minimize customer impact.

We successfully used Release Staging to release a large internal refactoring of the Auth and Endpoint interceptors to pipeline stages (see: [Rollout plan doc](https://quip-amazon.com/Kf4IAxaNCzpZ/Core-Interceptors-to-Pipeline-Stages-Migration-Rollout-Plan) and example [Release Staging Change Set](https://sas.corp.amazon.com/sharedSoftwareHub/releaseStaging/0b693fa0-2489-4380-a779-c58ed65b14ef)) and given the scope of the Credentials Caching/Refresh Resilience and Invalidate changes, we will follow a similar rollout strategy.  The changed credentials providers live in 2 different places in the SDK code: core modules (IMDS, Container and Process providers) and individual service modules (STS, SSO and Signin).  There is very little internal usage of the SSO or Signin/Login credential providers, but there is sufficient usage of STS, Container and IMDS providers.  There is some usage of Process credentials, but likely insufficient to get a good sample of usage from Release Staging.  See [Appendix 1 - Internal Credential Provider Usage](https://chorus.aws.dev/doc/GEkTod3QtZPX/Java-V2---Credentials-Refresh---Testing-and-Release-Plan#temp:028be88fc30fe22241) and [Appendix 2 - Brazil Packages + Version set Usage](https://chorus.aws.dev/doc/GEkTod3QtZPX/Java-V2---Credentials-Refresh---Testing-and-Release-Plan#temp:028be88fc30ffb2b41)  for details.

We will test using Release Staging using two Change Sets, each with their own target pipelines to cover different credential provider cases:

#### Change Set 1 - Core Providers only

This Change Set will include only the Core packages (see [Appendix 2 - Brazil Packages + Version set Usage](https://chorus.aws.dev/doc/GEkTod3QtZPX/Java-V2---Credentials-Refresh---Testing-and-Release-Plan#temp:028be88fc30ffb2b41) ) which will cover pipelines that *may* be using the core providers with changes: IMDS, Container and Profile.  Note - we can only target pipelines based on their package consumption so we cannot garuantee or control what credential provider usage this test actually covers.   Given the internal usage of IMDS and container providers we are likely to get at least a few pipelines with usage.

#### Change Set 2 - STS

This change set will include the core packages and STS (see [Appendix 2 - Brazil Packages + Version set Usage](https://chorus.aws.dev/doc/GEkTod3QtZPX/Java-V2---Credentials-Refresh---Testing-and-Release-Plan#temp:028be88fc30ffb2b41) ).  Note that we cannot target ONLY STS with this since the STS provider depends on the changes in core, so this change set may cover pipelines that are using the core providers as well - we have no control over this, but given the heavy usage of STS, its likely that at least some of the pipelines that include the dependency will use some variant of STS credentials.

#### Risks covered by Release Staging

It is unlikely that we will cover a credentials service outage during Release Staging, so we are unlikely to get coverage of any of the risks related to outages.  It is also unlikely that we will get coverage of process, SSO or Login credentials given the low internal usage.  Clock skew errors are also relatively unlikely on AWS controlled, internal services. However, our Release staging helps us validate the following risks:

- 1.1, 1.2 and 1.5
- 3.2 and 3.3

#### Risks NOT covered by Release Staging

- 1.3 and 1.4 (ProcessCredential related stale/prefetch default changes). It is unlikely we will have internal ProcessCredentialProvider usage in the sample of pipelines covered in Release Staging.  However - if we do, they will show in the metrics so we will know if this has been covered or not.
- 2.1, 2.2, 2.3 and 2.4 - Unless there are multiple credential service outages (IMDS, EKS, and STS) we will not cover these risks during Release Staging.

# Appendix

## Appendix 1 - Internal Credential Provider Usage

```markdown
          credential_provider          | unique_accounts | total_requests
--------------------------------------+-----------------+----------------
 CREDENTIALS_STS_ASSUME_ROLE          |        12144472 | 15694217078270
 CREDENTIALS_HTTP                     |          189573 | 12794922145309
 CREDENTIALS_IMDS                     |           72913 |  7757621467603
 CREDENTIALS_STS_ASSUME_ROLE_WEB_ID   |             304 |     3746113502
 CREDENTIALS_PROCESS                  |             907 |      550682894
 CREDENTIALS_PROFILE_PROCESS          |             821 |      539249341
 CREDENTIALS_SSO                      |             164 |         273566
 CREDENTIALS_PROFILE_SSO              |             132 |         250419
 CREDENTIALS_PROFILE_SSO_LEGACY       |              40 |          45223
 CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN |            2832 |          13116
```

## Appendix 2 - Brazil Packages + Version set Usage

**Core Packages** - 275590 version sets

- https://code.amazon.com/packages/AwsJavaSdk-Core-AwsCore/releases. (aws-core)
- https://code.amazon.com/packages/AwsJavaSdk-Core-SdkCore/releases. (sdk-core)
- https://code.amazon.com/packages/AwsJavaSdk-Core-Auth/releases  (auth)
- https://code.amazon.com/packages/AwsJavaSdk-Core-IdentitySpi/releases (identity-spi)
- https://code.amazon.com/packages/AwsJavaSdk-Core-Utils/releases (utils)

**Service Packages**

- https://code.amazon.com/packages/AwsJavaSdk-Signin/releases (121 versionsets)
- https://code.amazon.com/packages/AwsJavaSdk-Sso/releases (909 versionsets)
- https://code.amazon.com/packages/AwsJavaSdk-Sts/releases (117665 version sets)

## Appendix 3 - Tracking credential provider usage during Release Staging

Since we can only target pipelines based on their package dependencies, we need another way to track whether our Release Staging has actually given us some amount of real world coverage of the Credential Providers we are changing. We will use the SDK metrics to find coverage.  We add a business metric flag for credential provider usage and the SDK version added to the useragent for the Release Staging results will use a specific "-SNAPSHOT" version.