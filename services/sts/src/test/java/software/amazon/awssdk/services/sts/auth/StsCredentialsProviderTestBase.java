/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.sts.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Validates the functionality of {@link StsCredentialsProvider} and its subclasses.
 */
@ExtendWith(MockitoExtension.class)
public abstract class StsCredentialsProviderTestBase<RequestT, ResponseT> {

    protected static final String ARN = "arn:aws:ec2:us-east-1:123456789012:vpc/vpc-0e9801d129EXAMPLE";
    @Mock
    protected StsClient stsClient;

    @Test
    public void cachingDoesNotApplyToExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, false);
        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingDoesNotApplyToExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().minus(Duration.ofSeconds(5)), 2, true);
        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, false);
        callClient(verify(stsClient, times(1)), Mockito.any());
    }

    @Test
    public void cachingAppliesToNonExpiredSession_OverridePrefetchAndStaleTimes() {
        callClientWithCredentialsProvider(Instant.now().plus(Duration.ofHours(5)), 2, true);
        callClient(verify(stsClient, times(1)), Mockito.any());
    }

    /**
     * A 90 second session is shorter than the 5 minute advisory refresh window it selects, so it is inside that window as
     * soon as STS returns it and the next call refreshes. STS enforces a 15 minute minimum session duration, so this only
     * happens with a mock or a badly skewed host clock.
     */
    @Test
    public void expiringCredentialsAreRefreshedOnTheFollowingCall() {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, false);
        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    @Test
    public void distantExpiringCredentialsUpdatedInBackground_OverridePrefetchAndStaleTimes() throws InterruptedException {
        callClientWithCredentialsProvider(Instant.now().plusSeconds(90), 2, true);

        Instant endCheckTime = Instant.now().plus(Duration.ofSeconds(5));
        while (Mockito.mockingDetails(stsClient).getInvocations().size() < 2 && endCheckTime.isAfter(Instant.now())) {
            Thread.sleep(100);
        }

        callClient(verify(stsClient, times(2)), Mockito.any());
    }

    protected abstract RequestT getRequest();

    protected abstract ResponseT getResponse(Credentials credentials);

    protected abstract StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider>
    createCredentialsProviderBuilder(RequestT request);

    protected abstract ResponseT callClient(StsClient client, RequestT request);

    protected abstract String providerName();

    @Test
    public void refreshFailureReturnsCachedCredentials_staticStability() {
        // First call returns valid credentials that are already expired (to force a refresh on next call)
        Credentials validCredentials = Credentials.builder()
                                                  .accessKeyId("a")
                                                  .secretAccessKey("b")
                                                  .sessionToken("c")
                                                  .expiration(Instant.now().minus(Duration.ofSeconds(5)))
                                                  .build();
        RequestT request = getRequest();
        ResponseT response = getResponse(validCredentials);

        // First call succeeds, second call fails
        when(callClient(stsClient, request))
            .thenReturn(response)
            .thenThrow(SdkClientException.create("STS service unavailable"));

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            // First call should succeed and cache credentials
            AwsCredentials firstResult = credentialsProvider.resolveCredentials();
            assertThat(firstResult).isInstanceOf(AwsSessionCredentials.class);
            assertThat(((AwsSessionCredentials) firstResult).accessKeyId()).isEqualTo("a");

            // Second call should return cached credentials instead of throwing
            // because StaleValueBehavior.ALLOW is now set
            AwsCredentials secondResult = credentialsProvider.resolveCredentials();
            assertThat(secondResult).isInstanceOf(AwsSessionCredentials.class);
            assertThat(((AwsSessionCredentials) secondResult).accessKeyId()).isEqualTo("a");
            assertThat(((AwsSessionCredentials) secondResult).secretAccessKey()).isEqualTo("b");
            assertThat(((AwsSessionCredentials) secondResult).sessionToken()).isEqualTo("c");
        }
    }

    @Test
    public void initialFetchFailureThrowsException_noCachedCredentials() {
        RequestT request = getRequest();

        // The very first call to STS fails — no credentials have ever been cached
        when(callClient(stsClient, request))
            .thenThrow(SdkClientException.create("STS service unavailable"));

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            // Should throw because there are no cached credentials to fall back on
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("STS service unavailable");
        }
    }

    static Stream<String> nonRecoverableErrorCodes() {
        return StsCredentialsProvider.NON_RECOVERABLE_ERROR_CODES.stream();
    }

    /**
     * Non-recoverable STS errors must bypass static stability and propagate to the caller immediately,
     * even when cached credentials exist. This is because retrying the same request will never succeed
     * for these error codes — the underlying problem (e.g., revoked access, invalid policy) requires
     * operator intervention.
     */
    @ParameterizedTest
    @MethodSource("nonRecoverableErrorCodes")
    public void nonRecoverableError_bypassesStaticStability_throwsImmediately(String errorCode) {
        // First call returns valid but already-expired credentials (forces refresh on next call)
        Credentials validCredentials = Credentials.builder()
                                                  .accessKeyId("a")
                                                  .secretAccessKey("b")
                                                  .sessionToken("c")
                                                  .expiration(Instant.now().minus(Duration.ofSeconds(5)))
                                                  .build();
        RequestT request = getRequest();
        ResponseT response = getResponse(validCredentials);

        AwsServiceException nonRecoverableException = AwsServiceException.builder()
            .message("Access denied")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode(errorCode)
                                            .errorMessage("Non-recoverable STS error")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        // First call succeeds, second call fails with a non-recoverable error
        when(callClient(stsClient, request))
            .thenReturn(response)
            .thenThrow(nonRecoverableException);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            // First call succeeds and caches credentials
            AwsCredentials firstResult = credentialsProvider.resolveCredentials();
            assertThat(((AwsSessionCredentials) firstResult).accessKeyId()).isEqualTo("a");

            // Second call must throw because the error is non-recoverable — static stability must NOT absorb it
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo(errorCode));
        }
    }

    /**
     * Non-recoverable errors wrapped inside an SdkClientException (as a cause) must also bypass
     * static stability. The predicate extracts the AwsServiceException from the exception chain.
     */
    @Test
    public void nonRecoverableError_wrappedInSdkClientException_throwsImmediately() {
        Credentials validCredentials = Credentials.builder()
                                                  .accessKeyId("a")
                                                  .secretAccessKey("b")
                                                  .sessionToken("c")
                                                  .expiration(Instant.now().minus(Duration.ofSeconds(5)))
                                                  .build();
        RequestT request = getRequest();
        ResponseT response = getResponse(validCredentials);

        AwsServiceException accessDenied = AwsServiceException.builder()
            .message("Access denied")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("AccessDenied")
                                            .errorMessage("User is not authorized")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();
        // Wrap in SdkClientException as might happen in the real call path
        SdkClientException wrappedException = SdkClientException.create("Failed to assume role", accessDenied);

        when(callClient(stsClient, request))
            .thenReturn(response)
            .thenThrow(wrappedException);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            AwsCredentials firstResult = credentialsProvider.resolveCredentials();
            assertThat(((AwsSessionCredentials) firstResult).accessKeyId()).isEqualTo("a");

            // Must throw — the wrapped AccessDenied is non-recoverable
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(SdkClientException.class)
                .hasCauseInstanceOf(AwsServiceException.class);
        }
    }

    /**
     * Non-recoverable errors are cached for a short period (1-5 seconds) to protect the credential source
     * from callers that catch the error and retry in a tight loop. An immediate retry after receiving a
     * non-recoverable error should re-raise the cached error without contacting STS again.
     */
    @Test
    public void nonRecoverableError_cachedBriefly_immediateRetryDoesNotCallSts() {
        Credentials validCredentials = Credentials.builder()
                                                  .accessKeyId("a")
                                                  .secretAccessKey("b")
                                                  .sessionToken("c")
                                                  .expiration(Instant.now().minus(Duration.ofSeconds(5)))
                                                  .build();
        RequestT request = getRequest();
        ResponseT response = getResponse(validCredentials);

        AwsServiceException accessDenied = AwsServiceException.builder()
            .message("Access denied")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("AccessDenied")
                                            .errorMessage("User is not authorized")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        // First call succeeds (caches expired credentials), second call fails with non-recoverable error
        when(callClient(stsClient, request))
            .thenReturn(response)
            .thenThrow(accessDenied);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            // First call succeeds and caches credentials
            credentialsProvider.resolveCredentials();

            // Second call triggers refresh, hits non-recoverable error — thrown and cached
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo("AccessDenied"));

            // Third call: immediate retry — should re-raise cached error without calling STS
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo("AccessDenied"));

            // Verify STS was called only twice: initial fetch + one failed refresh.
            // The third resolveCredentials() re-raised the cached error without contacting STS.
            callClient(verify(stsClient, times(2)), Mockito.any());
        }
    }

    /**
     * Non-recoverable errors are cached for a short period on the initial fetch path as well.
     * When the very first STS call fails with a non-recoverable error and the caller retries immediately,
     * the cached error is re-raised without contacting STS again.
     */
    @Test
    public void nonRecoverableError_initialFetch_cachedBriefly_immediateRetryDoesNotCallSts() {
        RequestT request = getRequest();

        AwsServiceException accessDenied = AwsServiceException.builder()
            .message("Access denied")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("AccessDenied")
                                            .errorMessage("User is not authorized")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        when(callClient(stsClient, request))
            .thenThrow(accessDenied);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            // First call: initial fetch fails with non-recoverable error
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(AwsServiceException.class);

            // Second call: immediate retry — should re-raise cached error without calling STS
            assertThatThrownBy(credentialsProvider::resolveCredentials)
                .isInstanceOf(AwsServiceException.class);

            // Verify STS was called only once — the second call used the cached error
            callClient(verify(stsClient, times(1)), Mockito.any());
        }
    }

    /**
     * Recoverable errors (those with error codes NOT in the non-recoverable set) still
     * benefit from static stability — the provider returns cached credentials instead of throwing.
     * This is the complement to the non-recoverable error tests: a service unavailable or throttling
     * error should not propagate immediately.
     */
    @Test
    public void recoverableError_staticStabilityReturnsCachedCredentials() {
        Credentials validCredentials = Credentials.builder()
                                                  .accessKeyId("a")
                                                  .secretAccessKey("b")
                                                  .sessionToken("c")
                                                  .expiration(Instant.now().minus(Duration.ofSeconds(5)))
                                                  .build();
        RequestT request = getRequest();
        ResponseT response = getResponse(validCredentials);

        // A throttling error — recoverable, should be absorbed by static stability
        AwsServiceException throttlingException = AwsServiceException.builder()
            .message("Rate exceeded")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("Throttling")
                                            .errorMessage("Rate exceeded")
                                            .serviceName("STS")
                                            .build())
            .statusCode(400)
            .build();

        when(callClient(stsClient, request))
            .thenReturn(response)
            .thenThrow(throttlingException);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            AwsCredentials firstResult = credentialsProvider.resolveCredentials();
            assertThat(((AwsSessionCredentials) firstResult).accessKeyId()).isEqualTo("a");

            // Second call should return cached credentials — Throttling is recoverable
            AwsCredentials secondResult = credentialsProvider.resolveCredentials();
            assertThat(secondResult).isInstanceOf(AwsSessionCredentials.class);
            assertThat(((AwsSessionCredentials) secondResult).accessKeyId()).isEqualTo("a");
        }
    }

    /**
     * The advisory refresh window must be honored exactly, rather than being jittered to some later point. Here the
     * configured window covers the credential's entire lifetime, so the advisory window opens the moment the credentials are
     * issued and the very next call must contact STS. A jittered window would instead open at a random point up to a minute
     * before the mandatory refresh window, and the second call would be served from the cache.
     */
    @Test
    public void resolveCredentials_advisoryWindowIsNotJittered() {
        Duration lifetime = Duration.ofMinutes(10);
        Credentials credentials = Credentials.builder()
                                             .accessKeyId("a").secretAccessKey("b").sessionToken("c")
                                             .expiration(Instant.now().plus(lifetime))
                                             .build();
        RequestT request = getRequest();
        when(callClient(stsClient, request)).thenReturn(getResponse(credentials));

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder =
            createCredentialsProviderBuilder(request);
        credentialsProviderBuilder.prefetchTime(lifetime);

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            credentialsProvider.resolveCredentials();
            callClient(verify(stsClient, times(1)), Mockito.any());

            credentialsProvider.resolveCredentials();
            callClient(verify(stsClient, times(2)), Mockito.any());
        }
    }

    @Test
    public void invalidate_matchingAccessKeyId_causesRefresh() {
        Credentials credentials = Credentials.builder()
                                             .accessKeyId("a").secretAccessKey("b").sessionToken("c")
                                             .expiration(Instant.now().plus(Duration.ofHours(5)))
                                             .build();
        Credentials credentials2 = Credentials.builder()
                                              .accessKeyId("x").secretAccessKey("y").sessionToken("z")
                                              .expiration(Instant.now().plus(Duration.ofHours(5)))
                                              .build();
        RequestT request = getRequest();
        when(callClient(stsClient, request))
            .thenReturn(getResponse(credentials))
            .thenReturn(getResponse(credentials2));

        try (StsCredentialsProvider credentialsProvider = createCredentialsProviderBuilder(request).stsClient(stsClient).build()) {
            AwsCredentials first = credentialsProvider.resolveCredentials();
            assertThat(first.accessKeyId()).isEqualTo("a");

            AwsCredentialsIdentity identity = AwsBasicCredentials.create("a", "b");
            credentialsProvider.invalidate(identity).join();

            AwsCredentials second = credentialsProvider.resolveCredentials();
            assertThat(second.accessKeyId()).isEqualTo("x");
        }
    }

    @Test
    public void invalidate_nonMatchingAccessKeyId_doesNotCauseRefresh() {
        Credentials credentials = Credentials.builder()
                                             .accessKeyId("a").secretAccessKey("b").sessionToken("c")
                                             .expiration(Instant.now().plus(Duration.ofHours(5)))
                                             .build();
        RequestT request = getRequest();
        when(callClient(stsClient, request)).thenReturn(getResponse(credentials));

        try (StsCredentialsProvider credentialsProvider = createCredentialsProviderBuilder(request).stsClient(stsClient).build()) {
            AwsCredentials first = credentialsProvider.resolveCredentials();
            assertThat(first.accessKeyId()).isEqualTo("a");

            AwsCredentialsIdentity identity = AwsBasicCredentials.create("different", "b");
            credentialsProvider.invalidate(identity).join();

            AwsCredentials second = credentialsProvider.resolveCredentials();
            assertThat(second.accessKeyId()).isEqualTo("a");
            callClient(verify(stsClient, times(1)), Mockito.any());
        }
    }

    public void callClientWithCredentialsProvider(Instant credentialsExpirationDate, int numTimesInvokeCredentialsProvider, boolean overrideStaleAndPrefetchTimes) {
        Credentials credentials = Credentials.builder().accessKeyId("a").secretAccessKey("b").sessionToken("c").expiration(credentialsExpirationDate).build();
        RequestT request = getRequest();
        ResponseT response = getResponse(credentials);
        when(callClient(stsClient, request)).thenReturn(response);

        StsCredentialsProvider.BaseBuilder<?, ? extends StsCredentialsProvider> credentialsProviderBuilder = createCredentialsProviderBuilder(request);

        if(overrideStaleAndPrefetchTimes) {
            //do the same values as we would do without overriding the stale and prefetch times
            credentialsProviderBuilder.staleTime(Duration.ofMinutes(2));
            credentialsProviderBuilder.prefetchTime(Duration.ofMinutes(4));
        }

        try (StsCredentialsProvider credentialsProvider = credentialsProviderBuilder.stsClient(stsClient).build()) {
            if(overrideStaleAndPrefetchTimes) {
                //validate that we actually stored the override values in the build provider
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(2));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isEqualTo(Duration.ofMinutes(4));
            } else {
                //validate that the default values are used
                assertThat(credentialsProvider.staleTime()).as("stale time").isEqualTo(Duration.ofMinutes(1));
                assertThat(credentialsProvider.prefetchTime()).as("prefetch time").isNull();
            }

            for (int i = 0; i < numTimesInvokeCredentialsProvider; ++i) {
                AwsSessionCredentials providedCredentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
                assertThat(providedCredentials.accessKeyId()).isEqualTo("a");
                assertThat(providedCredentials.secretAccessKey()).isEqualTo("b");
                assertThat(providedCredentials.sessionToken()).isEqualTo("c");
                assertThat(providedCredentials.providerName()).isPresent().contains(providerName());
                if (!(credentialsProvider instanceof StsGetSessionTokenCredentialsProvider)) {
                    assertThat(providedCredentials.accountId()).isPresent();
                    assertThat(providedCredentials.accountId().get()).isEqualTo("123456789012");
                }
            }
        }
    }
}
