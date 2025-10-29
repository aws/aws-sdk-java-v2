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

package software.amazon.awssdk.services.signin.auth.internal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.utils.BinaryUtils;

import static org.junit.jupiter.api.Assertions.*;

public class OnDiskTokenManagerTest {

    private static final String LOGIN_SESSION_ID = "loginSessionId";

    @TempDir
    Path tempDir;

    private LoginAccessToken token;
    private AwsSessionCredentials creds;

    @BeforeEach
    void setUp() {
        creds = AwsSessionCredentials.builder()
                                     .accessKeyId("akid")
                                     .secretAccessKey("skid")
                                     .sessionToken("sessionToken")
                                     .accountId("123456789012")
                                     .expirationTime(Instant.parse("2025-01-01T00:00:00Z"))
                                     .build();

        token = LoginAccessToken.builder()
                                .accessToken(creds)
                                .clientId("client-123")
                                .dpopKey("dpop-key")
                                .refreshToken("refresh-token")
                                .tokenType("aws_sigv4")
                                .identityToken("id-token")
                                .build();
    }

    @Test
    void create_nullCacheLocation_raisesException() {
        assertThrows(NullPointerException.class, () -> OnDiskTokenManager.create(null, LOGIN_SESSION_ID));
    }

    @Test
    void create_blankSession_raisesException() {
        assertThrows(IllegalArgumentException.class, () -> OnDiskTokenManager.create(tempDir, " "));
    }

    @Test
    void storeAndLoadValidToken_succeeds() {
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
        manager.storeToken(token);

        Optional<LoginAccessToken> loaded = manager.loadToken();
        assertTrue(loaded.isPresent());

        LoginAccessToken loadedToken = loaded.get();
        assertEquals("client-123", loadedToken.getClientId());
        assertEquals("dpop-key", loadedToken.getDpopKey());
        assertEquals("refresh-token", loadedToken.getRefreshToken());
        assertEquals("aws_sigv4", loadedToken.getTokenType());
        assertEquals("id-token", loadedToken.getIdentityToken());

        AwsSessionCredentials loadedCreds = loadedToken.getAccessToken();
        assertEquals("akid", loadedCreds.accessKeyId());
        assertEquals("skid", loadedCreds.secretAccessKey());
        assertEquals("sessionToken", loadedCreds.sessionToken());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), loadedCreds.expirationTime().get());
        assertEquals("123456789012", loadedCreds.accountId().get());
    }

    @Test
    void loadToken_whenFileMissing_returnsEmpty() {
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
        assertFalse(manager.loadToken().isPresent());
    }

    @Test
    void loadToken_whenCorruptJson_raisesException() throws IOException {
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
        Files.write(tokenLocation(LOGIN_SESSION_ID), "{not valid json}".getBytes(StandardCharsets.UTF_8));
        assertThrows(SdkClientException.class, manager::loadToken);
    }

    @Test
    void storeToken_whenIoFails_raisesException() {
        Path readOnlyDir = tempDir.resolve("readonly");
        try {
            Files.createDirectory(readOnlyDir);
            readOnlyDir.toFile().setReadOnly();
        } catch (IOException e) {
            fail("Unable to set up readonly dir");
        }

        OnDiskTokenManager manager = OnDiskTokenManager.create(readOnlyDir, LOGIN_SESSION_ID);
        assertThrows(SdkClientException.class, () -> manager.storeToken(token));
    }

    @Test
    void unmarshalToken_whenMissingRequiredFields_raisesException() throws IOException {
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);

        String jsonMissingClientId = "{ \"accessToken\": { " +
                                     "\"accessKeyId\": \"AKIA\", " +
                                     "\"secretAccessKey\": \"SECRET\", " +
                                     "\"sessionToken\": \"SESSION\", " +
                                     "\"expiresAt\": \"2025-01-01T00:00:00Z\", " +
                                     "\"accountId\": \"123\" " +
                                     "}}";

        Files.write(tokenLocation(LOGIN_SESSION_ID), jsonMissingClientId.getBytes(StandardCharsets.UTF_8));

        assertThrows(SdkClientException.class, manager::loadToken);
    }

    private Path tokenLocation(String loginSession) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("sha256");
            sha1.update(loginSession.getBytes(StandardCharsets.UTF_8));
            String cacheKey = BinaryUtils.toHex(sha1.digest()).toLowerCase(Locale.ENGLISH);
            return tempDir.resolve(cacheKey + ".json");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to derive cache key", e);
        }
    }
}
