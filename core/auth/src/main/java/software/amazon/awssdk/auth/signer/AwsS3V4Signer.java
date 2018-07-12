/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.utils.Validate.validState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.internal.AbstractAws4Signer;
import software.amazon.awssdk.auth.signer.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.auth.signer.internal.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.AwsS3V4SignerParams;
import software.amazon.awssdk.core.exception.ResetException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
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

        final Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

        return doSign(request, requestParams, signingParams).build();
    }

    private AwsS3V4SignerParams constructAwsS3SignerParams(ExecutionAttributes executionAttributes) {
        final AwsS3V4SignerParams.Builder signerParams = extractSignerParams(AwsS3V4SignerParams.builder(),
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

        final Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signingParams);

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
            AwsChunkedEncodingInputStream chunkEncodededStream = new AwsChunkedEncodingInputStream(
                mutableRequest.content(),
                signingKey,
                signerRequestParams.getFormattedSigningDateTime(),
                signerRequestParams.getScope(),
                BinaryUtils.toHex(signature), this);
            mutableRequest.content(chunkEncodededStream);
        }
    }

    @Override
    protected String calculateContentHashPresign(SdkHttpFullRequest.Builder mutableRequest, Aws4PresignerParams signerParams) {
        return UNSIGNED_PAYLOAD;
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
                final String contentLength = mutableRequest.firstMatchingHeader(CONTENT_LENGTH)
                                                           .orElse(null);
                final long originalContentLength;
                if (contentLength != null) {
                    originalContentLength = Long.parseLong(contentLength);
                } else {
                    /**
                     * "Content-Length" header could be missing if the caller is
                     * uploading a stream without setting Content-Length in
                     * ObjectMetadata. Before using sigv4, we rely on HttpClient to
                     * add this header by using BufferedHttpEntity when creating the
                     * HttpRequest object. But now, we need this information
                     * immediately for the signing process, so we have to cache the
                     * stream here.
                     */
                    try {
                        originalContentLength = getContentLength(mutableRequest);
                    } catch (IOException e) {
                        throw SdkClientException.builder()
                                                .message("Cannot get the content-length of the request content.")
                                                .cause(e)
                                                .build();
                    }
                }
                mutableRequest.putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength));
                // Make sure "Content-Length" header is not empty so that HttpClient
                // won't cache the stream again to recover Content-Length
                mutableRequest.putHeader(CONTENT_LENGTH, Long.toString(
                    AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength)));
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
         * If we aren't using https we should always sign the payload.
         */
        if (!request.protocol().equals("https")) {
            return true;
        }

        Boolean isPayloadSigningEnabled = signerParams.enablePayloadSigning();
        return isPayloadSigningEnabled != null && isPayloadSigningEnabled;
    }

    /**
     * Read the content of the request to get the length of the stream. This
     * method will wrap the stream by SdkBufferedInputStream if it is not
     * mark-supported.
     */
    private static long getContentLength(SdkHttpFullRequest.Builder requestBuilder) throws IOException {
        final InputStream content = requestBuilder.content();
        validState(content.markSupported(), "Request input stream must have been made mark-and-resettable");

        long contentLength = 0;
        byte[] tmp = new byte[4096];
        int read;
        content.mark(getReadLimit());
        while ((read = content.read(tmp)) != -1) {
            contentLength += read;
        }
        try {
            content.reset();
        } catch (IOException ex) {
            throw ResetException.builder().message("Failed to reset the input stream").cause(ex).build();
        }
        return contentLength;
    }
}