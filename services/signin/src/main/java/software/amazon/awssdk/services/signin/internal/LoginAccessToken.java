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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;


/**
 * Represents a cached signin token:
 * accessToken - A JSON object containing the access token and its metadata.
 *     The accessKeyId, secretAccessKey, and sessionToken, returned from Sign-In.
 *     expiresAt - The expiration time of the accessToken as an RFC 3339 formatted timestamp.
 *     accountId - the 12-digit number that uniquely identifies an AWS account. Stored as a string.
 * tokenType - Returned by Sign-In, only expected to be aws_sigv4 initially.
 * identityToken - A JWT, containing info from Sign-In about which account/role/etc. the accessToken is for.
 * refreshToken - An opaque string returned by Sign-In.
 * clientId - The ARN of the client ID used when acquiring the token (arn:aws:signin:::devtools/same-device
 *    OR arn:aws:signin:::devtools/cross-device)
 * dpopKey - PEM file contents containing the base64-encoding of the ECPrivateKey format defined by
 *   RFC5915: Elliptic Curve Private Key Structure. It MUST include the public key coordinates.
 */
@SdkInternalApi
public class LoginAccessToken implements ToCopyableBuilder<LoginAccessToken.Builder, LoginAccessToken> {
    private final AwsSessionCredentials accessToken;
    private final String tokenType;
    private final String refreshToken;
    private final String identityToken;
    private final String clientId;
    private final String dpopKey;

    private LoginAccessToken(BuilderImpl builder) {
        this.accessToken = builder.accessToken;
        this.tokenType = builder.tokenType;
        this.refreshToken = builder.refreshToken;
        this.identityToken = builder.identityToken;
        this.clientId = builder.clientId;
        this.dpopKey = builder.dpopKey;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * AWS Credentials including expiration and accountID vended by Signin.
     */
    public AwsSessionCredentials getAccessToken() {
        return accessToken;
    }

    /**
     * The token type returned by Sign-In, this must match the access token type and must currently be `aws_sigv4`
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * An opaque string returned by Sign-In and used in refreshing the access token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     *  JWT, containing info from Sign-In about which account/role/etc. the accessToken is for.
     */
    public String getIdentityToken() {
        return identityToken;
    }

    /**
     * The ARN of the client ID used when acquiring the token (arn:aws:signin:::devtools/same-device
     * OR arn:aws:signin:::devtools/cross-device)
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * PEM file contents containing the base64-encoding of the ECPrivateKey format defined by
     * RFC5915: Elliptic Curve Private Key Structure. It MUST include the public key coordinates.
     */
    public String getDpopKey() {
        return dpopKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LoginAccessToken that = (LoginAccessToken) o;
        return Objects.equals(accessToken, that.accessToken) && Objects.equals(tokenType, that.tokenType)
               && Objects.equals(refreshToken, that.refreshToken) && Objects.equals(identityToken, that.identityToken)
               && Objects.equals(clientId, that.clientId) && Objects.equals(dpopKey, that.dpopKey);
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Objects.hashCode(accessToken);
        result = 31 * result + Objects.hashCode(tokenType);
        result = 31 * result + Objects.hashCode(refreshToken);
        result = 31 * result + Objects.hashCode(identityToken);
        result = 31 * result + Objects.hashCode(clientId);
        result = 31 * result + Objects.hashCode(dpopKey);
        return result;
    }

    @Override
    public String toString() {
        // DpopKey and refreshToken are sensitive and should not be included
        return "LoginAccessToken{" +
               "accessToken=" + accessToken +
               ", tokenType='" + tokenType + '\'' +
               ", identityToken='" + identityToken + '\'' +
               ", clientId='" + clientId + '\'' +
               '}';
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends CopyableBuilder<Builder, LoginAccessToken> {
        Builder accessToken(AwsSessionCredentials accessToken);

        Builder tokenType(String tokenType);

        Builder refreshToken(String refreshToken);

        Builder identityToken(String identityToken);

        Builder clientId(String clientId);

        Builder dpopKey(String dpopKey);

        LoginAccessToken build();
    }

    protected static class BuilderImpl implements Builder {
        private AwsSessionCredentials accessToken;
        private String tokenType;
        private String refreshToken;
        private String identityToken;
        private String clientId;
        private String dpopKey;

        private BuilderImpl() {

        }

        private BuilderImpl(LoginAccessToken loginAccessToken) {
            this.accessToken = loginAccessToken.accessToken.toBuilder().build();
            this.tokenType = loginAccessToken.tokenType;
            this.refreshToken = loginAccessToken.refreshToken;
            this.identityToken = loginAccessToken.identityToken;
            this.clientId = loginAccessToken.clientId;
            this.dpopKey = loginAccessToken.dpopKey;
        }

        @Override
        public Builder accessToken(AwsSessionCredentials accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        @Override
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        @Override
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;

        }

        @Override
        public Builder identityToken(String identityToken) {
            this.identityToken = identityToken;
            return this;
        }

        @Override
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public Builder dpopKey(String dpopKey) {
            this.dpopKey = dpopKey;
            return this;
        }

        @Override
        public LoginAccessToken build() {
            return new LoginAccessToken(this);
        }
    }
}