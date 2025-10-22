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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.checksumHeaderName;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.AWS_CHUNKED;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.CONTENT_ENCODING;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.moveContentLength;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.AsyncChunkEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChecksumTrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedInputStream;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedPublisher;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.SigV4ChunkExtensionProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.SigV4TrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.SyncChunkEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.TrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ResettableContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of a V4PayloadSigner which chunk-encodes a payload, optionally adding a chunk-signature chunk-extension,
 * and/or trailers representing trailing headers with their signature at the end.
 */
@SdkInternalApi
public final class AwsChunkedV4PayloadSigner implements V4PayloadSigner {
    private static final Logger LOG = Logger.loggerFor(AwsChunkedV4PayloadSigner.class);

    private final CredentialScope credentialScope;
    private final int chunkSize;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final PayloadChecksumStore payloadChecksumStore;
    private final List<Pair<String, List<String>>> preExistingTrailers = new ArrayList<>();

    private AwsChunkedV4PayloadSigner(Builder builder) {
        this.credentialScope = Validate.paramNotNull(builder.credentialScope, "CredentialScope");
        this.chunkSize = Validate.isPositive(builder.chunkSize, "ChunkSize");
        this.checksumAlgorithm = builder.checksumAlgorithm;
        this.payloadChecksumStore = builder.checksumStore == null ? NoOpPayloadChecksumStore.create() :
                                    builder.checksumStore;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, V4RequestSigningResult requestSigningResult) {
        ChunkedEncodedInputStream.Builder chunkedEncodedInputStreamBuilder = ChunkedEncodedInputStream
            .builder()
            .inputStream(payload.newStream())
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.remaining()).getBytes(StandardCharsets.UTF_8));

        SyncChunkEncodedPayload chunkedPayload = new SyncChunkEncodedPayload(chunkedEncodedInputStreamBuilder);
        signCommon(chunkedPayload, requestSigningResult);

        return new ResettableContentStreamProvider(chunkedEncodedInputStreamBuilder::build);
    }

    @Override
    public Publisher<ByteBuffer> signAsync(Publisher<ByteBuffer> payload, V4RequestSigningResult requestSigningResult) {
        ChunkedEncodedPublisher.Builder chunkedStreamBuilder = ChunkedEncodedPublisher.builder()
                                                                                      .publisher(payload)
                                                                                      .chunkSize(chunkSize)
                                                                                      .addEmptyTrailingChunk(true);

        AsyncChunkEncodedPayload chunkedPayload = new AsyncChunkEncodedPayload(chunkedStreamBuilder);
        signCommon(chunkedPayload, requestSigningResult);

        return chunkedStreamBuilder.build();
    }

    private void signCommon(ChunkedEncodedPayload payload, V4RequestSigningResult requestSigningResult) {
        preExistingTrailers.forEach(t -> payload.addTrailer(() -> t));

        SdkHttpRequest.Builder request = requestSigningResult.getSignedRequest();

        payload.decodedContentLength(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)
                                            .map(Long::parseLong)
                                            .orElseThrow(() -> {
                                                String msg = String.format("Expected header '%s' to be present",
                                                                           X_AMZ_DECODED_CONTENT_LENGTH);
                                                return new RuntimeException(msg);
                                            }));

        String checksum = request.firstMatchingHeader(X_AMZ_CONTENT_SHA256).orElseThrow(
            () -> new IllegalArgumentException(X_AMZ_CONTENT_SHA256 + " must be set!")
        );

        switch (checksum) {
            case STREAMING_SIGNED_PAYLOAD: {
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSigningKey(),
                                                                requestSigningResult.getSignature());
                payload.addExtension(new SigV4ChunkExtensionProvider(rollingSigner, credentialScope));
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                setupChecksumTrailerIfNeeded(payload);
                break;
            case STREAMING_SIGNED_PAYLOAD_TRAILER: {
                setupChecksumTrailerIfNeeded(payload);
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSigningKey(),
                                                                requestSigningResult.getSignature());
                payload.addExtension(new SigV4ChunkExtensionProvider(rollingSigner, credentialScope));
                payload.addTrailer(
                    new SigV4TrailerProvider(payload.trailers(), rollingSigner, credentialScope)
                );
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void beforeSigning(SdkHttpRequest.Builder request, ContentStreamProvider payload) {
        long encodedContentLength = 0;
        long contentLength = SignerUtils.computeAndMoveContentLength(request, payload);
        setupPreExistingTrailers(request);

        // pre-existing trailers
        encodedContentLength = calculateEncodedContentLength(request, contentLength);

        if (checksumAlgorithm != null) {
            String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
            request.appendHeader(X_AMZ_TRAILER, checksumHeaderName);
        }
        request.putHeader(Header.CONTENT_LENGTH, Long.toString(encodedContentLength));
        request.appendHeader(CONTENT_ENCODING, AWS_CHUNKED);
    }

    @Override
    public CompletableFuture<Pair<SdkHttpRequest.Builder, Optional<Publisher<ByteBuffer>>>> beforeSigningAsync(
        SdkHttpRequest.Builder request, Publisher<ByteBuffer> payload) {
        return moveContentLength(request, payload)
            .thenApply(p -> {
                SdkHttpRequest.Builder requestBuilder = p.left();
                setupPreExistingTrailers(requestBuilder);

                long decodedContentLength = requestBuilder.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)
                                                          .map(Long::parseLong)
                                                          // should not happen, this header is added by moveContentLength
                                                          .orElseThrow(() -> new RuntimeException(X_AMZ_DECODED_CONTENT_LENGTH
                                                                                                  + " header not present"));

                long encodedContentLength = calculateEncodedContentLength(request, decodedContentLength);

                if (checksumAlgorithm != null) {
                    String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
                    request.appendHeader(X_AMZ_TRAILER, checksumHeaderName);
                }
                request.putHeader(Header.CONTENT_LENGTH, Long.toString(encodedContentLength));
                request.appendHeader(CONTENT_ENCODING, AWS_CHUNKED);
                return Pair.of(requestBuilder, p.right());
            });
    }

    private long calculateEncodedContentLength(SdkHttpRequest.Builder requestBuilder, long decodedContentLength) {
        long encodedContentLength = 0;

        encodedContentLength += calculateExistingTrailersLength();

        String checksum = requestBuilder.firstMatchingHeader(X_AMZ_CONTENT_SHA256).orElseThrow(
            () -> new IllegalArgumentException(X_AMZ_CONTENT_SHA256 + " must be set!")
        );

        switch (checksum) {
            case STREAMING_SIGNED_PAYLOAD: {
                long extensionsLength = 81; // ;chunk-signature:<sigv4 hex signature, 64 bytes>
                encodedContentLength += calculateChunksLength(decodedContentLength, extensionsLength);
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                if (checksumAlgorithm != null) {
                    encodedContentLength += calculateChecksumTrailerLength(checksumHeaderName(checksumAlgorithm));
                }
                encodedContentLength += calculateChunksLength(decodedContentLength, 0);
                break;
            case STREAMING_SIGNED_PAYLOAD_TRAILER: {
                long extensionsLength = 81; // ;chunk-signature:<sigv4 hex signature, 64 bytes>
                encodedContentLength += calculateChunksLength(decodedContentLength, extensionsLength);
                if (checksumAlgorithm != null) {
                    encodedContentLength += calculateChecksumTrailerLength(checksumHeaderName(checksumAlgorithm));
                }
                encodedContentLength += 90; // x-amz-trailer-signature:<sigv4 hex signature, 64 bytes>\r\n
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        // terminating \r\n
        encodedContentLength += 2;

        return encodedContentLength;
    }

    /**
     * Set up a map of pre-existing trailer (headers) for the given request to be used when chunk-encoding the payload.
     * <p>
     * However, we need to validate that these are valid trailers. Since aws-chunked encoding adds the checksum as a trailer, it
     * isn't part of the request headers, but other trailers MUST be present in the request-headers.
     */
    private void setupPreExistingTrailers(SdkHttpRequest.Builder request) {
        for (String header : request.matchingHeaders(X_AMZ_TRAILER)) {
            List<String> values = request.matchingHeaders(header);
            if (values.isEmpty()) {
                throw new IllegalArgumentException(header + " must be present in the request headers to be a valid trailer.");
            }
            preExistingTrailers.add(Pair.of(header, values));
            request.removeHeader(header);
        }
    }

    private long calculateChunksLength(long contentLength, long extensionsLength) {
        long lengthInBytes = 0;
        long chunkHeaderLength = Integer.toHexString(chunkSize).length();
        long numChunks = contentLength / chunkSize;

        // normal chunks
        // x<metadata>\r\n<data>\r\n
        lengthInBytes += numChunks * (chunkHeaderLength + extensionsLength + 2 + chunkSize + 2);

        // remaining chunk
        // x<metadata>\r\n<data>\r\n
        long remainingBytes = contentLength % chunkSize;
        if (remainingBytes > 0) {
            long remainingChunkHeaderLength = Long.toHexString(remainingBytes).length();
            lengthInBytes += remainingChunkHeaderLength + extensionsLength + 2 + remainingBytes + 2;
        }

        // final chunk
        // 0<metadata>\r\n
        lengthInBytes += 1 + extensionsLength + 2;

        return lengthInBytes;
    }

    private long calculateExistingTrailersLength() {
        long lengthInBytes = 0;

        for (Pair<String, List<String>> trailer : preExistingTrailers) {
            // size of trailer
            lengthInBytes += calculateTrailerLength(trailer);
        }

        return lengthInBytes;
    }

    private long calculateTrailerLength(Pair<String, List<String>> trailer) {
        // size of trailer-header and colon
        long lengthInBytes = trailer.left().length() + 1L;

        // size of trailer-values
        for (String value : trailer.right()) {
            lengthInBytes += value.length();
        }

        // size of commas between trailer-values, 1 less comma than # of values
        lengthInBytes += trailer.right().size() - 1;

        // terminating \r\n
        return lengthInBytes + 2;
    }

    private long calculateChecksumTrailerLength(String checksumHeaderName) {
        // size of checksum trailer-header and colon
        long lengthInBytes = checksumHeaderName.length() + 1L;

        // get the base checksum for the algorithm
        SdkChecksum sdkChecksum = fromChecksumAlgorithm(checksumAlgorithm);
        // size of checksum value as encoded-string
        lengthInBytes += BinaryUtils.toBase64(sdkChecksum.getChecksumBytes()).length();

        // terminating \r\n
        return lengthInBytes + 2;
    }

    private void setupChecksumTrailerIfNeeded(ChunkedEncodedPayload payload) {
        if (checksumAlgorithm == null) {
            return;
        }

        String checksumHeaderName = checksumHeaderName(checksumAlgorithm);

        String cachedChecksum = getCachedChecksum();

        if (cachedChecksum != null) {
            LOG.debug(() -> String.format("Cached payload checksum available for algorithm %s: %s. Using cached value",
                                          checksumAlgorithm.algorithmId(), checksumHeaderName));
            payload.addTrailer(() -> Pair.of(checksumHeaderName, Collections.singletonList(cachedChecksum)));
            return;
        }

        SdkChecksum sdkChecksum = fromChecksumAlgorithm(checksumAlgorithm);

        TrailerProvider checksumTrailer =
            new ChecksumTrailerProvider(sdkChecksum, checksumHeaderName, checksumAlgorithm, payloadChecksumStore);

        payload.checksumPayload(sdkChecksum);
        payload.addTrailer(checksumTrailer);
    }

    private String getCachedChecksum() {
        byte[] checksumBytes = payloadChecksumStore.getChecksumValue(checksumAlgorithm);
        if (checksumBytes != null) {
            return BinaryUtils.toBase64(checksumBytes);
        }
        return null;
    }

    static class Builder {
        private CredentialScope credentialScope;
        private Integer chunkSize;
        private ChecksumAlgorithm checksumAlgorithm;
        private PayloadChecksumStore checksumStore;

        public Builder credentialScope(CredentialScope credentialScope) {
            this.credentialScope = credentialScope;
            return this;
        }

        public Builder chunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder checksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
            return this;
        }

        public Builder checksumStore(PayloadChecksumStore checksumStore) {
            this.checksumStore = checksumStore;
            return this;
        }

        public AwsChunkedV4PayloadSigner build() {
            return new AwsChunkedV4PayloadSigner(this);
        }
    }
}
