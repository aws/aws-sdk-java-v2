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
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.moveContentLength;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChecksumTrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkedEncodedInputStream;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.TrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ChecksumInputStream;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ResettableContentStreamProvider;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of a V4aPayloadSigner which chunk-encodes a payload, optionally adding a chunk-signature chunk-extension,
 * and/or trailers representing trailing headers with their signature at the end.
 */
@SdkInternalApi
public final class AwsChunkedV4aPayloadSigner implements V4aPayloadSigner {

    private final CredentialScope credentialScope;
    private final int chunkSize;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final List<Pair<String, List<String>>> preExistingTrailers = new ArrayList<>();

    private AwsChunkedV4aPayloadSigner(Builder builder) {
        this.credentialScope = Validate.paramNotNull(builder.credentialScope, "CredentialScope");
        this.chunkSize = Validate.isPositive(builder.chunkSize, "ChunkSize");
        this.checksumAlgorithm = builder.checksumAlgorithm;
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

        preExistingTrailers.forEach(trailer -> chunkedEncodedInputStreamBuilder.addTrailer(() -> trailer));

        switch (requestSigningResult.getSigningConfig().getSignedBodyValue()) {
            case STREAMING_ECDSA_SIGNED_PAYLOAD: {
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSignature(),
                                                                requestSigningResult.getSigningConfig());
                chunkedEncodedInputStreamBuilder.addExtension(new SigV4aChunkExtensionProvider(rollingSigner, credentialScope));
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                setupChecksumTrailerIfNeeded(chunkedEncodedInputStreamBuilder);
                break;
            case STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER: {
                RollingSigner rollingSigner = new RollingSigner(requestSigningResult.getSignature(),
                                                                requestSigningResult.getSigningConfig());
                chunkedEncodedInputStreamBuilder.addExtension(new SigV4aChunkExtensionProvider(rollingSigner, credentialScope));
                setupChecksumTrailerIfNeeded(chunkedEncodedInputStreamBuilder);
                chunkedEncodedInputStreamBuilder.addTrailer(
                    new SigV4aTrailerProvider(chunkedEncodedInputStreamBuilder.trailers(), rollingSigner, credentialScope)
                );
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        return new ResettableContentStreamProvider(chunkedEncodedInputStreamBuilder::build);
    }

    @Override
    public void beforeSigning(SdkHttpRequest.Builder request, ContentStreamProvider payload, String checksum) {
        long encodedContentLength = 0;
        long contentLength = moveContentLength(request, payload != null ? payload.newStream() : new StringInputStream(""));
        setupPreExistingTrailers(request);

        // pre-existing trailers
        encodedContentLength += calculateExistingTrailersLength();

        switch (checksum) {
            case STREAMING_ECDSA_SIGNED_PAYLOAD: {
                long extensionsLength = 161; // ;chunk-signature:<sigv4a-ecsda hex signature, 144 bytes>
                encodedContentLength += calculateChunksLength(contentLength, extensionsLength);
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                if (checksumAlgorithm != null) {
                    encodedContentLength += calculateChecksumTrailerLength(checksumHeaderName(checksumAlgorithm));
                }
                encodedContentLength += calculateChunksLength(contentLength, 0);
                break;
            case STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER: {
                long extensionsLength = 161; // ;chunk-signature:<sigv4a-ecsda hex signature, 144 bytes>
                encodedContentLength += calculateChunksLength(contentLength, extensionsLength);
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

        if (checksumAlgorithm != null) {
            String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
            request.appendHeader(X_AMZ_TRAILER, checksumHeaderName);
        }
        request.putHeader(Header.CONTENT_LENGTH, Long.toString(encodedContentLength));
        // CRT-signed request doesn't expect 'aws-chunked' Content-Encoding, so we don't add it
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

    /**
     * Add the checksum as a trailer to the chunk-encoded stream.
     * <p>
     * If the checksum-algorithm is not present, then nothing is done.
     */
    private void setupChecksumTrailerIfNeeded(ChunkedEncodedInputStream.Builder builder) {
        if (checksumAlgorithm == null) {
            return;
        }
        String checksumHeaderName = checksumHeaderName(checksumAlgorithm);
        SdkChecksum sdkChecksum = fromChecksumAlgorithm(checksumAlgorithm);
        ChecksumInputStream checksumInputStream = new ChecksumInputStream(
            builder.inputStream(),
            Collections.singleton(sdkChecksum)
        );

        TrailerProvider checksumTrailer = new ChecksumTrailerProvider(sdkChecksum, checksumHeaderName);

        builder.inputStream(checksumInputStream).addTrailer(checksumTrailer);
    }

    static final class Builder {
        private CredentialScope credentialScope;
        private Integer chunkSize;
        private ChecksumAlgorithm checksumAlgorithm;

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

        public AwsChunkedV4aPayloadSigner build() {
            return new AwsChunkedV4aPayloadSigner(this);
        }
    }
}
