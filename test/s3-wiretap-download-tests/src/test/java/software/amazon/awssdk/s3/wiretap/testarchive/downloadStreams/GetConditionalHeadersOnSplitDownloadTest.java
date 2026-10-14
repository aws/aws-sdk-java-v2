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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.List;
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
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class GetConditionalHeadersOnSplitDownloadTest extends AbstractGetBodyTest<S3AsyncClient> {

    private static final Logger LOG =
            LogManager.getLogger(GetConditionalHeadersOnSplitDownloadTest.class);

    private static final String STALE_ETAG = "\"00000000000000000000000000000000\"";
    private static final String OTHER_ETAG = "\"11111111111111111111111111111111\"";

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(shapes = StoredObject.Shape.MPU_CRC32_COMPOSITE)
    @Feature(feature = HttpClientFeature.class,
            values = { NettyNio.class, AwsCrtAsync.class, CrtCreate.class })
    @Feature(feature = MultipartFeature.class, values = MultipartFeature.EnabledDefault.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface SplitSweep {
    }

    @SplitSweep
    void callersIfMatchIsPreservedOnEveryRequestIncludingTheAnchor(S3AsyncClient s3,
                                                                   StoredObject object) {
        List<CapturedRequest> requests = downloadAndCapture(s3, object,
                b -> b.ifMatch(object.eTag()));

        assertTrue(requests.size() > 1,
                () -> "expected a split download, saw " + requests.size() + " request(s)");
        for (CapturedRequest request : requests) {
            List<String> ifMatch = request.headers().get("If-Match");
            assertNotNull(ifMatch, () -> "every request must carry the caller's If-Match — including "
                    + "the anchor, which is unconditioned when the caller sets nothing: "
                    + describe(request));
            assertEquals(1, ifMatch.size(),
                    () -> "expected a single If-Match value, saw " + ifMatch + " on "
                            + describe(request));
            assertEquals(object.eTag(), ifMatch.get(0),
                    () -> "the caller's ETag must be the one on the wire: " + describe(request));
        }
    }

    @SplitSweep
    void callersStaleIfMatchIsRefusedAtTheAnchorBeforeAnyChunk(S3AsyncClient s3,
                                                              StoredObject object) {
        GetMockResponders.register(wiretap(), KEY, object);
        GetObjectRequest request = requestFor(ChecksumModeOption.UNSET,
                b -> b.ifMatch(STALE_ETAG));

        Throwable thrown = null;
        try {
            s3.getObject(request, AsyncResponseTransformer.toBytes()).join();
        } catch (Throwable t) {
            thrown = t;
        }
        Throwable raised = thrown;
        assertNotNull(raised, "a stale If-Match must be refused rather than replaced by the "
                + "SDK's own");
        assertTrue(mentionsPreconditionFailure(raised),
                () -> "expected the 412 the caller's own condition earned, got: " + raised);

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        assertEquals(1, requests.size(),
                () -> "the download must stop at the anchor: " + describeAll(requests));
        assertEquals(STALE_ETAG, requests.get(0).header("If-Match"),
                "the anchor carried the caller's value, not one of the SDK's choosing");
        LOG.info("stale If-Match refused at the anchor after {} request(s): {}",
                requests.size(), describeAll(requests));

        wiretap().clearExchanges();
    }

    @SplitSweep
    void callersMatchingIfNoneMatchIsRefusedAtTheAnchorRatherThanReadAsEmpty(S3AsyncClient s3,
                                                                            StoredObject object) {
        GetMockResponders.register(wiretap(), KEY, object);
        GetObjectRequest request = requestFor(ChecksumModeOption.UNSET,
                b -> b.ifNoneMatch(object.eTag()));

        byte[] readBytes = null;
        Throwable thrown = null;
        try {
            readBytes = s3.getObject(request, AsyncResponseTransformer.toBytes()).join().asByteArray();
        } catch (Throwable t) {
            thrown = t;
        }
        byte[] returned = readBytes;
        Throwable raised = thrown;

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        LOG.info("matching If-None-Match: {} request(s), outcome {}", requests.size(),
                raised == null ? "RETURNED " + (returned == null ? "null" : returned.length + " bytes")
                        : raised.toString());

        assertNotNull(raised, () -> "a 304 carries no bytes, so the download cannot succeed — it "
                + "returned " + (returned == null ? "null" : returned.length + " bytes")
                + " instead of failing, which for a split download means an empty chunk was "
                + "reassembled into a short object with no error");
        assertEquals(304, statusOf(raised),
                () -> "expected the 304 to surface with its own status, got: " + raised);
        assertEquals(1, requests.size(),
                () -> "the 304 lands on the anchor, so no chunk should follow it: "
                        + describeAll(requests));
        assertEquals(object.eTag(), requests.get(0).header("If-None-Match"),
                "the anchor carried the caller's condition unchanged");

        wiretap().clearExchanges();
    }

    @SplitSweep
    void otherConditionalsAreReplicatedOntoEveryChunkAlongsideTheSdksIfMatch(S3AsyncClient s3,
                                                                            StoredObject object) {
        assertPreservedOnEveryChunk(s3, object, "If-None-Match", OTHER_ETAG,
                b -> b.ifNoneMatch(OTHER_ETAG));
        assertPreservedOnEveryChunk(s3, object, "If-Unmodified-Since",
                "Tue, 01 Jan 2030 00:00:00 GMT",
                b -> b.ifUnmodifiedSince(Instant.parse("2030-01-01T00:00:00Z")));
        assertPreservedOnEveryChunk(s3, object, "If-Modified-Since",
                "Sat, 01 Jan 2000 00:00:00 GMT",
                b -> b.ifModifiedSince(Instant.parse("2000-01-01T00:00:00Z")));
    }

    private void assertPreservedOnEveryChunk(S3AsyncClient s3, StoredObject object, String header,
                                             String expected,
                                             UnaryOperator<GetObjectRequest.Builder> apply) {
        List<CapturedRequest> requests = downloadAndCapture(s3, object, apply);
        assertTrue(requests.size() > 1,
                () -> header + ": expected a split download, saw " + requests.size() + " request(s)");

        for (CapturedRequest request : requests) {
            assertEquals(expected, request.header(header),
                    () -> header + " must reach every request of the split, missing on "
                            + describe(request));
        }

        List<CapturedRequest> unconditioned = requests.stream()
                .filter(r -> !r.headers().containsKey("If-Match"))
                .toList();
        assertEquals(1, unconditioned.size(),
                () -> header + ": the SDK's own If-Match should still cover every chunk after the "
                        + "anchor, so exactly one request should lack it; saw "
                        + unconditioned.size() + " of " + requests.size() + " in "
                        + describeAll(requests));
        assertEquals(requests.get(0).toString(), unconditioned.get(0).toString(),
                () -> header + ": the request lacking If-Match must be the anchor, i.e. the first one");
        for (CapturedRequest request : requests) {
            if (request.headers().containsKey("If-Match")) {
                assertEquals(object.eTag(), request.header("If-Match"),
                        () -> header + ": the SDK's If-Match must still name the anchor's ETag on "
                                + describe(request));
            }
        }
        LOG.info("{} reached all {} request(s), alongside the SDK's If-Match on {}",
                header, requests.size(), requests.size() - 1);
    }

    private List<CapturedRequest> downloadAndCapture(S3AsyncClient s3, StoredObject object,
                                                     UnaryOperator<GetObjectRequest.Builder> apply) {
        GetMockResponders.register(wiretap(), KEY, object);
        GetObjectRequest request = requestFor(ChecksumModeOption.UNSET, apply);

        byte[] read = readOrFail(s3, request, (client, req) ->
                client.getObject(req, AsyncResponseTransformer.toBytes()).join().asByteArray(),
                "conditional split download");
        assertEquals(object.size(), read.length,
                () -> "a condition that holds must not change what is returned");

        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> requests = wiretap().requests();
        List<CapturedRequest> copy = List.copyOf(requests);
        wiretap().clearPendingResponses();
        wiretap().clearExchanges();
        return copy;
    }

    private static int statusOf(Throwable raised) {
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof S3Exception s3) {
                return s3.statusCode();
            }
        }
        return -1;
    }

    private static boolean mentionsPreconditionFailure(Throwable raised) {
        for (Throwable t = raised; t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof S3Exception s3 && s3.statusCode() == 412) {
                return true;
            }
            String message = t.getMessage() == null ? "" : t.getMessage().toLowerCase(java.util.Locale.ROOT);
            if (message.contains("precondition") || message.contains("412")) {
                return true;
            }
        }
        return false;
    }

    private static String describe(CapturedRequest request) {
        return request.method()
                + (request.queryParameters().containsKey("partNumber")
                        ? " partNumber=" + request.queryParameter("partNumber") : "")
                + (request.headers().containsKey("Range") ? " " + request.header("Range") : "");
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
