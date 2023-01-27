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

package software.amazon.awssdk.authcrt.signer.internal;

import static software.amazon.awssdk.auth.signer.internal.AbstractAwsS3V4Signer.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.auth.signer.internal.Aws4SignerUtils.calculateRequestContentLength;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsSignedChunkedEncodingInputStream;
import software.amazon.awssdk.auth.signer.params.SignerChecksumParams;
import software.amazon.awssdk.authcrt.signer.AwsCrtS3V4aSigner;
import software.amazon.awssdk.authcrt.signer.internal.chunkedencoding.AwsS3V4aChunkSigner;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedEncodingConfig;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.RegionScope;

@SdkInternalApi
public final class DefaultAwsCrtS3V4aSigner implements AwsCrtS3V4aSigner {

    private final AwsCrt4aSigningAdapter signerAdapter;
    private final SigningConfigProvider configProvider;
    private final RegionScope defaultRegionScope;

    DefaultAwsCrtS3V4aSigner(AwsCrt4aSigningAdapter signerAdapter, SigningConfigProvider signingConfigProvider) {
        this(signerAdapter, signingConfigProvider, null);
    }

    DefaultAwsCrtS3V4aSigner(AwsCrt4aSigningAdapter signerAdapter, SigningConfigProvider signingConfigProvider,
                             RegionScope defaultRegionScope) {
        this.signerAdapter = signerAdapter;
        this.configProvider = signingConfigProvider;
        this.defaultRegionScope = defaultRegionScope;
    }

    private DefaultAwsCrtS3V4aSigner(BuilderImpl builder) {
        this(new AwsCrt4aSigningAdapter(), new SigningConfigProvider(), builder.defaultRegionScope);
    }

    public static AwsCrtS3V4aSigner create() {
        return builder().build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (credentialsAreAnonymous(executionAttributes)) {
            return request;
        }
        ExecutionAttributes defaultsApplied = applyDefaults(executionAttributes);
        AwsSigningConfig requestSigningConfig = configProvider.createS3CrtSigningConfig(defaultsApplied);
        SignerChecksumParams signerChecksumParams = signerChecksumParamsFromAttributes(defaultsApplied);

        if (shouldSignPayload(request, defaultsApplied)) {
            SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
            if (signerChecksumParams != null) {
                requestSigningConfig.setSignedBodyValue(
                    AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD_TRAILER);
                updateRequestWithTrailer(signerChecksumParams, mutableRequest);
            } else {
                requestSigningConfig.setSignedBodyValue(
                    AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
            }
            setHeaderContentLength(mutableRequest, signerChecksumParams);
            SdkSigningResult signingResult = signerAdapter.sign(mutableRequest.build(), requestSigningConfig);
            AwsSigningConfig chunkConfig = configProvider.createChunkedSigningConfig(defaultsApplied);
            return enablePayloadSigning(signingResult, chunkConfig, signerChecksumParams);
        } else {
            requestSigningConfig.setSignedBodyValue(signerChecksumParams != null
                                                    ? STREAMING_UNSIGNED_PAYLOAD_TRAILER
                                                    : AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
            return signerAdapter.signRequest(request, requestSigningConfig);
        }
    }

    private static SignerChecksumParams signerChecksumParamsFromAttributes(ExecutionAttributes executionAttributes) {
        ChecksumSpecs checksumSpecs = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        if (checksumSpecs == null) {
            return null;
        }
        return SignerChecksumParams.builder()
                                   .isStreamingRequest(checksumSpecs.isRequestStreaming())
                                   .algorithm(checksumSpecs.algorithm())
                                   .checksumHeaderName(checksumSpecs.headerName()).build();
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (credentialsAreAnonymous(executionAttributes)) {
            return request;
        }
        ExecutionAttributes defaultsApplied = applyDefaults(executionAttributes);
        return signerAdapter.signRequest(request, configProvider.createS3CrtPresigningConfig(defaultsApplied));
    }

    private boolean credentialsAreAnonymous(ExecutionAttributes executionAttributes) {
        return CredentialUtils.isAnonymous(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS));
    }

    private boolean shouldSignPayload(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (!request.protocol().equals("https") && request.contentStreamProvider().isPresent()) {
            return true;
        }
        boolean payloadSigning =
            booleanValue(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING));
        boolean chunkedEncoding =
            booleanValue(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING));

