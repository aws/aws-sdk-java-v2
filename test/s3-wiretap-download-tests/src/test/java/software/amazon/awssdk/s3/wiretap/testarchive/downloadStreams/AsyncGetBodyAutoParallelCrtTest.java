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

package software.amazon.awssdk.s3.wiretap.testarchive.downloadStreams;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllAsyncNonCrt;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.CrtCreate;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.MultipartFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.CapturedExchange;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class AsyncGetBodyAutoParallelCrtTest extends AbstractGetBodyTest<S3AsyncClient> {

    private static final Logger LOG = LogManager.getLogger(AsyncGetBodyAutoParallelCrtTest.class);

    private static final int SIZE = 50 * 1024 * 1024;
    private static final int DEFAULT_PART_SIZE = 8 * 1024 * 1024;
    private static final int NON_ALIGNED_PART_SIZE = 10 * 1024 * 1024;
    private static final int CRT_PART_SIZE = 16 * 1024 * 1024;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest
    @Feature(feature = HttpClientFeature.class, values = CrtCreate.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface CrtAutoParallelSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(size = SIZE, partSize = NON_ALIGNED_PART_SIZE)
    @Feature(feature = HttpClientFeature.class, values = CrtCreate.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface CrtSplitSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = {
            StoredObject.Shape.MPU_CRC32_COMPOSITE, StoredObject.Shape.MPU_CRC64_FULL_OBJECT
    }, size = SIZE, partSize = NON_ALIGNED_PART_SIZE)
    @Feature(feature = HttpClientFeature.class, group = AllAsyncNonCrt.class)
    @Feature(feature = MultipartFeature.class, values = MultipartFeature.EnabledDefault.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface PlainAsyncContrastSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = {
            StoredObject.Shape.MPU_CRC32_COMPOSITE, StoredObject.Shape.MPU_CRC64_FULL_OBJECT
    }, size = SIZE, partSize = DEFAULT_PART_SIZE)
    @Feature(feature = HttpClientFeature.class, values = CrtCreate.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface DefaultUploadLayoutSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = StoredObject.Shape.MPU_CRC32_COMPOSITE,
            size = SIZE, partSize = NON_ALIGNED_PART_SIZE)
    @Feature(feature = HttpClientFeature.class, values = CrtCreate.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface NonDefaultUploadLayoutSweep {
    }

    @CrtAutoParallelSweep
    void transformerToBytes(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertRangedSplit(s3, object, checksumMode, (client, req) ->
                client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray());
    }

    @CrtAutoParallelSweep
    void toDestinationPath(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertRangedSplit(s3, object, checksumMode, (client, req) ->
                intoTempFile(file -> client.getObject(req, file).join()));
    }

    @CrtAutoParallelSweep
    void transformerToFile(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertRangedSplit(s3, object, checksumMode, (client, req) ->
                intoTempFile(file ->
                        client.getObject(req, AsyncResponseTransformer.toFile(file)).join()));
    }

    @CrtAutoParallelSweep
    void transformerToBlockingInputStream(S3AsyncClient s3, StoredObject object,
                                          ChecksumModeOption checksumMode) {
        assertRangedSplit(s3, object, checksumMode, (client, req) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(req,
                    AsyncResponseTransformer.toBlockingInputStream()).join()) {
                return in.readAllBytes();
            }
        });
    }

    @CrtAutoParallelSweep
    void transformerToPublisher(S3AsyncClient s3, StoredObject object,
                                ChecksumModeOption checksumMode) {
        assertRangedSplit(s3, object, checksumMode, (client, req) ->
                drain(client.getObject(req, AsyncResponseTransformer.toPublisher()).join()));
    }

    private void assertRangedSplit(S3AsyncClient s3, StoredObject object,
                                   ChecksumModeOption checksumMode, Reader<S3AsyncClient> reader) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        GetMockResponders.register(wiretap(), KEY, object);

        byte[] read = readOrFail(s3, request, reader, "whole-object managed-CRT download");
        assertArrayEquals(object.payload().toByteArray(0, object.size()), read,
                () -> "the CRT split download of " + object.describe() + " must reassemble the object");

        GetVerifiers.awaitQuiescence(wiretap());
        List<GetVerifiers.Read> reads = GetVerifiers.latestAttemptPerScope(wiretap(), object);
        int concurrency = CapturedExchange.maxConcurrency(wiretap().exchanges());
        LOG.info("{} ({}): {} range read(s), max concurrency {}",
                object.describe(), checksumMode, reads.size(), concurrency);

        assertTrue(reads.size() > 1,
                () -> "expected the managed CRT client to auto-split the whole-object read of "
                        + object.describe() + " into several GETs, but it issued " + reads.size()
                        + ": " + reads);
        assertTrue(concurrency > 1,
                () -> "the CRT client should fetch the " + reads.size() + " split ranges of "
                        + object.describe() + " in parallel, but they never overlapped "
                        + "(max concurrency 1): " + reads);
        GetVerifiers.assertRangedDownloadCovers(wiretap(), object);
    }

    @CrtAutoParallelSweep
    void splitDownloadCorruption(S3AsyncClient s3, StoredObject object,
                                 ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        Reader<S3AsyncClient> reader = (client, req) ->
                client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray();

        GetMockResponders.register(wiretap(), KEY, object);
        readOrFail(s3, request, reader, "clean pass of the CRT split download");
        GetVerifiers.awaitQuiescence(wiretap());
        List<GetVerifiers.Read> cleanReads = GetVerifiers.latestAttemptPerScope(wiretap(), object);
        assertTrue(cleanReads.size() > 1,
                () -> "the clean pass should have split " + object.describe() + ", saw " + cleanReads);
        boolean verifiableAsAWhole = probeReportedFullObjectChecksum(wiretap().exchanges());
        wiretap().clearExchanges();

        assertSplitDownloadCorruption(s3, object, request, cleanReads, verifiableAsAWhole, reader);
    }

    @CrtAutoParallelSweep
    void everyRangeIsConditionedOnTheProbesETag(S3AsyncClient s3, StoredObject object,
                                                ChecksumModeOption checksumMode) {
        GetMockResponders.register(wiretap(), KEY, object);
        readOrFail(s3, requestFor(checksumMode, builder -> builder), (client, req) ->
                        client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray(),
                "whole-object managed-CRT download");
        GetVerifiers.awaitQuiescence(wiretap());

        List<CapturedRequest> gets = wiretap().requestsMatching(S3RequestFilters.get());
        assertTrue(gets.size() > 1, () -> "expected a split, saw " + gets.size() + " GET(s)");
        for (CapturedRequest get : gets) {
            assertEquals(object.eTag(), get.header("If-Match"),
                    () -> "every ranged GET must be conditioned on the probe's ETag, saw "
                            + get.headers());
        }

        GetVerifiers.assertRangedDownloadCovers(wiretap(), object);
    }

    @CrtAutoParallelSweep
    void objectReplacedMidDownloadIsRefused(S3AsyncClient s3, StoredObject object,
                                            ChecksumModeOption checksumMode) {
        assertMidDownloadReplacementIsRefused(s3, object,
                AsyncGetBodyAutoParallelTest.replacementFor(object),
                requestFor(checksumMode, builder -> builder), (client, req) ->
                        client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray());
    }

    @CrtSplitSweep
    void crtProbesWithHeadThenSplitsByOwnPartSize(S3AsyncClient s3, StoredObject object,
                                                  ChecksumModeOption checksumMode) {
        List<CapturedExchange> exchanges = downloadViaPath(s3, object, checksumMode);

        assertEquals("HEAD", exchanges.get(0).request().method(),
                () -> "CRT should probe with a HEAD first, but the first request was "
                        + exchanges.get(0).request().method() + " for " + object.describe());
        assertEquals(1, exchanges.stream().filter(e -> isMethod(e, "HEAD")).count(),
                () -> "expected exactly one HEAD probe for " + object.describe() + ", saw " + exchanges);
        assertFalse(exchanges.stream().anyMatch(AsyncGetBodyAutoParallelCrtTest::hasPartNumber),
                () -> "CRT should not use partNumber (it splits by byte range), but did: " + exchanges);

        List<long[]> ranges = rangeWindows(exchanges);
        assertContiguouslyCovers(ranges, object);

        if (object.multipart()) {
            for (long[] r : ranges) {
                long offset = r[0];
                long length = r[1];
                assertEquals(0, offset % CRT_PART_SIZE,
                        () -> "CRT range offset " + offset + " is not on its 16 MiB grid: " + show(ranges));
                assertTrue(length <= CRT_PART_SIZE,
                        () -> "CRT range length " + length + " exceeds its 16 MiB part size: " + show(ranges));
            }
            int expectedRanges = (SIZE + CRT_PART_SIZE - 1) / CRT_PART_SIZE;
            assertEquals(expectedRanges, ranges.size(),
                    () -> "a 50 MiB object should be read as " + expectedRanges + " CRT-part-sized "
                            + "ranges, saw " + show(ranges));
            assertNotEquals(object.partCount(), ranges.size(),
                    () -> "CRT split into as many ranges as the object had stored parts ("
                            + object.partCount() + "); the 10 MiB stored layout was chosen so it "
                            + "would not — it rounds up to a 16 MiB grid: " + show(ranges));
        } else {
            assertEquals(2, ranges.size(),
                    () -> "a single-PUT object should be split into 2 ranges by CRT, saw " + show(ranges));
        }
        wiretap().clearExchanges();
    }

    @DefaultUploadLayoutSweep
    void defaultLayoutIsReadBackOnMatchingBoundaries(S3AsyncClient s3, StoredObject object) {
        List<CapturedExchange> exchanges = downloadViaPath(s3, object, ChecksumModeOption.UNSET);

        assertFalse(exchanges.stream().anyMatch(AsyncGetBodyAutoParallelCrtTest::hasPartNumber),
                () -> "CRT should still not use partNumber even when its grid matches the stored "
                        + "layout — it estimates the part size from the ETag: " + exchanges);

        List<long[]> ranges = rangeWindows(exchanges);
        assertContiguouslyCovers(ranges, object);
        assertEquals(object.partCount(), ranges.size(),
                () -> "an object stored at the default " + DEFAULT_PART_SIZE + "-byte part size should "
                        + "be read back as one range per stored part (" + object.partCount() + "), saw "
                        + show(ranges));
        for (int i = 0; i < ranges.size(); i++) {
            long offset = ranges.get(i)[0];
            long length = ranges.get(i)[1];
            long expected = Math.min((long) DEFAULT_PART_SIZE, object.size() - offset);
            assertEquals((long) i * DEFAULT_PART_SIZE, offset,
                    () -> "range " + offset + " is not on the stored 8 MiB grid: " + show(ranges));
            assertEquals(expected, length,
                    () -> "range at " + offset + " should be " + expected + " bytes to land on one "
                            + "stored part, saw " + length + ": " + show(ranges));
        }
        wiretap().clearExchanges();
    }

    @PlainAsyncContrastSweep
    void plainAsyncUsesPartNumberNoHead(S3AsyncClient s3, StoredObject object,
                                        ChecksumModeOption checksumMode) {
        List<CapturedExchange> exchanges = downloadViaPath(s3, object, checksumMode);

        assertFalse(exchanges.stream().anyMatch(e -> isMethod(e, "HEAD")),
                () -> "the plain-async client should not issue a HEAD probe, but did: " + exchanges);
        assertTrue(exchanges.stream().allMatch(AsyncGetBodyAutoParallelCrtTest::hasPartNumber),
                () -> "the plain-async client should fetch every part by partNumber, but saw: " + exchanges);
        GetVerifiers.assertPartNumberDownloadCovers(wiretap(), object);
    }

    @DefaultUploadLayoutSweep
    void defaultPartSizeVerifiabilityDependsOnChecksumType(S3AsyncClient s3, StoredObject object) {
        List<GetVerifiers.Read> reads = downloadReadsChecksumEnabled(s3, object);
        if (object.checksumType() == StoredObject.ChecksumType.COMPOSITE) {
            GetVerifiers.assertEveryReadVerifiableAgainstStoredChecksum(wiretap(), object);
        } else {
            assertNoReadVerifiable(reads, object);
            wiretap().clearExchanges();
        }
    }

    @NonDefaultUploadLayoutSweep
    void nonDefaultPartSizeIsNotVerifiable(S3AsyncClient s3, StoredObject object) {
        List<GetVerifiers.Read> reads = downloadReadsChecksumEnabled(s3, object);
        assertNoReadVerifiable(reads, object);
        wiretap().clearExchanges();
    }

    private static byte[] drain(ResponsePublisher<GetObjectResponse> publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publisher.subscribe((ByteBuffer buffer) -> {
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            out.write(chunk, 0, chunk.length);
        }).join();
        return out.toByteArray();
    }

    private static byte[] intoTempFile(FileDownload download) throws Exception {
        Path file = Files.createTempFile("wiretap-crt-autoparallel-", ".dat");
        Files.delete(file);
        try {
            download.into(file);
            return Files.readAllBytes(file);
        } finally {
            Payload.deleteQuietly(file);
        }
    }

    private interface FileDownload {
        void into(Path file) throws Exception;
    }

    private List<CapturedExchange> downloadViaPath(S3AsyncClient s3, StoredObject object,
                                                   ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        GetMockResponders.register(wiretap(), KEY, object);
        try {
            intoTempFile(file -> s3.getObject(request, file).join());
        } catch (Exception e) {
            throw new IllegalStateException("download of " + object.describe() + " failed", e);
        }
        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedExchange> exchanges = wiretap().exchanges();
        LOG.info("{} ({}): {}", object.describe(), checksumMode,
                exchanges.stream().map(e -> e.request().method()
                        + (hasPartNumber(e) ? " partNumber=" + e.request().queryParameter("partNumber")
                        : e.request().optionalHeader("Range").map(r -> " " + r).orElse(""))).toList());
        return exchanges;
    }

    private List<GetVerifiers.Read> downloadReadsChecksumEnabled(S3AsyncClient s3, StoredObject object) {
        downloadViaPath(s3, object, ChecksumModeOption.ENABLED);
        List<GetVerifiers.Read> reads = GetVerifiers.reads(wiretap(), object);
        LOG.info("{}: reads {}", object.describe(), reads);
        return reads;
    }

    private static boolean probeReportedFullObjectChecksum(List<CapturedExchange> exchanges) {
        return exchanges.stream()
                .filter(e -> isMethod(e, "HEAD") && e.response() != null)
                .anyMatch(e -> e.response().optionalHeader("x-amz-checksum-type")
                        .filter(StoredObject.ChecksumType.FULL_OBJECT.name()::equals)
                        .isPresent());
    }

    private static boolean isMethod(CapturedExchange e, String method) {
        return method.equalsIgnoreCase(e.request().method());
    }

    private static boolean hasPartNumber(CapturedExchange e) {
        return e.request().queryParameters().containsKey("partNumber");
    }

    private static List<long[]> rangeWindows(List<CapturedExchange> exchanges) {
        List<long[]> windows = new ArrayList<>();
        for (CapturedExchange e : exchanges) {
            if (isMethod(e, "GET")) {
                e.request().optionalHeader("Range").ifPresent(range -> {
                    String spec = range.substring(range.indexOf('=') + 1);
                    String[] bounds = spec.split("-");
                    long first = Long.parseLong(bounds[0].trim());
                    long last = Long.parseLong(bounds[1].trim());
                    windows.add(new long[]{first, last - first + 1});
                });
            }
        }
        windows.sort((a, b) -> Long.compare(a[0], b[0]));
        return windows;
    }

    private static void assertContiguouslyCovers(List<long[]> ranges, StoredObject object) {
        assertFalse(ranges.isEmpty(), () -> "no Range GETs captured for " + object.describe());
        long expected = 0;
        for (long[] r : ranges) {
            long at = expected;
            assertEquals(at, r[0],
                    () -> "ranges must tile the object with no gap/overlap; expected next at " + at
                            + " but got " + show(ranges));
            expected = r[0] + r[1];
        }
        long covered = expected;
        assertEquals(object.size(), covered,
                () -> "ranges must cover the whole " + object.size() + "-byte object: " + show(ranges));
    }

    private static void assertNoReadVerifiable(List<GetVerifiers.Read> reads, StoredObject object) {
        assertTrue(reads.stream().noneMatch(r -> r.integrity() == GetVerifiers.Integrity.STORED_CHECKSUM),
                () -> "expected no ranged read of " + object.describe() + " to be verifiable against "
                        + "the stored checksum, but at least one was: " + reads);
    }

    private static String show(List<long[]> ranges) {
        StringBuilder sb = new StringBuilder("[");
        for (long[] r : ranges) {
            sb.append('[').append(r[0]).append(',').append(r[0] + r[1]).append(") ");
        }
        return sb.append(']').toString();
    }
}
