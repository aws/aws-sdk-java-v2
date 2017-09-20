/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import static software.amazon.awssdk.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.utils.Validate.validState;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.auth.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * AWS4 signer implementation for AWS S3
 */
public class AwsS3V4Signer extends Aws4Signer {
    private static final String CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

    /**
     * Sent to S3 in lieu of a payload hash when unsigned payloads are enabled
     */
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private static final String CONTENT_LENGTH = "Content-Length";

    private Boolean disableChunkedEncoding;

    private Boolean enablePayloadSigning;

    /**
     * Don't double-url-encode path elements; S3 expects path elements to be encoded only once in
     * the canonical URI.
     */
    public AwsS3V4Signer() {
        super(false);
    }

    /**
     * If necessary, creates a chunk-encoding wrapper on the request payload.
     */
    @Override
    protected void processRequestPayload(SdkHttpFullRequest.Builder requestBuilder,
                                         byte[] signature, byte[] signingKey,
                                         Aws4SignerRequestParams signerRequestParams) {
        if (useChunkEncoding(signerRequestParams)) {
            AwsChunkedEncodingInputStream chunkEncodededStream = new AwsChunkedEncodingInputStream(
                    signerRequestParams.httpRequest().getContent(), signingKey,
                    signerRequestParams.getFormattedSigningDateTime(),
                    signerRequestParams.getScope(),
                    BinaryUtils.toHex(signature), this);
            requestBuilder.content(chunkEncodededStream);
        }
    }

    @Override
    protected String calculateContentHashPresign(Aws4SignerRequestParams signerRequestParams,
                                                 SdkHttpFullRequest.Builder mutableRequest) {
        return "UNSIGNED-PAYLOAD";
    }

    /**
     * Returns the pre-defined header value and set other necessary headers if
     * the request needs to be chunk-encoded. Otherwise calls the superclass
     * method which calculates the hash of the whole content for signing.
     */
    @Override
    protected String calculateContentHash(Aws4SignerRequestParams signerRequestParams,
                                          SdkHttpFullRequest.Builder mutableRequest) {
        // To be consistent with other service clients using sig-v4,
        // we just set the header as "required", and AWS4Signer.sign() will be
        // notified to pick up the header value returned by this method.
        mutableRequest.header(X_AMZ_CONTENT_SHA256, "required");

        SdkHttpFullRequest requestToSign = signerRequestParams.httpRequest();

        if (isPayloadSigningEnabled(requestToSign)) {
            if (useChunkEncoding(signerRequestParams)) {
                final String contentLength = requestToSign.getFirstHeaderValue(CONTENT_LENGTH).orElse(null);
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
                        originalContentLength = getContentLength(signerRequestParams);
                    } catch (IOException e) {
                        throw new SdkClientException("Cannot get the content-length of the request content.", e);
                    }
                }
                mutableRequest.header("x-amz-decoded-content-length", Long.toString(originalContentLength));
                // Make sure "Content-Length" header is not empty so that HttpClient
                // won't cache the stream again to recover Content-Length
                mutableRequest.header(CONTENT_LENGTH, Long.toString(
                        AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength)));
                return CONTENT_SHA_256;
            } else {
                return super.calculateContentHash(signerRequestParams, mutableRequest);
            }
        }

        return UNSIGNED_PAYLOAD;
    }

    /**
     * Determine whether to use aws-chunked for signing
     */
    private boolean useChunkEncoding(Aws4SignerRequestParams signerRequestParams) {
        // If chunked encoding is explicitly disabled through client options return right here.
        // Chunked encoding only makes sense to do when the payload is signed
        if (!isPayloadSigningEnabled(signerRequestParams.httpRequest()) ||
            isChunkedEncodingDisabled()) {
            return false;
        }

        return signerRequestParams.originalRequest() instanceof PutObjectRequest ||
               signerRequestParams.originalRequest() instanceof UploadPartRequest;
    }

    /**
     * @return True if chunked encoding has been explicitly disabled per the request. False
     * otherwise.
     */
    private boolean isChunkedEncodingDisabled() {
        return disableChunkedEncoding != null && disableChunkedEncoding;
    }

    /**
     * @return True if payload signing is explicitly enabled.
     */
    private boolean isPayloadSigningEnabled(SdkHttpRequest request) {
        /**
         * If we aren't using https we should always sign the payload.
         */
        if (!request.getEndpoint().getScheme().equals("https")) {
            return true;
        }

        return enablePayloadSigning != null && enablePayloadSigning;
    }

    /**
     * Read the content of the request to get the length of the stream. This
     * method will wrap the stream by SdkBufferedInputStream if it is not
     * mark-supported.
     */
    private static long getContentLength(Aws4SignerRequestParams signerParams) throws IOException {
        final InputStream content = signerParams.httpRequest().getContent();
        validState(content.markSupported(), "Request input stream must have been made mark-and-resettable");

        long contentLength = 0;
        byte[] tmp = new byte[4096];
        int read;
        content.mark(getReadLimit(signerParams));
        while ((read = content.read(tmp)) != -1) {
            contentLength += read;
        }
        try {
            content.reset();
        } catch (IOException ex) {
            throw new ResetException("Failed to reset the input stream", ex);
        }
        return contentLength;
    }

    @ReviewBeforeRelease("Create a builder for signers and set this in it. Also currently unexercised")
    public void setDisableChunkedEncoding(boolean disableChunkedEncoding) {
        this.disableChunkedEncoding = disableChunkedEncoding;
    }

    @ReviewBeforeRelease("Create a builder for signers and set this in it. Also currently unexercised.")
    public void setEnablePayloadSigning(boolean enablePayloadSigning) {
        this.enablePayloadSigning = enablePayloadSigning;
    }
}