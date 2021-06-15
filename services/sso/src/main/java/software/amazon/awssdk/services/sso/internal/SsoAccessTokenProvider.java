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

package software.amazon.awssdk.services.sso.internal;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.sso.auth.ExpiredTokenException;
import software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Resolve the access token from the cached token file. If the token has expired then throw out an exception to ask the users to
 * update the token. This provider can also be replaced by any other implementation of resolving the access token. The users can
 * resolve the access token in their own way and add it to the {@link SsoCredentialsProvider.Builder#refreshRequest}.
 */
@SdkInternalApi
public final class SsoAccessTokenProvider {
    private static final JsonNodeParser PARSER = JsonNodeParser.builder().removeErrorLocations(true).build();

    private final Path cachedTokenFilePath;

    public SsoAccessTokenProvider(Path cachedTokenFilePath) {
        this.cachedTokenFilePath = cachedTokenFilePath;
    }

    public String resolveAccessToken() {
        try (InputStream cachedTokenStream = Files.newInputStream(cachedTokenFilePath)) {
            return getTokenFromJson(IoUtils.toUtf8String(cachedTokenStream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getTokenFromJson(String json) {
        JsonNode jsonNode = PARSER.parse(json);
        String expiration = jsonNode.field("expiresAt").map(JsonNode::text).orElse(null);

        Validate.notNull(expiration,
                         "The SSO session's expiration time could not be determined. Please refresh your SSO session.");

        if (tokenIsInvalid(expiration)) {
            throw ExpiredTokenException.builder().message("The SSO session associated with this profile has expired or is"
                                                          + " otherwise invalid. To refresh this SSO session run aws sso"
                                                          + " login with the corresponding profile.").build();
        }

        return jsonNode.asObject().get("accessToken").text();
    }

    private boolean tokenIsInvalid(String expirationTime) {
        return Instant.now().isAfter(Instant.parse(expirationTime).minus(15, MINUTES));
    }

}
