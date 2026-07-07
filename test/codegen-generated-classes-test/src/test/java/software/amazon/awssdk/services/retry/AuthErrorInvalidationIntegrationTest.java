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

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Integration test verifying the end-to-end flow:
 * service returns ExpiredToken → credentials invalidated → retry uses fresh credentials.
 *
 * Also verifies backoff interaction: when refresh backoff is active, invalidate() does not
 * bypass it, and stale cached credentials are returned until the backoff elapses.
 *
 * Validates Requirements: 6, 8, 9, 10, 21
 */
public class AuthErrorInvalidationIntegrationTest {

    /**
     * End-to-end: ExpiredToken → invalidation is triggered → next call uses fresh credentials.
     *
     * The SDK resolves identity once per API call (in beforeExecution interceptor), so the retry
     * within the same call reuses the same identity. However, invalidation marks the provider's
     * cache as stale, ensuring the NEXT API call resolves fresh credentials.
     *
     * Scenario:
     * 1. First API call: signed with "old-key", service returns 500 with ExpiredToken
     * 2. SDK calls invalidate() on the provider (switching it to return "new-key")
     * 3. SDK retries (same identity — "old-key" — because identity was already resolved)
     * 4. Second retry succeeds with 200
     * 5. Second API call: resolves fresh identity "new-key"
     * 6. Verify invalidate was called and second API call uses new credentials
     */
    @Test
    public void expiredToken_invalidatesCredentials_nextCallUsesFreshCredentials() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        InvalidationTrackingCredentialsProvider credentialsProvider =
            new InvalidationTrackingCredentialsProvider("old-key", "old-secret", "new-key", "new-secret");

        try (ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                 .credentialsProvider(credentialsProvider)
                 .region(Region.US_EAST_1)
                 .endpointOverride(URI.create("http://localhost"))
                 .httpClient(mockHttpClient)
                 .overrideConfiguration(c -> c.retryStrategy(RetryMode.STANDARD))
                 .build()) {

            // First API call: 500 ExpiredToken (retried), then 200 on retry
            mockHttpClient.stubResponses(expiredTokenResponse(), successResponse());

            // First call succeeds on retry (same credentials used for both attempts since
            // identity is resolved once per API call)
            client.allTypes();

            // Verify invalidate was called during the first API call
            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);

            // Verify first call's requests both used "old-key" (same identity, same call)
            List<SdkHttpRequest> firstCallRequests = mockHttpClient.getRequests();
            assertThat(firstCallRequests).hasSize(2);
            assertRequestUsedAccessKey(firstCallRequests.get(0), "old-key");
            assertRequestUsedAccessKey(firstCallRequests.get(1), "old-key");

            // Now make a second API call — this should resolve fresh credentials
            mockHttpClient.reset();
            mockHttpClient.stubResponses(successResponse());

            client.allTypes();

