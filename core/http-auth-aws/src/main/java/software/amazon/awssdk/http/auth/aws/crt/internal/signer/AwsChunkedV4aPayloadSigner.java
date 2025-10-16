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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.checksumHeaderName;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.computeAndMoveContentLength;

import java.io.InputStream;
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
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.NoOpPayloadChecksumStore;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.AsyncChunkEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChecksumTrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedInputStream;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedPublisher;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.SyncChunkEncodedPayload;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.TrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ResettableContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of a V4aPayloadSigner which chunk-encodes a payload, optionally adding a chunk-signature chunk-extension,
 * and/or trailers representing trailing headers with their signature at the end.
 */
@SdkInternalApi
public final class AwsChunkedV4aPayloadSigner implements V4aPayloadSigner {
    private static final Logger LOG = Logger.loggerFor(AwsChunkedV4aPayloadSigner.class);
    // ;chunk-signature:<sigv4a-ecsda hex signature, 144 bytes>
    private static final int CHUNK_SIGNATURE_EXTENSION_LENGTH = 161;

    private final CredentialScope credentialScope;
    private final int chunkSize;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final PayloadChecksumStore payloadChecksumStore;
    private final List<Pair<String, List<String>>> preExistingTrailers = new ArrayList<>();

    private AwsChunkedV4aPayloadSigner(Builder builder) {
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
    public ContentStreamProvider sign(ContentStreamProvider payload, V4aRequestSigningResult requestSigningResult) {
        InputStream inputStream = payload != null ? payload.newStream() : new StringInputStream("");
        ChunkedEncodedInputStream.Builder chunkedEncodedInputStreamBuilder = ChunkedEncodedInputStream
            .builder()
            .inputStream(inputStream)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.remaining()).getBytes(StandardCharsets.UTF_8));

        SyncChunkEncodedPayload chunkedPayload = new SyncChunkEncodedPayload(chunkedEncodedInputStreamBuilder);

        signCommon(chunkedPayload, requestSigningResult);

