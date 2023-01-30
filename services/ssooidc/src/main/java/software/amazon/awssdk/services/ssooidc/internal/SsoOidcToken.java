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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a cached SSO token.
 *
 * <code>
 * {
 *     "accessToken": "string",
 *     "expiresAt": "2019-11-14T04:05:45Z",
 *     "refreshToken": "string",
 *     "clientId": "ABCDEFG323242423121312312312312312",
 *     "clientSecret": "ABCDE123",
 *     "registrationExpiresAt": "2022-03-06T19:53:17Z",
 *     "region": "us-west-2",
 *     "startUrl": "https://d-abc123.awsapps.com/start"
 * }
 * </code>
 */
@SdkInternalApi
public final class SsoOidcToken implements SdkToken {
    private final String accessToken;
    private final Instant expiresAt;
    private final String refreshToken;
    private final String clientId;
    private final String clientSecret;
    private final Instant registrationExpiresAt;
    private final String region;
    private final String startUrl;

    private SsoOidcToken(BuilderImpl builder) {
        Validate.paramNotNull(builder.accessToken, "accessToken");
        Validate.paramNotNull(builder.expiresAt, "expiresAt");
        this.accessToken = builder.accessToken;
        this.expiresAt = builder.expiresAt;
        this.refreshToken = builder.refreshToken;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.registrationExpiresAt = builder.registrationExpiresAt;
        this.region = builder.region;
        this.startUrl = builder.startUrl;
    }

    @Override
    public String token() {
        return accessToken;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.of(expiresAt);
    }

    public String refreshToken() {
        return refreshToken;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public Instant registrationExpiresAt() {
        return registrationExpiresAt;
    }

    public String region() {
        return region;
    }

    public String startUrl() {
        return startUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SsoOidcToken ssoOidcToken = (SsoOidcToken) o;

        return Objects.equals(accessToken, ssoOidcToken.accessToken)
            && Objects.equals(expiresAt, ssoOidcToken.expiresAt)
            && Objects.equals(refreshToken, ssoOidcToken.refreshToken)
            && Objects.equals(clientId, ssoOidcToken.clientId)
            && Objects.equals(clientSecret, ssoOidcToken.clientSecret)
            && Objects.equals(registrationExpiresAt, ssoOidcToken.registrationExpiresAt)
            && Objects.equals(region, ssoOidcToken.region)
            && Objects.equals(startUrl, ssoOidcToken.startUrl);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(accessToken);
        result = 31 * result + Objects.hashCode(expiresAt);
        result = 31 * result + Objects.hashCode(refreshToken);
        result = 31 * result + Objects.hashCode(clientId);
        result = 31 * result + Objects.hashCode(clientSecret);
        result = 31 * result + Objects.hashCode(registrationExpiresAt);
        result = 31 * result + Objects.hashCode(region);
        result = 31 * result + Objects.hashCode(startUrl);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("SsoOidcToken")
                       .add("accessToken", accessToken)
                       .add("expiresAt", expiresAt)
                       .add("refreshToken", refreshToken)
                       .add("clientId", clientId)
                       .add("clientSecret", clientSecret)
                       .add("registrationExpiresAt", registrationExpiresAt)
                       .add("region", region)
                       .add("startUrl", startUrl)
                       .build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder accessToken(String accessToken);

        Builder expiresAt(Instant expiresAt);

        Builder refreshToken(String refreshToken);

        Builder clientId(String clientId);

        Builder clientSecret(String clientSecret);

        Builder registrationExpiresAt(Instant registrationExpiresAt);

        Builder region(String region);

        Builder startUrl(String startUrl);

        SsoOidcToken build();
    }

    private static class BuilderImpl implements Builder {
        private String accessToken;
        private Instant expiresAt;
        private String refreshToken;
        private String clientId;
        private String clientSecret;
        private Instant registrationExpiresAt;
        private String region;
        private String startUrl;

        @Override
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        @Override
        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        @Override
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        @Override
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        @Override
        public Builder registrationExpiresAt(Instant registrationExpiresAt) {
            this.registrationExpiresAt = registrationExpiresAt;
            return this;
        }

        @Override
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        @Override
        public SsoOidcToken build() {
            return new SsoOidcToken(this);
        }
    }
}
