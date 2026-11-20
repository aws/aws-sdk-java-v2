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

package software.amazon.awssdk.s3.mockserver.server.helpers.copy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.CapturedRequest;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;
import software.amazon.awssdk.s3.mockserver.server.S3Wiretap;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.StoredObject;

public final class CopyVerifiers {

    private static final long QUIESCENCE_IDLE_MILLIS = 500;
    private static final long QUIESCENCE_TIMEOUT_MILLIS = 15_000;

    private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d+)$");
    private static final Pattern PART_BLOCK = Pattern.compile("<Part>(.*?)</Part>", Pattern.DOTALL);
    private static final Pattern PART_NUMBER = Pattern.compile("<PartNumber>(\\d+)</PartNumber>");
    private static final Pattern PART_ETAG =
            Pattern.compile("<ETag>(?:\"|&quot;)?([0-9a-fA-F]{32})(?:\"|&quot;)?</ETag>");

    private CopyVerifiers() {
    }

    public static final class PartCopy {
        private final int partNumber;
        private final long offset;
        private final long length;
        private final String ifMatch;

        public PartCopy(int partNumber, long offset, long length, String ifMatch) {
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
            this.ifMatch = ifMatch;
        }

        public int partNumber() {
            return partNumber;
        }

        public long offset() {
            return offset;
        }

        public long length() {
            return length;
        }

        public String ifMatch() {
            return ifMatch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PartCopy that = (PartCopy) o;
            return partNumber == that.partNumber
                    && offset == that.offset
                    && length == that.length
                    && Objects.equals(ifMatch, that.ifMatch);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(partNumber);
            result = 31 * result + Objects.hashCode(offset);
            result = 31 * result + Objects.hashCode(length);
            result = 31 * result + Objects.hashCode(ifMatch);
            return result;
        }

        @Override
        public String toString() {
            return "PartCopy[partNumber=" + partNumber + ", offset=" + offset + ", length=" + length + ", ifMatch=" + ifMatch
                    + "]";
        }
    }

    public static void awaitQuiescence(S3Wiretap wiretap) {
        wiretap.awaitQuiescence(QUIESCENCE_IDLE_MILLIS, QUIESCENCE_TIMEOUT_MILLIS);
    }

    public static List<PartCopy> partCopies(S3Wiretap wiretap) {
        Map<Integer, CapturedRequest> byPart = new LinkedHashMap<>();
        for (CapturedRequest part : wiretap.requestsMatching(S3RequestFilters.uploadPartCopy())) {
            int partNumber = Integer.parseInt(part.queryParameter("partNumber"));
            CapturedRequest earlier = byPart.put(partNumber, part);
            String range = single(part, CopyHeader.COPY_SOURCE_RANGE);
            if (earlier != null && !java.util.Objects.equals(range,
                    single(earlier, CopyHeader.COPY_SOURCE_RANGE))) {
                throw new AssertionError("part " + partNumber + " was copied twice with different "
                        + "ranges: " + single(earlier, CopyHeader.COPY_SOURCE_RANGE) + " and " + range);
            }
        }
        List<PartCopy> parts = new ArrayList<>();
        for (Map.Entry<Integer, CapturedRequest> entry : byPart.entrySet()) {
            String range = single(entry.getValue(), CopyHeader.COPY_SOURCE_RANGE);
            Matcher m = range == null ? null : RANGE.matcher(range);
            if (m == null || !m.matches()) {
                throw new AssertionError("part " + entry.getKey() + " carries no closed "
                        + CopyHeader.COPY_SOURCE_RANGE + ": " + range);
            }
            long first = Long.parseLong(m.group(1));
            long last = Long.parseLong(m.group(2));
            parts.add(new PartCopy(entry.getKey(), first, last - first + 1,
                    single(entry.getValue(), CopyHeader.COPY_SOURCE_IF_MATCH)));
        }
        parts.sort(Comparator.comparingInt(PartCopy::partNumber));
        return parts;
    }

    public static void assertPartCopiesTile(List<PartCopy> parts, StoredObject source, long partSize) {
        long size = source.size();
        long expectedParts = (size + partSize - 1) / partSize;
        assertEquals(expectedParts, parts.size(), () -> "a " + size + "-byte source at a " + partSize
                + "-byte part size should be copied in " + expectedParts + " parts: " + parts);
        long offset = 0;
        for (int i = 0; i < parts.size(); i++) {
            PartCopy part = parts.get(i);
            long at = offset;
            assertEquals(i + 1, part.partNumber(), () -> "part numbers run 1..N with no gap: " + parts);
            assertEquals(at, part.offset(), () -> "part " + part.partNumber()
                    + " should start where the previous one ended: " + parts);
            assertEquals(Math.min(partSize, size - at), part.length(), () -> "part "
                    + part.partNumber() + " should be cut on the " + partSize + "-byte grid: " + parts);
            offset += part.length();
        }
        assertEquals(size, offset, () -> "the parts must together copy the whole source: " + parts);
    }

    public static void assertCopiedInParts(S3Wiretap wiretap, StoredObject source, long partSize) {
        awaitQuiescence(wiretap);
        assertCount(wiretap, S3RequestFilters.createMultipartUpload(), 1, "CreateMultipartUpload");
        List<PartCopy> parts = partCopies(wiretap);
        assertPartCopiesTile(parts, source, partSize);
        assertCount(wiretap, S3RequestFilters.completeMultipartUpload(), 1, "CompleteMultipartUpload");
        CapturedRequest complete = wiretap.requestsMatching(S3RequestFilters.completeMultipartUpload())
                .get(0);
        assertCompleteListsTheParts(complete, parts, source);
        String uploadId = complete.queryParameter("uploadId");
        for (CapturedRequest part : wiretap.requestsMatching(S3RequestFilters.uploadPartCopy())) {
            assertEquals(uploadId, part.queryParameter("uploadId"),
                    () -> "every part copy belongs to the upload that was completed: " + part.queryParameters());
        }
        assertCount(wiretap, S3RequestFilters.copyObject(), 0, "CopyObject");
        assertCount(wiretap, S3RequestFilters.abortMultipartUpload(), 0, "AbortMultipartUpload");
        assertNothingElse(wiretap);
        wiretap.clearExchanges();
    }

    public static void assertCopiedInOneRequest(S3Wiretap wiretap) {
        awaitQuiescence(wiretap);
        assertCount(wiretap, S3RequestFilters.copyObject(), 1, "CopyObject");
        assertCount(wiretap, S3RequestFilters.createMultipartUpload(), 0, "CreateMultipartUpload");
        assertCount(wiretap, S3RequestFilters.uploadPartCopy(), 0, "UploadPartCopy");
        assertCount(wiretap, S3RequestFilters.completeMultipartUpload(), 0, "CompleteMultipartUpload");
        assertNothingElse(wiretap);
        wiretap.clearExchanges();
    }

    private static void assertNothingElse(S3Wiretap wiretap) {
        java.util.function.Predicate<CapturedRequest> copyTraffic = S3RequestFilters.copyObject()
                .or(S3RequestFilters.uploadPartCopy()).or(S3RequestFilters.createMultipartUpload())
                .or(S3RequestFilters.completeMultipartUpload()).or(S3RequestFilters.head());
        List<CapturedRequest> other = wiretap.requests().stream().filter(copyTraffic.negate()).collect(Immutable.toList());
        assertEquals(Immutable.listOf(), other.stream().map(r -> r.method() + " " + r.path() + " "
                + r.queryParameters()).collect(Immutable.toList()), "a copy sends nothing but HEAD, copies and the handshake");
        assertTrue(wiretap.requestsMatching(S3RequestFilters.head()).size() <= 1,
                () -> "at most one HEAD: " + wiretap.requestsMatching(S3RequestFilters.head()).size());
    }

    static void assertCompleteListsTheParts(CapturedRequest complete, List<PartCopy> parts,
                                            StoredObject source) {
        byte[] body = complete.decodedBody() != null && complete.decodedBody().length > 0
                ? complete.decodedBody() : complete.rawBody();
        String xml = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        List<String> listed = new ArrayList<>();
        Matcher block = PART_BLOCK.matcher(xml);
        while (block.find()) {
            Matcher number = PART_NUMBER.matcher(block.group(1));
            Matcher eTag = PART_ETAG.matcher(block.group(1));
            listed.add((number.find() ? number.group(1) : "?") + ":"
                    + (eTag.find() ? eTag.group(1).toLowerCase(java.util.Locale.ROOT) : "?"));
        }
        List<String> copied = parts.stream()
                .map(p -> p.partNumber() + ":" + source.payload().md5Hex((int) p.offset(), (int) p.length()))
                .collect(Immutable.toList());
        assertEquals(copied, listed, () -> "the CompleteMultipartUpload must list exactly the copied "
                + "parts, in order, with the ETags their copies returned: " + xml);
    }

    private static void assertCount(S3Wiretap wiretap,
                                    java.util.function.Predicate<CapturedRequest> filter,
                                    int expected, String operation) {
        List<CapturedRequest> seen = wiretap.requestsMatching(filter);
        assertEquals(expected, seen.size(), () -> "expected " + expected + " " + operation
                + ", saw " + seen.size() + ": " + seen.stream()
                        .map(r -> r.method() + " " + r.path() + " " + r.queryParameters()).collect(Immutable.toList()));
    }

    private static String single(CapturedRequest request, String header) {
        List<String> values = request.headers().get(header);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
