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

package software.amazon.awssdk.s3.wiretap.testarchive.helpers;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.s3.wiretap.client.feature.BooleanFeature.State;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;

public final class PutChecksumProbe {

    public static final String KEY = "put-checksum-test";

    public static final String PAYLOAD_TEXT = "hello from wiretap durability checksum";

    public static final String PAYLOAD_CRC32 = Payload.ofString(PAYLOAD_TEXT).crc32Base64();

    private static final Payload PAYLOAD = Payload.ofString(PAYLOAD_TEXT);

    private PutChecksumProbe() {
    }

    public static byte[] payload() {
        return PAYLOAD.toByteArray();
    }

    public enum Placement { TRAILER, HEADER, ABSENT }

    public static Predicate<CapturedRequest> isPut() {
        return S3RequestFilters.put();
    }

    public static Placement classify(CapturedRequest put) {
        Optional<String> trailer = first(put.trailingHeaders().get("x-amz-checksum-crc32"));
        Optional<String> header = first(put.headers().get("x-amz-checksum-crc32"));
        if (trailer.isPresent()) {
            requireCorrect(trailer.get(), "trailer");
            return Placement.TRAILER;
        }
        if (header.isPresent()) {
            requireCorrect(header.get(), "header");
            return Placement.HEADER;
        }
        return Placement.ABSENT;
    }

    private static void requireCorrect(String actual, String where) {
        if (!PAYLOAD_CRC32.equals(actual)) {
            throw new IllegalStateException(where + " CRC32 must equal the bytes sent: expected "
                    + PAYLOAD_CRC32 + " but was " + actual);
        }
    }

    private static Optional<String> first(List<String> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst());
    }

    public static Placement expected(RequestChecksumCalculation calc, State chunked) {
        return expected(calc, chunked, false);
    }

    public static Placement expected(RequestChecksumCalculation calc, State chunked,
            boolean explicitRequestChecksum) {
        if (explicitRequestChecksum) {
            return Placement.HEADER;
        }
        if (calc == RequestChecksumCalculation.WHEN_REQUIRED) {
            return Placement.ABSENT;
        }
        return chunked == State.FALSE ? Placement.HEADER : Placement.TRAILER;
    }
}
