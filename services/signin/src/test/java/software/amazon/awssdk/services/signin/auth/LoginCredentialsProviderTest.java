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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.VALID_TEST_PEM;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.getJwtPayloadFromEncodedDpopHeader;
import static software.amazon.awssdk.services.signin.auth.internal.DpopTestUtils.verifySignature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.signin.SigninClient;
import software.amazon.awssdk.services.signin.internal.AccessTokenManager;
import software.amazon.awssdk.services.signin.internal.InvalidLoginTokenException;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.signin.model.CreateOAuth2TokenRequest;
import software.amazon.awssdk.services.signin.model.AccessDeniedException;
import software.amazon.awssdk.services.signin.model.OAuth2ErrorCode;
import software.amazon.awssdk.services.signin.model.SigninException;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringInputStream;

public class LoginCredentialsProviderTest {
    private static final String LOGIN_SESSION_ID = "loginSessionId";

    // Distinctive values used to verify that error messages never include token contents.
    private static final String SECRET_ACCESS_KEY = "SECRET-ACCESS-KEY-VALUE";
    private static final String SESSION_TOKEN = "SESSION-TOKEN-VALUE";
    private static final String REFRESH_TOKEN = "REFRESH-TOKEN-VALUE";
    private static final String INVALID_DPOP_KEY = "NOT-A-PEM-DPOP-KEY-VALUE";

