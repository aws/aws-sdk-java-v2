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
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.io.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream;
import software.amazon.awssdk.core.internal.util.HttpChecksumResolver;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Adds flexible checksum to the content trailer for sync clients.
 * The flexible checksum is added only if the following conditions are met:
 * <p>
 * 1. Checksum is not already calculated.
 * 2. Unsigned payload.
 * 3. Request has streaming payload.
 * 4. If Request has Algorithm checksum mentioned.
 * <p/>
 *
 */
@SdkInternalApi
public class SyncHttpChecksumInTrailerStage implements MutableRequestToRequestPipeline {
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context)
            throws Exception {
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(context.executionAttributes()).orElse(null);

        if (!shouldAddTrailerBasedChecksumInRequest(context.executionContext().interceptorContext(),
                                                    context.executionContext().executionAttributes(),
                                                    checksumSpecs)) {
            return input;
        }

        input.contentStreamProvider(new ChecksumCalculatingStreamProvider(input.contentStreamProvider(), checksumSpecs));

        ChecksumSpecs checksum = HttpChecksumResolver.getResolvedChecksumSpecs(context.executionAttributes());
        long checksumContentLength = AwsUnsignedChunkedEncodingInputStream.calculateChecksumContentLength(
            checksum.algorithm(), checksum.headerName());
        long originalContentLength =
            context.executionContext().interceptorContext().requestBody().get().optionalContentLength().orElse(0L);

        return updateHeadersForTrailerChecksum(input, checksum, checksumContentLength, originalContentLength);
    }

    private static SdkHttpFullRequest.Builder updateHeadersForTrailerChecksum(SdkHttpFullRequest.Builder input,
                                                                              ChecksumSpecs checksum,
                                                                              long checksumContentLength,
                                                                              long originalContentLength) {
        return input.putHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, checksum.headerName())
                    .appendHeader("Content-encoding", AWS_CHUNKED_HEADER)
                    .putHeader("x-amz-content-sha256", CONTENT_SHA_256_FOR_UNSIGNED_TRAILER)
                    .putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength))
                    .putHeader(CONTENT_LENGTH,
                               Long.toString(AwsUnsignedChunkedEncodingInputStream.calculateStreamContentLength(
                                   originalContentLength, AwsChunkedEncodingInputStream.DEFAULT_CHUNK_SIZE)
                                             + checksumContentLength));
    }

    private static boolean shouldAddTrailerBasedChecksumInRequest(InterceptorContext context,
                                                                  ExecutionAttributes executionAttributes,
                                                                  ChecksumSpecs checksumSpecs) {
        return checksumSpecs != null
               && context.requestBody().isPresent()
               && HttpChecksumUtils.isTrailerBasedChecksumForClientType(
            executionAttributes,
            context.httpRequest(),
            ClientType.SYNC, checksumSpecs,
            context.requestBody().isPresent(),
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
