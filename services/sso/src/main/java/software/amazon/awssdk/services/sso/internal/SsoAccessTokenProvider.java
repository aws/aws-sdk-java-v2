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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.services.sso.auth.ExpiredTokenException;
import software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Resolve the access token from the cached token file. If the token has expired then throw out an exception to ask the users to
 * update the token. This provider can also be replaced by any other implementation of resolving the access token. The users can
 * resolve the access token in their own way and add it to the {@link SsoCredentialsProvider.Builder#refreshRequest}.
 */
@SdkInternalApi
public final class SsoAccessTokenProvider {

    private Path cachedTokenFilePath;

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
        JsonNode jsonNode = JacksonUtils.sensitiveJsonNodeOf(json);

        if (validateToken(jsonNode.get("expiresAt").asText())) {
            throw ExpiredTokenException.builder().message("The SSO session associated with this profile has expired or is"
                                                          + " otherwise invalid. To refresh this SSO session run aws sso"
                                                          + " login with the corresponding profile.").build();
        }

        return jsonNode.get("accessToken").asText();
    }

    private boolean validateToken(String expirationTime) {
        // The input string is not valid ISO instant format.
        // Convert "2019-01-01T00:00:00UTC" to "2019-01-01T00:00:00Z"
        Instant expirationInstant = Instant.parse(expirationTime.replace("UTC", "Z"));
        return Instant.now().isAfter(expirationInstant.minus(15, MINUTES));
    }

}