    private AccessTokenManager tokenManager;
    private SigninClient signinClient;
    private MockSyncHttpClient mockHttpClient;
    private CaptureRequestInterceptor captureRequestInterceptor;
    private LoginCredentialsProvider loginCredentialsProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        captureRequestInterceptor = new CaptureRequestInterceptor();
        signinClient = SigninClient
            .builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("https://custom-signin-endpoint.com"))
            .httpClient(mockHttpClient)
            .overrideConfiguration(c -> c.addExecutionInterceptor(captureRequestInterceptor))
            .build();

        tokenManager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);

        loginCredentialsProvider = LoginCredentialsProvider
            .builder()
            .loginSession(LOGIN_SESSION_ID)
            .signinClient(signinClient)
            .tokenCacheLocation(tempDir)
            .build();
    }

    @Test
    public void missingSigninClient_throwsException() {
        assertThrows(NullPointerException.class, ()-> {
            LoginCredentialsProvider.builder().loginSession(LOGIN_SESSION_ID).build();
        });
    }

    @Test
    public void missingLoginSession_throwsException() {
        assertThrows(IllegalArgumentException.class, ()-> {
            LoginCredentialsProvider.builder().loginSession("").signinClient(signinClient).build();
        });
    }

    @Test
    public void resolveCredentials_whenCredentialsFresh_usesFromDisk() {
        AwsSessionCredentials creds = buildCredentials( Instant.now().plusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        AwsCredentials resolveCredentials = loginCredentialsProvider.resolveCredentials();

        assertEquals(0, mockHttpClient.getRequests().size());

        assertEquals(creds.accessKeyId(), resolveCredentials.accessKeyId());
        assertEquals(creds.secretAccessKey(), resolveCredentials.secretAccessKey());
        assertEquals(creds.accountId(), resolveCredentials.accountId());
        assertEquals(creds.expirationTime(), resolveCredentials.expirationTime());
        assertEquals(BusinessMetricFeatureId.CREDENTIALS_LOGIN.value(), resolveCredentials.providerName().orElse(null));
    }

    @Test
    public void resolveCredentials_whenCredentialsNearExpiration_refreshesAndUpdatesCache() throws Exception {
        // within the stale time
        AwsSessionCredentials creds = buildCredentials(Instant.now().plusSeconds(10));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        stubSuccessfulRefreshResponse();

        AwsCredentials resolvedCredentials = loginCredentialsProvider.resolveCredentials();

        // verify the service was called with correct arguments
        assertEquals(1, captureRequestInterceptor.requests.size());
        assertInstanceOf(CreateOAuth2TokenRequest.class, captureRequestInterceptor.requests.get(0));
        CreateOAuth2TokenRequest request = (CreateOAuth2TokenRequest) captureRequestInterceptor.requests.get(0);
        assertEquals(token.getClientId(), request.tokenInput().clientId());
        assertEquals(token.getRefreshToken(), request.tokenInput().refreshToken());
        assertEquals("refresh_token", request.tokenInput().grantType());

        // verify the request is correctly signed with DPoP header
        verifyDpopHeader(captureRequestInterceptor.httpRequests.get(0));

        // verify that returned credentials are updated
        verifyResolvedCredentialsAreUpdated(resolvedCredentials);

        // verify that the token is correctly refreshed on disk
        verifyTokenCacheUpdated();
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_refreshesAndUpdatesCache() throws Exception {
        // within the stale time
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        stubSuccessfulRefreshResponse();

        AwsCredentials resolvedCredentials = loginCredentialsProvider.resolveCredentials();

        // verify the service was called with correct arguments
        assertEquals(1, captureRequestInterceptor.requests.size());
        assertInstanceOf(CreateOAuth2TokenRequest.class, captureRequestInterceptor.requests.get(0));
        CreateOAuth2TokenRequest request = (CreateOAuth2TokenRequest) captureRequestInterceptor.requests.get(0);
        assertEquals(token.getClientId(), request.tokenInput().clientId());
        assertEquals(token.getRefreshToken(), request.tokenInput().refreshToken());
        assertEquals("refresh_token", request.tokenInput().grantType());

        // verify the request is correctly signed with DPoP header
        verifyDpopHeader(captureRequestInterceptor.httpRequests.get(0));

        // verify that returned credentials are updated
        verifyResolvedCredentialsAreUpdated(resolvedCredentials);

        // verify that the token is correctly refreshed on disk
        verifyTokenCacheUpdated();
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_serviceCallFailsWithGeneric500_raisesException() {
        // expired - no cached value in CachedSupplier yet, so ALLOW still throws on first failure
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        mockHttpClient.stubNextResponse(
            HttpExecuteResponse
                .builder()
                .response(SdkHttpResponse.builder().statusCode(500).build())
                .build()
        );
        assertThrows(SigninException.class, () -> loginCredentialsProvider.resolveCredentials());
    }

    @Test
    public void resolveCredentials_transientFailureAfterSuccessfulCache_returnsCachedCredentials() {
        // First: store token with expired credentials so it triggers refresh from service
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        // First response: successful refresh with short-lived credentials (expires in 30s)
        // staleTime will be now+30s - 1min = now-30s (already stale), so next get() will refresh again
        String shortLivedJsonBody =
            "{\"accessToken\":"
            + "{\"accessKeyId\":\"new-akid\","
            + "\"secretAccessKey\":\"new-skid\","
            + "\"sessionToken\":\"new-session-token\"},"
            + "\"tokenType\":\"aws_sigv4\","
            + "\"expiresIn\":30,"
            + "\"refreshToken\":\"new-refresh-token\"}";

        HttpExecuteResponse successResponse = HttpExecuteResponse
            .builder()
            .response(SdkHttpResponse.builder().statusCode(200).build())
            .responseBody(AbortableInputStream.create(
                new ByteArrayInputStream(shortLivedJsonBody.getBytes(StandardCharsets.UTF_8))))
            .build();

        // Second response: transient 500 error
        HttpExecuteResponse failureResponse = HttpExecuteResponse
            .builder()
            .response(SdkHttpResponse.builder().statusCode(500).build())
            .build();

        mockHttpClient.stubResponses(successResponse, failureResponse);

        // First call: succeeds and populates the CachedSupplier cache
        AwsCredentials firstResolve = loginCredentialsProvider.resolveCredentials();
        assertEquals("new-akid", firstResolve.accessKeyId());

        // Second call: the cached value is already stale (30s expiry - 1min staleTime < now),
        // so CachedSupplier tries to refresh, gets 500, and with ALLOW behavior returns cached value
        AwsCredentials secondResolve = loginCredentialsProvider.resolveCredentials();
        assertEquals("new-akid", secondResolve.accessKeyId());
        assertEquals("new-skid", secondResolve.secretAccessKey());
    }

    /**
     * The advisory refresh window must be honored exactly, rather than being jittered to some later point. Here the
     * configured window covers the lifetime of the refreshed credentials, so the advisory window opens the moment the Sign-In
     * service returns them and the very next call must refresh again. A jittered window would instead open at a random point
     * up to a minute before the mandatory refresh window, and the second call would be served from the cache.
     */
    @Test
    public void resolveCredentials_advisoryWindowIsNotJittered() {
        // Expired token on disk, so the first resolve refreshes from the Sign-In service.
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().minusSeconds(600))));

        // Both responses vend credentials with a 600 second lifetime, matching the configured advisory window below.
        mockHttpClient.stubResponses(successfulRefreshResponse("first-akid"), successfulRefreshResponse("second-akid"));

        // Async updates are disabled explicitly, rather than relying on that being the default, so that the advisory refresh
        // below happens on the calling thread and the assertions do not race a background refresh.
        try (LoginCredentialsProvider credentialsProvider = LoginCredentialsProvider.builder()
                                                                                   .loginSession(LOGIN_SESSION_ID)
                                                                                   .signinClient(signinClient)
                                                                                   .tokenCacheLocation(tempDir)
                                                                                   .prefetchTime(Duration.ofSeconds(600))
                                                                                   .asyncCredentialUpdateEnabled(false)
                                                                                   .build()) {
            assertEquals("first-akid", credentialsProvider.resolveCredentials().accessKeyId());
            assertEquals(1, mockHttpClient.getRequests().size());

            String secondAkid = credentialsProvider.resolveCredentials().accessKeyId();
            assertEquals(2, mockHttpClient.getRequests().size());
            assertEquals("second-akid", secondAkid);
        }
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_serviceCallFailsWithTokenExpired_raisesException() {
        // expired
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        stubAccessDeniedException(OAuth2ErrorCode.TOKEN_EXPIRED);
        SdkClientException e = assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertTrue(e.getMessage().contains("Your session has expired"));
        assertInstanceOf(AccessDeniedException.class, e.getCause());
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_serviceCallFailsWithUserExpired_raisesException() {
        // expired
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        stubAccessDeniedException(OAuth2ErrorCode.USER_CREDENTIALS_CHANGED);
        SdkClientException e = assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertTrue(e.getMessage().contains("change in your password"));
        assertInstanceOf(AccessDeniedException.class, e.getCause());
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_serviceCallFailsWithInsufficentPermissions_raisesException() {
        // expired
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        stubAccessDeniedException(OAuth2ErrorCode.INSUFFICIENT_PERMISSIONS);
        SdkClientException e = assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertTrue(e.getMessage().contains("insufficient permissions"));
    }

    @Test
    public void resolveCredentials_tokenExpired_cachedBriefly_immediateRetryDoesNotCallService() {
        // Expired credentials force a refresh on every call
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        // Service returns TOKEN_EXPIRED — non-recoverable
        stubAccessDeniedException(OAuth2ErrorCode.TOKEN_EXPIRED);

        // First call: hits service, gets non-recoverable error — thrown and cached
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(1, mockHttpClient.getRequests().size());

        // Second call: immediate retry — should re-raise cached error without calling service
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(1, mockHttpClient.getRequests().size()); // Still 1 — service NOT called again
    }

    @Test
    public void resolveCredentials_tokenMissing_cachedBriefly_immediateRetryDoesNotReadDiskAgain() {
        // No token on disk — will throw InvalidLoginTokenException (non-recoverable)
        // This is a client-side error that doesn't call the service at all

        // First call: fails with missing token
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(0, mockHttpClient.getRequests().size()); // Service never called

        // Second call: immediate retry — should re-raise cached error
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(0, mockHttpClient.getRequests().size()); // Service still never called
    }

    @Test
    public void resolveCredentials_userCredentialsChanged_cachedBriefly_immediateRetryDoesNotCallService() {
        // Expired credentials force a refresh
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        // Service returns USER_CREDENTIALS_CHANGED — non-recoverable
        stubAccessDeniedException(OAuth2ErrorCode.USER_CREDENTIALS_CHANGED);

        // First call: hits service, gets non-recoverable error — thrown and cached
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(1, mockHttpClient.getRequests().size());

        // Second call: immediate retry — should re-raise cached error without calling service
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
        assertEquals(1, mockHttpClient.getRequests().size()); // Still 1 — service NOT called again
    }

    @Test
    public void resolveCredentials_tokenCacheMissingAfterSuccessfulCache_throwsAndBypassesStaticStability() throws Exception {
        // Build a provider without async updates so refresh is synchronous
        LoginCredentialsProvider syncProvider = LoginCredentialsProvider
            .builder()
            .loginSession(LOGIN_SESSION_ID)
            .signinClient(signinClient)
            .tokenCacheLocation(tempDir)
            .asyncCredentialUpdateEnabled(false)
            .build();

        // First: store token with expired credentials so it triggers refresh from service
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        // First response: successful refresh with short-lived credentials (expires in 30s)
        String shortLivedJsonBody =
            "{\"accessToken\":"
            + "{\"accessKeyId\":\"new-akid\","
            + "\"secretAccessKey\":\"new-skid\","
            + "\"sessionToken\":\"new-session-token\"},"
            + "\"tokenType\":\"aws_sigv4\","
            + "\"expiresIn\":30,"
            + "\"refreshToken\":\"new-refresh-token\"}";

        HttpExecuteResponse successResponse = HttpExecuteResponse
            .builder()
            .response(SdkHttpResponse.builder().statusCode(200).build())
            .responseBody(AbortableInputStream.create(
                new ByteArrayInputStream(shortLivedJsonBody.getBytes(StandardCharsets.UTF_8))))
            .build();

        mockHttpClient.stubResponses(successResponse);

        // First call: succeeds and populates the CachedSupplier cache
        AwsCredentials firstResolve = syncProvider.resolveCredentials();
        assertEquals("new-akid", firstResolve.accessKeyId());

        // Now delete all token cache files from the temp directory to simulate token being removed
        Files.list(tempDir)
             .filter(p -> p.toString().endsWith(".json"))
             .forEach(p -> {
                 try {
                     Files.delete(p);
                 } catch (IOException e) {
                     throw new UncheckedIOException(e);
                 }
             });

        // Second call: the cached credentials expire in 30s, which is inside the mandatory refresh window, so this
        // call refreshes inline. The token cache file is gone, so the refresh fails with an InvalidLoginTokenException,
        // which is non-recoverable and propagates to the caller.
        SdkClientException e = assertThrows(SdkClientException.class,
                                            () -> syncProvider.resolveCredentials());
        assertTrue(e.getMessage().contains("not found"));
        assertTrue(e.getMessage().contains(tempDir.toString()), e.getMessage());
        syncProvider.close();
    }

    /**
     * Every client-side failure to use the cached token file is non-recoverable: it is raised before any Sign-In request,
     * and it is cached briefly, so an immediate retry rethrows the same error even after a valid file is restored.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokenFiles")
    public void resolveCredentials_whenTokenFileInvalidOnInitialFetch_throwsNonRecoverableAndCachesError(
        String description, String fileContents, String expectedFragment) throws IOException {
        writeTokenFile(fileContents);

        InvalidLoginTokenException first = assertThrows(InvalidLoginTokenException.class,
                                                        loginCredentialsProvider::resolveCredentials);
        assertActionableAndNoSecrets(first, expectedFragment);
        assertEquals(0, mockHttpClient.getRequests().size());

        // A valid, fresh token is restored, but the immediate retry is inside the error cache window.
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().plusSeconds(600))));
        InvalidLoginTokenException retry = assertThrows(InvalidLoginTokenException.class,
                                                        loginCredentialsProvider::resolveCredentials);
        assertSame(first, retry);
        assertEquals(0, mockHttpClient.getRequests().size());
    }

    /**
     * A non-recoverable token failure bypasses static stability: the cached credentials are not returned, no backoff is
     * applied, and no Sign-In request is made for the invalid file.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokenFiles")
    public void resolveCredentials_whenTokenFileInvalidAfterSuccessfulCache_throwsNonRecoverableAndBypassesStaticStability(
        String description, String fileContents, String expectedFragment) throws IOException {
        // An expired token on disk, so the first resolve refreshes from the Sign-In service.
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().minusSeconds(600))));
        // The refreshed credentials expire in 30 seconds, inside the 1 minute mandatory refresh window, so the next call
        // refreshes again.
        mockHttpClient.stubNextResponse(successfulRefreshResponse("new-akid", 30));

        assertEquals("new-akid", loginCredentialsProvider.resolveCredentials().accessKeyId());
        assertEquals(1, mockHttpClient.getRequests().size());

        writeTokenFile(fileContents);

        InvalidLoginTokenException first = assertThrows(InvalidLoginTokenException.class,
                                                        loginCredentialsProvider::resolveCredentials);
        assertActionableAndNoSecrets(first, expectedFragment);
        assertEquals(1, mockHttpClient.getRequests().size());

        // With static stability or a backoff, this retry would return "new-akid". Instead the cached error is rethrown.
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().plusSeconds(600))));
        InvalidLoginTokenException retry = assertThrows(InvalidLoginTokenException.class,
                                                        loginCredentialsProvider::resolveCredentials);
        assertSame(first, retry);
        assertEquals(1, mockHttpClient.getRequests().size());
    }

    /**
     * A failure to read a token file that exists may be transient, so it is recoverable: it is raised when no credentials
     * are cached, but it is not cached as a non-recoverable error, so the next call can succeed.
     */
    @Test
    public void resolveCredentials_whenTokenFileUnreadableOnInitialFetch_throwsRecoverableError() throws IOException {
        // A directory at the token location exists, but reading it fails with an IOException on every OS.
        Files.createDirectory(tokenFilePath());

        SdkClientException e = assertThrows(SdkClientException.class, loginCredentialsProvider::resolveCredentials);
        assertFalse(e instanceof InvalidLoginTokenException, e.getClass().getName());
        assertTrue(e.getMessage().contains("Unable to read the token cache file"), e.getMessage());
        assertTrue(e.getMessage().contains(tempDir.toString()), e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
        assertEquals(0, mockHttpClient.getRequests().size());

        // Once the file is readable again, the next call succeeds instead of rethrowing a cached error.
        Files.delete(tokenFilePath());
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().plusSeconds(600))));
        assertNotNull(loginCredentialsProvider.resolveCredentials());
        assertEquals(0, mockHttpClient.getRequests().size());
    }

    /**
     * A recoverable token read failure keeps static stability: the cached credentials are returned and no Sign-In request is
     * made.
     */
    @Test
    public void resolveCredentials_whenTokenFileUnreadableAfterSuccessfulCache_returnsCachedCredentials() throws IOException {
        // An expired token on disk, so the first resolve refreshes from the Sign-In service.
        tokenManager.storeToken(buildAccessToken(buildCredentials(Instant.now().minusSeconds(600))));
        // The refreshed credentials expire in 30 seconds, inside the 1 minute mandatory refresh window, so the next call
        // refreshes again.
        mockHttpClient.stubNextResponse(successfulRefreshResponse("new-akid", 30));

        assertEquals("new-akid", loginCredentialsProvider.resolveCredentials().accessKeyId());
        assertEquals(1, mockHttpClient.getRequests().size());

        // Replace the token file with a directory, so reading it fails with an IOException.
        Files.delete(tokenFilePath());
        Files.createDirectory(tokenFilePath());

        assertEquals("new-akid", loginCredentialsProvider.resolveCredentials().accessKeyId());
        assertEquals(1, mockHttpClient.getRequests().size());
    }

    @Test
    public void resolveCredentials_whenDpopKeyInvalid_preservesParseCause() throws IOException {
        writeTokenFile(tokenJsonWith("dpopKey", quoted(INVALID_DPOP_KEY)));

        InvalidLoginTokenException e = assertThrows(InvalidLoginTokenException.class,
                                                    loginCredentialsProvider::resolveCredentials);
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals(0, mockHttpClient.getRequests().size());
    }

    private static Stream<Arguments> invalidTokenFiles() {
        return Stream.concat(emptyRequiredMemberTokenFiles(), Stream.of(
            Arguments.of("missing accessToken", tokenJsonWithout("accessToken"),
                         "required member 'accessToken' not found"),
            Arguments.of("missing accessToken.accessKeyId", tokenJsonWithout("accessToken.accessKeyId"),
                         "required member 'accessToken.accessKeyId' not found"),
            Arguments.of("missing accessToken.secretAccessKey", tokenJsonWithout("accessToken.secretAccessKey"),
                         "required member 'accessToken.secretAccessKey' not found"),
            Arguments.of("missing accessToken.sessionToken", tokenJsonWithout("accessToken.sessionToken"),
                         "required member 'accessToken.sessionToken' not found"),
            Arguments.of("missing accessToken.expiresAt", tokenJsonWithout("accessToken.expiresAt"),
                         "required member 'accessToken.expiresAt' not found"),
            Arguments.of("missing accessToken.accountId", tokenJsonWithout("accessToken.accountId"),
                         "required member 'accessToken.accountId' not found"),
            Arguments.of("missing clientId", tokenJsonWithout("clientId"),
                         "required member 'clientId' not found"),
            Arguments.of("missing dpopKey", tokenJsonWithout("dpopKey"),
                         "required member 'dpopKey' not found"),
            Arguments.of("missing refreshToken", tokenJsonWithout("refreshToken"),
                         "required member 'refreshToken' not found"),
            Arguments.of("accessToken not an object", tokenJsonWith("accessToken", quoted("text")),
                         "required member 'accessToken.accessKeyId' not found"),
            Arguments.of("malformed JSON", "{\"accessToken\": {",
                         "the file is not valid JSON"),
            Arguments.of("empty file", "",
                         "the file does not contain a JSON object"),
            Arguments.of("JSON null", "null",
                         "the file does not contain a JSON object"),
            Arguments.of("JSON array", "[]",
                         "the file does not contain a JSON object"),
            Arguments.of("unparseable expiresAt", tokenJsonWith("accessToken.expiresAt", quoted("yesterday")),
                         "member 'accessToken.expiresAt' is not a valid ISO-8601 timestamp"),
            // The token is expired, so a refresh is attempted and the key is validated before the Sign-In call.
            Arguments.of("invalid dpopKey", tokenJsonWith("dpopKey", quoted(INVALID_DPOP_KEY)),
                         "member 'dpopKey' is not a valid EC private key")));
    }

    /**
     * A token file for each required text member set to an empty string, and to a whitespace-only string.
     */
    private static Stream<Arguments> emptyRequiredMemberTokenFiles() {
        List<String> requiredTextMembers = Arrays.asList("accessToken.accessKeyId", "accessToken.secretAccessKey",
                                                         "accessToken.sessionToken", "accessToken.expiresAt",
                                                         "accessToken.accountId", "clientId", "dpopKey", "refreshToken");
        // The whitespace-only value is a JSON string of a space, a tab, a newline, and a space.
        return requiredTextMembers.stream().flatMap(member -> Stream.of(
            Arguments.of("empty " + member, tokenJsonWith(member, quoted("")),
                         "member '" + member + "' is empty"),
            Arguments.of("whitespace-only " + member, tokenJsonWith(member, quoted(" \\t\\n ")),
                         "member '" + member + "' is empty")));
    }

    /**
     * The members of a valid, expired token file, keyed by member name (members of {@code accessToken} use a dotted name),
     * with raw JSON values.
     */
    private static Map<String, String> validTokenMembers() {
        Map<String, String> members = new LinkedHashMap<>();
        members.put("accessToken.accessKeyId", quoted("akid"));
        members.put("accessToken.secretAccessKey", quoted(SECRET_ACCESS_KEY));
        members.put("accessToken.sessionToken", quoted(SESSION_TOKEN));
        members.put("accessToken.expiresAt", quoted(Instant.now().minusSeconds(600).toString()));
        members.put("accessToken.accountId", quoted("123456789012"));
        members.put("clientId", quoted("client-123"));
        members.put("dpopKey", quoted(VALID_TEST_PEM.replace("\n", "\\n")));
        members.put("refreshToken", quoted(REFRESH_TOKEN));
        members.put("tokenType", quoted("aws_sigv4"));
        return members;
    }

    /**
     * A valid, expired token file without {@code member}. Omitting {@code accessToken} omits all of its members.
     */
    private static String tokenJsonWithout(String member) {
        Map<String, String> members = validTokenMembers();
        members.keySet().removeIf(name -> name.equals(member) || name.startsWith(member + "."));
        return toJson(members);
    }

    /**
     * A valid, expired token file with {@code member} set to {@code rawJsonValue}. Setting {@code accessToken} replaces all
     * of its members.
     */
    private static String tokenJsonWith(String member, String rawJsonValue) {
        Map<String, String> members = validTokenMembers();
        members.keySet().removeIf(name -> name.startsWith(member + "."));
        members.put(member, rawJsonValue);
        return toJson(members);
    }

    private static String toJson(Map<String, String> members) {
        Map<String, String> accessToken = new LinkedHashMap<>();
        Map<String, String> token = new LinkedHashMap<>();
        members.forEach((name, value) -> {
            if (name.startsWith("accessToken.")) {
                accessToken.put(name.substring("accessToken.".length()), value);
            } else {
                token.put(name, value);
            }
        });
        if (!accessToken.isEmpty()) {
            token.put("accessToken", toJsonObject(accessToken));
        }
        return toJsonObject(token);
    }

    private static String toJsonObject(Map<String, String> members) {
        return members.entrySet().stream()
                      .map(e -> quoted(e.getKey()) + ":" + e.getValue())
                      .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quoted(String value) {
        return "\"" + value + "\"";
    }

    private Path tokenFilePath() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(LOGIN_SESSION_ID.getBytes(StandardCharsets.UTF_8));
            String cacheKey = BinaryUtils.toHex(sha256.digest()).toLowerCase(Locale.ENGLISH);
            return tempDir.resolve(cacheKey + ".json");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to derive cache key", e);
        }
    }

    private void writeTokenFile(String contents) throws IOException {
        Files.write(tokenFilePath(), contents.getBytes(StandardCharsets.UTF_8));
    }

    private void assertActionableAndNoSecrets(SdkClientException e, String expectedFragment) {
        String message = e.getMessage();
        assertTrue(message.contains(expectedFragment), message);
        assertTrue(message.contains("You must re-authenticate"), message);
        assertTrue(message.contains(LOGIN_SESSION_ID), message);
        assertTrue(message.contains(tempDir.toString()), message);
        for (String secret : Arrays.asList(SECRET_ACCESS_KEY, SESSION_TOKEN, REFRESH_TOKEN, INVALID_DPOP_KEY, "PRIVATE KEY")) {
            assertFalse(message.contains(secret), message);
        }
    }

    private static void verifyResolvedCredentialsAreUpdated(AwsCredentials resolvedCredentials) {
        assertEquals("new-akid", resolvedCredentials.accessKeyId());
        assertEquals("new-skid", resolvedCredentials.secretAccessKey());
        assertInstanceOf(AwsSessionCredentials.class, resolvedCredentials);
        assertEquals("new-session-token", ((AwsSessionCredentials) resolvedCredentials).sessionToken());

        // assert that the expiration is close to expected, since its being added to the current time it won't be exact.
        Instant expectedExpirationTime = Instant.now().plusSeconds(600);
        Instant resolvedExpirationTime = resolvedCredentials.expirationTime().orElse(Instant.MIN);
        assertTrue(Math.abs(resolvedExpirationTime.toEpochMilli() - expectedExpirationTime.toEpochMilli()) < 1000);
    }

    private void verifyDpopHeader(SdkHttpRequest httpRequest) throws Exception {
        List<String> dpopHeader = httpRequest.headers().get("DPoP");
        assertNotNull(dpopHeader);
        assertEquals(1, dpopHeader.size());
        assertTrue(verifySignature(dpopHeader.get(0)));

        Map<String, Object> payload = getJwtPayloadFromEncodedDpopHeader(dpopHeader.get(0));
        assertEquals("POST", payload.get("htm"));
        assertEquals("https://custom-signin-endpoint.com/v1/token", payload.get("htu"));
    }


    private void verifyTokenCacheUpdated() {
        LoginAccessToken updatedToken = tokenManager.loadToken()
                                                    .orElseThrow(() -> new RuntimeException("Token not found after refresh"));
        assertEquals("new-akid", updatedToken.getAccessToken().accessKeyId());
        assertEquals("new-skid", updatedToken.getAccessToken().secretAccessKey());
        assertEquals("new-session-token", updatedToken.getAccessToken().sessionToken());
        assertEquals("new-refresh-token", updatedToken.getRefreshToken());
    }

    private void stubSuccessfulRefreshResponse() {
        mockHttpClient.stubNextResponse(successfulRefreshResponse("new-akid"));
    }

    private HttpExecuteResponse successfulRefreshResponse(String accessKeyId) {
        return successfulRefreshResponse(accessKeyId, 600);
    }

    private HttpExecuteResponse successfulRefreshResponse(String accessKeyId, int expiresInSeconds) {
        String jsonBody =
            "{\"accessToken\":"
            + "{\"accessKeyId\":\"" + accessKeyId + "\","
            + "\"secretAccessKey\":\"new-skid\","
            + "\"sessionToken\":\"new-session-token\"},"
            + "\"tokenType\":\"aws_sigv4\","
            + "\"expiresIn\":" + expiresInSeconds + ","
            + "\"refreshToken\":\"new-refresh-token\"}";

        return HttpExecuteResponse
            .builder()
            .response(SdkHttpResponse.builder().statusCode(200).build())
            .responseBody(AbortableInputStream.create(new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8))))
            .build();
    }

    private void stubAccessDeniedException(OAuth2ErrorCode errorCode) {
        String errorBody = "{\"error\":\"" + errorCode + "\",\"message\":\"The refresh token has expired.\"}";
        mockHttpClient.stubNextResponse(
            HttpExecuteResponse
                .builder()
                .response(
                    SdkHttpResponse
                        .builder()
                        .putHeader("X-Amzn-Errortype", "AccessDeniedException")
                        .statusCode(401)
                        .build())
                .responseBody(AbortableInputStream.create(new StringInputStream(errorBody)))
                .build()
        );
    }

    private AwsSessionCredentials buildCredentials(Instant expirationTime) {
        return AwsSessionCredentials.builder()
                             .accessKeyId("akid")
                             .secretAccessKey("skid")
                             .sessionToken("sessionToken")
                             .accountId("123456789012")
                             .expirationTime(expirationTime)
                             .build();
    }

    private LoginAccessToken buildAccessToken(AwsSessionCredentials credentials) {
        return LoginAccessToken.builder()
                               .accessToken(credentials)
                               .clientId("client-123")
                               .dpopKey(VALID_TEST_PEM)
                               .refreshToken("refresh-token")
                               .tokenType("aws_sigv4")
                               .identityToken("id-token")
                               .build();
    }

    private static class CaptureRequestInterceptor implements ExecutionInterceptor {

        private List<SdkHttpRequest> httpRequests = new ArrayList<>();
        private List<SdkRequest> requests = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.httpRequests.add(context.httpRequest());
            this.requests.add(context.request());
        }
    }
}
