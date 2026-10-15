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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.s3.wiretap.server.CapturedExchange;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.ExchangeState;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.S3TestBase;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

abstract class AbstractGetBodyTest<C> extends S3TestBase {

    private static final Logger LOG = LogManager.getLogger(AbstractGetBodyTest.class);

    protected static final String KEY = "get-body/object";

    protected static final int MD5_SUFFIX = 16;

    protected interface Reader<C> {
        byte[] read(C client, GetObjectRequest request) throws Exception;
    }

    protected void wholeObject(C client, StoredObject object, ChecksumModeOption checksumMode,
                               Reader<C> reader) {
        runGet(client, object, checksumMode, 0, object.size(), builder -> builder, reader,
                GetVerifiers.Scope.WHOLE_OBJECT);
    }

    protected void partNumber(C client, StoredObject object, ChecksumModeOption checksumMode,
                              int part, Reader<C> reader) {
        runGet(client, object, checksumMode, object.partOffset(part), object.partLength(part),
                request -> request.partNumber(part), reader, GetVerifiers.Scope.PART_NUMBER);
    }

    protected void range(C client, StoredObject object, ChecksumModeOption checksumMode,
                         int offset, int length, Reader<C> reader) {
        runGet(client, object, checksumMode, offset, length,
                request -> request.range(byteRange(offset, length)), reader,
                GetVerifiers.Scope.RANGE);
    }

    protected void runGet(C client, StoredObject object, ChecksumModeOption checksumMode,
                          int offset, int length,
                          UnaryOperator<GetObjectRequest.Builder> parameterize,
                          Reader<C> reader, GetVerifiers.Scope expectedScope) {
        GetObjectRequest request = requestFor(checksumMode, parameterize);

        GetMockResponders.register(wiretap(), KEY, object);
        byte[] read = readOrFail(client, request, reader, "clean pass");
        assertEquals(length, read.length,
                () -> "bytes returned by the clean read of " + object.describe());
        assertArrayEquals(object.payload().toByteArray(offset, length), read,
                () -> "clean read of " + object.describe() + " returned bytes that differ from the stored object");

        GetVerifiers.Read decoded = singleRead(object);
        assertEquals(expectedScope, decoded.scope(), () -> "decoded scope of " + decoded);
        assertEquals(offset, decoded.offset(), () -> "decoded offset of " + decoded);
        assertEquals(length, decoded.length(), () -> "decoded length of " + decoded);
        GetVerifiers.Integrity integrity = decoded.integrity();
        assertReadAccountedFor();

        wiretap().clearPendingResponses();
        GetMockResponders.registerCorrupting(wiretap(), KEY, object);
        boolean shouldDetect = integrity == GetVerifiers.Integrity.STORED_CHECKSUM
                || integrity == GetVerifiers.Integrity.TRANSFER_MD5;

        byte[] readBytes = null;
        Throwable thrown = null;
        try {
            readBytes = reader.read(client, request);
        } catch (Throwable t) {
            thrown = t;
        }
        byte[] returned = readBytes;
        Throwable raised = thrown;

        if (shouldDetect) {
            assertNotNull(raised, () -> "expected an integrity failure on a read the response "
                    + "makes verifiable (" + integrity + ") — " + decoded
                    + " on " + object.describe() + ", but the read completed without an error");
            assertIntegrityFailure(raised, integrity, decoded);
            LOG.info("corruption detected as predicted ({}): {}", integrity, raised.toString());
        } else {
            assertNoThrow(raised, () -> "no integrity information is available for this read ("
                    + integrity + "), so expected the read to complete with the altered bytes and"
                    + " no integrity error, but it raised: " + raised);
            assertNotNull(returned, "expected bytes to be returned");
            assertEquals(length, returned.length, "the corrupt read should be the same length");
            byte[] expected = object.payload().toByteArray(offset, length);
            expected[0]++;
            assertArrayEquals(expected, returned,
                    () -> "the corrupt bytes should have been returned verbatim (" + integrity + ")");
        }
        assertReadAccountedFor();
    }

    protected void assertReadAccountedFor() {
        assertTotalRequests(1);
    }

    protected void assertSplitDownloadCorruption(C client, StoredObject object,
                                                 GetObjectRequest request,
                                                 List<GetVerifiers.Read> cleanReads,
                                                 Reader<C> reader) {
        assertSplitDownloadCorruption(client, object, request, cleanReads, false, reader);
    }

