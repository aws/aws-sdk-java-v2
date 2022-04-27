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

package software.amazon.awssdk.auth.credentials.internal;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.regions.util.HttpResourcesUtils;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Helper class that contains the common behavior of the CredentialsProviders that loads the credentials from a local endpoint on
 * a container (e.g. an EC2 instance).
 */
@SdkInternalApi
public final class HttpCredentialsLoader {
    private static final JsonNodeParser SENSITIVE_PARSER =
        JsonNodeParser.builder()
                      .removeErrorLocations(true)
                      .build();

    private static final Pattern TRAILING_ZERO_OFFSET_TIME_PATTERN = Pattern.compile("\\+0000$");

    private HttpCredentialsLoader() {
    }

    public static HttpCredentialsLoader create() {
        return new HttpCredentialsLoader();
    }

    public LoadedCredentials loadCredentials(ResourcesEndpointProvider endpoint) {
        try {
            String credentialsResponse = HttpResourcesUtils.instance().readResource(endpoint);

            Map<String, JsonNode> node = SENSITIVE_PARSER.parse(credentialsResponse).asObject();
            JsonNode accessKey = node.get("AccessKeyId");
            JsonNode secretKey = node.get("SecretAccessKey");
            JsonNode token = node.get("Token");
            JsonNode expiration = node.get("Expiration");

            Validate.notNull(accessKey, "Failed to load access key from metadata service.");
            Validate.notNull(secretKey, "Failed to load secret key from metadata service.");

            return new LoadedCredentials(accessKey.text(),
                                         secretKey.text(),
                                         token != null ? token.text() : null,
                                         expiration != null ? expiration.text() : null);
        } catch (SdkClientException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
            throw SdkClientException.builder()
                                    .message("Failed to load credentials from metadata service.")
                                    .cause(e)
                                    .build();
        }
    }

    public static final class LoadedCredentials {
        private final String accessKeyId;
        private final String secretKey;
        private final String token;
        private final Instant expiration;

        private LoadedCredentials(String accessKeyId, String secretKey, String token, String expiration) {
            this.accessKeyId = Validate.paramNotBlank(accessKeyId, "accessKeyId");
            this.secretKey = Validate.paramNotBlank(secretKey, "secretKey");
            this.token = token;
            this.expiration = expiration == null ? null : parseExpiration(expiration);
        }

        public AwsCredentials getAwsCredentials() {
            if (token == null) {
                return AwsBasicCredentials.create(accessKeyId, secretKey);
            } else {
                return AwsSessionCredentials.create(accessKeyId, secretKey, token);
            }
        }

        public Optional<Instant> getExpiration() {
            return Optional.ofNullable(expiration);
        }

        private static Instant parseExpiration(String expiration) {
            if (expiration == null) {
                return null;
            }

            // Convert the expirationNode string to ISO-8601 format.
            String expirationValue = TRAILING_ZERO_OFFSET_TIME_PATTERN.matcher(expiration).replaceAll("Z");

            try {
                return DateUtils.parseIso8601Date(expirationValue);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Unable to parse credentials expiration date from metadata service.", e);
            }
        }
    }
}