        return payloadSigning && chunkedEncoding;
    }

    private void setHeaderContentLength(SdkHttpFullRequest.Builder mutableRequest, SignerChecksumParams signerChecksumParams) {
        long originalContentLength = calculateRequestContentLength(mutableRequest);
        mutableRequest.putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength));

        String totalLength = Long.toString(
            AwsSignedChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength,
                                                                             AwsS3V4aChunkSigner.getSignatureLength(),
                                                                             AwsChunkedEncodingConfig.create(),
                                                                             signerChecksumParams != null)
            + getChecksumTrailerLength(signerChecksumParams, AwsS3V4aChunkSigner.getSignatureLength()));

        mutableRequest.putHeader(CONTENT_LENGTH, totalLength);

    }

    private SdkHttpFullRequest enablePayloadSigning(SdkSigningResult signingResult, AwsSigningConfig chunkConfig,
                                                    SignerChecksumParams signerChecksumParams) {
        SdkHttpFullRequest signedRequest = signingResult.getSignedRequest();
        byte[] signature = signingResult.getSignature();
        SdkHttpFullRequest.Builder mutableSignedRequest = signedRequest.toBuilder();
        ContentStreamProvider streamProvider = mutableSignedRequest.contentStreamProvider();
        AwsS3V4aChunkSigner chunkSigner = new AwsS3V4aChunkSigner(signerAdapter, chunkConfig);

        String checksumHeader = signerChecksumParams != null ? signerChecksumParams.checksumHeaderName() : null;
        SdkChecksum sdkChecksum = signerChecksumParams != null ?
                                       SdkChecksum.forAlgorithm(signerChecksumParams.algorithm()) : null;

        mutableSignedRequest.contentStreamProvider(
            () -> AwsSignedChunkedEncodingInputStream.builder()
                                                     .inputStream(streamProvider.newStream())
                                                     .awsChunkSigner(chunkSigner)
                                                     .checksumHeaderForTrailer(checksumHeader)
                                                     .sdkChecksum(sdkChecksum)
                                                     .headerSignature(new String(signature, StandardCharsets.UTF_8))
                                                     .awsChunkedEncodingConfig(AwsChunkedEncodingConfig.create())
                                                     .build());

        return mutableSignedRequest.build();
    }

    private boolean booleanValue(Boolean attribute) {
        return Boolean.TRUE.equals(attribute);
    }

    /**
     * Applies preconfigured defaults for values that are not present in {@code executionAttributes}.
     */
    private ExecutionAttributes applyDefaults(ExecutionAttributes executionAttributes) {
        return applyDefaultRegionScope(executionAttributes);
    }

    private ExecutionAttributes applyDefaultRegionScope(ExecutionAttributes executionAttributes) {
        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE) != null) {
            return executionAttributes;
        }

        if (defaultRegionScope == null) {
            return executionAttributes;
        }

        return executionAttributes.copy()
                                  .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, defaultRegionScope);
    }

    private static class BuilderImpl implements Builder {
        private RegionScope defaultRegionScope;

        @Override
        public Builder defaultRegionScope(RegionScope defaultRegionScope) {
            this.defaultRegionScope = defaultRegionScope;
            return this;
        }

        @Override
        public AwsCrtS3V4aSigner build() {
            return new DefaultAwsCrtS3V4aSigner(this);
        }
    }

    private static long getChecksumTrailerLength(SignerChecksumParams signerParams, int signatureLength) {
        return signerParams == null ? 0
                                    : AwsSignedChunkedEncodingInputStream.calculateChecksumContentLength(
                                        signerParams.algorithm(),
                                        signerParams.checksumHeaderName(), signatureLength);
    }

    private static void updateRequestWithTrailer(SignerChecksumParams signerChecksumParams,
                                                 SdkHttpFullRequest.Builder mutableRequest) {
        mutableRequest.putHeader("x-amz-trailer", signerChecksumParams.checksumHeaderName());
        mutableRequest.appendHeader("Content-Encoding", "aws-chunked");
    }
}
