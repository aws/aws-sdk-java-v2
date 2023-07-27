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

import static software.amazon.awssdk.http.auth.aws.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.spi.SignerProperty.validatedProperty;

import java.time.Clock;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


/**
 * An interface which contains "properties" used by a v4-signer at various steps.
 * These properties are derived from the {@link SignerProperty}'s on a {@link SignRequest}.
 */
@SdkProtectedApi
public interface AwsV4Properties {

    static AwsV4Properties create(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        // required
        String regionName = validatedProperty(signRequest, AwsV4HttpSigner.REGION_NAME);
        String serviceSigningName = validatedProperty(signRequest, AwsV4HttpSigner.SERVICE_SIGNING_NAME);

        // optional
        Clock signingClock = validatedProperty(signRequest, AwsV4HttpSigner.SIGNING_CLOCK, Clock.systemUTC());
        boolean doubleUrlEncode = validatedProperty(signRequest, AwsV4HttpSigner.DOUBLE_URL_ENCODE, true);
        boolean normalizePath = validatedProperty(signRequest, AwsV4HttpSigner.NORMALIZE_PATH, true);

        // auxiliary
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(signRequest.identity());
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);

        return new AwsV4HttpPropertiesImpl(
            credentials,
            credentialScope,
            signingClock,
            doubleUrlEncode,
            normalizePath
        );
    }

    AwsCredentialsIdentity getCredentials();

    CredentialScope getCredentialScope();

    Clock getSigningClock();

    boolean shouldDoubleUrlEncode();

    boolean shouldNormalizePath();

    final class AwsV4HttpPropertiesImpl implements AwsV4Properties {
        private final AwsCredentialsIdentity credentials;
        private final CredentialScope credentialScope;
        private final Clock signingClock;
        private final boolean doubleUrlEncode;
        private final boolean normalizePath;


        private AwsV4HttpPropertiesImpl(AwsCredentialsIdentity credentials, CredentialScope credentialScope,
                                        Clock signingClock, boolean doubleUrlEncode, boolean normalizePath) {
            this.credentials = credentials;
            this.credentialScope = credentialScope;
            this.signingClock = signingClock;
            this.doubleUrlEncode = doubleUrlEncode;
            this.normalizePath = normalizePath;
        }

        @Override
        public AwsCredentialsIdentity getCredentials() {
            return credentials;
        }

        @Override
        public CredentialScope getCredentialScope() {
            return credentialScope;
        }

        @Override
        public Clock getSigningClock() {
            return signingClock;
        }

        @Override
        public boolean shouldDoubleUrlEncode() {
            return doubleUrlEncode;
        }

        @Override
        public boolean shouldNormalizePath() {
            return normalizePath;
        }
    }
}
