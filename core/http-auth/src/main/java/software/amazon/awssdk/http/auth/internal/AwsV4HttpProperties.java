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

package software.amazon.awssdk.http.auth.internal;

import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.CHECKSUM_HEADER_NAME;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.DOUBLE_URL_ENCODE;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.NORMALIZE_PATH;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.SIGNING_CLOCK;
import static software.amazon.awssdk.http.auth.internal.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.time.Clock;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


@SdkProtectedApi
public interface AwsV4HttpProperties {

    static AwsV4HttpProperties create(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        // required
        String regionName = validatedProperty(signRequest, REGION_NAME);
        String serviceSigningName = validatedProperty(signRequest, SERVICE_SIGNING_NAME);

        // optional
        Clock signingClock = validatedProperty(signRequest, SIGNING_CLOCK, Clock.systemUTC());
        String checksumHeader = validatedProperty(signRequest, CHECKSUM_HEADER_NAME, "");
        ChecksumAlgorithm checksumAlgorithm = validatedProperty(signRequest, CHECKSUM_ALGORITHM, null);
        boolean doubleUrlEncode = validatedProperty(signRequest, DOUBLE_URL_ENCODE, true);
        boolean normalizePath = validatedProperty(signRequest, NORMALIZE_PATH, true);

        // auxiliary
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(signRequest.identity());
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);

        return new AwsV4HttpPropertiesImpl(
            credentials,
            credentialScope,
            signingClock,
            checksumAlgorithm,
            checksumHeader,
            doubleUrlEncode,
            normalizePath
        );
    }

    AwsCredentialsIdentity getCredentials();

    CredentialScope getCredentialScope();

    Clock getSigningClock();

    ChecksumAlgorithm getChecksumAlgorithm();

    String getChecksumHeader();

    boolean shouldDoubleUrlEncode();

    boolean shouldNormalizePath();

    final class AwsV4HttpPropertiesImpl implements AwsV4HttpProperties {
        private final AwsCredentialsIdentity credentials;
        private final CredentialScope credentialScope;
        private final Clock signingClock;
        private final ChecksumAlgorithm checksumAlgorithm;
        private final String checksumHeader;
        private final boolean doubleUrlEncode;
        private final boolean normalizePath;


        private AwsV4HttpPropertiesImpl(AwsCredentialsIdentity credentials, CredentialScope credentialScope,
                                        Clock signingClock, ChecksumAlgorithm checksumAlgorithm, String checksumHeader,
                                        boolean doubleUrlEncode,
                                        boolean normalizePath) {
            this.credentials = credentials;
            this.credentialScope = credentialScope;
            this.signingClock = signingClock;
            this.checksumAlgorithm = checksumAlgorithm;
            this.checksumHeader = checksumHeader;
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
        public ChecksumAlgorithm getChecksumAlgorithm() {
            return checksumAlgorithm;
        }

        @Override
        public String getChecksumHeader() {
            return checksumHeader;
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
