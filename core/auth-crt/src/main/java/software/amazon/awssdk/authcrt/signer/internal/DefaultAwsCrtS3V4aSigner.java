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

import static software.amazon.awssdk.auth.signer.internal.Aws4SignerUtils.calculateRequestContentLength;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.authcrt.signer.AwsCrtS3V4aSigner;
import software.amazon.awssdk.authcrt.signer.internal.chunkedencoding.AwsS3V4aChunkSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public final class DefaultAwsCrtS3V4aSigner implements AwsCrtS3V4aSigner {

    private final AwsCrt4aSigningAdapter signerAdapter;
    private final SigningConfigProvider configProvider;

    DefaultAwsCrtS3V4aSigner(AwsCrt4aSigningAdapter signerAdapter, SigningConfigProvider signingConfigProvider) {
        this.signerAdapter = signerAdapter;
        this.configProvider = signingConfigProvider;
    }

    public static AwsCrtS3V4aSigner create() {
        return new DefaultAwsCrtS3V4aSigner(new AwsCrt4aSigningAdapter(), new SigningConfigProvider());
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (credentialsAreAnonymous(executionAttributes)) {
            return request;
        }
        AwsSigningConfig requestSigningConfig = configProvider.createS3CrtSigningConfig(executionAttributes);
        if (shouldSignPayload(request, executionAttributes)) {
            requestSigningConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);
            SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
            setHeaderContentLength(mutableRequest);
            SdkSigningResult signingResult = signerAdapter.sign(mutableRequest.build(), requestSigningConfig);
            AwsSigningConfig chunkConfig = configProvider.createChunkedSigningConfig(executionAttributes);
            return enablePayloadSigning(signingResult, chunkConfig);
        } else {
            requestSigningConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
            return signerAdapter.signRequest(request, requestSigningConfig);
        }
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (credentialsAreAnonymous(executionAttributes)) {
            return request;
        }
        return signerAdapter.signRequest(request, configProvider.createS3CrtPresigningConfig(executionAttributes));
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

    private void setHeaderContentLength(SdkHttpFullRequest.Builder mutableRequest) {
        long originalContentLength = calculateRequestContentLength(mutableRequest);
        mutableRequest.putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength));
        mutableRequest.putHeader(CONTENT_LENGTH, Long.toString(
            AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength,
                                                                       AwsS3V4aChunkSigner.getSignatureLength(),
                                                                       AwsChunkedEncodingConfig.create())));
    }

    private SdkHttpFullRequest enablePayloadSigning(SdkSigningResult signingResult, AwsSigningConfig chunkConfig) {
        SdkHttpFullRequest signedRequest = signingResult.getSignedRequest();
        byte[] signature = signingResult.getSignature();
        SdkHttpFullRequest.Builder mutableSignedRequest = signedRequest.toBuilder();
        ContentStreamProvider streamProvider = mutableSignedRequest.contentStreamProvider();
        AwsS3V4aChunkSigner chunkSigner = new AwsS3V4aChunkSigner(signerAdapter, chunkConfig);
        mutableSignedRequest.contentStreamProvider(
            () -> new AwsChunkedEncodingInputStream(streamProvider.newStream(),
                                                    new String(signature, StandardCharsets.UTF_8),
                                                    chunkSigner,
                                                    AwsChunkedEncodingConfig.create()));
        return mutableSignedRequest.build();
    }

    private boolean booleanValue(Boolean attribute) {
        return Boolean.TRUE.equals(attribute);
    }

}
