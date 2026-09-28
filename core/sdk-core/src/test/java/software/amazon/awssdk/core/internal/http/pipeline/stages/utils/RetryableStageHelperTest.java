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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Either;

public class RetryableStageHelperTest {
    private RetryStrategy mockRetryStrategy;

    @BeforeEach
    void setup() {
        mockRetryStrategy = mock(RetryStrategy.class);
    }

    @ParameterizedTest(name = "IS_LONG_POLLING = {0}, expected = {1}")
    @MethodSource("longPollingValueTestParams")
    void tryRefreshToken_forwardsLongPollingAttrValue(Boolean attribute, boolean expected) {
        ExecutionAttributes.Builder attributes = ExecutionAttributes.builder();
        if (attribute != null) {
            attributes.put(SdkInternalExecutionAttribute.IS_LONG_POLLING, attribute);
        }

        RetryableStageHelper helper = makeTestHelper(attributes.build());

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
        when(mockRefreshResponse.delay()).thenReturn(Duration.ZERO);
        ArgumentCaptor<RefreshRetryTokenRequest> refreshRequestCaptor = ArgumentCaptor.forClass(RefreshRetryTokenRequest.class);

        when(mockRetryStrategy.refreshRetryToken(any())).thenReturn(mockRefreshResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());
        helper.tryRefreshToken(Duration.ZERO);

        verify(mockRetryStrategy).refreshRetryToken(refreshRequestCaptor.capture());

        assertThat(refreshRequestCaptor.getValue().isLongPolling()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "delay on successful refresh = {0}, delay on failed refresh = {1}")
    @MethodSource("refreshBackoffTestParams")
    void tryRefreshToken_returnsCorrectBackoff(Duration successDelay, Duration failureDelay) {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        if (successDelay != null) {
            RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
            when(mockRefreshResponse.delay()).thenReturn(successDelay);
            when(mockRetryStrategy.refreshRetryToken(any())).thenReturn(mockRefreshResponse);
        } else {
            when(mockRetryStrategy.refreshRetryToken(any())).thenThrow(
                new TokenAcquisitionFailedException("failed", token, null, failureDelay)
            );
        }

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());
        Either<Duration, Duration> backoff = helper.tryRefreshToken(Duration.ZERO);

        if (successDelay != null) {
            assertThat(backoff.left().get()).isEqualTo(successDelay);
        } else {
            assertThat(backoff.right().get()).isEqualTo(failureDelay);
        }
    }

    @Test
    void tryRefreshTokenAsync_refreshThrowsAcquireFailure_wrappedInCompletionException_acquireFailureDurationReturned() {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        Duration failureAcquireDuration = Duration.ofSeconds(1);
        TokenAcquisitionFailedException acquireException = new TokenAcquisitionFailedException("could not acquire",
                                                                                               mock(RetryToken.class), null,
                                                                                               failureAcquireDuration);

        // This chaining is important to make sure whenComplete sees a CompletionException instead of the
        // TokenAcquisitionFailedException directly
        CompletableFuture<RefreshRetryTokenResponse> future1 = CompletableFutureUtils.failedFuture(acquireException);
        CompletableFuture<RefreshRetryTokenResponse> toReturn = future1.thenApply(Function.identity());

        when(mockRetryStrategy.refreshRetryTokenAsync(any(RefreshRetryTokenRequest.class)))
            .thenReturn(toReturn);

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());

