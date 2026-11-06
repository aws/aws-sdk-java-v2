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

package software.amazon.awssdk.s3.wiretap.server.helpers.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;

public final class GetVerifiers {

    private static final long QUIESCENCE_IDLE_MILLIS = 500;
    private static final long QUIESCENCE_TIMEOUT_MILLIS = 15_000;

    private GetVerifiers() {
    }

    public enum Scope {
        WHOLE_OBJECT,
        RANGE,
        PART_NUMBER
    }

    public enum Integrity {
        STORED_CHECKSUM,
        REPORTED_NOT_VERIFIABLE,
        TRANSFER_MD5,
        NONE
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Read {
        private final CapturedRequest request;
        private final Scope scope;
        private final int offset;
        private final int length;
        private final Integer partNumber;
        private final boolean checksumMode;
        private final Integrity integrity;

        private Read(CapturedRequest request, Scope scope, int offset, int length,
                     Integer partNumber, boolean checksumMode, Integrity integrity) {
            this.request = request;
            this.scope = scope;
            this.offset = offset;
            this.length = length;
            this.partNumber = partNumber;
            this.checksumMode = checksumMode;
            this.integrity = integrity;
        }

        public int end() {
            return offset + length;
        }

        @Override
        public String toString() {
            return scope + "[" + offset + "," + end() + ")"
                    + (partNumber != null ? " partNumber=" + partNumber : "")
                    + (checksumMode ? " checksumMode" : "")
                    + " → " + integrity;
        }
    }

    public static void awaitQuiescence(S3Wiretap wiretap) {
        wiretap.awaitQuiescence(QUIESCENCE_IDLE_MILLIS, QUIESCENCE_TIMEOUT_MILLIS);
    }

    public static List<Read> reads(S3Wiretap wiretap, StoredObject object) {
        List<Read> reads = new ArrayList<>();
        for (CapturedRequest request : wiretap.requestsMatching(S3RequestFilters.getObject())) {
            reads.add(decode(request, object));
        }
        return reads;
    }

    public static void assertWholeObjectGet(S3Wiretap wiretap, StoredObject object) {
        List<Read> reads = reads(wiretap, object);
        assertEquals(1, reads.size(),
                () -> "expected a single whole-object GET of " + object.describe()
                        + ", saw " + reads);
        Read read = reads.get(0);
        assertEquals(Scope.WHOLE_OBJECT, read.scope(),
                () -> "expected an unscoped GET, saw " + read);
        assertEquals(object.size(), read.length(), () -> "read length for " + read);
        wiretap.clearExchanges();
    }

    public static void assertRangedDownloadCovers(S3Wiretap wiretap, StoredObject object) {
        List<Read> reads = latestAttemptPerScope(wiretap, object);
        assertFalse(reads.isEmpty(),
                () -> "expected at least one ranged GET of " + object.describe());
        for (Read read : reads) {
            assertEquals(Scope.RANGE, read.scope(),
                    () -> "expected every read to be a Range read, saw " + read
                            + " among " + reads);
        }
        assertReadsCover(object, reads);
        wiretap.clearExchanges();
    }

    public static void assertPartNumberDownloadCovers(S3Wiretap wiretap, StoredObject object) {
        List<Read> reads = latestAttemptPerScope(wiretap, object);
        assertEquals(object.partCount(), reads.size(),
                () -> "expected one read per part of " + object.describe() + ", saw " + reads);
        for (Read read : reads) {
            assertEquals(Scope.PART_NUMBER, read.scope(),
                    () -> "expected every read to be a partNumber read, saw " + read);
        }
        assertReadsCover(object, reads);
        wiretap.clearExchanges();
    }

