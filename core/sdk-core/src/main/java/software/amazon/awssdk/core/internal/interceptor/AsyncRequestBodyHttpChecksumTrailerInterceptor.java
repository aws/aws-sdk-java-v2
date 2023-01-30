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

package software.amazon.awssdk.core.internal.interceptor;

import static software.amazon.awssdk.core.HttpChecksumConstant.DEFAULT_ASYNC_CHUNK_SIZE;
import static software.amazon.awssdk.core.internal.io.AwsUnsignedChunkedEncodingInputStream.calculateStreamContentLength;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.async.ChecksumCalculatingAsyncRequestBody;
import software.amazon.awssdk.core.internal.util.ChunkContentUtils;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Interceptor to add Flexible Checksum to Trailers for Async request body.
 * The Checksum will be added by this interceptor only if following condition is met
 * <ol>
 *     <li>If Checksum was not already added in the headers.</li>
 *     <li>If Input request has streaming payload.</li>
 *     <li>If Input Request checksum algorithm is specified.</li>
 *     <li>If the request is Unsigned request.</li>
 * </ol>
 */
@SdkInternalApi
public final class AsyncRequestBodyHttpChecksumTrailerInterceptor implements ExecutionInterceptor {

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {
        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);

        if (checksumSpecs == null ||
            checksumSpecs.headerName() == null ||
            !HttpChecksumUtils
                .isTrailerBasedChecksumForClientType(
                    executionAttributes,
                    context.httpRequest(),
                    ClientType.ASYNC, checksumSpecs,
                    context.asyncRequestBody().isPresent(),
                    context.requestBody().map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false))) {
            return context.asyncRequestBody();
        }

        return context.asyncRequestBody().isPresent() ?
               Optional.of(ChecksumCalculatingAsyncRequestBody.builder()
                                                              .asyncRequestBody(context.asyncRequestBody().get())
                                                              .algorithm(checksumSpecs.algorithm())
                                                              .trailerHeader(checksumSpecs.headerName()).build())
                                                      : context.asyncRequestBody();

    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        ChecksumSpecs checksumSpecs =
            HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);
        if (checksumSpecs == null ||
            checksumSpecs.headerName() == null ||
            !HttpChecksumUtils
                .isTrailerBasedChecksumForClientType(
                    executionAttributes,
                    context.httpRequest(),
                    ClientType.ASYNC, checksumSpecs,
                    context.asyncRequestBody().isPresent(),
                    context.requestBody().map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false))) {
            return context.httpRequest();
        }
        long checksumContentLength = ChunkContentUtils.calculateChecksumContentLength(
            checksumSpecs.algorithm(), checksumSpecs.headerName());
        long originalContentLength = context.asyncRequestBody().isPresent() ?
                                     context.asyncRequestBody().get().contentLength().orElse(0L) : 0;
        return updateHeadersForTrailerChecksum(context, checksumSpecs, checksumContentLength, originalContentLength);
    }

    private static SdkHttpRequest updateHeadersForTrailerChecksum(Context.ModifyHttpRequest context, ChecksumSpecs checksum,
                                                                  long checksumContentLength, long originalContentLength) {

        long chunkLength =
            calculateStreamContentLength(originalContentLength, DEFAULT_ASYNC_CHUNK_SIZE);

        return context.httpRequest().copy(r ->
                r.putHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, checksum.headerName())
                        .appendHeader("Content-encoding", HttpChecksumConstant.AWS_CHUNKED_HEADER)
                        .putHeader("x-amz-content-sha256", HttpChecksumConstant.CONTENT_SHA_256_FOR_UNSIGNED_TRAILER)
                        .putHeader("x-amz-decoded-content-length", Long.toString(originalContentLength))
                        .putHeader(Header.CONTENT_LENGTH,
                                   Long.toString(chunkLength + checksumContentLength)));
    }
}