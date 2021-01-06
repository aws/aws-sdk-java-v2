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

package software.amazon.awssdk.auth.signer;

import static software.amazon.awssdk.auth.signer.internal.Aws4SignerUtils.calculateRequestContentLength;
import static software.amazon.awssdk.auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256;

import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.internal.AbstractAws4Signer;
import software.amazon.awssdk.auth.signer.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsS3V4ChunkSigner;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.AwsS3V4SignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * AWS4 signer implementation for AWS S3
 */
@SdkPublicApi
public final class AwsS3V4Signer extends AbstractAws4Signer<AwsS3V4SignerParams, Aws4PresignerParams> {

    private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

    /**
     * Sent to S3 in lieu of a payload hash when unsigned payloads are enabled
     */
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final String CONTENT_LENGTH = "Content-Length";

    private AwsS3V4Signer() {
    }

    public static AwsS3V4Signer create() {
        return new AwsS3V4Signer();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        AwsS3V4SignerParams signingParams = constructAwsS3SignerParams(executionAttributes);

        return sign(request, signingParams);
    }

    /**
     * A method to sign the given #request. The parameters required for signing are provided through the modeled
     * {@link AwsS3V4Signer} class.
     *
     * @param request The request to sign
     * @param signingParams Class with the parameters used for signing the request
     * @return A signed version of the input request
     */
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, AwsS3V4SignerParams signingParams) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request;
        }

        Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return doSign(request, requestParams, signingParams).build();
    }

    private AwsS3V4SignerParams constructAwsS3SignerParams(ExecutionAttributes executionAttributes) {
        AwsS3V4SignerParams.Builder signerParams = extractSignerParams(AwsS3V4SignerParams.builder(),
                                                                             executionAttributes);

        Optional.ofNullable(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING))
                .ifPresent(signerParams::enableChunkedEncoding);

        Optional.ofNullable(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING))
                .ifPresent(signerParams::enablePayloadSigning);

        return signerParams.build();
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        Aws4PresignerParams signingParams =
            extractPresignerParams(Aws4PresignerParams.builder(), executionAttributes).build();

        return presign(request, signingParams);
    }

    /**
     * A method to pre sign the given #request. The parameters required for pre signing are provided through the modeled
     * {@link Aws4PresignerParams} class.
     *
     * @param request The request to pre-sign
     * @param signingParams Class with the parameters used for pre signing the request
     * @return A pre signed version of the input request
     */
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, Aws4PresignerParams signingParams) {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request;
        }

        Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return doPresign(request, requestParams, signingParams).build();
    }

    /**
     * If necessary, creates a chunk-encoding wrapper on the request payload.
     */
    @Override
    protected void processRequestPayload(SdkHttpFullRequest.Builder mutableRequest,
                                         byte[] signature,
                                         byte[] signingKey,
                                         Aws4SignerRequestParams signerRequestParams,
                                         AwsS3V4SignerParams signerParams) {

        if (useChunkEncoding(mutableRequest, signerParams)) {
            if (mutableRequest.contentStreamProvider() != null) {
                ContentStreamProvider streamProvider = mutableRequest.contentStreamProvider();
                mutableRequest.contentStreamProvider(() -> AwsS3V4Signer.this.asChunkEncodedStream(
                        streamProvider.newStream(),
                        signature,
                        signingKey,
                        signerRequestParams
                ));
            }
        }
    }

    @Override
    protected String calculateContentHashPresign(SdkHttpFullRequest.Builder mutableRequest, Aws4PresignerParams signerParams) {
        return UNSIGNED_PAYLOAD;
    }

    private AwsChunkedEncodingInputStream asChunkEncodedStream(InputStream inputStream,
                                                               byte[] signature,
                                                               byte[] signingKey,
                                                               Aws4SignerRequestParams signerRequestParams) {
        AwsS3V4ChunkSigner chunkSigner = new AwsS3V4ChunkSigner(signingKey,
                                                                signerRequestParams.getFormattedRequestSigningDateTime(),
                                                                signerRequestParams.getScope());
        String signatureHex = BinaryUtils.toHex(signature);
        return new AwsChunkedEncodingInputStream(inputStream, signatureHex, chunkSigner, AwsChunkedEncodingConfig.create());
    }

    /**
     * Returns the pre-defined header value and set other necessary headers if
     * the request needs to be chunk-encoded. Otherwise calls the superclass
     * method which calculates the hash of the whole content for signing.
     */
    @Override
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, AwsS3V4SignerParams signerParams) {
        // To be consistent with other service clients using sig-v4,
        // we just set the header as "required", and AWS4Signer.sign() will be
        // notified to pick up the header value returned by this method.
        mutableRequest.putHeader(X_AMZ_CONTENT_SHA256, "required");

        if (isPayloadSigningEnabled(mutableRequest, signerParams)) {
            if (useChunkEncoding(mutableRequest, signerParams)) {
                long originalContentLength = calculateRequestContentLength(mutableRequest);
                mutableRequest.putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength));
                mutableRequest.putHeader(CONTENT_LENGTH, Long.toString(
                    AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength,
                                                                               AwsS3V4ChunkSigner.getSignatureLength(),
                                                                               AwsChunkedEncodingConfig.create())));
                return CONTENT_SHA_256;
            } else {
                return super.calculateContentHash(mutableRequest, signerParams);
            }
        }

        return UNSIGNED_PAYLOAD;
    }

    /**
     * Determine whether to use aws-chunked for signing
     */
    private boolean useChunkEncoding(SdkHttpFullRequest.Builder mutableRequest, AwsS3V4SignerParams signerParams) {
        // Chunked encoding only makes sense to do when the payload is signed
        return isPayloadSigningEnabled(mutableRequest, signerParams) && isChunkedEncodingEnabled(signerParams);
    }

    /**
     * @return True if chunked encoding has been enabled. Otherwise false.
     */
    private boolean isChunkedEncodingEnabled(AwsS3V4SignerParams signerParams) {
        Boolean isChunkedEncodingEnabled = signerParams.enableChunkedEncoding();
        return isChunkedEncodingEnabled != null && isChunkedEncodingEnabled;
    }

    /**
     * @return True if payload signing is explicitly enabled.
     */
    private boolean isPayloadSigningEnabled(SdkHttpFullRequest.Builder request, AwsS3V4SignerParams signerParams) {
        /**
         * If we aren't using https we should always sign the payload unless there is no payload
         */
        if (!request.protocol().equals("https") && request.contentStreamProvider() != null) {
            return true;
        }

        Boolean isPayloadSigningEnabled = signerParams.enablePayloadSigning();
        return isPayloadSigningEnabled != null && isPayloadSigningEnabled;
    }

}
