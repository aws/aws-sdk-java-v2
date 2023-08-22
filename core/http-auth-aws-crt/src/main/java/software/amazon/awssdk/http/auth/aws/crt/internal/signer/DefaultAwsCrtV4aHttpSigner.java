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

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.sanitizeRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * An implementation of a {@link AwsV4aHttpSigner} that uses properties to compose v4a-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsCrtV4aHttpSigner implements AwsV4aHttpSigner {

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

    private static SyncSignedRequest doSign(SyncSignRequest<? extends AwsCredentialsIdentity> request,
                                            AwsSigningConfig signingConfig,
                                            V4aPayloadSigner payloadSigner) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
        }

        SdkHttpRequest sanitizedRequest = sanitizeRequest(request.request());

        HttpRequest crtRequest = toRequest(sanitizedRequest, request.payload().orElse(null));

        V4aContext v4aContext = sign(sanitizedRequest, crtRequest, signingConfig);

        ContentStreamProvider payload = payloadSigner.sign(request.payload().orElse(null), v4aContext);

        return SyncSignedRequest.builder()
                                .request(v4aContext.getSignedRequest())
                                .payload(payload)
                                .build();
    }

    private static V4aContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig) {
        AwsSigningResult signingResult = CompletableFutureUtils.joinLikeSync(AwsSigner.sign(crtRequest, signingConfig));
        return new V4aContext(
            toRequest(request, signingResult.getSignedRequest())
        );
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        AwsSigningConfig signingConfig = signingConfig(request);
        V4aPayloadSigner payloadSigner = V4aPayloadSigner.create();
        return doSign(request, signingConfig, payloadSigner);
    }

    private AwsSigningConfig signingConfig(SignRequest<?, ? extends AwsCredentialsIdentity> request) {
        String regionName = request.requireProperty(REGION_NAME);
        String serviceSigningName = request.requireProperty(SERVICE_SIGNING_NAME);
        Clock signingClock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        boolean doubleUrlEncode = request.requireProperty(DOUBLE_URL_ENCODE, true);
        boolean normalizePath = request.requireProperty(NORMALIZE_PATH, true);
        AuthLocation authLocation = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = request.property(EXPIRATION_DURATION);
        boolean isPayloadSigning = request.requireProperty(PAYLOAD_SIGNING_ENABLED, true);

        AwsSigningConfig signingConfig = new AwsSigningConfig();
        signingConfig.setCredentials(toCredentials(request.identity()));
        signingConfig.setService(serviceSigningName);
        signingConfig.setRegion(regionName);
        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setTime(signingClock.instant().toEpochMilli());
        signingConfig.setUseDoubleUriEncode(doubleUrlEncode);
        signingConfig.setShouldNormalizeUriPath(normalizePath);

        switch (authLocation) {
            case HEADER:
                signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);
                if (request.hasProperty(EXPIRATION_DURATION)) {
                    throw new UnsupportedOperationException(
                        String.format("%s is not supported for %s.", EXPIRATION_DURATION, AuthLocation.HEADER)
                    );
                }
                break;
            case QUERY_STRING:
                signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS);
                if (request.hasProperty(EXPIRATION_DURATION)) {
                    signingConfig.setExpirationInSeconds(validateExpirationDuration(expirationDuration).getSeconds());
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown auth-location: " + authLocation);
        }

        if (!isPayloadSigning) {
            signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
        }

        return signingConfig;
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // There isn't currently a concept of async for crt signers
        throw new UnsupportedOperationException();
    }

}
