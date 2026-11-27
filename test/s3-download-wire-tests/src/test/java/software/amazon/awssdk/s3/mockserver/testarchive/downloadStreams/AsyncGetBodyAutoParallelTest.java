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

package software.amazon.awssdk.s3.mockserver.testarchive.downloadStreams;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.HttpClientFeature.NettyNio;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.MultipartFeature;
import software.amazon.awssdk.s3.mockserver.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.CapturedExchange;
import software.amazon.awssdk.s3.mockserver.server.CapturedRequest;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.mockserver.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.mockserver.testarchive.Feature;
import software.amazon.awssdk.s3.mockserver.testarchive.StoredObjectTest;
import software.amazon.awssdk.s3.mockserver.testarchive.helpers.Downloads;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

public class AsyncGetBodyAutoParallelTest extends AbstractGetBodyTest<S3AsyncClient> {

    private static final int SIZE = 50 * 1024 * 1024;

    private static final Logger LOG = LogManager.getLogger(AsyncGetBodyAutoParallelTest.class);

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(size = SIZE)
    @Feature(feature = HttpClientFeature.class, values = NettyNio.class)
    @Feature(feature = MultipartFeature.class, values = MultipartFeature.EnabledDefault.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface AutoParallelSweep {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = { StoredObject.Shape.MPU_CRC32_COMPOSITE,
            StoredObject.Shape.MPU_CRC64_FULL_OBJECT }, size = SIZE)
    @Feature(feature = HttpClientFeature.class, values = NettyNio.class)
    @Feature(feature = MultipartFeature.class, values = MultipartFeature.EnabledDefault.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface MultipartOnlySweep {
    }

    @AutoParallelSweep
    void transformerToBytes(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertSplitByStoredPart(s3, object, checksumMode, Concurrency.SEQUENTIAL, (client, req) ->
                client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray());
    }

    @AutoParallelSweep
    void toDestinationPath(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertSplitByStoredPart(s3, object, checksumMode, Concurrency.PARALLEL, (client, req) ->
                intoTempFile(file -> client.getObject(req, file).join()));
    }

    @AutoParallelSweep
    void transformerToFile(S3AsyncClient s3, StoredObject object, ChecksumModeOption checksumMode) {
        assertSplitByStoredPart(s3, object, checksumMode, Concurrency.PARALLEL, (client, req) ->
                intoTempFile(file ->
                        client.getObject(req, AsyncResponseTransformer.toFile(file)).join()));
    }

    @AutoParallelSweep
    void transformerToBlockingInputStream(S3AsyncClient s3, StoredObject object,
                                          ChecksumModeOption checksumMode) {
        assertSplitByStoredPart(s3, object, checksumMode, Concurrency.SEQUENTIAL, (client, req) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(req,
                    AsyncResponseTransformer.toBlockingInputStream()).join()) {
                return IoUtils.toByteArray(in);
            }
        });
    }

    @AutoParallelSweep
    void transformerToPublisher(S3AsyncClient s3, StoredObject object,
                                ChecksumModeOption checksumMode) {
        assertSplitByStoredPart(s3, object, checksumMode, Concurrency.SEQUENTIAL, (client, req) ->
                Downloads.drain(client.getObject(req, AsyncResponseTransformer.toPublisher()).join()));
    }

    @AutoParallelSweep
    void splitDownloadCorruption(S3AsyncClient s3, StoredObject object,
                                 ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        Reader<S3AsyncClient> reader = (client, req) ->
                client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray();

        GetMockResponders.register(wiretap(), KEY, object);
        readOrFail(s3, request, reader, "clean pass of the split download");
        GetVerifiers.awaitQuiescence(wiretap());
        List<GetVerifiers.Read> cleanReads = GetVerifiers.latestAttemptPerScope(wiretap(), object);
        assertEquals(object.partCount(), cleanReads.size(),
                () -> "the clean pass should split " + object.describe() + " by stored part, saw "
                        + cleanReads);
        assertTotalRequests(cleanReads.size());

        assertSplitDownloadCorruption(s3, object, request, cleanReads, reader);
    }

