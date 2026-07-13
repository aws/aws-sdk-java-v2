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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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

/**
 * Integration test verifying the end-to-end flow:
 * service returns ExpiredToken → credentials invalidated → next call uses fresh credentials.
 */
public class AuthErrorInvalidationFunctionalTest {

    /**
     * End-to-end: ExpiredToken → invalidation is triggered → next call uses fresh credentials.
     *
     * ExpiredToken exceptions are not retryable, so the first API call fails immediately.
     * However, invalidation is still triggered on the provider, ensuring the next API call
     * resolves fresh credentials.
     *
     * Scenario:
     * 1. First API call: signed with "old-key", service returns ExpiredToken (not retryable)
     * 2. SDK calls invalidate() on the provider (switching it to return "new-key")
     * 3. First call fails with ExpiredToken exception
     * 4. Second API call: resolves fresh identity "new-key", succeeds
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
                 .build()) {

            // First API call: ExpiredToken (not retryable — fails immediately)
            mockHttpClient.stubResponses(expiredTokenResponse());

            assertThatThrownBy(() -> client.allTypes())
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo("ExpiredToken"));

            // Verify invalidate was called during the first API call
            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(1);

            // Verify first call used "old-key"
            List<SdkHttpRequest> firstCallRequests = mockHttpClient.getRequests();
            assertThat(firstCallRequests).hasSize(1);
            assertRequestUsedAccessKey(firstCallRequests.get(0), "old-key");

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
     * Verify that AccessDenied does NOT trigger invalidation and the exception propagates.
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
                 .build()) {

            mockHttpClient.stubResponses(accessDeniedResponse());

            assertThatThrownBy(() -> client.allTypes())
                .isInstanceOf(AwsServiceException.class)
                .satisfies(e -> assertThat(((AwsServiceException) e).awsErrorDetails().errorCode())
                    .isEqualTo("AccessDenied"));

            // Verify invalidate was NOT called
            assertThat(credentialsProvider.invalidateCallCount()).isEqualTo(0);
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
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(403)
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
}
