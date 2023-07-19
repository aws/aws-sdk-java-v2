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

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.CONTENT_ENCODING;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;
import static software.amazon.awssdk.http.auth.aws.util.SignerConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.checksum.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.aws.checksum.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.http.auth.aws.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.crt.internal.chunkedencoding.AwsS3V4aChunkSigner;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.StringUtils;

/**
 * An implementation of {@link AwsCrtV4aHttpSigner} which signs requests using the CRT configured with
 * S3-specific configurations and properties.
 * TODO: Rename this correctly once the interface is gone and checkstyle can pass
 */
@SdkInternalApi
public final class DefaultAwsCrtS3V4aHttpSigner implements BaseAwsCrtV4aHttpSigner<AwsCrtVS34aHttpProperties> {

    private final BaseAwsCrtV4aHttpSigner<AwsCrtV4aHttpProperties> v4aSigner;
    private final AwsChunkedEncodingConfig encodingConfig;

    public DefaultAwsCrtS3V4aHttpSigner(BaseAwsCrtV4aHttpSigner<AwsCrtV4aHttpProperties> v4aSigner,
                                        AwsChunkedEncodingConfig encodingConfig) {
        this.v4aSigner = v4aSigner;
        this.encodingConfig = encodingConfig;
    }

    @Override
    public AwsSigningConfig createSigningConfig(AwsCrtVS34aHttpProperties properties) {
        AwsSigningConfig signingConfig = v4aSigner.createSigningConfig(properties);

        // make s3-specific changes to the config
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
        signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        // check chunked-encoding + signed-payload
        if (properties.shouldSignPayload() && properties.shouldChunkEncode()) {
            if (properties.shouldTrail()) {
                signingConfig.setSignedBodyValue(
                    AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER);
            } else {
                signingConfig.setSignedBodyValue(
                    AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
            }
        } else {
            if (properties.shouldTrail()) {
                signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.STREAMING_UNSIGNED_PAYLOAD_TRAILER);
            } else {
                signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
            }
        }

        return signingConfig;
    }

    @Override
    public SdkHttpRequest adaptRequest(SdkHttpRequest request, AwsCrtVS34aHttpProperties properties) {
        SdkHttpRequest.Builder requestBuilder = request.toBuilder();

        if (properties.shouldSignPayload() && properties.shouldChunkEncode()) {
            boolean hasTrailingChecksum = properties.getChecksumAlgorithm() != null && isNotBlank(properties.getChecksumHeader());
            if (hasTrailingChecksum) {
                requestBuilder.putHeader(X_AMZ_TRAILER, properties.getChecksumHeader());
                requestBuilder.appendHeader(CONTENT_ENCODING, "aws-chunked");
            }
            requestBuilder.putHeader(X_AMZ_DECODED_CONTENT_LENGTH, Long.toString(properties.getContentLength()));
            long streamContentLength = AwsSignedChunkedEncodingInputStream
                .calculateStreamContentLength(
                    properties.getContentLength(), AwsS3V4aChunkSigner.getSignatureLength(),
                    encodingConfig, hasTrailingChecksum);
            long checksumTrailerLength = hasTrailingChecksum ?
                getChecksumTrailerLength(properties.getChecksumAlgorithm(), properties.getChecksumHeader()) :
                0;

            requestBuilder.putHeader(CONTENT_LENGTH, Long.toString(
                streamContentLength + checksumTrailerLength));
        }

        return v4aSigner.adaptRequest(requestBuilder.build(), properties);
    }

    @Override
    public HttpRequest transformRequest(SdkHttpRequest request, ContentStreamProvider payload,
                                        AwsCrtVS34aHttpProperties properties) {
        return v4aSigner.transformRequest(request, payload, properties);
    }

    @Override
    public SigV4aRequestContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig,
                                     AwsCrtVS34aHttpProperties properties) {
        return v4aSigner.sign(request, crtRequest, signingConfig, properties);
    }

    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, SigV4aRequestContext sigV4aRequestContext,
                                      AwsCrtVS34aHttpProperties properties) {
        if (properties.shouldSignPayload() && properties.shouldChunkEncode()) {
            AwsSigningConfig signingConfig = sigV4aRequestContext.getSigningConfig().clone();
            signingConfig.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);
            signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
            AwsS3V4aChunkSigner chunkSigner = new AwsS3V4aChunkSigner(signingConfig);

            String checksumHeader = properties.getChecksumHeader();
            SdkChecksum sdkChecksum = properties.getChecksumAlgorithm() != null ?
                SdkChecksum.forAlgorithm(properties.getChecksumAlgorithm()) : null;

            return () -> AwsSignedChunkedEncodingInputStream.builder()
                .inputStream(payload.newStream())
                .awsChunkSigner(chunkSigner)
                .checksumHeaderForTrailer(checksumHeader)
                .sdkChecksum(sdkChecksum)
                .headerSignature(new String(sigV4aRequestContext.getSignature(), StandardCharsets.UTF_8))
                .awsChunkedEncodingConfig(AwsChunkedEncodingConfig.create())
                .build();
        }
        return payload;
    }

    @Override
    public AwsCrtVS34aHttpProperties getProperties(SignRequest<?, ? extends AwsCredentialsIdentity> signRequest) {
        return AwsCrtVS34aHttpProperties.create(signRequest);
    }

    private long getChecksumTrailerLength(ChecksumAlgorithm algorithm, String checksumHeaderName) {
        if (algorithm == null || StringUtils.isBlank(checksumHeaderName)) {
            return 0;
        }
        return AwsSignedChunkedEncodingInputStream.calculateChecksumContentLength(
            algorithm,
            checksumHeaderName,
            AwsS3V4aChunkSigner.getSignatureLength());
    }
}