        assertThat(helper.tryRefreshTokenAsync(Duration.ZERO).join().right()).hasValue(failureAcquireDuration);
    }

    @Test
    void tryRefreshTokenAsync_refreshThrowsAcquireFailure__acquireFailureDurationReturned() {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        Duration failureAcquireDuration = Duration.ofSeconds(1);
        TokenAcquisitionFailedException acquireException = new TokenAcquisitionFailedException("could not acquire",
                                                                                               mock(RetryToken.class), null,
                                                                                               failureAcquireDuration);

        CompletableFuture<RefreshRetryTokenResponse> future = CompletableFutureUtils.failedFuture(acquireException);

        when(mockRetryStrategy.refreshRetryTokenAsync(any(RefreshRetryTokenRequest.class)))
            .thenReturn(future);

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());

        assertThat(helper.tryRefreshTokenAsync(Duration.ZERO).join().right()).hasValue(failureAcquireDuration);
    }

    @Test
    void resolveIdentityForAttempt_onFirstAttempt_keepsIdentity() {
        TrackingIdentityProvider provider = new TrackingIdentityProvider(CompletableFuture.completedFuture(null));
        SelectedAuthScheme<TestIdentity> scheme = schemeWith(provider);
        ExecutionAttributes attributes = ExecutionAttributes.builder()
                                                            .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, scheme)
                                                            .build();
        RetryableStageHelper helper = makeTestHelper(attributes);

        helper.startingAttempt();
        helper.resolveIdentityForAttempt();

        assertThat(provider.resolveCount()).isZero();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isSameAs(scheme);
    }

    @Test
    void resolveIdentityForAttempt_onEachRetryAttempt_resolvesIdentityAgain() {
        TrackingIdentityProvider provider = new TrackingIdentityProvider(CompletableFuture.completedFuture(null));
        ExecutionAttributes attributes = ExecutionAttributes.builder()
                                                            .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                                                 schemeWith(provider))
                                                            .build();
        RetryableStageHelper helper = makeTestHelper(attributes);

        helper.startingAttempt();
        helper.resolveIdentityForAttempt();
        helper.startingAttempt();
        helper.resolveIdentityForAttempt();
        helper.startingAttempt();
        helper.resolveIdentityForAttempt();

        assertThat(provider.resolveCount()).isEqualTo(2);
        SelectedAuthScheme<?> current = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(current.identity().join()).isSameAs(provider.lastResolvedIdentity());
    }

    @Test
    void tryRefreshToken_onAuthError_waitsForInvalidationBeforeRefreshingRetryToken() {
        CompletableFuture<Void> invalidation = new CompletableFuture<>();
        TrackingIdentityProvider provider = new TrackingIdentityProvider(invalidation);
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder()
                                                                        .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                                                             schemeWith(provider))
                                                                        .build());
        stubAcquireInitialToken();

        RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
        when(mockRefreshResponse.delay()).thenReturn(Duration.ZERO);
        AtomicBoolean invalidationDoneWhenRefreshing = new AtomicBoolean(false);
        when(mockRetryStrategy.refreshRetryToken(any())).thenAnswer(invocation -> {
            invalidationDoneWhenRefreshing.set(invalidation.isDone());
            return mockRefreshResponse;
        });

        helper.acquireInitialToken();
        helper.setLastException(new AuthErrorException());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            executor.schedule(() -> invalidation.complete(null), 100, TimeUnit.MILLISECONDS);
            helper.tryRefreshToken(Duration.ZERO);
        } finally {
            executor.shutdownNow();
        }

        assertThat(provider.invalidateCount()).isEqualTo(1);
        assertThat(invalidationDoneWhenRefreshing).isTrue();
    }

    @Test
    void tryRefreshTokenAsync_onAuthError_waitsForInvalidationBeforeRefreshingRetryToken() {
        CompletableFuture<Void> invalidation = new CompletableFuture<>();
        TrackingIdentityProvider provider = new TrackingIdentityProvider(invalidation);
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder()
                                                                        .put(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                                                             schemeWith(provider))
                                                                        .build());
        stubAcquireInitialToken();

        RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
        when(mockRefreshResponse.delay()).thenReturn(Duration.ofMillis(5));
        when(mockRetryStrategy.refreshRetryTokenAsync(any()))
            .thenReturn(CompletableFuture.completedFuture(mockRefreshResponse));

        helper.acquireInitialToken();
        helper.setLastException(new AuthErrorException());

        CompletableFuture<Either<Duration, Duration>> backoff = helper.tryRefreshTokenAsync(Duration.ZERO);

        assertThat(provider.invalidateCount()).isEqualTo(1);
        assertThat(backoff).isNotDone();
        verify(mockRetryStrategy, never()).refreshRetryTokenAsync(any());

        invalidation.complete(null);

        assertThat(backoff.join().left()).hasValue(Duration.ofMillis(5));
        verify(mockRetryStrategy).refreshRetryTokenAsync(any());
    }

    private void stubAcquireInitialToken() {
        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);
    }

    @SuppressWarnings("unchecked")
    private static SelectedAuthScheme<TestIdentity> schemeWith(IdentityProvider<TestIdentity> provider) {
        return SelectedAuthScheme.<TestIdentity>builder()
                                 .identity(CompletableFuture.completedFuture(new TestIdentity()))
                                 .signer((HttpSigner<TestIdentity>) mock(HttpSigner.class))
                                 .authSchemeOption(AuthSchemeOption.builder().schemeId("test").build())
                                 .identityProvider(provider)
                                 .build();
    }

    RetryableStageHelper makeTestHelper(ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(executionAttributes)
                                                            .build();

        RequestExecutionContext requestExecutionContext = RequestExecutionContext.builder()
                                                                                 .originalRequest(mock(SdkRequest.class))
                                                                                 .executionContext(executionContext)
                                                                                 .build();

        RetryStrategy retryStrategy = mockRetryStrategy;

        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(SdkClientOption.RETRY_STRATEGY, retryStrategy)
                                                                    .build();

        HttpClientDependencies dependencies = HttpClientDependencies.builder()
                                                                    .clientConfiguration(clientConfig)
                                                                    .build();

        return new RetryableStageHelper(httpRequest, requestExecutionContext, dependencies);
    }

    private static Stream<Arguments> longPollingValueTestParams() {
        return Stream.of(
            // Absent should default to false
            Arguments.of(null, false),
            Arguments.of(true, true),
            Arguments.of(false, false)
        );
    }

    private static Stream<Arguments> refreshBackoffTestParams() {
        return Stream.of(
            Arguments.of(null, Duration.ofSeconds(1)),
            Arguments.of(Duration.ofSeconds(1), null)
        );
    }

    private static final class TestIdentity implements Identity {
    }

    /**
     * An identity provider that counts identity resolutions, and whose invalidation completes with a given future.
     */
    private static final class TrackingIdentityProvider implements IdentityProvider<TestIdentity> {
        private final CompletableFuture<Void> invalidation;
        private final AtomicInteger resolveCount = new AtomicInteger();
        private final AtomicInteger invalidateCount = new AtomicInteger();
        private volatile TestIdentity lastResolvedIdentity;

        TrackingIdentityProvider(CompletableFuture<Void> invalidation) {
            this.invalidation = invalidation;
        }

        @Override
        public Class<TestIdentity> identityType() {
            return TestIdentity.class;
        }

        @Override
        public CompletableFuture<TestIdentity> resolveIdentity(ResolveIdentityRequest request) {
            resolveCount.incrementAndGet();
            lastResolvedIdentity = new TestIdentity();
            return CompletableFuture.completedFuture(lastResolvedIdentity);
        }

        @Override
        public CompletableFuture<Void> invalidate(TestIdentity identity) {
            invalidateCount.incrementAndGet();
            return invalidation;
        }

        int resolveCount() {
            return resolveCount.get();
        }

        int invalidateCount() {
            return invalidateCount.get();
        }

        TestIdentity lastResolvedIdentity() {
            return lastResolvedIdentity;
        }
    }

    /**
     * A service exception that reports itself as an authentication error.
     */
    private static final class AuthErrorException extends SdkServiceException {
        AuthErrorException() {
            super(SdkServiceException.builder().message("expired").statusCode(400));
        }

        @Override
        public boolean isAuthenticationError() {
            return true;
        }
    }
}
