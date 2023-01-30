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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class SsoAccessToken implements SdkToken {
    private final String accessToken;
    private final Instant expiresAt;

    private SsoAccessToken(BuilderImpl builder) {
        this.accessToken = Validate.paramNotNull(builder.accessToken, "accessToken");
        this.expiresAt = Validate.paramNotNull(builder.expiresAt, "expiresAt");
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String token() {
        return accessToken;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.of(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SsoAccessToken ssoAccessToken = (SsoAccessToken) o;

        return Objects.equals(accessToken, ssoAccessToken.accessToken)
               && Objects.equals(expiresAt, ssoAccessToken.expiresAt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(accessToken);
        result = 31 * result + Objects.hashCode(expiresAt);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("SsoAccessToken")
                       .add("accessToken", accessToken)
                       .add("expiresAt", expiresAt)
                       .build();
    }

    public interface Builder {
        Builder accessToken(String accessToken);

        Builder expiresAt(Instant expiresAt);

        SsoAccessToken build();
    }

    private static class BuilderImpl implements Builder {
        private String accessToken;
        private Instant expiresAt;

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
        public SsoAccessToken build() {
            return new SsoAccessToken(this);
        }
    }
}