    public static void assertReadsCover(StoredObject object, List<Read> reads) {
        List<Read> ordered = new ArrayList<>(reads);
        ordered.sort(Comparator.comparingInt(Read::offset));
        int expectedOffset = 0;
        for (Read read : ordered) {
            int expected = expectedOffset;
            assertEquals(expected, read.offset(),
                    () -> "reads must tile the object with no gap or overlap; expected the next "
                            + "read to start at " + expected + " but " + read
                            + " does not. Reads: " + ordered);
            expectedOffset = read.end();
        }
        int covered = expectedOffset;
        assertEquals(object.size(), covered,
                () -> "the reads must together cover the whole object (" + object.describe()
                        + "); they cover [0," + covered + "). Reads: " + ordered);
    }

    public static void assertEveryReadVerifiableAgainstStoredChecksum(S3Wiretap wiretap,
                                                                      StoredObject object) {
        List<Read> reads = reads(wiretap, object);
        assertFalse(reads.isEmpty(), "no GETs were captured, so nothing was verified");
        List<Read> unverifiable = new ArrayList<>();
        for (Read read : reads) {
            if (read.integrity() != Integrity.STORED_CHECKSUM) {
                unverifiable.add(read);
            }
        }
        assertTrue(unverifiable.isEmpty(),
                () -> "these reads of " + object.describe() + " could not be validated against "
                        + "the stored checksum: " + unverifiable + " (all reads: " + reads + ")");
        wiretap.clearExchanges();
    }

    public static List<Read> latestAttemptPerScope(S3Wiretap wiretap, StoredObject object) {
        Map<String, Read> byScope = new LinkedHashMap<>();
        for (Read read : reads(wiretap, object)) {
            byScope.put(read.scope() + ":" + read.offset() + ":" + read.length(), read);
        }
        List<Read> reads = new ArrayList<>(byScope.values());
        reads.sort(Comparator.comparingInt(Read::offset));
        return reads;
    }

    private static Read decode(CapturedRequest request, StoredObject object) {
        boolean checksumMode = request.optionalHeader("x-amz-checksum-mode")
                .filter("ENABLED"::equalsIgnoreCase).isPresent();
        boolean appendMd5 = request.optionalHeader("x-amz-te")
                .filter("append-md5"::equalsIgnoreCase).isPresent();
        String range = request.optionalHeader("Range").orElse(null);
        Integer partNumber = request.queryParameters().containsKey("partNumber")
                ? Integer.valueOf(request.queryParameter("partNumber"))
                : null;

        Scope scope;
        int offset;
        int length;
        if (partNumber != null) {
            scope = Scope.PART_NUMBER;
            offset = object.partOffset(partNumber);
            length = object.partLength(partNumber);
        } else if (range != null) {
            long[] window = object.windowForRange(range);
            scope = Scope.RANGE;
            offset = (int) window[0];
            length = (int) window[1];
        } else {
            scope = Scope.WHOLE_OBJECT;
            offset = 0;
            length = object.size();
        }

        return new Read(request, scope, offset, length, partNumber, checksumMode,
                classify(object, scope, partNumber, offset, length, checksumMode, appendMd5));
    }

    private static Integrity classify(StoredObject object, Scope scope, Integer partNumber,
                                      int offset, int length, boolean checksumMode,
                                      boolean appendMd5) {
        if (!checksumMode) {
            return appendMd5 ? Integrity.TRANSFER_MD5 : Integrity.NONE;
        }
        if (scope != Scope.WHOLE_OBJECT) {
            Integer part = scope == Scope.PART_NUMBER
                    ? partNumber
                    : object.partCoveringExactly(offset, length);
            if (part != null && object.partChecksum(part) != null) {
                return Integrity.STORED_CHECKSUM;
            }
        }

        boolean wholeObject = offset == 0 && length == object.size();
        if (wholeObject) {
            return object.checksumType() == StoredObject.ChecksumType.COMPOSITE
                    ? Integrity.REPORTED_NOT_VERIFIABLE
                    : Integrity.STORED_CHECKSUM;
        }
        return Integrity.NONE;
    }
}
