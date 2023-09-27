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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils.sanitizeCredentials;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.OptionalDependencyLoaderUtil.getEventStreamV4PayloadSigner;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.PRESIGN_URL_MAX_EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_EVENTS_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An implementation of a {@link AwsV4HttpSigner} that uses properties to compose v4-signers in order to delegate signing of a
 * request and payload (if applicable) accordingly.
 */
@SdkInternalApi
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {

    private static final int DEFAULT_CHUNK_SIZE_IN_BYTES = 128 * 1024;

    private static V4Properties v4Properties(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        Clock signingClock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = sanitizeCredentials(request.identity());
        String regionName = request.requireProperty(AwsV4HttpSigner.REGION_NAME);
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

    private static Checksummer checksummer(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);
        boolean isTrailing = request.request().firstMatchingHeader(X_AMZ_TRAILER).isPresent();
        boolean isFlexible = request.hasProperty(CHECKSUM_ALGORITHM);

        if (isEventStreaming) {
            return Checksummer.forPrecomputed256Checksum(STREAMING_EVENTS_PAYLOAD);
        }

        if (isPayloadSigning) {
            if (isChunkEncoding) {
                if (isFlexible || isTrailing) {
                    return Checksummer.forPrecomputed256Checksum(STREAMING_SIGNED_PAYLOAD_TRAILER);
                }
                return Checksummer.forPrecomputed256Checksum(STREAMING_SIGNED_PAYLOAD);
            }

            if (request.hasProperty(CHECKSUM_ALGORITHM)) {
                return Checksummer.forFlexibleChecksum(request.property(CHECKSUM_ALGORITHM));
            }
            return Checksummer.create();
        }

        if (isChunkEncoding) {
            if (isFlexible || isTrailing) {
                return Checksummer.forPrecomputed256Checksum(STREAMING_UNSIGNED_PAYLOAD_TRAILER);
            }
            throw new UnsupportedOperationException("Chunk-Encoding without Payload-Signing must have a trailer!");
        }

        if (isFlexible) {
            return Checksummer.forFlexibleChecksum(UNSIGNED_PAYLOAD, request.property(CHECKSUM_ALGORITHM));
        }

        return Checksummer.forPrecomputed256Checksum(UNSIGNED_PAYLOAD);
    }

    private static V4PayloadSigner v4PayloadSigner(
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4Properties properties) {

        boolean isPayloadSigning = isPayloadSigning(request);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);

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

        if (isChunkEncoding) {
            return AwsChunkedV4PayloadSigner.builder()
                                            .credentialScope(properties.getCredentialScope())
                                            .chunkSize(DEFAULT_CHUNK_SIZE_IN_BYTES)
                                            .checksumAlgorithm(request.property(CHECKSUM_ALGORITHM))
                                            .build();
        }

        return V4PayloadSigner.create();
    }

    private static V4PayloadSigner v4PayloadAsyncSigner(
        BaseSignRequest<?, ? extends AwsCredentialsIdentity> request,
        V4Properties properties) {

        boolean isPayloadSigning = request.requireProperty(PAYLOAD_SIGNING_ENABLED, true);
        boolean isEventStreaming = isEventStreaming(request.request());
        boolean isChunkEncoding = request.requireProperty(CHUNK_ENCODING_ENABLED, false);

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

        if (isChunkEncoding && isPayloadSigning) {
            throw new UnsupportedOperationException("Chunked encoding and payload signing is not supported in async client. Use"
                                                    + " sync client instead");
        }

        return V4PayloadSigner.create();
    }

    private static SignedRequest doSign(SignRequest<? extends AwsCredentialsIdentity> request,
                                        Checksummer checksummer,
                                        V4RequestSigner requestSigner,
                                        V4PayloadSigner payloadSigner) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SignedRequest.builder()
                                .request(request.request())
                                .payload(request.payload().orElse(null))
                                .build();
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        checksummer.checksum(request.payload().orElse(null), requestBuilder);

        payloadSigner.beforeSigning(requestBuilder, request.payload().orElse(null));

        V4Context v4Context = requestSigner.sign(requestBuilder);

        ContentStreamProvider payload = payloadSigner.sign(request.payload().orElse(null), v4Context);

        return SignedRequest.builder()
                            .request(v4Context.getSignedRequest().build())
                            .payload(payload)
                            .build();
    }

    private static CompletableFuture<AsyncSignedRequest> doSign(AsyncSignRequest<? extends AwsCredentialsIdentity> request,
                                                                Checksummer checksummer,
                                                                V4RequestSigner requestSigner,
                                                                V4PayloadSigner payloadSigner) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return CompletableFuture.completedFuture(
                AsyncSignedRequest.builder()
                                  .request(request.request())
                                  .payload(request.payload().orElse(null))
                                  .build()
            );
        }

        SdkHttpRequest.Builder requestBuilder = request.request().toBuilder();

        CompletableFuture<V4Context> futureV4Context =
            checksummer.checksum(request.payload().orElse(null), requestBuilder)
                       .thenApply(__ -> requestSigner.sign(requestBuilder));

        return futureV4Context.thenApply(
            v4Context -> AsyncSignedRequest.builder()
                                           .request(v4Context.getSignedRequest().build())
                                           .payload(payloadSigner.signAsync(request.payload().orElse(null), v4Context))
                                           .build()
        );
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

    private static boolean isPayloadSigning(BaseSignRequest<?, ? extends AwsCredentialsIdentity> request) {
        return request.requireProperty(PAYLOAD_SIGNING_ENABLED, true) || !"https".equals(request.request().protocol());
    }

    private static boolean isEventStreaming(SdkHttpRequest request) {
        return "application/vnd.amazon.eventstream".equals(request.firstMatchingHeader(Header.CONTENT_TYPE).orElse(""));
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = checksummer(request);
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = v4PayloadSigner(request, v4Properties);

        return doSign(request, checksummer, v4RequestSigner, payloadSigner);
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        Checksummer checksummer = checksummer(request);
        V4Properties v4Properties = v4Properties(request);
        V4RequestSigner v4RequestSigner = v4RequestSigner(request, v4Properties);
        V4PayloadSigner payloadSigner = v4PayloadAsyncSigner(request, v4Properties);

        return doSign(request, checksummer, v4RequestSigner, payloadSigner);
    }
}
