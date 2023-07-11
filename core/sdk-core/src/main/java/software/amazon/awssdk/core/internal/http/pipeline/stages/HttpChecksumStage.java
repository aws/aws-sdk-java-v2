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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.HttpChecksumConstant.AWS_CHUNKED_HEADER;
import static software.amazon.awssdk.core.HttpChecksumConstant.CONTENT_SHA_256_FOR_UNSIGNED_TRAILER;
import static software.amazon.awssdk.core.HttpChecksumConstant.DEFAULT_ASYNC_CHUNK_SIZE;
import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.internal.io.AwsChunkedEncodingInputStream.DEFAULT_CHUNK_SIZE;
import static software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream.calculateStreamContentLength;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.calculateChecksumContentLength;
import static software.amazon.awssdk.core.internal.util.HttpChecksumResolver.getResolvedChecksumSpecs;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.async.ChecksumCalculatingAsyncRequestBody;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * Stage to implement the "httpChecksum" and "httpChecksumRequired" C2J traits, and flexible checksums.
 */
@SdkInternalApi
public class HttpChecksumStage implements MutableRequestToRequestPipeline {

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        ChecksumSpecs checksumSpecs = getResolvedChecksumSpecs(context.executionAttributes());
        httpChecksumRequired(request, context);
        httpChecksumInTrailer(request, context, ClientType.SYNC, checksumSpecs);
        httpChecksumInTrailer(request, context, ClientType.ASYNC, checksumSpecs);
        httpChecksumInHeader(request, context, checksumSpecs);
        return request;
    }

    /**
     * Implements the "httpChecksumRequired" C2J trait. Operations with that trait applied will automatically include a
     * "Content-MD5" header, containing a checksum of the payload.
     *
     * <p>This is NOT supported for asynchronous HTTP content, which is currently only used for streaming upload operations.
     * If such operations are added in the future, we'll have to find a way to support them in a non-blocking manner. That will
     * likely require interface changes of some sort, because it's not currently possible to do a non-blocking update to
     * request headers.
     *
     * <p>
     * Calculates the MD5 checksum of the provided request (and base64 encodes it), and adds the header to the request.
     *
     * <p>Note: This assumes that the content stream provider can create multiple new streams. If it only supports one (e.g. with
     * an input stream that doesn't support mark/reset), we could consider buffering the content in memory here and updating the
     * request body to use that buffered content. We obviously don't want to do that for giant streams, so we haven't opted to do
     * that yet.
     */
    private void httpChecksumRequired(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        boolean isHttpChecksumRequired = isHttpChecksumRequired(context.executionAttributes());
        boolean requestAlreadyHasMd5 = request.firstMatchingHeader(Header.CONTENT_MD5).isPresent();

        if (!isHttpChecksumRequired || requestAlreadyHasMd5) {
            return;
        }
        if (context.requestProvider() != null) {
            throw new IllegalArgumentException("This operation requires a content-MD5 checksum, but one cannot be calculated "
                                               + "for non-blocking content.");
        }
        if (context.executionContext().interceptorContext().requestBody().isPresent()) {
            try {
                String payloadMd5 = Md5Utils.md5AsBase64(request.contentStreamProvider().newStream());
                request.putHeader(Header.CONTENT_MD5, payloadMd5);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Adds flexible checksum to trailers.
     *
     * <p>The flexible checksum is added only if the following conditions are met:
     * <ol>
     *     <li>Checksum is not already calculated.</li>
     *     <li>Unsigned payload.</li>
     *     <li>Request has streaming payload.</li>
     *     <li>Request has the algorithm checksum mentioned.</li>
     * </ol>
     */
    private void httpChecksumInTrailer(SdkHttpFullRequest.Builder request, RequestExecutionContext context,
                                       ClientType clientType, ChecksumSpecs checksumSpecs) {
        if (!shouldAddTrailerBasedChecksumInRequest(context.executionContext().interceptorContext(),
                                                    context.executionContext().executionAttributes(),
                                                    checksumSpecs, clientType)) {
            return;
        }

        long originalContentLength = 0;
        int chunkSize = 0;
        if (clientType.equals(ClientType.SYNC)) {
            request.contentStreamProvider(new ChecksumCalculatingStreamProvider(request.contentStreamProvider(), checksumSpecs));
            originalContentLength =
                context.executionContext().interceptorContext().requestBody().get().optionalContentLength().orElse(0L);
            chunkSize = DEFAULT_CHUNK_SIZE;
        } else if (clientType.equals(ClientType.ASYNC)) {
            if (context.requestProvider() != null) {
                context.requestProvider(ChecksumCalculatingAsyncRequestBody.builder()
                                                                           .asyncRequestBody(context.requestProvider())
                                                                           .algorithm(checksumSpecs.algorithm())
                                                                           .trailerHeader(checksumSpecs.headerName()).build());
                originalContentLength =
                    context.executionContext().interceptorContext().asyncRequestBody().get().contentLength().orElse(0L);
                chunkSize = DEFAULT_ASYNC_CHUNK_SIZE;
            }
        }

        long checksumContentLength = calculateChecksumContentLength(checksumSpecs.algorithm(), checksumSpecs.headerName());
        long contentLen = checksumContentLength + calculateStreamContentLength(originalContentLength, chunkSize);

        request.putHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, checksumSpecs.headerName())
               .appendHeader("Content-encoding", AWS_CHUNKED_HEADER)
               .putHeader("x-amz-content-sha256", CONTENT_SHA_256_FOR_UNSIGNED_TRAILER)
               .putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength))
               .putHeader(CONTENT_LENGTH, Long.toString(contentLen));
    }

    /**
     * Implements the "HttpChecksum" C2J trait for a request.
     * HttpChecksum is added in the header only in following cases:
     * <ol>
     *     <li>Non-streaming payload and Unsigned Payload </li>
     *     <li>Non-streaming payload and Header-based Signing auth</li>
     *     <li>Streaming payload and Header-based Signing auth</li>
     * </ol>
     * This stage will inject the Http checksum only for case 1 as above i.e. for unsigned payloads.
     * For the other two cases, the http checksum will be injected by the signers.
     *
     * <p>
     * Calculates the checksum of the provided request (and base64 encodes it), and adds the header to the request.
     *
     * <p>Note: This assumes that the content stream provider can create multiple new streams. If it only supports one (e.g. with
     * an input stream that doesn't support mark/reset), we could consider buffering the content in memory here and updating the
     * request body to use that buffered content. We obviously don't want to do that for giant streams, so we haven't opted to do
     * that yet.
     */
    private void httpChecksumInHeader(SdkHttpFullRequest.Builder request, RequestExecutionContext context,
                                      ChecksumSpecs checksumSpecs) {
        Optional<RequestBody> syncContent = context.executionContext().interceptorContext().requestBody();
        if (shouldSkipHttpChecksumInHeader(context.executionContext().interceptorContext(), context.executionAttributes(),
                                           checksumSpecs) || !syncContent.isPresent()) {
            return;
        }

        try {
            String payloadChecksum = BinaryUtils.toBase64(HttpChecksumUtils.computeChecksum(
                syncContent.get().contentStreamProvider().newStream(), checksumSpecs.algorithm()));
            request.putHeader(checksumSpecs.headerName(), payloadChecksum);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isHttpChecksumRequired(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED) != null
               || HttpChecksumUtils.isMd5ChecksumRequired(executionAttributes);
    }

    private boolean shouldSkipHttpChecksumInHeader(InterceptorContext context, ExecutionAttributes executionAttributes,
                                                   ChecksumSpecs headerChecksumSpecs) {
        return headerChecksumSpecs == null ||
               headerChecksumSpecs.algorithm() == null ||
               HttpChecksumUtils.isHttpChecksumPresent(context.httpRequest(), headerChecksumSpecs) ||
               !HttpChecksumUtils.isUnsignedPayload(
                   executionAttributes.getAttribute(SIGNING_METHOD),
                   context.httpRequest().protocol(),
                   context.requestBody().map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false)) ||
               headerChecksumSpecs.isRequestStreaming();
    }

    private static boolean shouldAddTrailerBasedChecksumInRequest(InterceptorContext context,
                                                                  ExecutionAttributes executionAttributes,
                                                                  ChecksumSpecs checksumSpecs, ClientType clientType) {
        boolean hasRequestBody = true;
        if (clientType.equals(ClientType.SYNC)) {
            hasRequestBody = context.requestBody().isPresent();
        } else if (clientType.equals(ClientType.ASYNC)) {
            hasRequestBody = context.asyncRequestBody().isPresent();
        }

        return checksumSpecs != null
               && checksumSpecs.headerName() != null
               && HttpChecksumUtils.isTrailerBasedChecksumForClientType(
                   executionAttributes,
                   context.httpRequest(),
                   clientType, checksumSpecs,
                   hasRequestBody,
                   context.requestBody().map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false));
    }

    static final class ChecksumCalculatingStreamProvider implements ContentStreamProvider {
        private final ContentStreamProvider underlyingInputStreamProvider;
        private final String checksumHeaderForTrailer;
        private final ChecksumSpecs checksumSpecs;
        private InputStream currentStream;
        private SdkChecksum sdkChecksum;

        ChecksumCalculatingStreamProvider(ContentStreamProvider underlyingInputStreamProvider,
                                          ChecksumSpecs checksumSpecs) {
            this.underlyingInputStreamProvider = underlyingInputStreamProvider;
            this.sdkChecksum = SdkChecksum.forAlgorithm(checksumSpecs.algorithm());
            this.checksumHeaderForTrailer = checksumSpecs.headerName();
            this.checksumSpecs = checksumSpecs;
        }

        @Override
        public InputStream newStream() {
            closeCurrentStream();
            currentStream = AwsUnsignedChunkedEncodingInputStream.builder()
                                                                 .inputStream(underlyingInputStreamProvider.newStream())
                                                                 .sdkChecksum(sdkChecksum)
                                                                 .checksumHeaderForTrailer(checksumHeaderForTrailer)
                                                                 .build();
            return currentStream;
        }

        private void closeCurrentStream() {
            sdkChecksum = SdkChecksum.forAlgorithm(checksumSpecs.algorithm());
            if (currentStream != null) {
                IoUtils.closeQuietly(currentStream, null);
                currentStream = null;
            }
        }
    }
}
