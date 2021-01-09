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

package software.amazon.awssdk.core.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.util.Optional;
import java.util.zip.GZIPInputStream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.util.Crc32ChecksumValidatingInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Validate and decompress input data if necessary.
 */
@SdkProtectedApi
public final class Crc32Validation {

    private Crc32Validation() {
    }

    public static SdkHttpFullResponse validate(boolean calculateCrc32FromCompressedData,
                                               SdkHttpFullResponse httpResponse) {

        if (!httpResponse.content().isPresent()) {
            return httpResponse;
        }

        return httpResponse.toBuilder().content(
            process(calculateCrc32FromCompressedData, httpResponse,
                    httpResponse.content().get())).build();
    }

    private static AbortableInputStream process(boolean calculateCrc32FromCompressedData,
                                                SdkHttpFullResponse httpResponse,
                                                AbortableInputStream content) {
        Optional<Long> crc32Checksum = getCrc32Checksum(httpResponse);

        if (shouldDecompress(httpResponse)) {
            if (calculateCrc32FromCompressedData && crc32Checksum.isPresent()) {
                return decompressing(crc32Validating(content, crc32Checksum.get()));
            }

            if (crc32Checksum.isPresent()) {
                return crc32Validating(decompressing(content), crc32Checksum.get());
            }

            return decompressing(content);

        }

        return crc32Checksum.map(aLong -> crc32Validating(content, aLong)).orElse(content);
    }

    private static AbortableInputStream crc32Validating(AbortableInputStream source, long expectedChecksum) {
        return AbortableInputStream.create(new Crc32ChecksumValidatingInputStream(source, expectedChecksum), source);
    }

    private static Optional<Long> getCrc32Checksum(SdkHttpFullResponse httpResponse) {
        return httpResponse.firstMatchingHeader("x-amz-crc32")
                           .map(Long::valueOf);
    }

    private static boolean shouldDecompress(SdkHttpFullResponse httpResponse) {
        return httpResponse.firstMatchingHeader("Content-Encoding")
                           .filter(e -> e.equals("gzip"))
                           .isPresent();
    }

    private static AbortableInputStream decompressing(AbortableInputStream source) {
        return AbortableInputStream.create(invokeSafely(() -> new GZIPInputStream(source)), source);
    }
}
