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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AwsCrtAsync;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.CrtCreate;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.NettyNio;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.MultipartFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.VersionedObject;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class GetVersionedSplitDownloadTest extends AbstractGetBodyTest<S3AsyncClient> {

    private static final Logger LOG =
            LogManager.getLogger(GetVersionedSplitDownloadTest.class);

    private static final int SIZE = 24 * 1024 * 1024;
    private static final int PART_SIZE = 8 * 1024 * 1024;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = StoredObject.Shape.MPU_CRC32_COMPOSITE,
            size = SIZE, partSize = PART_SIZE)
    @Feature(feature = HttpClientFeature.class,
            values = { NettyNio.class, AwsCrtAsync.class, CrtCreate.class })
    @Feature(feature = MultipartFeature.class, values = MultipartFeature.EnabledDefault.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface VersionedSplitSweep {
    }

    @VersionedSplitSweep
    void callersVersionIdReachesEveryRequestOfTheSplit(S3AsyncClient s3, StoredObject object) {
        VersionedObject stack = stackFor(object);
        StoredObject pinned = stack.versions().get(0);
        assertNotEquals(pinned.size(), stack.current().size(),
                "the pinned version must differ from the current one or this proves nothing");
        GetMockResponders.register(wiretap(), KEY, stack);

        byte[] read = readOrFail(s3, requestForVersion(pinned.versionId()), toBytes(),
                "version-pinned split download");
        assertSameBytes(pinned, read, "the pinned version");

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        assertTrue(requests.size() > 1,
                () -> "expected a split download, saw " + requests.size() + " request(s)");
        for (CapturedRequest request : requests) {
            assertEquals(pinned.versionId(), versionIdOf(request),
                    () -> "every request of the split must carry the caller's versionId — a chunk "
                            + "without it reads the CURRENT version and the result is a mixture of "
                            + "two versions: " + describe(request));
        }
        LOG.info("versionId reached all {} request(s) of the split: {}",
                requests.size(), describeAll(requests));

        wiretap().clearExchanges();
    }

    @VersionedSplitSweep
    void aPinnedVersionIsUnaffectedByANewVersionWrittenMidDownload(S3AsyncClient s3,
                                                                  StoredObject object) {
        VersionedObject before = stackBefore(object);
        StoredObject pinned = before.current();
        VersionedObject after = stackFor(object);
        GetMockResponders.registerSwitchingAfterFirstRequest(wiretap(), KEY, before, after);

        byte[] read = readOrFail(s3, requestForVersion(pinned.versionId()), toBytes(),
                "pinned split download across a mid-download write");
        assertSameBytes(pinned, read, "the pinned version, across a mid-download write");

        GetVerifiers.awaitQuiescence(wiretap());
        LOG.info("pinned read survived a mid-download new version: {} request(s)",
                wiretap().requestCount());
        wiretap().clearExchanges();
    }

    @VersionedSplitSweep
    void withoutAVersionIdANewVersionMidDownloadIsRefusedRatherThanMixed(S3AsyncClient s3,
                                                                        StoredObject object) {
        VersionedObject before = stackBefore(object);
        VersionedObject after = stackFor(object);
        assertNotEquals(before.current().eTag(), after.current().eTag(),
                "the new version must have a different ETag or If-Match could not notice it");
        GetMockResponders.registerSwitchingAfterFirstRequest(wiretap(), KEY, before, after);

        byte[] readBytes = null;
        Throwable thrown = null;
        try {
            readBytes = toBytes().read(s3, requestForVersion(null));
        } catch (Throwable t) {
            thrown = t;
        }
        byte[] returned = readBytes;
        Throwable raised = thrown;

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        long conditioned = requests.stream()
                .filter(r -> r.headers().containsKey("If-Match"))
                .count();
        LOG.info("unpinned read across a mid-download new version: {} request(s), {} conditioned, "
                        + "outcome {}", requests.size(), conditioned,
                raised == null ? "RETURNED " + (returned == null ? "null" : returned.length + " bytes")
                        : raised.toString());

        assertNotNull(raised, () -> "a new version arrived mid-download and the unpinned read "
                + "returned " + (returned == null ? "no" : String.valueOf(returned.length))
                + " bytes without error — of " + requests.size() + " request(s), " + conditioned
                + " carried If-Match. An unconditioned split download reassembles two versions into "
                + "one plausible-looking object, which is the outcome this suite exists to rule out");
        assertTrue(mentionsPreconditionFailure(raised),
                () -> "expected the 412 the SDK's own If-Match earned, got: " + causeChain(raised));
        assertTrue(conditioned >= requests.size() - 1,
                () -> "every request after the anchor must be conditioned on the anchor's ETag; only "
                        + conditioned + " of " + requests.size() + " were: " + describeAll(requests));

        wiretap().clearExchanges();
    }

    @VersionedSplitSweep
    void theSdkDoesNotPinTheAnchorsVersionIdOntoLaterChunks(S3AsyncClient s3, StoredObject object) {
        VersionedObject stack = stackBefore(object);
        GetMockResponders.register(wiretap(), KEY, stack);

        byte[] read = readOrFail(s3, requestForVersion(null), toBytes(), "unpinned split download");
        assertSameBytes(stack.current(), read, "the current version");

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        assertTrue(requests.size() > 1,
                () -> "expected a split download, saw " + requests.size() + " request(s)");
        List<CapturedRequest> pinned = requests.stream()
                .filter(r -> versionIdOf(r) != null)
                .toList();
        assertEquals(List.of(), pinned.stream().map(GetVersionedSplitDownloadTest::describe).toList(),
                () -> "the SDK does not pin the anchor's version id onto later chunks — if this now "
                        + "fails, the SDK has started doing so, which is a strengthening: every "
                        + "unpinned split download became a snapshot read. Update this test and "
                        + "withoutAVersionIdANewVersionMidDownloadIsRefusedRatherThanMixed together, "
                        + "since the latter's 412 would no longer happen");

        long conditioned = requests.stream()
                .filter(r -> r.headers().containsKey("If-Match"))
                .count();
        assertEquals(requests.size() - 1, conditioned,
                () -> "with no versionId pinned, If-Match on every request after the anchor is the "
                        + "ONLY thing keeping the split consistent: " + describeAll(requests));

        wiretap().clearExchanges();
    }

    private static VersionedObject stackFor(StoredObject object) {
        return VersionedObject.withSyntheticVersionIds(
                object, AsyncGetBodyAutoParallelTest.replacementFor(object));
    }

    private static VersionedObject stackBefore(StoredObject object) {
        return VersionedObject.withSyntheticVersionIds(object);
    }

    private GetObjectRequest requestForVersion(String versionId) {
        return versionedRequest(builder -> versionId == null
                ? builder
                : builder.versionId(versionId));
    }

    private GetObjectRequest versionedRequest(UnaryOperator<GetObjectRequest.Builder> parameterize) {
        return parameterize.apply(
                GetObjectRequest.builder().bucket(versionedBucket()).key(KEY)).build();
    }

    private static Reader<S3AsyncClient> toBytes() {
        return (client, request) ->
                client.getObject(request, AsyncResponseTransformer.toBytes()).join().asByteArray();
    }

    private static void assertSameBytes(StoredObject expected, byte[] read, String what) {
        assertEquals(expected.size(), read.length,
                () -> what + ": unexpected number of bytes returned (" + expected.describe() + ")");
        assertArrayEquals(expected.payload().sha256(), sha256(read),
                () -> what + ": the bytes returned are not " + expected.describe()
                        + " — a split download that read more than one version would look like this");
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every JVM", e);
        }
    }

    private static String versionIdOf(CapturedRequest request) {
        List<String> values = request.queryParameters().get("versionId");
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean mentionsPreconditionFailure(Throwable raised) {
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof S3Exception s3 && s3.statusCode() == 412) {
                return true;
            }
            String message = t.getMessage() == null
                    ? "" : t.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("precondition") || message.contains("412")) {
                return true;
            }
        }
        return false;
    }

    private static String causeChain(Throwable raised) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            sb.append("\n  ").append(t);
        }
        return sb.toString();
    }

    private static String describe(CapturedRequest request) {
        return request.method()
                + (request.queryParameters().containsKey("partNumber")
                        ? " partNumber=" + request.queryParameter("partNumber") : "")
                + (request.headers().containsKey("Range") ? " " + request.header("Range") : "")
                + " versionId=" + versionIdOf(request);
    }

    private static String describeAll(List<CapturedRequest> requests) {
        StringBuilder sb = new StringBuilder();
        for (CapturedRequest request : requests) {
            sb.append("\n  ").append(describe(request))
                    .append(" If-Match=").append(request.headers().get("If-Match"));
        }
        return sb.toString();
    }
}
