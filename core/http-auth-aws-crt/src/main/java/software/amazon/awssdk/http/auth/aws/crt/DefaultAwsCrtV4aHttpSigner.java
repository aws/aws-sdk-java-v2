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
import static software.amazon.awssdk.http.auth.aws.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_TRAILER;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.CredentialScope;
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

    private static final int DEFAULT_CHUNK_SIZE_IN_BYTES = 128 * 1024;

    private static V4aProperties v4aProperties(SignRequest<?, ? extends AwsCredentialsIdentity> request) {
        Clock signingClock = request.requireProperty(AwsV4HttpSigner.SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(request.identity());
        String regionName = request.requireProperty(AwsV4HttpSigner.REGION_NAME);
        String serviceSigningName = request.requireProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);
        boolean doubleUrlEncode = request.requireProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, true);
        boolean normalizePath = request.requireProperty(AwsV4HttpSigner.NORMALIZE_PATH, true);

        return V4aProperties
            .builder()
            .credentials(credentials)
            .credentialScope(credentialScope)
            .signingClock(signingClock)
            .doubleUrlEncode(doubleUrlEncode)
            .normalizePath(normalizePath)
            .build();
    }

    private static V4aPayloadSigner v4aPayloadSigner(
        SignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4aProperties v4aProperties) {

        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);

        if (isChunkEncoding) {
            return new AwsChunkedV4aPayloadSigner(v4aProperties.getCredentialScope(), DEFAULT_CHUNK_SIZE_IN_BYTES);
        }

        return V4aPayloadSigner.create();
    }

    private static boolean hasTrailer(SdkHttpRequest request) {
        // TODO: Trailer would be determined by being a flexible-checksum enabled request, we will need to update
        // this once flexible checksums is enabled
        return request.firstMatchingHeader(X_AMZ_TRAILER).isPresent();
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

    private static AwsSigningConfig signingConfig(
        SignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4aProperties v4aProperties) {

        AuthLocation authLocation = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = request.property(EXPIRATION_DURATION);
        boolean isPayloadSigning = request.requireProperty(PAYLOAD_SIGNING_ENABLED, true);
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = hasTrailer(request.request());

        AwsSigningConfig signingConfig = new AwsSigningConfig();
        signingConfig.setCredentials(toCredentials(v4aProperties.getCredentials()));
        signingConfig.setService(v4aProperties.getCredentialScope().getService());
        signingConfig.setRegion(v4aProperties.getCredentialScope().getRegion());
        signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        signingConfig.setTime(v4aProperties.getCredentialScope().getInstant().toEpochMilli());
        signingConfig.setUseDoubleUriEncode(v4aProperties.shouldDoubleUrlEncode());
        signingConfig.setShouldNormalizeUriPath(v4aProperties.shouldNormalizePath());
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);

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
            if (isChunkEncoding) {
                if (isTrailing) {
                    signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.STREAMING_UNSIGNED_PAYLOAD_TRAILER);
                } else {
                    throw new UnsupportedOperationException("Chunk-Encoding without Payload-Signing must have a trailer!");
                }
            } else {
                signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
            }
        } else {
            if (isChunkEncoding) {
                if (isTrailing) {
                    signingConfig.setSignedBodyValue(
                        AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER
                    );
                } else {
                    signingConfig.setSignedBodyValue(
                        AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD
                    );
                }
            }
        }

        return signingConfig;
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
                                .request(v4aContext.getSignedRequest().build())
                                .payload(payload)
                                .build();
    }

    private static V4aContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig) {
        AwsSigningResult signingResult = CompletableFutureUtils.joinLikeSync(AwsSigner.sign(crtRequest, signingConfig));
        return new V4aContext(
            toRequest(request, signingResult.getSignedRequest()).toBuilder(),
            signingResult.getSignature(),
            signingConfig);
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        V4aProperties v4aProperties = v4aProperties(request);
        AwsSigningConfig signingConfig = signingConfig(request, v4aProperties);
        V4aPayloadSigner payloadSigner = v4aPayloadSigner(request, v4aProperties);
        return doSign(request, signingConfig, payloadSigner);
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // There isn't currently a concept of async for crt signers
        throw new UnsupportedOperationException();
    }
}
