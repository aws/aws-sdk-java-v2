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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsS3V4HttpSigner;
import software.amazon.awssdk.http.auth.aws.checksum.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.aws.checksum.ContentChecksum;
import software.amazon.awssdk.http.auth.aws.checksum.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.AwsS3V4ChunkSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpProperties;
import software.amazon.awssdk.http.auth.aws.signer.BaseAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.SigV4RequestContext;
import software.amazon.awssdk.http.auth.aws.util.CanonicalRequestV2;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.StringUtils;

/**
 * An implementation of a {@link AwsS3V4HttpSigner} for S3 use-cases, which includes chunked-encoded request payloads.
 * TODO: Rename this correctly once the interface is gone and checkstyle can pass
 */
@SdkProtectedApi
public final class DefaultAwsS3V4HttpSigner implements BaseAwsV4HttpSigner<AwsS3V4HttpProperties> {

    private static final String CONTENT_SHA_256_WITH_CHECKSUM = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER";
    private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

    private final BaseAwsV4HttpSigner<AwsV4HttpProperties> v4Signer;
    private final AwsChunkedEncodingConfig encodingConfig;

    public DefaultAwsS3V4HttpSigner(BaseAwsV4HttpSigner<AwsV4HttpProperties> v4HttpSigner,
                                    AwsChunkedEncodingConfig encodingConfig) {
        this.v4Signer = v4HttpSigner;
        this.encodingConfig = encodingConfig;
    }

    private long getChecksumTrailerLength(ChecksumAlgorithm algorithm, String checksumHeaderName) {
        if (algorithm == null || StringUtils.isBlank(checksumHeaderName)) {
            return 0;
        }
        return AwsSignedChunkedEncodingInputStream.calculateChecksumContentLength(
            algorithm,
            checksumHeaderName,
            AwsS3V4ChunkSigner.SIGNATURE_LENGTH);
    }

    @Override
    public AsyncSignedRequest signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request)
            throws UnsupportedOperationException {
        // There isn't currently a concept of async for this s3 signer
        throw new UnsupportedOperationException();
    }

    @Override
    public SdkChecksum createSdkChecksum(SignRequest<?, ?> signRequest, AwsS3V4HttpProperties properties) {
        return v4Signer.createSdkChecksum(signRequest, properties);
    }

    @Override
    public ContentChecksum createChecksum(SyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                          AwsS3V4HttpProperties properties) {
        SdkChecksum sdkChecksum = createSdkChecksum(signRequest, properties);
        String contentHash = createContentHash(signRequest.payload().orElse(null), sdkChecksum, properties);
        return new ContentChecksum(contentHash, sdkChecksum);
    }

    @Override
    public CompletableFuture<ContentChecksum> createChecksum(AsyncSignRequest<? extends AwsCredentialsIdentity> signRequest,
                                                             AwsS3V4HttpProperties properties)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createContentHash(ContentStreamProvider payload, SdkChecksum sdkChecksum,
                                    AwsS3V4HttpProperties properties) {
        boolean shouldTrailChecksum = sdkChecksum != null;

        if (!properties.shouldSignPayload()) {
            return properties.shouldTrail() ? STREAMING_UNSIGNED_PAYLOAD_TRAILER : UNSIGNED_PAYLOAD;
        }
        if (properties.shouldChunkEncode()) {
            return shouldTrailChecksum ? CONTENT_SHA_256_WITH_CHECKSUM : CONTENT_SHA_256;
        } else {
            return v4Signer.createContentHash(payload, sdkChecksum, properties);
        }
    }

    @Override
    public CompletableFuture<String> createContentHash(Publisher<ByteBuffer> payload, SdkChecksum sdkChecksum,
                                                       AwsS3V4HttpProperties properties) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPrerequisites(SdkHttpRequest.Builder requestBuilder,
                                 ContentChecksum contentChecksum, AwsS3V4HttpProperties properties) {
        boolean shouldTrailChecksum = contentChecksum.contentFlexibleChecksum() != null;

        if (!properties.shouldTrail()) {
            // To be consistent with other service clients using sig-v4,
            // we just set the header as "required", and the content hash will
            // be added when delegating
            requestBuilder.putHeader(X_AMZ_CONTENT_SHA256, "required");
        }

        if (properties.shouldSignPayload() && properties.shouldChunkEncode()) {
            requestBuilder.putHeader("x-amz-decoded-content-length", Long.toString(properties.getContentLength()));
            if (shouldTrailChecksum) {
                requestBuilder.putHeader("x-amz-trailer", properties.getChecksumHeader());
            }
            requestBuilder.appendHeader("Content-Encoding", "aws-chunked");
            long streamContentLength = AwsSignedChunkedEncodingInputStream
                .calculateStreamContentLength(
                    properties.getContentLength(), AwsS3V4ChunkSigner.getSignatureLength(),
                    encodingConfig, shouldTrailChecksum);
            long checksumTrailerLength = shouldTrailChecksum ?
                getChecksumTrailerLength(properties.getChecksumAlgorithm(), properties.getChecksumHeader()) :
                0;

            requestBuilder.putHeader(CONTENT_LENGTH, Long.toString(
                streamContentLength + checksumTrailerLength));
        }

        v4Signer.addPrerequisites(requestBuilder, contentChecksum, properties);
    }

    @Override
    public CanonicalRequestV2 createCanonicalRequest(SdkHttpRequest request, ContentChecksum contentChecksum,
                                                     AwsS3V4HttpProperties properties) {
        return v4Signer.createCanonicalRequest(request, contentChecksum, properties);
    }

    @Override
    public String createSignString(String canonicalRequestHash, AwsS3V4HttpProperties properties) {
        return v4Signer.createSignString(canonicalRequestHash, properties);
    }

    @Override
    public byte[] createSigningKey(AwsS3V4HttpProperties properties) {
        return v4Signer.createSigningKey(properties);
    }

    @Override
    public String createSignature(String stringToSign, byte[] signingKey, AwsS3V4HttpProperties properties) {
        return v4Signer.createSignature(stringToSign, signingKey, properties);
    }

    @Override
    public void addSignature(SdkHttpRequest.Builder requestBuilder,
                             CanonicalRequestV2 canonicalRequest,
                             String signature,
                             AwsS3V4HttpProperties properties) {
        v4Signer.addSignature(requestBuilder, canonicalRequest, signature, properties);
    }

    @Override
    public ContentStreamProvider processPayload(ContentStreamProvider payload,
                                                SigV4RequestContext v4RequestContext, AwsS3V4HttpProperties properties) {
        if (payload != null && properties.shouldChunkEncode()) {
            AwsS3V4ChunkSigner chunkSigner =
                new AwsS3V4ChunkSigner(v4RequestContext.getSigningKey(), properties.getCredentialScope());
            AwsSignedChunkedEncodingInputStream encodedStream = AwsSignedChunkedEncodingInputStream.builder()
                .inputStream(payload.newStream())
                .sdkChecksum(v4RequestContext.getContentChecksum().contentFlexibleChecksum())
                .checksumHeaderForTrailer(properties.getChecksumHeader())
                .awsChunkSigner(chunkSigner)
                .headerSignature(v4RequestContext.getSignature())
                .awsChunkedEncodingConfig(encodingConfig)
                .build();

            return () -> encodedStream;
        }
        return payload;
    }

    @Override
    public Publisher<ByteBuffer> processPayload(Publisher<ByteBuffer> payload,
                                                CompletableFuture<SigV4RequestContext> futureV4RequestContext,
                                                AwsS3V4HttpProperties properties) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AwsS3V4HttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsS3V4HttpProperties.create(signRequest);
    }
}
