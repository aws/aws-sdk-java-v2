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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.checksummer;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.hasChecksumHeader;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.isEventStreaming;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.isPayloadSigning;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.useChunkEncoding;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.OptionalDependencyLoaderUtil.getEventStreamV4PayloadSigner;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.http.auth.spi.signer.SdkInternalHttpSignerProperty.CHECKSUM_STORE;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses properties to compose v4-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    private static final int DEFAULT_CHUNK_SIZE_IN_BYTES = 128 * 1024;
    private static final Logger LOG = Logger.loggerFor(DefaultAwsV4HttpSigner.class);

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = checksummer(request, null, checksumStore(request));
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = v4PayloadSigner(request, v4Properties);

        return doSign(request, checksummer, v4RequestSigner, payloadSigner);
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = asyncChecksummer(request, checksumStore(request));
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = v4PayloadAsyncSigner(request, v4Properties);

        return doSignAsync(request, checksummer, v4RequestSigner, payloadSigner);
    }

    private static V4Properties v4Properties(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        Clock signingClock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(request.identity());
        String regionName = request.requireProperty(REGION_NAME);
        String serviceSigningName = request.requireProperty(SERVICE_SIGNING_NAME);
        CredentialScope credentialScope = new CredentialScope(regionName, serviceSigningName, signingInstant);
        boolean doubleUrlEncode = request.requireProperty(DOUBLE_URL_ENCODE, true);
        boolean normalizePath = request.requireProperty(NORMALIZE_PATH, true);

        return V4Properties.builder()
                           .credentials(credentials)
                           .credentialScope(credentialScope)
                           .signingClock(signingClock)
                           .doubleUrlEncode(doubleUrlEncode)
                           .normalizePath(normalizePath)
                           .build();
    }

    private static V4RequestSigner v4RequestSigner(
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4Properties v4Properties) {

        AuthLocation authLocation = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        Duration expirationDuration = request.property(EXPIRATION_DURATION);
        boolean isAnonymous = CredentialUtils.isAnonymous(request.identity());

        if (isAnonymous) {
            return V4RequestSigner.anonymous(v4Properties);
        }

        Function<V4Properties, V4RequestSigner> requestSigner;
        switch (authLocation) {
            case HEADER:
                if (expirationDuration != null) {
                    throw new UnsupportedOperationException(
                        String.format("%s is not supported for %s.", EXPIRATION_DURATION, AuthLocation.HEADER));
                }
                requestSigner = V4RequestSigner::header;
                break;
            case QUERY_STRING:
                requestSigner = expirationDuration == null ? V4RequestSigner::query :
                                properties -> V4RequestSigner.presigned(properties,
                                                                        validateExpirationDuration(expirationDuration));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported authLocation " + authLocation);
        }

        return requestSigner.apply(v4Properties);
    }

    // TODO: remove this once we consolidate the behavior for plaintext HTTP signing for sync and async
    private static Checksummer asyncChecksummer(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
                                                PayloadChecksumStore checksumStore) {
        boolean shouldTreatAsUnsigned = asyncShouldTreatAsUnsigned(request);

        // set the override to false if it should be treated as unsigned, otherwise, null should be passed so that the normal
        // check for payload signing is done.
        Boolean overridePayloadSigning = shouldTreatAsUnsigned ? false : null;

        return checksummer(request, overridePayloadSigning, checksumStore);
    }

    // TODO: remove this once we consolidate the behavior for plaintext HTTP signing for sync and async
    private static boolean asyncShouldTreatAsUnsigned(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        boolean isHttp = !"https".equals(request.request().protocol());
        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);

        return isHttp && isPayloadSigning && isChunkEncoding;
    }

    private static V4PayloadSigner v4PayloadSigner(
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request, V4Properties properties) {

        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM) && !hasChecksumHeader(request);

        if (isEventStreaming) {
            if (isPayloadSigning) {
                return getEventStreamV4PayloadSigner(
                    properties.getCredentials(),
                    properties.getCredentialScope(),
                    properties.getSigningClock()
                );
            }
            throw new UnsupportedOperationException("Unsigned payload is not supported with event-streaming.");
        }

        if (useChunkEncoding(isPayloadSigning, isChunkEncoding, isTrailing || isFlexible)) {
            return AwsChunkedV4PayloadSigner.builder()
                                            .credentialScope(properties.getCredentialScope())
                                            .chunkSize(DEFAULT_CHUNK_SIZE_IN_BYTES)
                                            .checksumStore(checksumStore(request))
                                            .checksumAlgorithm(request.property(CHECKSUM_ALGORITHM))
                                            .build();
        }

        return V4PayloadSigner.create();
    }

    // TODO: remove this once we consolidate the behavior for plaintext HTTP signing for sync and async
    private static V4PayloadSigner v4PayloadAsyncSigner(
        AsyncSignRequest<? extends AwsCredentialsIdentity> request,
        V4Properties properties) {

        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM) && !hasChecksumHeader(request);

        if (isEventStreaming) {
            if (isPayloadSigning) {
                return getEventStreamV4PayloadSigner(
                    properties.getCredentials(),
                    properties.getCredentialScope(),
                    properties.getSigningClock()
                );
            }
            throw new UnsupportedOperationException("Unsigned payload is not supported with event-streaming.");
        }

        // Note: this check is done after we check if the request is eventstreaming, during which we just use the same logic
        // as sync to determine if the body should be signed. If it's not eventstreaming, then async needs to treat this
        // request differently to maintain current behavior re: plain HTTP requests.
        boolean nonEvenstreamingPayloadSigning = isPayloadSigning;
        if (asyncShouldTreatAsUnsigned(request)) {
            nonEvenstreamingPayloadSigning = false;
        }

        if (useChunkEncoding(nonEvenstreamingPayloadSigning, isChunkEncoding, isTrailing || isFlexible)) {
            return AwsChunkedV4PayloadSigner.builder()
                                            .credentialScope(properties.getCredentialScope())
                                            .chunkSize(DEFAULT_CHUNK_SIZE_IN_BYTES)
                                            .checksumStore(checksumStore(request))
                                            .checksumAlgorithm(request.property(CHECKSUM_ALGORITHM))
                                            .build();
        }

        return V4PayloadSigner.create();
    }

    private static SignedRequest doSign(SignRequest<? extends AwsCredentialsIdentity> request,
                                        Checksummer checksummer,
                                        V4RequestSigner requestSigner,
                                        V4PayloadSigner payloadSigner) {

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();
        ContentStreamProvider requestPayload = request.payload().orElse(null);

        checksummer.checksum(requestPayload, requestBuilder);

        payloadSigner.beforeSigning(requestBuilder, requestPayload);

        V4RequestSigningResult requestSigningResult = requestSigner.sign(requestBuilder);

        ContentStreamProvider signedPayload = null;
        if (requestPayload != null) {
            signedPayload = payloadSigner.sign(requestPayload, requestSigningResult);
        }
        return SignedRequest.builder()
                            .request(requestSigningResult.getSignedRequest().build())
                            .payload(signedPayload)
                            .build();
    }

    private static CompletableFuture<AsyncSignedRequest> doSignAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request,
                                                                     Checksummer checksummer,
                                                                     V4RequestSigner requestSigner,
                                                                     V4PayloadSigner payloadSigner) {

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();
        Publisher<ByteBuffer> requestPayload = request.payload().orElse(null);

        return checksummer.checksum(requestPayload, requestBuilder)
                          .thenCompose(checksummedPayload ->
                                           payloadSigner.beforeSigningAsync(requestBuilder, checksummedPayload))
                          .thenApply(p -> {
                              SdkHttpRequest.Builder requestToSign = p.left();
                              Publisher<ByteBuffer> payloadToSign = p.right().orElse(null);

                              V4RequestSigningResult requestSigningResult = requestSigner.sign(requestToSign);

                              Publisher<ByteBuffer> signedPayload = null;
                              if (payloadToSign != null) {
                                  signedPayload = payloadSigner.signAsync(payloadToSign, requestSigningResult);
                              }
                              return AsyncSignedRequest.builder()
                                                       .request(requestSigningResult.getSignedRequest().build())
                                                       .payload(signedPayload)
                                                       .build();
                          });
    }

    private static Duration validateExpirationDuration(Duration expirationDuration) {
        if (!isBetweenInclusive(Duration.ofSeconds(1), expirationDuration, PRESIGN_URL_MAX_EXPIRATION_DURATION)) {
            throw new IllegalArgumentException(
                "Requests that are pre-signed by SigV4 algorithm are valid for at least 1 second and at most 7" +
                " days. The expiration duration set on the current request [" + expirationDuration + "]" +
                " does not meet these bounds."
            );
        }
        return expirationDuration;
    }

    private static boolean isBetweenInclusive(Duration start, Duration x, Duration end) {
        return start.compareTo(x) <= 0 && x.compareTo(end) <= 0;
    }

    private static PayloadChecksumStore checksumStore(BaseSignRequest<?, ?> request) {
        PayloadChecksumStore cache = request.property(CHECKSUM_STORE);
        if (cache == null) {
            return NoOpPayloadChecksumStore.create();
        }
        return cache;
    }
}
