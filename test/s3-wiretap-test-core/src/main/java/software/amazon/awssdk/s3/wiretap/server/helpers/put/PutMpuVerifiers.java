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

package software.amazon.awssdk.s3.wiretap.server.helpers.put;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;

public final class PutMpuVerifiers {

    private static final long QUIESCENCE_IDLE_MILLIS = 500;
    private static final long QUIESCENCE_TIMEOUT_MILLIS = 15_000;

    private PutMpuVerifiers() {
    }

    public static void awaitQuiescence(S3Wiretap wiretap) {
        wiretap.awaitQuiescence(QUIESCENCE_IDLE_MILLIS, QUIESCENCE_TIMEOUT_MILLIS);
    }

    public static void assertMultipartUpload(S3Wiretap wiretap, Payload payload) {
        awaitQuiescence(wiretap);
        assertMultipartOpened(wiretap, payload.size());
        assertEquals(1, wiretap.requestsMatching(S3RequestFilters.completeMultipartUpload()).size(),
                () -> "a multipart upload of " + payload.size()
                        + " bytes should be completed exactly once");
        assertPartsCover(payload, latestAttemptPerPart(wiretap));
        wiretap.clearExchanges();
    }

    public static void assertMultipartOpenedNotCompleted(S3Wiretap wiretap, int size) {
        awaitQuiescence(wiretap);
        assertMultipartOpened(wiretap, size);
        assertTrue(wiretap.requestsMatching(S3RequestFilters.completeMultipartUpload()).isEmpty(),
                () -> "an upload that could not finish must not be completed (size " + size + ")");
        wiretap.clearExchanges();
    }

    public static void assertSimplePut(S3Wiretap wiretap, Payload payload, int attempts) {
        awaitQuiescence(wiretap);
        int size = payload.size();
        assertTrue(wiretap.requestsMatching(S3RequestFilters.createMultipartUpload()).isEmpty(),
                () -> "a simple PutObject of " + size
                        + " bytes must not open a CreateMultipartUpload");

        List<CapturedRequest> puts = wiretap.requestsMatching(S3RequestFilters.simplePut());
        assertEquals(attempts, puts.size(),
                () -> "expected " + attempts + " simple PUT attempt(s) for " + size
                        + " bytes, saw " + puts.size());
        for (CapturedRequest put : puts) {
            assertEquals(size, put.fullBodyLength(), "each PUT attempt must carry the full payload");
            assertArrayEquals(payload.sha256(), put.fullBodyDigest(),
                    "each PUT attempt body must equal the payload");
        }
        wiretap.clearExchanges();
    }

    public static void assertPartsCover(Payload payload, List<CapturedRequest> parts) {
        int offset = 0;
        for (CapturedRequest part : parts) {
            int len = Math.toIntExact(part.fullBodyLength());
            int partOffset = offset;
            assertArrayEquals(payload.sha256(partOffset, len), part.fullBodyDigest(),
                    () -> "part at offset " + partOffset + " (len " + len
                            + ") bytes must equal the payload's byte range");
            offset += len;
        }
        assertEquals(payload.size(), offset, "the parts must together cover the whole payload");
    }

    public static void assertPartsAtSize(Payload payload, List<CapturedRequest> parts, int partSize) {
        int size = payload.size();
        int expectedParts = (size + partSize - 1) / partSize;
        assertEquals(expectedParts, parts.size(),
                () -> "a " + size + "-byte payload at a " + partSize + "-byte part size should be "
                        + expectedParts + " parts, saw " + parts.size() + ": " + partLengths(parts));
        for (int i = 0; i < parts.size(); i++) {
            long expected = Math.min((long) partSize, size - (long) i * partSize);
            int at = i;
            assertEquals(expected, parts.get(i).fullBodyLength(),
                    () -> "part " + (at + 1) + " of " + size + " bytes should be " + expected
                            + " bytes on a " + partSize + " grid, saw " + partLengths(parts));
        }
    }

    private static List<Long> partLengths(List<CapturedRequest> parts) {
        return parts.stream().map(CapturedRequest::fullBodyLength).toList();
    }

    public static List<CapturedRequest> latestAttemptPerPart(S3Wiretap wiretap) {
        Map<Integer, CapturedRequest> byPart = new LinkedHashMap<>();
        for (CapturedRequest part : wiretap.requestsMatching(S3RequestFilters.uploadPart())) {
            byPart.put(Integer.parseInt(part.queryParameter("partNumber")), part);
        }
        List<CapturedRequest> parts = new ArrayList<>(byPart.values());
        parts.sort(Comparator.comparingInt(r -> Integer.parseInt(r.queryParameter("partNumber"))));
        return parts;
    }

    private static void assertMultipartOpened(S3Wiretap wiretap, int size) {
        assertEquals(1, wiretap.requestsMatching(S3RequestFilters.createMultipartUpload()).size(),
                () -> "size " + size + " should open exactly one CreateMultipartUpload, saw "
                        + wiretap.requestsMatching(S3RequestFilters.createMultipartUpload()).size());
        assertTrue(wiretap.requestsMatching(S3RequestFilters.simplePut()).isEmpty(),
                () -> "a multipart upload must not also send a simple PutObject (size " + size + ")");
    }
}
