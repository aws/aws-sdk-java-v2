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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import software.amazon.awssdk.util.Crc32ChecksumValidatingInputStream;

/**
 * Adapts a {@link SdkHttpFullResponse} object to the legacy {@link HttpResponse}.
 *
 * TODO this should eventually be removed and SdkHttpFullResponse should completely replace HttpResponse
 */
public class SdkHttpResponseAdapter {

    public static HttpResponse adapt(boolean calculateCrc32FromCompressedData,
                                     SdkHttpFullRequest request,
                                     SdkHttpFullResponse awsHttpResponse) {
        final HttpResponse httpResponse = new HttpResponse(request, awsHttpResponse.getContent());
        httpResponse.setStatusCode(awsHttpResponse.getStatusCode());
        httpResponse.setStatusText(awsHttpResponse.getStatusText());

        // Legacy HttpResponse only supports a single value for a header
        awsHttpResponse.getHeaders()
                       .forEach((k, v) -> httpResponse.addHeader(k, v.get(0)));

        httpResponse.setContent(getContent(calculateCrc32FromCompressedData, awsHttpResponse, httpResponse));

        return httpResponse;
    }

    private static InputStream crc32Validating(InputStream source, long expectedChecksum) {
        return new Crc32ChecksumValidatingInputStream(source, expectedChecksum);
    }

    private static InputStream getContent(boolean calculateCrc32FromCompressedData,
                                          SdkHttpFullResponse awsHttpResponse,
                                          HttpResponse httpResponse) {
        final Optional<Long> crc32Checksum = getCrc32Checksum(httpResponse);
        if (shouldDecompress(httpResponse)) {
            if (calculateCrc32FromCompressedData && crc32Checksum.isPresent()) {
                return decompressing(crc32Validating(awsHttpResponse.getContent(), crc32Checksum.get()));
            } else if (crc32Checksum.isPresent()) {
                return crc32Validating(decompressing(awsHttpResponse.getContent()), crc32Checksum.get());
            } else {
                return decompressing(awsHttpResponse.getContent());
            }
        } else if (crc32Checksum.isPresent()) {
            return crc32Validating(awsHttpResponse.getContent(), crc32Checksum.get());
        }
        return awsHttpResponse.getContent();
    }

    private static Optional<Long> getCrc32Checksum(HttpResponse httpResponse) {
        return Optional.ofNullable(httpResponse.getHeader("x-amz-crc32"))
                       .map(Long::valueOf);
    }

    private static boolean shouldDecompress(HttpResponse httpResponse) {
        return "gzip".equals(httpResponse.getHeader("Content-Encoding"));
    }

    private static InputStream decompressing(InputStream source) {
        return invokeSafely(() -> new GZIPInputStream(source));
    }
}
