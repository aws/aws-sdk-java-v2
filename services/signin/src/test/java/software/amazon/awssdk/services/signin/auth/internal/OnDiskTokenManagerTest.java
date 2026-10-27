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
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.services.signin.internal.InvalidLoginTokenException;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokenFiles")
    void loadToken_whenTokenFileInvalid_throwsInvalidLoginTokenException(String description, String contents,
                                                                         String expectedFragment) throws IOException {
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);
        Files.write(tokenLocation(LOGIN_SESSION_ID), contents.getBytes(StandardCharsets.UTF_8));

        InvalidLoginTokenException e = assertThrows(InvalidLoginTokenException.class, manager::loadToken);
        assertInvalidTokenMessage(e, expectedFragment);
    }

    @Test
    void loadToken_whenFileUnreadable_throwsInvalidLoginTokenException() throws IOException {
        // A directory at the token location exists, but reading it fails with an IOException on every OS.
        Files.createDirectory(tokenLocation(LOGIN_SESSION_ID));
        OnDiskTokenManager manager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);

        InvalidLoginTokenException e = assertThrows(InvalidLoginTokenException.class, manager::loadToken);
        assertInvalidTokenMessage(e, "the file could not be read");
        assertInstanceOf(IOException.class, e.getCause());
    }

    private static Stream<Arguments> invalidTokenFiles() {
        String jsonMissingClientId = "{ \"accessToken\": { " +
                                     "\"accessKeyId\": \"AKIA\", " +
                                     "\"secretAccessKey\": \"SECRET\", " +
                                     "\"sessionToken\": \"SESSION\", " +
                                     "\"expiresAt\": \"2025-01-01T00:00:00Z\", " +
                                     "\"accountId\": \"123\" " +
                                     "}}";
        String jsonUnparseableExpiresAt = "{ \"accessToken\": { " +
                                          "\"accessKeyId\": \"AKIA\", " +
                                          "\"secretAccessKey\": \"SECRET\", " +
                                          "\"sessionToken\": \"SESSION\", " +
                                          "\"expiresAt\": \"yesterday\", " +
                                          "\"accountId\": \"123\" " +
                                          "}, " +
                                          "\"clientId\": \"client-123\", " +
                                          "\"dpopKey\": \"dpop-key\", " +
                                          "\"refreshToken\": \"refresh-token\" " +
                                          "}";
        return Stream.concat(emptyRequiredMemberTokenFiles(), Stream.of(
            Arguments.of("corrupt JSON", "{not valid json}", "the file is not valid JSON"),
            Arguments.of("missing clientId", jsonMissingClientId, "required member 'clientId' not found"),
            Arguments.of("empty file", "", "the file does not contain a JSON object"),
            Arguments.of("whitespace only", "  \n", "the file does not contain a JSON object"),
            Arguments.of("JSON null", "null", "the file does not contain a JSON object"),
            Arguments.of("JSON array", "[]", "the file does not contain a JSON object"),
            Arguments.of("JSON string", "\"text\"", "the file does not contain a JSON object"),
            Arguments.of("unparseable expiresAt", jsonUnparseableExpiresAt,
                         "member 'accessToken.expiresAt' is not a valid ISO-8601 timestamp")));
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
            Arguments.of("empty " + member, tokenJsonWith(member, ""),
                         "member '" + member + "' is empty"),
            Arguments.of("whitespace-only " + member, tokenJsonWith(member, " \\t\\n "),
                         "member '" + member + "' is empty")));
    }

    /**
     * A valid token file with {@code member} (members of {@code accessToken} use a dotted name) set to the JSON string
     * {@code value}, which is inserted without escaping.
     */
    private static String tokenJsonWith(String member, String value) {
        Map<String, String> accessToken = new LinkedHashMap<>();
        accessToken.put("accessKeyId", "AKIA");
        accessToken.put("secretAccessKey", "SECRET");
        accessToken.put("sessionToken", "SESSION");
        accessToken.put("expiresAt", "2025-01-01T00:00:00Z");
        accessToken.put("accountId", "123");
        Map<String, String> token = new LinkedHashMap<>();
        token.put("clientId", "client-123");
        token.put("dpopKey", "dpop-key");
        token.put("refreshToken", "refresh-token");
        if (member.startsWith("accessToken.")) {
            accessToken.put(member.substring("accessToken.".length()), value);
        } else {
            token.put(member, value);
        }
        return "{\"accessToken\":{" + toJsonMembers(accessToken) + "}," + toJsonMembers(token) + "}";
    }

    private static String toJsonMembers(Map<String, String> members) {
        return members.entrySet().stream()
                      .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                      .collect(Collectors.joining(","));
    }

    private void assertInvalidTokenMessage(InvalidLoginTokenException e, String expectedFragment) {
        String message = e.getMessage();
        assertTrue(message.contains(expectedFragment), message);
        assertTrue(message.contains(tokenLocation(LOGIN_SESSION_ID).toString()), message);
        assertTrue(message.contains(LOGIN_SESSION_ID), message);
        assertTrue(message.contains("You must re-authenticate"), message);
    }

    @Test
    void storeToken_whenIoFails_raisesException() {
        Path readOnlyDir = tempDir.resolve("pathdoesnotexist");
        OnDiskTokenManager manager = OnDiskTokenManager.create(readOnlyDir, LOGIN_SESSION_ID);
        SdkClientException e = assertThrows(SdkClientException.class, () -> manager.storeToken(token));
    }

    private Path tokenLocation(String loginSession) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
            sha1.update(loginSession.getBytes(StandardCharsets.UTF_8));
            String cacheKey = BinaryUtils.toHex(sha1.digest()).toLowerCase(Locale.ENGLISH);
            return tempDir.resolve(cacheKey + ".json");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to derive cache key", e);
        }
    }
}
