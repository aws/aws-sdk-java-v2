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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An implementation of a {@link AwsCrtV4aHttpSigner} that uses properties to compose v4a-signers in order to delegate signing of
 * a request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsCrtV4aHttpSigner implements AwsCrtV4aHttpSigner {

    /**
     * Given a request with a set of properties, determine which signer to delegate to, and call it with the request.
     */
    private static AwsCrtV4aHttpSigner getDelegate(
        SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        String regionName = signRequest.requireProperty(REGION_NAME);
        String serviceSigningName = signRequest.requireProperty(SERVICE_SIGNING_NAME);
        Clock signingClock = signRequest.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        boolean doubleUrlEncode = signRequest.requireProperty(DOUBLE_URL_ENCODE, true);
        boolean normalizePath = signRequest.requireProperty(NORMALIZE_PATH, true);
        AuthLocation authLocation = signRequest.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = validateExpirationDuration(
            signRequest.requireProperty(EXPIRATION_DURATION, PRESIGN_URL_MAX_EXPIRATION_DURATION)
        );
        boolean isPayloadSigning = signRequest.requireProperty(PAYLOAD_SIGNING_ENABLED, true);

        AwsSigningConfig signingConfig = new AwsSigningConfig();
        signingConfig.setCredentials(toCredentials(signRequest.identity()));
        signingConfig.setService(serviceSigningName);
        signingConfig.setRegion(regionName);
        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setTime(signingClock.instant().toEpochMilli());
        signingConfig.setUseDoubleUriEncode(doubleUrlEncode);
        signingConfig.setShouldNormalizeUriPath(normalizePath);

        if (authLocation == AuthLocation.HEADER) {
            signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
        } else {
            signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);
            if (signRequest.hasProperty(EXPIRATION_DURATION)) {
                signingConfig.setExpirationInSeconds(expirationDuration.getSeconds());
            }
        }

        if (!isPayloadSigning) {
            signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
        }

        V4aPayloadSigner payloadSigner = V4aPayloadSigner.create();

        return new CrtV4aHttpSigner(signingConfig, payloadSigner);
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
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        throw new UnsupportedOperationException();
    }

}
