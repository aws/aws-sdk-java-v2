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

package software.amazon.awssdk.services.signin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.VALID_TEST_PEM;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.signin.SigninClient;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.signin.model.ValidationException;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.cache.CachedSupplier;

/**
 * Tests for {@link LoginCredentialsProvider#invalidate}: after a service rejects the Login credentials, the next refresh must
 * not return the same credentials from the token cache file, even when the token has not reached its refresh window.
 *
 * <p>Each token in the cache file is a 15-minute Login token with 14 minutes left, so it is outside both the 1-minute
 * mandatory and the 5-minute advisory refresh windows. Without a rejection, the provider returns its credentials without
 * calling the Sign-In service.
 */
public class LoginCredentialsProviderInvalidateTest {
    private static final String LOGIN_SESSION_ID = "loginSessionId";
    private static final String REJECTED_AKID = "akid-rejected";
    private static final int OUTSIDE_REFRESH_WINDOWS_SECONDS = 840;

    @TempDir
    Path tempDir;

    private MockSyncHttpClient signinHttpClient;
    private SigninClient signinClient;
    private OnDiskTokenManager tokenManager;
    private final List<AutoCloseable> resources = new ArrayList<>();

    @BeforeEach
    public void setup() {
        signinHttpClient = new MockSyncHttpClient();
        // Retries are disabled so that each refresh attempt makes exactly one Sign-In request.
        signinClient = SigninClient.builder()
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(URI.create("https://custom-signin-endpoint.com"))
                                   .httpClient(signinHttpClient)
                                   .overrideConfiguration(c -> c.retryStrategy(AwsRetryStrategy.doNotRetry()))
                                   .build();
        resources.add(signinClient);
        tokenManager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (AutoCloseable resource : resources) {
            resource.close();
        }
    }

