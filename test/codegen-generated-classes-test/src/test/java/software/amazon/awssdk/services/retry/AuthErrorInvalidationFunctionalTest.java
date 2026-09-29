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

package software.amazon.awssdk.services.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Functional tests for how the SDK handles a service rejecting a request's credentials, and how it resolves credentials on
 * retries:
 * <ul>
 *     <li>An {@code ExpiredToken} or {@code InvalidToken} rejection invalidates the provider's cached credentials, and the
 *     request is retried. The retry attempt resolves the credentials again, so it is signed with the refreshed credentials.</li>
 *     <li>Every retry attempt resolves the credentials again, whatever the reason for the retry.</li>
 *     <li>Authorization errors such as {@code AccessDenied} neither invalidate the credentials nor are retried.</li>
 * </ul>
 */
public class AuthErrorInvalidationFunctionalTest {

    private static final int STANDARD_MAX_ATTEMPTS = 3;

    @Test
    public void expiredToken_invalidatesCredentials_andRetryUsesRefreshedCredentials() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.refreshedOnInvalidate();

        try (ProtocolRestJsonClient client = syncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(authErrorResponse("ExpiredToken"), successResponse());

            client.allTypes();

            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-1");
        }
    }

    @Test
    public void invalidToken_invalidatesCredentials_andRetryUsesRefreshedCredentials() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.refreshedOnInvalidate();

        try (ProtocolRestJsonClient client = syncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(authErrorResponse("InvalidToken"), successResponse());

            client.allTypes();

            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-1");
        }
    }

    @Test
    public void async_expiredToken_invalidatesCredentials_andRetryUsesRefreshedCredentials() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.refreshedOnInvalidate();

        try (ProtocolRestJsonAsyncClient client = asyncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(authErrorResponse("ExpiredToken"), successResponse());

            client.allTypes().join();

            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-1");
        } finally {
            mockHttpClient.close();
        }
    }

    /**
     * When invalidation cannot produce new credentials (for example, static credentials that have expired), every attempt is
     * rejected, and the request fails once the retries are exhausted.
     */
    @Test
    public void expiredToken_whenCredentialsAreNotRefreshed_failsAfterRetriesAreExhausted() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.neverRefreshed();

        try (ProtocolRestJsonClient client =
                 ProtocolRestJsonClient.builder()
                                       .credentialsProvider(credentialsProvider)
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost"))
                                       .httpClient(mockHttpClient)
                                       .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                       .build()) {
            mockHttpClient.stubResponses(authErrorResponse("ExpiredToken"));

            assertThatThrownBy(client::allTypes)
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> {
                    AwsServiceException serviceException = (AwsServiceException) e;
                    assertThat(serviceException.awsErrorDetails().errorCode()).isEqualTo("ExpiredToken");
                    assertThat(serviceException.numAttempts()).isEqualTo(STANDARD_MAX_ATTEMPTS);
                });

            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(STANDARD_MAX_ATTEMPTS);
            assertThat(credentialsProvider.resolveCallCount()).isEqualTo(STANDARD_MAX_ATTEMPTS);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-0", "key-0");
        }
    }

    @Test
    public void async_expiredToken_whenCredentialsAreNotRefreshed_failsAfterRetriesAreExhausted() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.neverRefreshed();

        try (ProtocolRestJsonAsyncClient client =
                 ProtocolRestJsonAsyncClient.builder()
                                            .credentialsProvider(credentialsProvider)
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost"))
                                            .httpClient(mockHttpClient)
                                            .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                            .build()) {
            mockHttpClient.stubResponses(authErrorResponse("ExpiredToken"));

            assertThatThrownBy(() -> client.allTypes().join())
                .isInstanceOf(CompletionException.class)
                .satisfies(e -> {
                    AwsServiceException serviceException = (AwsServiceException) e.getCause();
                    assertThat(serviceException.awsErrorDetails().errorCode()).isEqualTo("ExpiredToken");
                    assertThat(serviceException.numAttempts()).isEqualTo(STANDARD_MAX_ATTEMPTS);
                });

            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(STANDARD_MAX_ATTEMPTS);
            assertThat(credentialsProvider.resolveCallCount()).isEqualTo(STANDARD_MAX_ATTEMPTS);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-0", "key-0");
        } finally {
            mockHttpClient.close();
        }
    }

    /**
     * Credentials are resolved again on every retry attempt, not only after an authentication error.
     */
    @Test
    public void serverError_retryResolvesCredentialsAgain() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.newCredentialsOnEveryResolve();

        try (ProtocolRestJsonClient client = syncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(serverErrorResponse(), successResponse());

            client.allTypes();

            assertThat(credentialsProvider.invalidateCallCount()).isZero();
            assertThat(credentialsProvider.resolveCallCount()).isEqualTo(2);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-1");
        }
    }

    @Test
    public void async_serverError_retryResolvesCredentialsAgain() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.newCredentialsOnEveryResolve();

        try (ProtocolRestJsonAsyncClient client = asyncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(serverErrorResponse(), successResponse());

            client.allTypes().join();

            assertThat(credentialsProvider.invalidateCallCount()).isZero();
            assertThat(credentialsProvider.resolveCallCount()).isEqualTo(2);
            assertThat(accessKeysUsed(mockHttpClient.getRequests())).containsExactly("key-0", "key-1");
        } finally {
            mockHttpClient.close();
        }
    }

    /**
     * An async retry attempt resolves the credentials on the thread that runs the attempt, which is the thread that signs the
     * request and runs the attempt's beforeTransmission interceptors. That is a future completion executor thread, not the
     * scheduler thread that fired the backoff timer. The first attempt resolves them on the calling thread.
     */
    @Test
    public void async_retryResolvesCredentialsOnTheThreadThatRunsTheAttempt() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.newCredentialsOnEveryResolve();
        List<Thread> beforeTransmissionThreads = new CopyOnWriteArrayList<>();
        ExecutionInterceptor threadCapturingInterceptor = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
                beforeTransmissionThreads.add(Thread.currentThread());
            }
        };

        try (ProtocolRestJsonAsyncClient client =
                 ProtocolRestJsonAsyncClient.builder()
                                            .credentialsProvider(credentialsProvider)
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost"))
                                            .httpClient(mockHttpClient)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(threadCapturingInterceptor))
                                            .build()) {
            mockHttpClient.stubResponses(serverErrorResponse(), successResponse());

            client.allTypes().join();

            List<Thread> resolveThreads = credentialsProvider.resolveThreads();
            assertThat(resolveThreads).hasSize(2);
            assertThat(beforeTransmissionThreads).hasSize(2);

            assertThat(resolveThreads.get(0)).isSameAs(Thread.currentThread());
            assertThat(resolveThreads.get(1)).isNotSameAs(Thread.currentThread())
                                             .isSameAs(beforeTransmissionThreads.get(1));
            assertThat(resolveThreads.get(1).getName()).startsWith("sdk-async-response");
        } finally {
            mockHttpClient.close();
        }
    }

    /**
     * A retry that blocks while refreshing credentials must not delay the timers of other requests on the client. The
     * client's scheduler has a single thread, so a refresh running on it would hold back every timer until it finished.
     */
    @Test
    public void async_blockingCredentialRefreshOnRetry_doesNotDelayOtherRequestsTimeouts() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider =
            TrackingCredentialsProvider.refreshedOnInvalidateBlockingFor(Duration.ofSeconds(2));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        try (ProtocolRestJsonAsyncClient client =
                 ProtocolRestJsonAsyncClient.builder()
                                            .credentialsProvider(credentialsProvider)
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost"))
                                            .httpClient(mockHttpClient)
                                            .overrideConfiguration(o -> o.scheduledExecutorService(scheduler))
                                            .build()) {
            // Responses are handed out in request order: the slow call's response takes 5 seconds, and the other call is
            // rejected with ExpiredToken, then succeeds on retry after the blocking refresh.
            mockHttpClient.stubResponses(Pair.of(successResponse(), Duration.ofSeconds(5)),
                                         Pair.of(authErrorResponse("ExpiredToken"), Duration.ZERO),
                                         Pair.of(successResponse(), Duration.ZERO));

            long start = System.nanoTime();
            CompletableFuture<?> slowCall =
                client.allTypes(r -> r.overrideConfiguration(o -> o.apiCallTimeout(Duration.ofMillis(300))));
            CompletableFuture<?> rejectedCall = client.allTypes();

            assertThatThrownBy(slowCall::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1500));

            rejectedCall.join();
            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);
        } finally {
            mockHttpClient.close();
            scheduler.shutdownNow();
        }
    }

    @Test
    public void accessDenied_doesNotInvalidateCredentials_andIsNotRetried() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.refreshedOnInvalidate();

        try (ProtocolRestJsonClient client = syncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(authErrorResponse("AccessDenied"));

            assertThatThrownBy(client::allTypes)
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo("AccessDenied"));

            assertThat(credentialsProvider.invalidateCallCount()).isZero();
            assertThat(mockHttpClient.getRequests()).hasSize(1);
        }
    }

    @Test
    public void async_accessDenied_doesNotInvalidateCredentials_andIsNotRetried() {
        MockAsyncHttpClient mockHttpClient = new MockAsyncHttpClient();
        TrackingCredentialsProvider credentialsProvider = TrackingCredentialsProvider.refreshedOnInvalidate();

        try (ProtocolRestJsonAsyncClient client = asyncClient(mockHttpClient, credentialsProvider)) {
            mockHttpClient.stubResponses(authErrorResponse("AccessDenied"));

            assertThatThrownBy(() -> client.allTypes().join())
                .hasCauseInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e.getCause()).awsErrorDetails().errorCode())
                    .isEqualTo("AccessDenied"));

            assertThat(credentialsProvider.invalidateCallCount()).isZero();
            assertThat(mockHttpClient.getRequests()).hasSize(1);
        } finally {
            mockHttpClient.close();
        }
    }

    // --- Helper methods ---

    private static ProtocolRestJsonClient syncClient(MockSyncHttpClient httpClient, AwsCredentialsProvider credentialsProvider) {
        return ProtocolRestJsonClient.builder()
                                     .credentialsProvider(credentialsProvider)
                                     .region(Region.US_EAST_1)
                                     .endpointOverride(URI.create("http://localhost"))
                                     .httpClient(httpClient)
                                     .build();
    }

    private static ProtocolRestJsonAsyncClient asyncClient(MockAsyncHttpClient httpClient,
                                                           AwsCredentialsProvider credentialsProvider) {
        return ProtocolRestJsonAsyncClient.builder()
                                          .credentialsProvider(credentialsProvider)
                                          .region(Region.US_EAST_1)
                                          .endpointOverride(URI.create("http://localhost"))
                                          .httpClient(httpClient)
                                          .build();
    }

    private static List<String> accessKeysUsed(List<SdkHttpRequest> requests) {
        return requests.stream()
                       .map(AuthErrorInvalidationFunctionalTest::accessKeyUsed)
                       .collect(Collectors.toList());
    }

    private static String accessKeyUsed(SdkHttpRequest request) {
        String authorization = request.firstMatchingHeader("Authorization")
                                      .orElseThrow(() -> new AssertionError("Request was not signed"));
        String credentialPrefix = "Credential=";
        int start = authorization.indexOf(credentialPrefix) + credentialPrefix.length();
        return authorization.substring(start, authorization.indexOf('/', start));
    }

    private static HttpExecuteResponse authErrorResponse(String errorCode) {
        String errorBody = "{\"message\":\"The request was rejected\"}";
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(403)
                                                           .putHeader("x-amzn-ErrorType", errorCode)
                                                           .putHeader("content-length",
                                                                      String.valueOf(errorBody.length()))
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(errorBody)))
                                  .build();
    }

    private static HttpExecuteResponse serverErrorResponse() {
        String errorBody = "{\"message\":\"Internal failure\"}";
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .putHeader("x-amzn-ErrorType", "InternalFailure")
                                                           .putHeader("content-length",
                                                                      String.valueOf(errorBody.length()))
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(errorBody)))
                                  .build();
    }

    private static HttpExecuteResponse successResponse() {
        String body = "{}";
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .putHeader("content-length",
                                                                      String.valueOf(body.length()))
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                                  .build();
    }

    // --- Test doubles ---

    /**
     * A credentials provider that vends credentials with access key {@code key-N}, where N is the current generation, and
     * records every resolution and invalidation. Depending on how it is created, the generation advances when the
     * provider is invalidated (simulating a caching provider that refreshes after invalidation), on every resolution
     * (simulating a provider whose credentials change between attempts), or never (simulating static credentials).
     * A provider can also block for a time on the first resolution after an invalidation, simulating a refresh from a slow
     * credential source on the calling thread, as the SDK's caching providers do.
     */
    private static final class TrackingCredentialsProvider implements AwsCredentialsProvider {
        private final boolean advanceOnInvalidate;
        private final boolean advanceOnResolve;
        private final Duration blockingRefreshDuration;
        private final AtomicInteger generation = new AtomicInteger();
        private final AtomicInteger resolveCount = new AtomicInteger();
        private final AtomicInteger invalidateCount = new AtomicInteger();
        private final AtomicBoolean refreshPending = new AtomicBoolean();
        private final List<Thread> resolveThreads = new CopyOnWriteArrayList<>();

        private TrackingCredentialsProvider(boolean advanceOnInvalidate, boolean advanceOnResolve,
                                            Duration blockingRefreshDuration) {
            this.advanceOnInvalidate = advanceOnInvalidate;
            this.advanceOnResolve = advanceOnResolve;
            this.blockingRefreshDuration = blockingRefreshDuration;
        }

        static TrackingCredentialsProvider refreshedOnInvalidate() {
            return new TrackingCredentialsProvider(true, false, Duration.ZERO);
        }

        static TrackingCredentialsProvider refreshedOnInvalidateBlockingFor(Duration blockingRefreshDuration) {
            return new TrackingCredentialsProvider(true, false, blockingRefreshDuration);
        }

        static TrackingCredentialsProvider newCredentialsOnEveryResolve() {
            return new TrackingCredentialsProvider(false, true, Duration.ZERO);
        }

        static TrackingCredentialsProvider neverRefreshed() {
            return new TrackingCredentialsProvider(false, false, Duration.ZERO);
        }

        @Override
        public AwsCredentials resolveCredentials() {
            if (refreshPending.compareAndSet(true, false) && !blockingRefreshDuration.isZero()) {
                try {
                    Thread.sleep(blockingRefreshDuration.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            int current = advanceOnResolve ? generation.getAndIncrement() : generation.get();
            return AwsBasicCredentials.create("key-" + current, "secret-" + current);
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            resolveCount.incrementAndGet();
            resolveThreads.add(Thread.currentThread());
            return CompletableFuture.completedFuture(resolveCredentials());
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<Void> invalidate(AwsCredentialsIdentity identity) {
            invalidateCount.incrementAndGet();
            if (advanceOnInvalidate) {
                generation.incrementAndGet();
                refreshPending.set(true);
            }
            return CompletableFuture.completedFuture(null);
        }

        int invalidateCallCount() {
            return invalidateCount.get();
        }

        int resolveCallCount() {
            return resolveCount.get();
        }

        List<Thread> resolveThreads() {
            return resolveThreads;
        }
    }
}
