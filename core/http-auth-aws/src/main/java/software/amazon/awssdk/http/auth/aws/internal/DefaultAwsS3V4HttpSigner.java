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

package software.amazon.awssdk.http.auth.aws.internal;

import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.internal.util.SignerUtils.validatedProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsS3V4HttpSigner;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.AwsS3V4ChunkSigner;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.http.auth.aws.internal.io.AwsChunkedEncodingConfig;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A default implementation of {@link AwsS3V4HttpSigner}.
 */
@SdkInternalApi
public class DefaultAwsS3V4HttpSigner extends DefaultAwsV4HttpSigner implements AwsS3V4HttpSigner {

    public static final String CONTENT_SHA_256_WITH_CHECKSUM = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER";
    public static final String STREAMING_UNSIGNED_PAYLOAD_TRAILER = "STREAMING-UNSIGNED-PAYLOAD-TRAILER";
    private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final String CONTENT_LENGTH = "Content-Length";
    protected AwsChunkedEncodingConfig encodingConfig = AwsChunkedEncodingConfig.create();
    // optional
    private Boolean chunkedEncoding;
    private Boolean payloadSigning;
    // auxilliary
    private boolean unsignedStreamingTrailer;
    private boolean trailingChecksum;
    // compound
    private long contentLength = -1;

    /**
     * Calculates the content length of a request. If the content-length isn't in the header,
     * the method reads the whole input stream to get the length.
     */
    private static <T> long calculateContentLength(T payload) {
        long contentLength;
        if (payload instanceof ContentStreamProvider) {
            try {
                contentLength = getContentLength(((ContentStreamProvider) payload).newStream());
            } catch (IOException e) {
                throw new RuntimeException("Cannot get the content-length of the request content.");
            }
            return contentLength;
        } else {
            throw new IllegalArgumentException("Cannot get the content-length from paylaod of type " +
                payload.getClass().getSimpleName() + " .");
        }
    }

    /**
     * Read a stream to get the length.
     */
    private static long getContentLength(InputStream content) throws IOException {
        long contentLength = 0;
        byte[] tmp = new byte[4096];
        int read;
        while ((read = content.read(tmp)) != -1) {
            contentLength += read;
        }
        return contentLength;
    }

    private static long getChecksumTrailerLength(ChecksumAlgorithm algorithm, String checksumHeaderName) {
        if (algorithm == null || StringUtils.isBlank(checksumHeaderName)) {
            return 0;
        }
        return AwsSignedChunkedEncodingInputStream.calculateChecksumContentLength(
            algorithm,
            checksumHeaderName,
            AwsS3V4ChunkSigner.SIGNATURE_LENGTH);
    }

    /**
     * parse a string into a Long, or return null if it's un-parseable
     */
    private static Optional<Long> safeParseLong(String longStr) {
        try {
            return Optional.of(Long.parseLong(longStr));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request)
            throws UnsupportedOperationException {
        // There isn't currently a concept of async for this s3 signer
        throw new UnsupportedOperationException();
    }

    @Override
    protected void setParameters(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        super.setParameters(signRequest);

        // optional
        payloadSigning = validatedProperty(signRequest, PAYLOAD_SIGNING, false);
        chunkedEncoding = validatedProperty(signRequest, CHUNKED_ENCODING, false);

        // we re-check these parameters, because the defaults are different from other v4 signers
        doubleUrlEncode = validatedProperty(signRequest, DOUBLE_URL_ENCODE, false);
        normalizePath = validatedProperty(signRequest, NORMALIZE_PATH, false);

        // auxilliary
        unsignedStreamingTrailer = signRequest.request().firstMatchingHeader("x-amz-content-sha256")
            .map("STREAMING_UNSIGNED_PAYLOAD_TRAILER"::equals)
            .orElse(false);
        trailingChecksum = sdkChecksum != null;

        // compound
        Optional<Long> maybeContentLength = safeParseLong(signRequest.request().firstMatchingHeader(Header.CONTENT_LENGTH)
            .orElse(null));
        contentLength = maybeContentLength
            .orElse(signRequest.payload()
                .map(DefaultAwsS3V4HttpSigner::calculateContentLength)
                .orElse(0L));
    }

    @Override
    protected void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                    ContentChecksum contentChecksum) {
        if (!unsignedStreamingTrailer) {
            // To be consistent with other service clients using sig-v4,
            // we just set the header as "required", and the content hash will
            // be added when adding the super's pre-requisites.
            requestBuilder.putHeader(X_AMZ_CONTENT_SHA256, "required");
        }

        if (payloadSigning && chunkedEncoding) {
            requestBuilder.putHeader("x-amz-decoded-content-length", Long.toString(contentLength));
            if (trailingChecksum) {
                requestBuilder.putHeader("x-amz-trailer", checksumHeaderName);
            }
            requestBuilder.appendHeader("Content-Encoding", "aws-chunked");
            long streamContentLength = AwsSignedChunkedEncodingInputStream
                .calculateStreamContentLength(
                    contentLength, AwsS3V4ChunkSigner.getSignatureLength(),
                    encodingConfig, trailingChecksum);
            long checksumTrailerLength = trailingChecksum ?
                getChecksumTrailerLength(checksumAlgorithm, checksumHeaderName) :
                0;

            requestBuilder.putHeader(CONTENT_LENGTH, Long.toString(
                streamContentLength + checksumTrailerLength));
        }

        super.addPrerequisites(requestBuilder, contentChecksum);
    }

    @Override
    protected String createContentHash(ContentStreamProvider payload) {
        if (!payloadSigning) {
            return unsignedStreamingTrailer ? STREAMING_UNSIGNED_PAYLOAD_TRAILER : UNSIGNED_PAYLOAD;
        }
        if (chunkedEncoding) {
            return trailingChecksum ? CONTENT_SHA_256_WITH_CHECKSUM : CONTENT_SHA_256;
        } else {
            return super.createContentHash(payload);
        }
    }

    @Override
    protected ContentStreamProvider processPayload(ContentStreamProvider payload) {
        if (payload != null && chunkedEncoding) {
            AwsS3V4ChunkSigner chunkSigner = new AwsS3V4ChunkSigner(signingKey, credentialScope);
            AwsSignedChunkedEncodingInputStream encodedStream = AwsSignedChunkedEncodingInputStream.builder()
                .inputStream(payload.newStream())
                .sdkChecksum(sdkChecksum)
                .checksumHeaderForTrailer(checksumHeaderName)
                .awsChunkSigner(chunkSigner)
                .headerSignature(signature)
                .awsChunkedEncodingConfig(encodingConfig)
                .build();

            return () -> encodedStream;
        }
        return null;
    }
}