    protected void assertSplitDownloadCorruption(C client, StoredObject object,
                                                 GetObjectRequest request,
                                                 List<GetVerifiers.Read> cleanReads,
                                                 boolean verifiableAsAWhole,
                                                 Reader<C> reader) {
        wiretap().clearPendingResponses();
        GetMockResponders.registerCorrupting(wiretap(), KEY, object);

        boolean shouldDetect = verifiableAsAWhole || cleanReads.stream().anyMatch(read ->
                read.integrity() == GetVerifiers.Integrity.STORED_CHECKSUM
                        || read.integrity() == GetVerifiers.Integrity.TRANSFER_MD5);

        byte[] readBytes = null;
        Throwable thrown = null;
        try {
            readBytes = reader.read(client, request);
        } catch (Throwable t) {
            thrown = t;
        }
        byte[] returned = readBytes;
        Throwable raised = thrown;

        if (shouldDetect) {
            assertNotNull(raised, () -> (verifiableAsAWhole
                    ? "the client verifies this split download as a whole"
                    : "at least one chunk of this split download was verifiable")
                    + " " + integritiesOf(cleanReads) + ", so altering it should raise — but "
                    + object.describe() + " completed without an error");
            assertIntegrityFailure(raised, cleanReads.get(0).integrity(), cleanReads.get(0));
            LOG.info("split download detected corruption as predicted {}: {}",
                    integritiesOf(cleanReads), raised.toString());
        } else {
            assertNoThrow(raised, () -> "no chunk of this split download carried anything to verify "
                    + integritiesOf(cleanReads) + ", so expected the altered bytes to be returned "
                    + "with no integrity error, but it raised: " + raised);
            assertNotNull(returned, "expected bytes to be returned");
            assertEquals(object.size(), returned.length,
                    "the corrupt split download should still reassemble to the full length");
            byte[] expected = object.payload().toByteArray(0, object.size());
            for (GetVerifiers.Read read : cleanReads) {
                expected[read.offset()]++;
            }
            assertArrayEquals(expected, returned,
                    () -> "expected the corrupt bytes verbatim, with one flipped byte at the head of "
                            + "each of the " + cleanReads.size() + " chunks of " + object.describe());
            LOG.info("split download completed with the altered bytes and no integrity error, as predicted {}",
                    integritiesOf(cleanReads));
        }

        GetVerifiers.awaitQuiescence(wiretap());
        wiretap().clearExchanges();
    }

    protected void assertMidDownloadReplacementIsRefused(C client, StoredObject before,
                                                         StoredObject after,
                                                         GetObjectRequest request,
                                                         Reader<C> reader) {
        assertNotEquals(before.eTag(), after.eTag(),
                "the replacement must have a different ETag or this test proves nothing");

        GetMockResponders.registerSwitchingAfterFirstRequest(wiretap(), KEY, before, after);

        byte[] readBytes = null;
        Throwable thrown = null;
        try {
            readBytes = reader.read(client, request);
        } catch (Throwable t) {
            thrown = t;
        }
        byte[] returned = readBytes;
        Throwable raised = thrown;

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> reads = wiretap().requestsMatching(S3RequestFilters.get());
        long conditioned = reads.stream()
                .filter(read -> read.headers().containsKey("If-Match"))
                .count();
        LOG.info("mid-download replacement: {} GET(s), {} conditioned, outcome {}",
                reads.size(), conditioned, raised == null ? "RETURNED BYTES" : raised.toString());

        assertNotNull(raised, () -> "the object was replaced mid-download (" + before.describe()
                + " → " + after.describe() + ") and the SDK returned "
                + (returned == null ? "no" : String.valueOf(returned.length))
                + " bytes without error. Of " + reads.size() + " GET(s), " + conditioned
                + " carried If-Match — an unconditioned split download can reassemble two different "
                + "objects into one plausible-looking result");

        assertTrue(mentionsPreconditionFailure(raised),
                () -> "expected the failure to be the stale If-Match (412 / PreconditionFailed), but "
                        + "the SDK failed for another reason: " + causeChain(raised));

        assertTrue(conditioned >= reads.size() - 1,
                () -> "every request after the first should be conditioned on the anchor's ETag; only "
                        + conditioned + " of " + reads.size() + " were: " + reads);

        wiretap().clearExchanges();
    }