    @AutoParallelSweep
    void everyPartAfterTheFirstIsConditionedOnTheAnchorsETag(S3AsyncClient s3, StoredObject object,
                                                             ChecksumModeOption checksumMode) {
        GetMockResponders.register(wiretap(), KEY, object);
        readOrFail(s3, requestFor(checksumMode, builder -> builder), (client, req) ->
                        client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray(),
                "whole-object multipart download");
        GetVerifiers.awaitQuiescence(wiretap());

        List<CapturedRequest> gets = wiretap().requestsMatching(S3RequestFilters.get());
        assertEquals(object.partCount(), gets.size(),
                () -> "expected one GET per stored part, saw " + gets.size());

        List<CapturedRequest> unconditioned = gets.stream()
                .filter(get -> !get.headers().containsKey("If-Match"))
                .collect(Immutable.toList());
        assertEquals(1, unconditioned.size(),
                () -> "exactly one request — the anchor — should be unconditioned, saw "
                        + unconditioned.size() + " of " + gets.size());
        assertEquals("1", unconditioned.get(0).queryParameter("partNumber"),
                "the unconditioned request must be partNumber=1, the anchor");

        for (CapturedRequest get : gets) {
            if (get.headers().containsKey("If-Match")) {
                assertEquals(object.eTag(), get.header("If-Match"),
                        "every conditioned part must name the object's own ETag");
            }
        }

        GetVerifiers.assertPartNumberDownloadCovers(wiretap(), object);
    }

    @MultipartOnlySweep
    void objectReplacedMidDownloadIsRefused(S3AsyncClient s3, StoredObject object,
                                            ChecksumModeOption checksumMode) {
        assertMidDownloadReplacementIsRefused(s3, object, replacementFor(object),
                requestFor(checksumMode, builder -> builder), (client, req) ->
                        client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray());
    }

    static StoredObject replacementFor(StoredObject object) {
        return object.shape().create(object.size() / 2, Math.max(object.partSize(), 1));
    }

    private enum Concurrency { PARALLEL, SEQUENTIAL }

    private void assertSplitByStoredPart(S3AsyncClient s3, StoredObject object,
                                         ChecksumModeOption checksumMode, Concurrency expected,
                                         Reader<S3AsyncClient> reader) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        GetMockResponders.register(wiretap(), KEY, object);

        byte[] read = readOrFail(s3, request, reader, "whole-object multipart download");
        assertEquals(object.size(), read.length,
                () -> "reassembled length of the split download of " + object.describe());
        assertArrayEquals(object.payload().toByteArray(0, object.size()), read,
                () -> "the split download of " + object.describe() + " must reassemble the object");

        GetVerifiers.awaitQuiescence(wiretap());
        List<GetVerifiers.Read> reads = GetVerifiers.latestAttemptPerScope(wiretap(), object);
        int concurrency = CapturedExchange.maxConcurrency(wiretap().exchanges());
        LOG.info("{} ({}): {} distinct read(s), max concurrency {}, expected {}",
                object.describe(), checksumMode, reads.size(), concurrency, expected);

        assertEquals(object.partCount(), reads.size(),
                () -> "the part-based downloader issues one partNumber GET per stored part of "
                        + object.describe() + ", so a " + object.partCount() + "-part object should "
                        + "produce " + object.partCount() + " read(s), but saw: " + reads);

        assertConcurrency(object, expected, concurrency);

        GetVerifiers.assertPartNumberDownloadCovers(wiretap(), object);
    }

    private static void assertConcurrency(StoredObject object, Concurrency expected, int observed) {
        if (object.partCount() == 1) {
            assertEquals(1, observed,
                    () -> "a single-part object is a single request, so its download cannot be "
                            + "concurrent regardless of overload — " + object.describe());
            return;
        }
        if (expected == Concurrency.PARALLEL) {
            assertTrue(observed > 1,
                    () -> "this overload should fetch the " + object.partCount() + " parts of "
                            + object.describe() + " in parallel, but the part GETs never overlapped "
                            + "(max concurrency 1)");
        } else {
            assertEquals(1, observed,
                    () -> "this overload should fetch the parts of " + object.describe()
                            + " sequentially, but " + observed + " part GETs were in flight at once");
        }
    }


    private static byte[] intoTempFile(FileDownload download) throws Exception {
        Path file = Downloads.freshFile("wiretap-async-autoparallel-");
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
}
