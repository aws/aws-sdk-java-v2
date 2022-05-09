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

package software.amazon.awssdk.services.ssooidc.internal;

import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link TokenManager} that can load and store SSO tokens
 * from and to disk.
 */
@SdkInternalApi
public final class OnDiskTokenManager implements TokenManager<SsoOidcToken> {
    private static final Path DEFAULT_TOKEN_LOCATION = Paths.get(userHomeDirectory(), ".aws", "sso", "cache");

    private final JsonNodeParser jsonParser = JsonNodeParser.builder().removeErrorLocations(true).build();

    private final String sessionName;
    private final Path tokenLocation;

    private OnDiskTokenManager(Path cacheLocation, String sessionName) {
        Validate.notNull(cacheLocation, "cacheLocation must not be null");
        this.sessionName = Validate.notNull(sessionName, "sessionName must not be null");
        Validate.notBlank(sessionName, "sessionName must not be blank");
        String cacheKey = deriveCacheKey(sessionName);
        this.tokenLocation = cacheLocation.resolve(cacheKey + ".json");
    }

    @Override
    public Optional<SsoOidcToken> loadToken() {
        if (!Files.exists(tokenLocation)) {
            return Optional.empty();
        }

        try (InputStream cachedTokenStream = Files.newInputStream(tokenLocation)) {
            String content = IoUtils.toUtf8String(cachedTokenStream);
            return Optional.of(unmarshalToken(content));
        } catch (IOException e) {
            throw SdkClientException.create("Failed to load cached token at " + tokenLocation, e);
        }
    }

    @Override
    public void storeToken(SsoOidcToken token) {
        try (OutputStream os = Files.newOutputStream(tokenLocation)) {
            os.write(marshalToken(token));
        } catch (IOException e) {
            throw SdkClientException.create("Unable to write token to location " + tokenLocation, e);
        }
    }

    @Override
    public void close() {
    }

    public static OnDiskTokenManager create(Path cacheLocation, String sessionName) {
        return new OnDiskTokenManager(cacheLocation, sessionName);
    }

    public static OnDiskTokenManager create(String sessionName) {
        return create(DEFAULT_TOKEN_LOCATION, sessionName);
    }

    private SsoOidcToken unmarshalToken(String contents) {
        JsonNode node = jsonParser.parse(contents);
        SsoOidcToken.Builder tokenBuilder = SsoOidcToken.builder();

        JsonNode accessToken = node.field("accessToken")
                                   .orElseThrow(() -> SdkClientException.create("required member 'accessToken' not found"));
        tokenBuilder.accessToken(accessToken.text());

        JsonNode expiresAt = node.field("expiresAt")
                                 .orElseThrow(() -> SdkClientException.create("required member 'expiresAt' not found"));
        tokenBuilder.expiresAt(Instant.parse(expiresAt.text()));

        node.field("refreshToken").map(JsonNode::text).ifPresent(tokenBuilder::refreshToken);
        node.field("clientId").map(JsonNode::text).ifPresent(tokenBuilder::clientId);
        node.field("clientSecret").map(JsonNode::text).ifPresent(tokenBuilder::clientSecret);
        node.field("registrationExpiresAt")
            .map(JsonNode::text)
            .map(Instant::parse)
            .ifPresent(tokenBuilder::registrationExpiresAt);
        node.field("region").map(JsonNode::text).ifPresent(tokenBuilder::region);
        node.field("startUrl").map(JsonNode::text).ifPresent(tokenBuilder::startUrl);

        return tokenBuilder.build();
    }

    private byte[] marshalToken(SsoOidcToken token) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator generator = null;
        try {
            generator = JsonNodeParser.DEFAULT_JSON_FACTORY.createGenerator(baos);
            generator.writeStartObject();

            generator.writeStringField("accessToken", token.token());

            generator.writeStringField("expiresAt", DateTimeFormatter.ISO_INSTANT.format(token.expirationTime().get()));
            if (token.refreshToken() != null) {
                generator.writeStringField("refreshToken", token.refreshToken());

            }
            if (token.clientId() != null) {
                generator.writeStringField("clientId", token.clientId());
            }

            if (token.clientSecret() != null) {
                generator.writeStringField("clientSecret", token.clientSecret());
            }

            if (token.registrationExpiresAt() != null) {
                generator.writeStringField("registrationExpiresAt",
                                           DateTimeFormatter.ISO_INSTANT.format(token.registrationExpiresAt()));
            }

            if (token.region() != null) {
                generator.writeStringField("region", token.region());
            }

            if (token.startUrl() != null) {
                generator.writeStringField("startUrl", token.startUrl());
            }
            generator.writeEndObject();

            generator.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw SdkClientException.create("Unable to marshal token to JSON", e);
        } finally {
            if (generator != null) {
                IoUtils.closeQuietly(generator, null);
            }
        }
    }

    private static String deriveCacheKey(String sessionName) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("sha1");
            sha1.update(sessionName.getBytes(StandardCharsets.UTF_8));
            return BinaryUtils.toHex(sha1.digest()).toLowerCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException e) {
            throw SdkClientException.create("Unable to derive cache key", e);
        }
    }
}