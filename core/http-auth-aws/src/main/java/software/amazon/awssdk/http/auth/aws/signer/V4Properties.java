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

package software.amazon.awssdk.http.auth.aws.signer;

import java.time.Clock;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Validate;


/**
 * A class which contains "properties" relevant to SigV4. These properties can be derived {@link SignerProperty}'s on a
 * {@link SignRequest}.
 */
@SdkProtectedApi
@Immutable
public final class V4Properties {
    private final AwsCredentialsIdentity credentials;
    private final CredentialScope credentialScope;
    private final Clock signingClock;
    private final boolean doubleUrlEncode;
    private final boolean normalizePath;


    private V4Properties(BuilderImpl builder) {
        this.credentials = Validate.paramNotNull(builder.credentials, "Credentials");
        this.credentialScope = Validate.paramNotNull(builder.credentialScope, "CredentialScope");
        this.signingClock = Validate.paramNotNull(builder.signingClock, "SigningClock");
        this.doubleUrlEncode = builder.doubleUrlEncode;
        this.normalizePath = builder.normalizePath;
    }

    public AwsCredentialsIdentity getCredentials() {
        return credentials;
    }

    public CredentialScope getCredentialScope() {
        return credentialScope;
    }

    public Clock getSigningClock() {
        return signingClock;
    }

    public boolean shouldDoubleUrlEncode() {
        return doubleUrlEncode;
    }

    public boolean shouldNormalizePath() {
        return normalizePath;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder credentials(AwsCredentialsIdentity credentials);

        Builder credentialScope(CredentialScope credentialScope);

        Builder signingClock(Clock signingClock);

        Builder doubleUrlEncode(boolean doubleUrlEncode);

        Builder normalizePath(boolean normalizePath);

        V4Properties build();
    }

    private static class BuilderImpl implements Builder {
        private AwsCredentialsIdentity credentials;
        private CredentialScope credentialScope;
        private Clock signingClock;
        private boolean doubleUrlEncode;
        private boolean normalizePath;

        @Override
        public Builder credentials(AwsCredentialsIdentity credentials) {
            this.credentials = Validate.paramNotNull(credentials, "Credentials");
            return this;
        }

        @Override
        public Builder credentialScope(CredentialScope credentialScope) {
            this.credentialScope = Validate.paramNotNull(credentialScope, "CredentialScope");
            return this;
        }

        @Override
        public Builder signingClock(Clock signingClock) {
            this.signingClock = signingClock;
            return this;
        }

        @Override
        public Builder doubleUrlEncode(boolean doubleUrlEncode) {
            this.doubleUrlEncode = doubleUrlEncode;
            return this;
        }

        @Override
        public Builder normalizePath(boolean normalizePath) {
            this.normalizePath = normalizePath;
            return this;
        }

        @Override
        public V4Properties build() {
            return new V4Properties(this);
        }
    }
}