    private static boolean mentionsPreconditionFailure(Throwable raised) {
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            String message = t.getMessage() == null ? "" : t.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("precondition") || message.contains("412")) {
                return true;
            }
            if (t instanceof S3Exception s3 && s3.statusCode() == 412) {
                return true;
            }
        }
        return false;
    }

    private static String causeChain(Throwable raised) {
        StringBuilder chain = new StringBuilder();
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            chain.append(t).append(" | ");
        }
        return chain.toString();
    }

    private static String integritiesOf(List<GetVerifiers.Read> reads) {
        return reads.stream()
                .map(read -> read.integrity().name())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.joining("+", "[", "]"));
    }

    protected void assertAbandonedResponseRecorded(StoredObject object) {
        CapturedExchange exchange = wiretap().exchangeMatching(S3RequestFilters.get());
        assertEquals(200, exchange.response().statusCode(), "the response head was still captured");

        assertEquals(object.size() + expectedMd5Suffix(exchange), contentLength(exchange),
                () -> "the response head should still promise the whole body for " + object.describe());

        byte[] captured = exchange.response().body();
        long streamed = exchange.response().bodyTotalLength();
        long fullLength = object.size() + expectedMd5Suffix(exchange);
        if (exchange.state() == ExchangeState.COMPLETE) {
            assertEquals(fullLength, streamed,
                    () -> "a client that drained the abandoned body transferred all of it, so the wire"
                            + " should show the full length for " + object.describe());
        } else {
            assertTrue(streamed < fullLength,
                    () -> "an aborting client leaves the exchange mid-flight, so less than the full "
                            + fullLength + " bytes should have streamed; got " + streamed);
        }

        assertTrue(captured.length > 0,
                () -> "expected a non-empty captured prefix for " + object.describe());
        assertTrue(streamed >= captured.length,
                () -> "the streamed count cannot be less than what was captured from it");
        byte[] expectedPrefix = object.payload().toByteArray(0, captured.length);
        expectedPrefix[0]++;
        assertArrayEquals(expectedPrefix, captured,
                "the captured prefix of an abandoned response must be the bytes actually sent");

        assertTotalRequests(1);
    }

    private static long contentLength(CapturedExchange exchange) {
        return Long.parseLong(exchange.response().header("Content-Length"));
    }

    private static int expectedMd5Suffix(CapturedExchange exchange) {
        return exchange.response().headers().containsKey("x-amz-transfer-encoding") ? MD5_SUFFIX : 0;
    }

    protected static void assertIntegrityFailure(Throwable raised, GetVerifiers.Integrity integrity,
                                                 GetVerifiers.Read read) {
        StringBuilder chain = new StringBuilder();
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            chain.append(t).append(" | ");
            String message = t.getMessage() == null ? "" : t.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("checksum") || message.contains("digest")
                    || message.contains("md5")) {
                return;
            }
        }
        fail("expected an integrity failure for " + read + " (" + integrity
                + "), but the SDK failed for an unrelated reason: " + chain);
    }

    protected static <C> byte[] readOrFail(C client, GetObjectRequest request, Reader<C> reader,
                                           String what) {
        try {
            return reader.read(client, request);
        } catch (Exception e) {
            throw new IllegalStateException(what + " should have succeeded", e);
        }
    }

    protected GetVerifiers.Read singleRead(StoredObject object) {
        List<GetVerifiers.Read> reads = GetVerifiers.reads(wiretap(), object);
        assertEquals(1, reads.size(), () -> "expected exactly one GET, saw " + reads);
        return reads.get(0);
    }

    protected GetObjectRequest requestFor(ChecksumModeOption checksumMode,
            UnaryOperator<GetObjectRequest.Builder> parameterize) {
        GetObjectRequest.Builder builder = GetObjectRequest.builder().bucket(bucket()).key(KEY);
        return checksumMode.apply(parameterize.apply(builder)).build();
    }

    protected static String byteRange(int offset, int length) {
        return "bytes=" + offset + "-" + (offset + length - 1);
    }

    protected static void assertNoThrow(Throwable raised, Supplier<String> message) {
        if (raised != null) {
            fail(message.get());
        }
    }
}