            // Second call should use new credentials (provider was invalidated)
            List<SdkHttpRequest> secondCallRequests = mockHttpClient.getRequests();
            assertThat(secondCallRequests).hasSize(1);
            assertRequestUsedAccessKey(secondCallRequests.get(0), "new-key");
        }
    }

    /**
     * Verify that AccessDenied does NOT trigger invalidation.
     *
     * Scenario:
     * 1. Client makes a request signed with "my-key"
     * 2. Service returns 403 with error code "AccessDenied"
     * 3. SDK does NOT invalidate credentials (AccessDenied is not an auth error for invalidation)
     * 4. Request fails (403 is not retryable by default for standard retry)
     */
    @Test
    public void accessDenied_doesNotInvalidateCredentials() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        InvalidationTrackingCredentialsProvider credentialsProvider =
            new InvalidationTrackingCredentialsProvider("my-key", "my-secret", "new-key", "new-secret");

        try (ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                 .credentialsProvider(credentialsProvider)
                 .region(Region.US_EAST_1)
                 .endpointOverride(URI.create("http://localhost"))
                 .httpClient(mockHttpClient)
                 .overrideConfiguration(c -> c.retryStrategy(RetryMode.STANDARD))
                 .build()) {

            // Response: 403 with AccessDenied error code
            HttpExecuteResponse accessDeniedResponse = accessDeniedResponse();
            mockHttpClient.stubResponses(accessDeniedResponse);

            try {
                client.allTypes();
            } catch (Exception e) {
                // Expected — 403 is not retryable
            }

            // Verify invalidate was NOT called
            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(0);
        }
    }

    /**
     * Backoff interaction: invalidation during active backoff returns stale credentials.
     *
     * This test verifies that when CachedSupplier has an active refresh backoff
     * (nextAllowedRefreshTime in the future), calling invalidate() marks the cache stale
     * but does NOT bypass the backoff. The next get() still returns cached credentials
     * until the backoff elapses.
     *
     * Validates Requirements: 9, 10
     */
    @Test
    public void invalidation_duringActiveBackoff_returnsStaleCredentials() {
        AdjustableClock clock = new AdjustableClock();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        AtomicInteger fetchCount = new AtomicInteger(0);
        AtomicReference<Supplier<RefreshResult<String>>> supplierRef = new AtomicReference<>();

        // Set up the initial supplier that returns credentials
        supplierRef.set(() -> RefreshResult.builder("access-key-1")
                                           .staleTime(now.plusSeconds(60))
                                           .prefetchTime(now.plusSeconds(30))
                                           .build());

        try (CachedSupplier<String> cache = CachedSupplier.builder(() -> {
                 fetchCount.incrementAndGet();
                 return supplierRef.get().get();
             })
                 .staleValueBehavior(CachedSupplier.StaleValueBehavior.ALLOW)
                 .clock(clock)
                 .build()) {

            // Initial fetch
            assertThat(cache.get()).isEqualTo("access-key-1");
            assertThat(fetchCount.get()).isEqualTo(1);

            // Advance past stale time and trigger a refresh failure to set nextAllowedRefreshTime
            clock.time = now.plusSeconds(61);
            supplierRef.set(() -> {
                throw new RuntimeException("credential source unavailable");
            });
            // This get() will try to refresh, fail, set backoff, and return stale value
            assertThat(cache.get()).isEqualTo("access-key-1");

            // Now call invalidate — marks staleTime = now but does NOT clear backoff
            clock.time = now.plusSeconds(62);
            cache.invalidate(v -> v.equals("access-key-1"));

            // Set up fresh credentials in the supplier
            supplierRef.set(() -> RefreshResult.builder("access-key-2")
                                               .staleTime(now.plusSeconds(7200))
                                               .prefetchTime(now.plusSeconds(5400))
                                               .build());

            // get() should return STALE credentials because backoff is still active
            // (nextAllowedRefreshTime = ~now+61 + [300,600]s, we're at now+62)
            assertThat(cache.get()).isEqualTo("access-key-1");

            // Advance past maximum possible backoff (61 + 600 = 661 seconds from original now)
            clock.time = now.plusSeconds(700);

            // Now backoff has elapsed — next get() should refresh and return fresh value
            assertThat(cache.get()).isEqualTo("access-key-2");
        }
    }

    /**
     * Verify that after backoff elapses following invalidation, fresh credentials are obtained.
     *
     * This tests the complete lifecycle:
     * 1. Cache has credentials
     * 2. Refresh fails → backoff is set
     * 3. invalidate() is called → staleTime set to now (but backoff stays)
     * 4. During backoff → stale value returned
     * 5. After backoff → fresh value obtained
     *
     * Validates Requirements: 8, 9, 10
     */
    @Test
    public void afterBackoffElapses_invalidatedCache_obtainsFreshCredentials() {
        AdjustableClock clock = new AdjustableClock();
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        clock.time = now;

        AtomicReference<Supplier<RefreshResult<String>>> supplierRef = new AtomicReference<>();
        supplierRef.set(() -> RefreshResult.builder("cred-A")
                                           .staleTime(now.plusSeconds(60))
                                           .prefetchTime(now.plusSeconds(30))
                                           .build());

        try (CachedSupplier<String> cache = CachedSupplier.builder(() -> supplierRef.get().get())
                 .staleValueBehavior(CachedSupplier.StaleValueBehavior.ALLOW)
                 .clock(clock)
                 .build()) {

            // Initial fetch
            assertThat(cache.get()).isEqualTo("cred-A");

            // Advance time so cache is stale and trigger failure
            clock.time = now.plusSeconds(61);
            supplierRef.set(() -> {
                throw new RuntimeException("source down");
            });
            assertThat(cache.get()).isEqualTo("cred-A"); // stale value returned

            // Invalidate
            clock.time = now.plusSeconds(62);
            cache.invalidate(v -> v.equals("cred-A"));

            // Set up fresh supplier
            supplierRef.set(() -> RefreshResult.builder("cred-B")
                                               .staleTime(now.plusSeconds(7200))
                                               .prefetchTime(now.plusSeconds(5400))
                                               .build());

            // Still within backoff — stale returned
            clock.time = now.plusSeconds(100);
            assertThat(cache.get()).isEqualTo("cred-A");

            // Advance past max backoff
            clock.time = now.plusSeconds(700);
            assertThat(cache.get()).isEqualTo("cred-B");
        }
    }

    // --- Helper methods ---

    private void assertRequestUsedAccessKey(SdkHttpRequest request, String expectedAccessKeyId) {
        assertThat(request.firstMatchingHeader("Authorization"))
            .isPresent()
            .hasValueSatisfying(authHeader ->
                assertThat(authHeader).contains("Credential=" + expectedAccessKeyId + "/")
            );
    }

    private HttpExecuteResponse expiredTokenResponse() {
        String errorBody = "{\"__type\":\"ExpiredTokenException\",\"message\":\"The security token included in the request "
                           + "is expired\"}";
        // Use 500 so the standard retry strategy retries it.
        // In production, services may return 4xx for expired tokens, but the SDK only retries
        // on 5xx status codes or specific retryable error codes. Using 500 ensures the retry
        // happens so we can verify the full invalidation → retry → fresh credentials flow.
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .putHeader("x-amzn-ErrorType", "ExpiredToken")
                                                           .putHeader("content-length",
                                                                      String.valueOf(errorBody.length()))
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(errorBody)))
                                  .build();
    }

    private HttpExecuteResponse accessDeniedResponse() {
        String errorBody = "{\"__type\":\"AccessDeniedException\",\"message\":\"User is not authorized\"}";
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(403)
                                                           .putHeader("x-amzn-ErrorType", "AccessDenied")
                                                           .putHeader("content-length",
                                                                      String.valueOf(errorBody.length()))
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(errorBody)))
                                  .build();
    }

    private HttpExecuteResponse successResponse() {
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
     * A credentials provider that tracks invalidation calls and switches to fresh credentials
     * after invalidation is triggered. This simulates the behavior of a caching provider
     * (like IMDS or Container provider) that serves stale credentials until invalidated.
     */
    private static class InvalidationTrackingCredentialsProvider implements AwsCredentialsProvider {
        private final AwsBasicCredentials oldCredentials;
        private final AwsBasicCredentials newCredentials;
        private final AtomicInteger invalidateCount = new AtomicInteger(0);
        private volatile boolean invalidated = false;

        InvalidationTrackingCredentialsProvider(String oldAccessKey, String oldSecretKey,
                                               String newAccessKey, String newSecretKey) {
            this.oldCredentials = AwsBasicCredentials.create(oldAccessKey, oldSecretKey);
            this.newCredentials = AwsBasicCredentials.create(newAccessKey, newSecretKey);
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return invalidated ? newCredentials : oldCredentials;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            AwsCredentials creds = resolveCredentials();
            return CompletableFuture.completedFuture(creds);
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<Void> invalidate(AwsCredentialsIdentity identity) {
            invalidateCount.incrementAndGet();
            invalidated = true;
            return CompletableFuture.completedFuture(null);
        }

        int invalidateCallCount() {
            return invalidateCount.get();
        }
    }

    /**
     * A clock whose time can be manually adjusted for testing backoff behavior.
     */
    private static class AdjustableClock extends Clock {
        volatile Instant time = Instant.now();

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return time;
        }
    }
}
