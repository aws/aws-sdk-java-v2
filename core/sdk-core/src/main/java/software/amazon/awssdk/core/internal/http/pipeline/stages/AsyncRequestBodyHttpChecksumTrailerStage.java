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

import static software.amazon.awssdk.core.HttpChecksumConstant.DEFAULT_ASYNC_CHUNK_SIZE;
import static software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream.calculateStreamContentLength;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.internal.async.ChecksumCalculatingAsyncRequestBody;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.util.ChunkContentUtils;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Adds flexible checksum to trailers for async request body.
 * The checksum will be added only if following conditions are met:
 * <ol>
 *     <li>If Checksum was not already added in the headers.</li>
 *     <li>If Input request has streaming payload.</li>
 *     <li>If Input Request checksum algorithm is specified.</li>
 *     <li>If the request is Unsigned request.</li>
 * </ol>
 */
@SdkInternalApi
public class AsyncRequestBodyHttpChecksumTrailerStage implements MutableRequestToRequestPipeline {
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder input, RequestExecutionContext context)
            throws Exception {

        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(context.executionAttributes()).orElse(null);

        if (shouldNotAddTrailerBasedChecksumInAsyncRequestBody(checksumSpecs, context)) {
            return input;
        }

        long originalContentLength = 0;
        if (context.requestProvider() != null) {
            context.requestProvider(ChecksumCalculatingAsyncRequestBody.builder()
                                                                       .asyncRequestBody(context.requestProvider())
                                                                       .algorithm(checksumSpecs.algorithm())
                                                                       .trailerHeader(checksumSpecs.headerName()).build());
            originalContentLength =
                context.executionContext().interceptorContext().asyncRequestBody().get().contentLength().orElse(0L);
        }

        long checksumContentLength = ChunkContentUtils.calculateChecksumContentLength(
            checksumSpecs.algorithm(), checksumSpecs.headerName());
        return updateHeadersForTrailerChecksum(input, checksumSpecs, checksumContentLength, originalContentLength);
    }

    private static SdkHttpFullRequest.Builder updateHeadersForTrailerChecksum(SdkHttpFullRequest.Builder input,
                                                                              ChecksumSpecs checksum,
                                                                              long checksumContentLength,
                                                                              long originalContentLength) {

        long chunkLength =
            calculateStreamContentLength(originalContentLength, DEFAULT_ASYNC_CHUNK_SIZE);

        return input.putHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, checksum.headerName())
                    .appendHeader("Content-encoding", HttpChecksumConstant.AWS_CHUNKED_HEADER)
                    .putHeader("x-amz-content-sha256", HttpChecksumConstant.CONTENT_SHA_256_FOR_UNSIGNED_TRAILER)
                    .putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength))
                    .putHeader(Header.CONTENT_LENGTH, Long.toString(chunkLength + checksumContentLength));
    }

    private static boolean shouldNotAddTrailerBasedChecksumInAsyncRequestBody(ChecksumSpecs checksumSpecs,
                                                                              RequestExecutionContext context) {
        return checksumSpecs == null ||
               checksumSpecs.headerName() == null ||
               !HttpChecksumUtils
                   .isTrailerBasedChecksumForClientType(
                       context.executionAttributes(),
                       context.executionContext().interceptorContext().httpRequest(),
                       ClientType.ASYNC, checksumSpecs,
                       context.requestProvider() != null,
                       context.executionContext().interceptorContext().requestBody()
                              .map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false));
    }
}
