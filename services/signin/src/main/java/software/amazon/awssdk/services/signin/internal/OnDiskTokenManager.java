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

package software.amazon.awssdk.services.signin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class OnDiskTokenManager implements AccessTokenManager {
    private static final Set<PosixFilePermission> OWNER_ONLY_PERMISSIONS =
        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final JsonNodeParser jsonParser = JsonNodeParser.builder().removeErrorLocations(true).build();

    private final Path tokenLocation;

    private OnDiskTokenManager(Path cacheLocation, String loginSession) {
        Validate.paramNotNull(cacheLocation, "cacheLocation");
        Validate.paramNotBlank(loginSession, "loginSession");
        String cacheKey = deriveCacheKey(loginSession);
        this.tokenLocation = cacheLocation.resolve(cacheKey + ".json");
    }

    private String deriveCacheKey(String loginSession) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
            sha1.update(loginSession.getBytes(StandardCharsets.UTF_8));
            return BinaryUtils.toHex(sha1.digest()).toLowerCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException e) {
            throw SdkClientException.create("Unable to derive cache key", e);
        }
    }

    public static OnDiskTokenManager create(Path cacheLocation, String loginSession) {
        return new OnDiskTokenManager(cacheLocation, loginSession);
    }

    @Override
    public Optional<LoginAccessToken> loadToken() {
        if (!Files.exists(tokenLocation)) {
            return Optional.empty();
        }

        try (InputStream cachedTokenStream = Files.newInputStream(tokenLocation)) {
            String content = IoUtils.toUtf8String(cachedTokenStream);
            return Optional.of(unmarshalToken(content));
        } catch (IOException | UncheckedIOException e) {
            throw SdkClientException.create("Failed to load cached token at " + tokenLocation, e);
        }
    }

    @Override
    public void storeToken(LoginAccessToken token) {
        // Write to a temp file first, then move to the destination to avoid partial reads.
        try {
            Path temp = createOwnerOnlyTempFile(tokenLocation.getParent(), "token-", ".tmp");
            try (OutputStream os = Files.newOutputStream(temp)) {
                os.write(marshalToken(token));
            }
            atomicOrFallbackMove(temp, tokenLocation);
        } catch (IOException | UncheckedIOException e) {
            throw SdkClientException.create("Unable to write token to location " + tokenLocation, e);
        }
    }

    /**
     * Creates a temp file with owner-only read/write permissions (0600) on POSIX-compatible file systems.
     * On non-POSIX file systems (e.g., Windows), falls back to default permissions.
     */
    private static Path createOwnerOnlyTempFile(Path dir, String prefix, String suffix) throws IOException {
        try {
            FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(OWNER_ONLY_PERMISSIONS);
            return Files.createTempFile(dir, prefix, suffix, attr);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            // File system does not support POSIX permissions (e.g., Windows, or in-memory file systems);
            // fall back to default permissions.
            return Files.createTempFile(dir, prefix, suffix);
        }
    }

    /**
     * Attempts an atomic move, falling back to a non-atomic replace if the file system does not support it.
     */
    private static void atomicOrFallbackMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void close() {

    }

    private LoginAccessToken unmarshalToken(String contents) {
        JsonNode node = jsonParser.parse(contents);
        LoginAccessToken.Builder tokenBuilder = LoginAccessToken.builder();

        JsonNode accessToken = node.field("accessToken")
                                   .orElseThrow(() -> SdkClientException.create("required member 'accessToken' not found"));
        AwsSessionCredentials awsSessionCredentials = unmarshalAccessTokenCredentials(accessToken);
        tokenBuilder.accessToken(awsSessionCredentials);

        String clientId = node.field("clientId").map(JsonNode::text)
                              .orElseThrow(() -> SdkClientException.create("required member 'clientId' not found"));
        tokenBuilder.clientId(clientId);

        String dpopKey = node.field("dpopKey").map(JsonNode::text)
                                 .orElseThrow(() -> SdkClientException.create("required member 'dpopKey' not found"));
        tokenBuilder.dpopKey(dpopKey);

        String refreshToken = node.field("refreshToken").map(JsonNode::text)
                                      .orElseThrow(() -> SdkClientException.create("required member 'refreshToken' not found"));
        tokenBuilder.refreshToken(refreshToken);

        // optional fields
        node.field("tokenType").map(JsonNode::text).ifPresent(tokenBuilder::tokenType);
        node.field("identityToken").map(JsonNode::text).ifPresent(tokenBuilder::identityToken);
        return tokenBuilder.build();
    }

    private static AwsSessionCredentials unmarshalAccessTokenCredentials(JsonNode accessToken) {
        AwsSessionCredentials.Builder awsSessionCredentials = AwsSessionCredentials.builder();
        String accessKeyId = accessToken.field("accessKeyId").map(JsonNode::text)
                                          .orElseThrow(() -> SdkClientException
                                              .create("required member 'accessKeyId' not found"));
        awsSessionCredentials.accessKeyId(accessKeyId);

        String secretAccessKey = accessToken.field("secretAccessKey").map(JsonNode::text)
                                              .orElseThrow(() -> SdkClientException
                                                  .create("required member 'secretAccessKey' not found"));
        awsSessionCredentials.secretAccessKey(secretAccessKey);

        String sessionToken = accessToken.field("sessionToken").map(JsonNode::text)
                                           .orElseThrow(() -> SdkClientException
                                               .create("required member 'sessionToken' not found"));
        awsSessionCredentials.sessionToken(sessionToken);

        String  expiresAt = accessToken.field("expiresAt").map(JsonNode::text)
                                        .orElseThrow(() -> SdkClientException
                                            .create("required member 'expiresAt' not found"));
        awsSessionCredentials.expirationTime(Instant.parse(expiresAt));

        String accountId = accessToken.field("accountId").map(JsonNode::text)
                                      .orElseThrow(() -> SdkClientException
                                          .create("required member 'accountId' not found"));
        awsSessionCredentials.accountId(accountId);
        return awsSessionCredentials.build();
    }

    private byte[] marshalToken(LoginAccessToken token) {
        try (JsonWriter jsonWriter = JsonWriter.create()) {
            jsonWriter.writeStartObject();

            jsonWriter.writeFieldName("accessToken");
            jsonWriter.writeStartObject();

            jsonWriter.writeFieldName("accessKeyId");
            jsonWriter.writeValue(token.getAccessToken().accessKeyId());

            jsonWriter.writeFieldName("secretAccessKey");
            jsonWriter.writeValue(token.getAccessToken().secretAccessKey());

            jsonWriter.writeFieldName("sessionToken");
            jsonWriter.writeValue(token.getAccessToken().sessionToken());

            jsonWriter.writeFieldName("accountId");
            jsonWriter.writeValue(
                token.getAccessToken().accountId()
                     .orElseThrow(() -> SdkClientException
                         .create("required member 'accountId' not found"))
            );

            jsonWriter.writeFieldName("expiresAt");
            jsonWriter.writeValue(
                DateTimeFormatter.ISO_INSTANT.format(
                    token.getAccessToken().expirationTime()
                         .orElseThrow(() -> SdkClientException
                             .create("required member 'expiresAt' not found"))
                )
            );

            jsonWriter.writeEndObject();

            if (token.getTokenType() != null) {
                jsonWriter.writeFieldName("tokenType");
                jsonWriter.writeValue(token.getTokenType());
            }

            if (token.getRefreshToken() != null) {
                jsonWriter.writeFieldName("refreshToken");
                jsonWriter.writeValue(token.getRefreshToken());
            }

            if (token.getIdentityToken() != null) {
                jsonWriter.writeFieldName("identityToken");
                jsonWriter.writeValue(token.getIdentityToken());
            }

            if (token.getClientId() != null) {
                jsonWriter.writeFieldName("clientId");
                jsonWriter.writeValue(token.getClientId());
            }

            if (token.getDpopKey() != null) {
                jsonWriter.writeFieldName("dpopKey");
                jsonWriter.writeValue(token.getDpopKey());
            }

            jsonWriter.writeEndObject();
            return jsonWriter.getBytes();
        }
    }
}