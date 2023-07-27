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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.spi.SignerProperty.validatedProperty;

import java.time.Clock;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4Properties;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An extension of {@link AwsV4Properties}, which provides access to more specific parameters
 * used by {@link AwsV4PresignedHttpSigner}.
 */
@SdkInternalApi
interface AwsV4PresignedProperties extends AwsV4Properties {

    static AwsV4PresignedProperties create(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        Duration expirationDuration = validatedProperty(signRequest, AwsV4HttpSigner.EXPIRATION_DURATION,
            PRESIGN_URL_MAX_EXPIRATION_DURATION
        );

        if (expirationDuration.compareTo(PRESIGN_URL_MAX_EXPIRATION_DURATION) > 0) {
            throw new IllegalArgumentException("Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                " days. The expiration duration set on the current request [" + expirationDuration + "]" +
                " has exceeded this limit."
            );
        }

        return new AwsV4PresignedHttpPropertiesImpl(
            expirationDuration,
            AwsV4Properties.create(signRequest)
        );
    }

    Duration getExpirationDuration();

    final class AwsV4PresignedHttpPropertiesImpl implements AwsV4PresignedProperties {
        private final Duration expirationDuration;
        private final AwsV4Properties v4Properties;

        private AwsV4PresignedHttpPropertiesImpl(Duration expirationDuration, AwsV4Properties v4HttpProperties) {
            this.expirationDuration = expirationDuration;
            this.v4Properties = v4HttpProperties;
        }

        @Override
        public Duration getExpirationDuration() {
            return expirationDuration;
        }

        @Override
        public AwsCredentialsIdentity getCredentials() {
            return v4Properties.getCredentials();
        }

        @Override
        public CredentialScope getCredentialScope() {
            return v4Properties.getCredentialScope();
        }

        @Override
        public Clock getSigningClock() {
            return v4Properties.getSigningClock();
        }

        @Override
        public boolean shouldDoubleUrlEncode() {
            return v4Properties.shouldDoubleUrlEncode();
        }

        @Override
        public boolean shouldNormalizePath() {
            return v4Properties.shouldNormalizePath();
        }
    }
}