        return new ResettableContentStreamProvider(chunkedEncodedInputStreamBuilder::build);
    }

    /**
     * Given a payload and result of request signing, sign the payload via the SigV4 process.
     */
    @Override
    public Publisher<ByteBuffer> signAsync(Publisher<ByteBuffer> payload, V4aRequestSigningResult requestSigningResult) {
        ChunkedEncodedPublisher.Builder chunkedStreamBuilder = ChunkedEncodedPublisher.builder()
                                                                                      .publisher(payload)
                                                                                      .chunkSize(chunkSize)
                                                                                      .addEmptyTrailingChunk(true);
        AsyncChunkEncodedPayload chunkedPayload = new AsyncChunkEncodedPayload(chunkedStreamBuilder);

        signCommon(chunkedPayload, requestSigningResult);

        return chunkedStreamBuilder.build();
    }

    private ChunkedEncodedPayload signCommon(ChunkedEncodedPayload payload, V4aRequestSigningResult requestSigningResult) {
        SdkHttpRequest.Builder request = requestSigningResult.getSignedRequest();

        payload.decodedContentLength(request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)
                                            .map(Long::parseLong)
                                            .orElseThrow(() -> {
                                                String msg = String.format("Expected header '%s' to be present",
                                                                           X_AMZ_DECODED_CONTENT_LENGTH);
                                                return new RuntimeException(msg);
                                            }));

        preExistingTrailers.forEach(trailer -> payload.addTrailer(() -> trailer));

        switch (requestSigningResult.getSigningConfig().getSignedBodyValue()) {
            case STREAMING_ECDSA_SIGNED_PAYLOAD: {
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSignature(),
                                                                requestSigningResult.getSigningConfig());
                payload.addExtension(new SigV4aChunkExtensionProvider(rollingSigner, credentialScope));
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                setupChecksumTrailerIfNeeded(payload);
                break;
            case STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER: {
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSignature(),
                                                                requestSigningResult.getSigningConfig());
                payload.addExtension(new SigV4aChunkExtensionProvider(rollingSigner, credentialScope));
                setupChecksumTrailerIfNeeded(payload);
                payload.addTrailer(
                    new SigV4aTrailerProvider(payload.trailers(), rollingSigner, credentialScope)
                );
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        return payload;
    }

    @Override
    public void beforeSigning(SdkHttpRequest.Builder request, ContentStreamProvider payload, String checksum) {
        long contentLength = computeAndMoveContentLength(request, payload);
        setupPreExistingTrailers(request);

        long encodedContentLength = calculateEncodedContentLength(contentLength, checksum);

        if (checksumAlgorithm != null) {
            String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
            request.appendHeader(X_AMZ_TRAILER, checksumHeaderName);
        }
        request.putHeader(Header.CONTENT_LENGTH, Long.toString(encodedContentLength));
        // CRT-signed request doesn't expect 'aws-chunked' Content-Encoding, so we don't add it
    }

    @Override
    public CompletableFuture<Pair<SdkHttpRequest.Builder, Optional<Publisher<ByteBuffer>>>> beforeSigningAsync(
        SdkHttpRequest.Builder request, Publisher<ByteBuffer> payload, String checksum) {

        return SignerUtils.moveContentLength(request, payload)
                          .thenApply(p -> {
                              SdkHttpRequest.Builder requestBuilder = p.left();
                              setupPreExistingTrailers(requestBuilder);

                              long decodedContentLength =
                                  requestBuilder.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH)
                                                .map(Long::parseLong)
                                                // should not happen, this header is added by
                                                // moveContentLength
                                                .orElseThrow(() -> new RuntimeException(
                                                    X_AMZ_DECODED_CONTENT_LENGTH + " header not present"));

                              long encodedContentLength = calculateEncodedContentLength(decodedContentLength, checksum);

                              if (checksumAlgorithm != null) {
                                  String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
                                  request.appendHeader(X_AMZ_TRAILER, checksumHeaderName);
                              }
                              request.putHeader(Header.CONTENT_LENGTH, Long.toString(encodedContentLength));

                              return Pair.of(requestBuilder, p.right());
                          });
    }

    private long calculateEncodedContentLength(long decodedContentLength, String checksum) {
        long encodedContentLength = 0;

        encodedContentLength += calculateExistingTrailersLength();

        switch (checksum) {
            case STREAMING_ECDSA_SIGNED_PAYLOAD: {
                encodedContentLength += calculateChunksLength(decodedContentLength, CHUNK_SIGNATURE_EXTENSION_LENGTH);
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                if (checksumAlgorithm != null) {
                    encodedContentLength += calculateChecksumTrailerLength(checksumHeaderName(checksumAlgorithm));
                }
                encodedContentLength += calculateChunksLength(decodedContentLength, 0);
                break;
            case STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER: {
                encodedContentLength += calculateChunksLength(decodedContentLength, CHUNK_SIGNATURE_EXTENSION_LENGTH);
                if (checksumAlgorithm != null) {
                    encodedContentLength += calculateChecksumTrailerLength(checksumHeaderName(checksumAlgorithm));
                }
                encodedContentLength += 170; // x-amz-trailer-signature:<sigv4a-ecsda hex signature, 144 bytes>\r\n
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
        payload.checksumPayload(sdkChecksum);

        TrailerProvider checksumTrailer =
            new ChecksumTrailerProvider(sdkChecksum, checksumHeaderName, checksumAlgorithm, payloadChecksumStore);

        payload.addTrailer(checksumTrailer);
    }

    private String getCachedChecksum() {
        byte[] checksumBytes = payloadChecksumStore.getChecksumValue(checksumAlgorithm);
        if (checksumBytes != null) {
            return BinaryUtils.toBase64(checksumBytes);
        }
        return null;
    }

    static final class Builder {
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

        public AwsChunkedV4aPayloadSigner build() {
            return new AwsChunkedV4aPayloadSigner(this);
        }
    }
}