    @ParameterizedTest(name = "asyncCredentialUpdateEnabled={0}")
    @ValueSource(booleans = {false, true})
    public void invalidate_whenTokenFileHoldsRejectedKey_refreshesFromSignin(boolean asyncCredentialUpdateEnabled) {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(asyncCredentialUpdateEnabled);
        AwsCredentials rejected = provider.resolveCredentials();
        assertThat(rejected.accessKeyId()).isEqualTo(REJECTED_AKID);
        assertThat(signinRequests()).isEmpty();

        signinHttpClient.stubNextResponse(successfulRefreshResponse("akid-new"));
        provider.invalidate(rejected).join();

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid-new");
        assertThat(signinRequests()).hasSize(1);
        assertThat(signinRequests().get(0).encodedPath()).isEqualTo("/v1/token");
        LoginAccessToken storedToken = tokenManager.loadToken().get();
        assertThat(storedToken.getAccessToken().accessKeyId()).isEqualTo("akid-new");
        assertThat(storedToken.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(recordedRejection(provider)).isNull();

        // A late rejection of the replaced credentials does not cause another Sign-In call.
        provider.invalidate(rejected).join();
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid-new");
        assertThat(signinRequests()).hasSize(1);
    }

    @Test
    public void invalidate_whenRejectedKeyIsNotCached_keepsCachedCredentialsWithoutCallingSignin() {
        storeToken("akid-current", OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        AwsCredentials cached = provider.resolveCredentials();

        provider.invalidate(AwsBasicCredentials.create("akid-other", "skid")).join();

        assertThat(provider.resolveCredentials()).isSameAs(cached);
        assertThat(signinRequests()).isEmpty();
        assertThat(tokenManager.loadToken().get().getAccessToken().accessKeyId()).isEqualTo("akid-current");
    }

    @Test
    public void invalidate_whenTokenFileRefreshedByAnotherProcess_usesFileWithoutCallingSignin() {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        AwsCredentials rejected = provider.resolveCredentials();

        // Another process, such as the AWS CLI, refreshed the token cache file.
        storeToken("akid-from-cli", 900);
        provider.invalidate(rejected).join();

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid-from-cli");
        assertThat(signinRequests()).isEmpty();
        assertThat(recordedRejection(provider)).isNull();
    }

    /**
     * A transient Sign-In failure keeps static stability and the refresh backoff, as for any failed refresh. The rejection
     * stays recorded, so once the backoff has elapsed, the next refresh calls the Sign-In service again instead of returning
     * the rejected credentials from the token cache file.
     */
    @Test
    public void invalidate_whenSigninFailsTransiently_returnsCachedCredentialsAndCallsSigninAgainAfterBackoff() {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        AwsCredentials rejected = provider.resolveCredentials();

        signinHttpClient.stubResponses(errorResponse("InternalServerException", 500, "{\"message\":\"Internal error\"}"),
                                       successfulRefreshResponse("akid-new"));
        provider.invalidate(rejected).join();

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo(REJECTED_AKID);
            assertThat(signinRequests()).hasSize(1);
            assertThat(refreshBackoffWarnings(logCaptor)).hasSize(1);

            // Inside the backoff, the cached credentials are returned without another Sign-In request.
            assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo(REJECTED_AKID);
            assertThat(signinRequests()).hasSize(1);
        }
        assertThat(recordedRejection(provider)).isEqualTo(REJECTED_AKID);

        // The provider does not expose the cache's clock, so the end of the backoff is simulated.
        expireRefreshBackoff(provider);

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid-new");
        assertThat(signinRequests()).hasSize(2);
        assertThat(recordedRejection(provider)).isNull();
    }

    /**
     * A non-recoverable Sign-In failure is raised, and an immediate retry rethrows the cached error; the rejected
     * credentials are never returned.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("nonRecoverableSigninErrors")
    public void invalidate_whenSigninFailsNonRecoverably_raisesError(String errorType, int statusCode, String errorBody,
                                                                    Class<? extends Throwable> expectedType,
                                                                    String expectedMessage) {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        AwsCredentials rejected = provider.resolveCredentials();

        signinHttpClient.stubNextResponse(errorResponse(errorType, statusCode, errorBody));
        provider.invalidate(rejected).join();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(expectedType).hasMessageContaining(expectedMessage);
        assertThat(signinRequests()).hasSize(1);

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(expectedType);
        assertThat(signinRequests()).hasSize(1);
    }

    private static Stream<Arguments> nonRecoverableSigninErrors() {
        return Stream.of(
            Arguments.of("ValidationException", 400,
                         "{\"error\":\"INVALID_REQUEST\",\"message\":\"The refresh token is malformed.\"}",
                         ValidationException.class, "The refresh token is malformed."),
            Arguments.of("AccessDeniedException", 401,
                         "{\"error\":\"TOKEN_EXPIRED\",\"message\":\"The refresh token has expired.\"}",
                         SdkClientException.class, "Your session has expired. Please reauthenticate."));
    }

    /**
     * End to end: a SigV4 call signed with the Login credentials gets an unmodeled {@code ExpiredToken} error. The SDK
     * invalidates the credentials and retries, and the retry is signed with new credentials from the Sign-In service.
     */
    @Test
    public void invalidate_whenServiceReturnsExpiredToken_retryIsSignedWithRefreshedCredentials() {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        signinHttpClient.stubNextResponse(successfulRefreshResponse("akid-new"));

        MockSyncHttpClient targetHttpClient = new MockSyncHttpClient();
        targetHttpClient.stubResponses(
            errorResponse("ExpiredToken", 400, "{\"message\":\"The security token included in the request is expired\"}"),
            response(200, "{}"));
        try (SigninClient target = SigninClient.builder()
                                               .region(Region.US_EAST_1)
                                               .endpointOverride(URI.create("https://target.example.com"))
                                               .httpClient(targetHttpClient)
                                               .credentialsProvider(provider)
                                               .build()) {
            target.getResourcePolicy(r -> { });
        }

        List<String> signingKeys = targetHttpClient.getRequests().stream()
                                                   .map(LoginCredentialsProviderInvalidateTest::signingAccessKeyId)
                                                   .collect(Collectors.toList());
        assertThat(signingKeys).containsExactly(REJECTED_AKID, "akid-new");
        assertThat(signinRequests()).hasSize(1);
    }

    @Test
    public void invalidate_whenRefreshedKeyIsAlsoRejected_refreshesFromSigninAgain() {
        storeToken(REJECTED_AKID, OUTSIDE_REFRESH_WINDOWS_SECONDS);
        LoginCredentialsProvider provider = provider(false);
        AwsCredentials firstRejected = provider.resolveCredentials();
        signinHttpClient.stubResponses(successfulRefreshResponse("akid-new-1"), successfulRefreshResponse("akid-new-2"));

        provider.invalidate(firstRejected).join();
        AwsCredentials secondRejected = provider.resolveCredentials();
        assertThat(secondRejected.accessKeyId()).isEqualTo("akid-new-1");
        assertThat(signinRequests()).hasSize(1);

        provider.invalidate(secondRejected).join();
        assertThat(recordedRejection(provider)).isEqualTo("akid-new-1");

        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("akid-new-2");
        assertThat(signinRequests()).hasSize(2);
        assertThat(tokenManager.loadToken().get().getAccessToken().accessKeyId()).isEqualTo("akid-new-2");
        assertThat(recordedRejection(provider)).isNull();
    }

    private LoginCredentialsProvider provider(boolean asyncCredentialUpdateEnabled) {
        LoginCredentialsProvider provider = LoginCredentialsProvider.builder()
                                                                    .loginSession(LOGIN_SESSION_ID)
                                                                    .signinClient(signinClient)
                                                                    .tokenCacheLocation(tempDir)
                                                                    .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                                                    .build();
        resources.add(provider);
        return provider;
    }

    private void storeToken(String accessKeyId, int remainingSeconds) {
        AwsSessionCredentials credentials = AwsSessionCredentials.builder()
                                                                 .accessKeyId(accessKeyId)
                                                                 .secretAccessKey("skid")
                                                                 .sessionToken("sessionToken")
                                                                 .accountId("123456789012")
                                                                 .expirationTime(Instant.now().plusSeconds(remainingSeconds))
                                                                 .build();
        tokenManager.storeToken(LoginAccessToken.builder()
                                                .accessToken(credentials)
                                                .clientId("client-123")
                                                .dpopKey(VALID_TEST_PEM)
                                                .refreshToken("refresh-token")
                                                .tokenType("aws_sigv4")
                                                .identityToken("id-token")
                                                .build());
    }

    private List<SdkHttpRequest> signinRequests() {
        return signinHttpClient.getRequests();
    }

    private static HttpExecuteResponse successfulRefreshResponse(String accessKeyId) {
        return response(200, "{\"accessToken\":{\"accessKeyId\":\"" + accessKeyId + "\",\"secretAccessKey\":\"new-skid\","
                             + "\"sessionToken\":\"new-session-token\"},\"tokenType\":\"aws_sigv4\",\"expiresIn\":900,"
                             + "\"refreshToken\":\"new-refresh-token\"}");
    }

    private static HttpExecuteResponse errorResponse(String errorType, int statusCode, String body) {
        return response(SdkHttpResponse.builder().statusCode(statusCode).putHeader("X-Amzn-Errortype", errorType), body);
    }

    private static HttpExecuteResponse response(int statusCode, String body) {
        return response(SdkHttpResponse.builder().statusCode(statusCode), body);
    }

    private static HttpExecuteResponse response(SdkHttpResponse.Builder response, String body) {
        return HttpExecuteResponse.builder()
                                  .response(response.build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                                  .build();
    }

    private static String signingAccessKeyId(SdkHttpRequest request) {
        String authorization = request.firstMatchingHeader("Authorization").orElse("");
        int start = authorization.indexOf("Credential=") + "Credential=".length();
        return authorization.substring(start, authorization.indexOf('/', start));
    }

    private static List<String> refreshBackoffWarnings(LogCaptor logCaptor) {
        return logCaptor.loggedEvents().stream()
                        .filter(event -> event.getLevel() == Level.WARN)
                        .map(event -> event.getMessage().getFormattedMessage())
                        .filter(message -> message.contains("Credential refresh failed"))
                        .collect(Collectors.toList());
    }

    /**
     * The access key ID of the rejection that the provider has recorded and not yet handled, or {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static String recordedRejection(LoginCredentialsProvider provider) {
        return ((AtomicReference<String>) readField(LoginCredentialsProvider.class, provider, "rejectedAccessKeyId")).get();
    }

    /**
     * Ends the provider's refresh backoff, as if it had elapsed.
     */
    private static void expireRefreshBackoff(LoginCredentialsProvider provider) {
        Object credentialCache = readField(LoginCredentialsProvider.class, provider, "credentialCache");
        try {
            Field nextAllowedRefreshTime = CachedSupplier.class.getDeclaredField("nextAllowedRefreshTime");
            nextAllowedRefreshTime.setAccessible(true);
            nextAllowedRefreshTime.set(credentialCache, Instant.now().minusSeconds(1));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object readField(Class<?> type, Object target, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
