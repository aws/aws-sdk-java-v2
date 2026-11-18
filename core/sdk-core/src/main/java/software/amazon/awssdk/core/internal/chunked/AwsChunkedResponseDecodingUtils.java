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

package software.amazon.awssdk.core.internal.chunked;

import static software.amazon.awssdk.core.HttpChecksumConstant.AWS_CHUNKED_HEADER;
import static software.amazon.awssdk.core.HttpChecksumConstant.X_AMZ_TRAILER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Header handling for aws-chunked response decoding, shared by the sync and async response paths.
 */
@SdkInternalApi
public final class AwsChunkedResponseDecodingUtils {

    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String DECODED_CONTENT_LENGTH = "x-amz-decoded-content-length";

    private AwsChunkedResponseDecodingUtils() {
    }

    /**
     * Returns whether the response body must be aws-chunked decoded: the operation supports it and {@code aws-chunked} is
     * the last {@code Content-Encoding} value.
     *
     * @throws SdkClientException if {@code aws-chunked} appears more than once or is not the last encoding
     */
    public static boolean shouldDecode(SdkHttpResponse response, ExecutionAttributes executionAttributes) {
        if (!Boolean.TRUE.equals(executionAttributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING))) {
            return false;
        }
        List<String> encodings = contentEncodings(response);
        long awsChunkedCount = encodings.stream().filter(AwsChunkedResponseDecodingUtils::isAwsChunked).count();
        if (awsChunkedCount == 0) {
            return false;
        }
        if (awsChunkedCount > 1 || !isAwsChunked(encodings.get(encodings.size() - 1))) {
            throw malformed("aws-chunked must appear exactly once, as the last Content-Encoding value");
        }
        return true;
    }

    /**
     * Returns the {@code x-amz-decoded-content-length} value.
     *
     * @throws SdkClientException if the header is missing or not a non-negative integer
     */
    public static long decodedContentLength(SdkHttpResponse response) {
        String value = response.firstMatchingHeader(DECODED_CONTENT_LENGTH)
                               .orElseThrow(() -> malformed(DECODED_CONTENT_LENGTH + " header is missing"));
        try {
            long length = Long.parseLong(value.trim());
            if (length >= 0) {
                return length;
            }
        } catch (NumberFormatException e) {
            // Fall through
        }
        throw malformed(DECODED_CONTENT_LENGTH + " header is not a valid non-negative integer");
    }

    /**
     * Returns the lower-case trailer names declared in {@code x-amz-trailer}, or an empty set if the header is absent.
     */
    public static Set<String> declaredTrailers(SdkHttpResponse response) {
        Set<String> names = new HashSet<>();
        for (String header : response.matchingHeaders(X_AMZ_TRAILER)) {
            for (String name : header.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /**
     * Rewrites the headers that describe the aws-chunked framing so they describe the decoded body:
     * {@code Content-Length} becomes the decoded length, {@code aws-chunked} is removed from {@code Content-Encoding}
     * (removing the header if nothing remains), and {@code x-amz-decoded-content-length} and {@code x-amz-trailer} are
     * removed.
     */
    public static SdkHttpFullResponse rewriteHeaders(SdkHttpFullResponse response, long decodedContentLength) {
        SdkHttpFullResponse.Builder builder = response.toBuilder();
        rewriteHeaders(builder, contentEncodings(response), decodedContentLength);
        return builder.build();
    }

    /**
     * Same as {@link #rewriteHeaders(SdkHttpFullResponse, long)}, for the async path where the body arrives separately.
     */
    public static SdkHttpResponse rewriteHeaders(SdkHttpResponse response, long decodedContentLength) {
        SdkHttpResponse.Builder builder = response.toBuilder();
        rewriteHeaders(builder, contentEncodings(response), decodedContentLength);
        return builder.build();
    }

    private static void rewriteHeaders(SdkHttpResponse.Builder builder, List<String> encodings, long decodedContentLength) {
        encodings.removeIf(AwsChunkedResponseDecodingUtils::isAwsChunked);
        builder.removeHeader(CONTENT_ENCODING)
               .removeHeader(DECODED_CONTENT_LENGTH)
               .removeHeader(X_AMZ_TRAILER)
               .putHeader(CONTENT_LENGTH, Long.toString(decodedContentLength));
        if (!encodings.isEmpty()) {
            builder.putHeader(CONTENT_ENCODING, String.join(",", encodings));
        }
    }

    /**
     * Returns the trailers from a completed trailers future.
     *
     * @throws IllegalStateException if the future has not completed successfully
     */
    public static Map<String, String> completedTrailers(CompletableFuture<Map<String, String>> trailersFuture) {
        if (!trailersFuture.isDone() || trailersFuture.isCompletedExceptionally()) {
            throw new IllegalStateException("Trailers are only available after the stream has been fully decoded");
        }
        return trailersFuture.join();
    }

    private static List<String> contentEncodings(SdkHttpResponse response) {
        List<String> encodings = new ArrayList<>();
        for (String header : response.matchingHeaders(CONTENT_ENCODING)) {
            for (String encoding : header.split(",")) {
                String trimmed = encoding.trim();
                if (!trimmed.isEmpty()) {
                    encodings.add(trimmed);
                }
            }
        }
        return encodings;
    }

    private static boolean isAwsChunked(String encoding) {
        return AWS_CHUNKED_HEADER.equalsIgnoreCase(encoding);
    }

    private static SdkClientException malformed(String message) {
        return SdkClientException.builder().message("Malformed aws-chunked response: " + message).build();
    }
}
