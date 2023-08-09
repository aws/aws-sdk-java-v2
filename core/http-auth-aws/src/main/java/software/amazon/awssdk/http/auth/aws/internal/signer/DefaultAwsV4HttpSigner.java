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

import static software.amazon.awssdk.http.auth.aws.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.UNSIGNED_PAYLOAD;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.V4PayloadSigner;
import software.amazon.awssdk.http.auth.aws.signer.V4Properties;
import software.amazon.awssdk.http.auth.aws.signer.V4RequestSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses properties to compose v4-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

    /**
     * Given a request with a set of properties and a base signer, compose an implementation with the base signer based on
     * properties, and delegate the request to the composed signer.
     */
    private static AwsV4HttpSigner getDelegate(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        String regionName = signRequest.requireProperty(AwsV4HttpSigner.REGION_NAME);
        String serviceSigningName = signRequest.requireProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
        Clock signingClock = signRequest.requireProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.systemUTC());
        boolean doubleUrlEncode = signRequest.requireProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, true);
        boolean normalizePath = signRequest.requireProperty(AwsV4HttpSigner.NORMALIZE_PATH, true);
        AuthLocation authLocation = signRequest.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = validateExpirationDuration(
            signRequest.requireProperty(EXPIRATION_DURATION, PRESIGN_URL_MAX_EXPIRATION_DURATION)
        );
        boolean isPayloadSigning = signRequest.requireProperty(PAYLOAD_SIGNING_ENABLED, true);

        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(signRequest.identity());
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);
        Checksummer checksummer = Checksummer.create();
        Function<V4Properties, V4RequestSigner> requestSigner = properties -> V4RequestSigner.create(null);
        V4PayloadSigner payloadSigner = V4PayloadSigner.create();

        if (authLocation == AuthLocation.HEADER) {
            requestSigner = V4RequestSigner::header;
            if (signRequest.hasProperty(EXPIRATION_DURATION)) {
                throw new UnsupportedOperationException(
                    EXPIRATION_DURATION + " is not supported for " + AuthLocation.HEADER + "."
                );
            }
        } else if (authLocation == AuthLocation.QUERY_STRING) {
            requestSigner = V4RequestSigner::query;
            if (signRequest.hasProperty(EXPIRATION_DURATION)) {
                requestSigner = properties -> V4RequestSigner.presigned(properties, expirationDuration);
            }
        }

        if (!isPayloadSigning) {
            checksummer = new PrecomputedChecksummer(() -> UNSIGNED_PAYLOAD);
        }

        V4Properties properties = new V4Properties(
            credentials,
            credentialScope,
            signingClock,
            doubleUrlEncode,
            normalizePath
        );

        return new V4HttpSigner(checksummer, requestSigner.apply(properties), payloadSigner);
    }

    private static Duration validateExpirationDuration(Duration expirationDuration) {
        if (expirationDuration.compareTo(PRESIGN_URL_MAX_EXPIRATION_DURATION) > 0) {
            throw new IllegalArgumentException(
                "Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                " days. The expiration duration set on the current request [" + expirationDuration + "]" +
                " has exceeded this limit."
            );
        }
        return expirationDuration;
    }


    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).sign(request);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        return getDelegate(request).signAsync(request);
    }
}
