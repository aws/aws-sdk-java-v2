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

import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.crt.auth.signing.AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.sanitizeRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.hasChecksumHeader;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.isPayloadSigning;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.useChunkEncoding;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils.isAnonymous;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;

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
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of a {@link AwsV4aHttpSigner} that uses properties to compose v4a-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsCrtV4aHttpSigner implements AwsV4aHttpSigner {

    private static final int DEFAULT_CHUNK_SIZE_IN_BYTES = 128 * 1024;
    private static final Logger LOG = Logger.loggerFor(DefaultAwsCrtV4aHttpSigner.class);

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
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

    private static V4aProperties v4aProperties(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        Clock signingClock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(request.identity());
        RegionSet regionSet = request.requireProperty(REGION_SET);
        String serviceSigningName = request.requireProperty(SERVICE_SIGNING_NAME);
        CredentialScope credentialScope = new CredentialScope(regionSet.asString(), serviceSigningName, signingInstant);
        boolean doubleUrlEncode = request.requireProperty(DOUBLE_URL_ENCODE, true);
        boolean normalizePath = request.requireProperty(NORMALIZE_PATH, true);

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
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4aProperties v4aProperties) {

        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM);

        if (useChunkEncoding(isPayloadSigning, isChunkEncoding, isTrailing || isFlexible)) {
            return AwsChunkedV4aPayloadSigner.builder()
                                             .credentialScope(v4aProperties.getCredentialScope())
                                             .chunkSize(DEFAULT_CHUNK_SIZE_IN_BYTES)
                                             .checksumAlgorithm(request.property(CHECKSUM_ALGORITHM))
                                             .build();
        }

        return V4aPayloadSigner.create();
    }

    // TODO: this is a bit different from validateExpirationDuration in DefaultAwsV4HttpSigner
    // fix it
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
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4aProperties v4aProperties) {

        AuthLocation authLocation = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = request.property(EXPIRATION_DURATION);
        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM) && !hasChecksumHeader(request);

        AwsSigningConfig signingConfig = new AwsSigningConfig();
        signingConfig.setCredentials(toCredentials(v4aProperties.getCredentials()));
        signingConfig.setService(v4aProperties.getCredentialScope().getService());
        signingConfig.setRegion(v4aProperties.getCredentialScope().getRegion());
        signingConfig.setAlgorithm(SIGV4_ASYMMETRIC);
        signingConfig.setTime(v4aProperties.getCredentialScope().getInstant().toEpochMilli());
        signingConfig.setUseDoubleUriEncode(v4aProperties.shouldDoubleUrlEncode());
        signingConfig.setShouldNormalizeUriPath(v4aProperties.shouldNormalizePath());
        signingConfig.setSignedBodyHeader(X_AMZ_CONTENT_SHA256);

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
                signingConfig.setSignatureType(HTTP_REQUEST_VIA_QUERY_PARAMS);
                if (request.hasProperty(EXPIRATION_DURATION)) {
                    signingConfig.setExpirationInSeconds(validateExpirationDuration(expirationDuration).getSeconds());
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown auth-location: " + authLocation);
        }

        if (isPayloadSigning) {
            configurePayloadSigning(signingConfig, isChunkEncoding, isTrailing || isFlexible);
        } else {
            configureUnsignedPayload(signingConfig, isChunkEncoding, isTrailing || isFlexible);
        }

        return signingConfig;
    }

    private static void configureUnsignedPayload(AwsSigningConfig signingConfig, boolean isChunkEncoding,
                                                 boolean isTrailingOrFlexible) {
        if (isChunkEncoding && isTrailingOrFlexible) {
            signingConfig.setSignedBodyValue(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
        } else {
            signingConfig.setSignedBodyValue(UNSIGNED_PAYLOAD);
        }
    }

    private static void configurePayloadSigning(AwsSigningConfig signingConfig, boolean isChunkEncoding,
                                                boolean isTrailingOrFlexible) {

        if (isChunkEncoding) {
            if (isTrailingOrFlexible) {
                signingConfig.setSignedBodyValue(STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER);
            } else {
                signingConfig.setSignedBodyValue(STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
            }
        }
        // if it's non-streaming, crt should calcualte the SHA256 body-hash
    }

    private static SignedRequest doSign(SignRequest<? extends AwsCredentialsIdentity> request,
                                        AwsSigningConfig signingConfig,
                                        V4aPayloadSigner payloadSigner) {
        ContentStreamProvider contentStreamProvider = request.payload().orElse(null);
        if (isAnonymous(request.identity())) {
            return SignedRequest.builder()
                                .request(request.request())
                                .payload(contentStreamProvider)
                                .build();
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        payloadSigner.beforeSigning(requestBuilder, contentStreamProvider, signingConfig.getSignedBodyValue());

        SdkHttpRequest sdkHttpRequest = requestBuilder.build();
        HttpRequest crtRequest = toCrtRequest(sdkHttpRequest, contentStreamProvider);

        V4aRequestSigningResult requestSigningResult = sign(sdkHttpRequest, crtRequest, signingConfig);

        ContentStreamProvider payload = payloadSigner.sign(contentStreamProvider, requestSigningResult);

        return SignedRequest.builder()
                            .request(requestSigningResult.getSignedRequest().build())
                            .payload(payload)
                            .build();
    }

    private static HttpRequest toCrtRequest(SdkHttpRequest sdkHttpRequest, ContentStreamProvider contentStreamProvider) {
        SdkHttpRequest sanitizedRequest = sanitizeRequest(sdkHttpRequest);

        HttpRequest crtRequest = toRequest(sanitizedRequest, contentStreamProvider);
        return crtRequest;
    }

    private static V4aRequestSigningResult sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig) {
        AwsSigningResult signingResult = CompletableFutureUtils.joinLikeSync(AwsSigner.sign(crtRequest, signingConfig));
        return new V4aRequestSigningResult(
            toRequest(request, signingResult.getSignedRequest()).toBuilder(),
            signingResult.getSignature(),
            signingConfig);
    }
}
