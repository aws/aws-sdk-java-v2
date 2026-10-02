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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.sso.auth.ExpiredTokenException;
import software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Resolve the access token from the cached token file. If the token has expired then throw out an exception to ask the users to
 * update the token. This provider can also be replaced by any other implementation of resolving the access token. The users can
 * resolve the access token in their own way and add it to the {@link SsoCredentialsProvider.Builder#refreshRequest}.
 */
@SdkInternalApi
public final class SsoAccessTokenProvider implements SdkTokenProvider {
    private static final JsonNodeParser PARSER = JsonNodeParser.builder().removeErrorLocations(true).build();

    private final Path cachedTokenFilePath;

    public SsoAccessTokenProvider(Path cachedTokenFilePath) {
        this.cachedTokenFilePath = cachedTokenFilePath;
    }

    @Override
    public SdkToken resolveToken() {
        try {
            return tokenFromFile();
        } catch (ExpiredTokenException e) {
            throw e;
        } catch (Exception e) {
            // Any exception raised while trying to read the token file (invalid file, unable to access, does not exist, ect)
            // should be treated as an invalid/expired token and requires the user to re-authenticate
            throw ExpiredTokenException.builder()
                                       .cause(e)
                                       .build();
        }
    }

    private SdkToken tokenFromFile() {
        try (InputStream cachedTokenStream = Files.newInputStream(cachedTokenFilePath)) {
            return getTokenFromJson(IoUtils.toUtf8String(cachedTokenStream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SdkToken getTokenFromJson(String json) {
        JsonNode jsonNode = PARSER.parse(json);
        String expirationStr = jsonNode.field("expiresAt").map(JsonNode::text).orElse(null);

        if (expirationStr == null) {
            throw ExpiredTokenException.builder().build();
        }

        Instant expiration = Instant.parse(expirationStr);

        if (Instant.now().isAfter(expiration)) {
            throw ExpiredTokenException.builder().build();
        }

        return SsoAccessToken.builder()
                             .accessToken(jsonNode.asObject().get("accessToken").text())
                             .expiresAt(expiration).build();
    }


}
