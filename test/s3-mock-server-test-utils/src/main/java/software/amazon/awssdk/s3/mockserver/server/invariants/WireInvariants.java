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

package software.amazon.awssdk.s3.mockserver.server.invariants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.invariants.Invariant;
import software.amazon.awssdk.s3.mockserver.invariants.Observation;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.s3.mockserver.server.CapturedExchange;
import software.amazon.awssdk.s3.mockserver.server.CapturedRequest;
import software.amazon.awssdk.s3.mockserver.server.CapturedResponse;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;

public final class WireInvariants {

    private static final Pattern PART = Pattern.compile("<Part>(.*?)</Part>", Pattern.DOTALL);
    private static final Pattern PART_NUMBER = Pattern.compile("<PartNumber>\\s*(\\d+)\\s*</PartNumber>");
    private static final Pattern ETAG = Pattern.compile("<ETag>(.*?)</ETag>", Pattern.DOTALL);
    private static final Pattern COPY_RANGE = Pattern.compile("bytes=(\\d+)-(\\d+)");

    private WireInvariants() {
    }

    public static void check(List<CapturedExchange> history, Collection<?> sources, ObservationSink sink) {
        List<CapturedExchange> settled = history.stream().filter(CapturedExchange::settled).collect(Immutable.toList());
        List<Payload> payloads = sources.stream().filter(Payload.class::isInstance).map(Payload.class::cast)
                .collect(Immutable.toList());
        SourceBinder binder = new SourceBinder(payloads);
        checkDeclaredLengths(settled, sink);
        checkAttempts(settled, binder, sink);
        checkStoredObjects(settled, binder, sink);
    }

    private static void checkDeclaredLengths(List<CapturedExchange> settled, ObservationSink sink) {
        for (CapturedExchange e : settled) {
            CapturedRequest r = e.request();
            if (!bodyReadInFull(e)) {
                continue;
            }
            if (isAwsChunked(r)) {
                sink.checked(Invariant.BODY_LENGTH_MATCHES_DECLARED);
                String declared = r.optionalHeader("x-amz-decoded-content-length").orElse(null);
                if (declared == null) {
                    sink.record(new Observation(Invariant.BODY_LENGTH_MATCHES_DECLARED, describe(e)
                            + ": an aws-chunked body of " + r.fullBodyLength()
                            + " decoded bytes carries no x-amz-decoded-content-length"));
                } else if (parseLong(declared) != r.fullBodyLength()) {
                    sink.record(new Observation(Invariant.BODY_LENGTH_MATCHES_DECLARED, describe(e)
                            + ": x-amz-decoded-content-length is " + declared + " but the chunks decode to "
                            + r.fullBodyLength() + " bytes"));
                }
            }
        }
    }

    private static void checkAttempts(List<CapturedExchange> settled, SourceBinder binder, ObservationSink sink) {
        Map<String, List<CapturedExchange>> calls = new LinkedHashMap<>();
        for (CapturedExchange e : settled) {
            CapturedRequest r = e.request();
            if (!carriesBody(e)) {
                continue;
            }
            String invocation = r.optionalHeader("amz-sdk-invocation-id").orElse(null);
            if (invocation == null) {
                continue;
            }
            String call = invocation + " " + r.method() + " " + host(r) + r.path()
                    + " uploadId=" + query(r, "uploadId") + " partNumber=" + query(r, "partNumber");
            calls.computeIfAbsent(call, k -> new ArrayList<>()).add(e);
        }
        for (List<CapturedExchange> attempts : calls.values()) {
            if (attempts.size() >= 2) {
                checkOneCall(attempts, binder, sink);
            }
        }
    }

    private static void checkOneCall(List<CapturedExchange> attempts, SourceBinder binder, ObservationSink sink) {
        List<CapturedExchange> complete = attempts.stream().filter(WireInvariants::bodyReadInFull).collect(Immutable.toList());
        if (complete.isEmpty()) {
            return;
        }
        sink.checked(Invariant.ATTEMPTS_CARRY_THE_SAME_BYTES);
        CapturedRequest first = complete.get(0).request();
        boolean differ = complete.stream().map(CapturedExchange::request).anyMatch(r ->
                r.fullBodyLength() != first.fullBodyLength()
                        || !Arrays.equals(r.fullBodyDigest(), first.fullBodyDigest()));
        if (differ) {
            if (!eachIsADifferentWholeSource(complete, binder)) {
                sink.record(new Observation(Invariant.ATTEMPTS_CARRY_THE_SAME_BYTES,
                        "attempts of " + describe(complete.get(0)) + " carried different bodies: "
                                + complete.stream().map(WireInvariants::bodySummary).collect(Immutable.toList())));
            }
            return;
        }
        for (CapturedExchange aborted : attempts) {
            if (!aborted.requestAborted()) {
                continue;
            }
            byte[] partial = payloadPrefix(aborted.request());
            byte[] whole = payloadPrefix(complete.get(0).request());
            int n = Math.min(partial.length, whole.length);
            if (!ByteArrays.rangeEquals(partial, 0, n, whole, 0, n)) {
                sink.record(new Observation(Invariant.ATTEMPTS_CARRY_THE_SAME_BYTES,
                        "the abandoned attempt " + describe(aborted) + " does not begin with the bytes the "
                                + "complete attempt " + describe(complete.get(0)) + " carried (first "
                                + firstDifference(partial, whole, n) + " of " + n + " compared)"));
                return;
            }
        }
    }

    private static boolean eachIsADifferentWholeSource(List<CapturedExchange> complete, SourceBinder binder) {
        Set<Payload> seen = new HashSet<>();
        for (CapturedExchange e : complete) {
            Payload source = binder.wholeSource(e.request());
            if (source == null || !seen.add(source)) {
                return false;
            }
        }
        return true;
    }

    private static void checkStoredObjects(List<CapturedExchange> settled, SourceBinder binder,
                                           ObservationSink sink) {
        for (CapturedExchange e : settled) {
            CapturedRequest r = e.request();
            if (isPutObject(r) && answered2xx(e) && bodyReadInFull(e)) {
                bindStored(Immutable.listOf(r), describe(e), binder, sink);
            }
        }
        for (CapturedExchange complete : settled) {
            CapturedRequest r = complete.request();
            if (!S3RequestFilters.completeMultipartUpload().test(r) || !bodyReadInFull(complete)) {
                continue;
            }
            List<CapturedRequest> parts = checkCompletedParts(complete, settled, sink);
            if (parts != null && answered2xx(complete)) {
                bindStored(parts, describe(complete), binder, sink);
            }
        }
    }

    private static List<CapturedRequest> checkCompletedParts(CapturedExchange complete,
                                                             List<CapturedExchange> settled,
                                                             ObservationSink sink) {
        CapturedRequest r = complete.request();
        if (payloadPrefix(r).length < r.fullBodyLength()) {
            return null;
        }
        sink.checked(Invariant.COMPLETED_PARTS_ARE_CONTIGUOUS);
        String xml = new String(payloadPrefix(r), StandardCharsets.UTF_8);
        List<Long> listed = new ArrayList<>();
        List<String> eTags = new ArrayList<>();
        Matcher part = PART.matcher(xml);
        while (part.find()) {
            Matcher number = PART_NUMBER.matcher(part.group(1));
            Matcher eTag = ETAG.matcher(part.group(1));
            listed.add(number.find() ? Long.parseLong(number.group(1)) : -1L);
            eTags.add(eTag.find() ? normalizeETag(eTag.group(1)) : null);
        }
        String where = describe(complete);
        for (int i = 0; i < listed.size(); i++) {
            if (listed.get(i) != i + 1) {
                sink.record(new Observation(Invariant.COMPLETED_PARTS_ARE_CONTIGUOUS, where + " lists part numbers "
                        + listed + "; expected 1.." + listed.size()
                        + " in order, each once"));
                return null;
            }
        }
        String uploadId = query(r, "uploadId");
        List<CapturedRequest> parts = new ArrayList<>();
        boolean allOnThisWire = true;
        long nextCopyByte = 0;
        for (int i = 0; i < listed.size(); i++) {
            int number = i + 1;
            String eTag = eTags.get(i);
            CapturedExchange sent = partSentAs(settled, r, uploadId, number, eTag, complete.startNanos());
            if (sent == null) {
                if (!listedByListParts(settled, r, uploadId, number, eTag)) {
                    sink.record(new Observation(Invariant.COMPLETED_PARTS_ARE_CONTIGUOUS, where + " lists part "
                            + number + " with ETag " + eTag + ", but no UploadPart of part " + number
                            + " of upload " + uploadId + " was answered with that ETag before it, and no"
                            + " ListParts listed one"));
                    return null;
                }
                allOnThisWire = false;
                continue;
            }
            if (S3RequestFilters.uploadPartCopy().test(sent.request())) {
                allOnThisWire = false;
                long[] range = copyRange(sent.request());
                if (range != null) {
                    if (range[0] != nextCopyByte) {
                        sink.record(new Observation(Invariant.COMPLETED_PARTS_ARE_CONTIGUOUS, where
                                + ": copied part " + number + " starts at source byte " + range[0]
                                + " where part " + (number - 1) + " ended at " + (nextCopyByte - 1)));
                        return null;
                    }
                    nextCopyByte = range[1] + 1;
                }
                continue;
            }
            parts.add(sent.request());
        }
        return allOnThisWire ? parts : null;
    }

    private static CapturedExchange partSentAs(List<CapturedExchange> settled, CapturedRequest complete,
                                               String uploadId, int number, String eTag, long completeStart) {
        CapturedExchange found = null;
        for (CapturedExchange e : settled) {
            CapturedRequest r = e.request();
            if (!"PUT".equals(r.method()) || !sameObject(r, complete)
                    || !uploadId.equals(query(r, "uploadId"))
                    || !Integer.toString(number).equals(query(r, "partNumber"))
                    || !answered2xx(e) || e.endNanos() > completeStart) {
                continue;
            }
            String answered = S3RequestFilters.uploadPartCopy().test(r)
                    ? copyResultETag(e.response())
                    : e.response().optionalHeader("ETag").map(WireInvariants::normalizeETag).orElse(null);
            if (eTag != null && eTag.equals(answered)) {
                found = e;
            }
        }
        return found;
    }

    private static boolean listedByListParts(List<CapturedExchange> settled, CapturedRequest complete,
                                             String uploadId, int number, String eTag) {
        for (CapturedExchange e : settled) {
            CapturedRequest r = e.request();
            if (!S3RequestFilters.listParts().test(r) || !sameObject(r, complete)
                    || !uploadId.equals(query(r, "uploadId")) || !answered2xx(e)) {
                continue;
            }
            Matcher part = PART.matcher(new String(responseBody(e.response()), StandardCharsets.UTF_8));
            while (part.find()) {
                Matcher n = PART_NUMBER.matcher(part.group(1));
                Matcher t = ETAG.matcher(part.group(1));
                if (n.find() && Integer.parseInt(n.group(1)) == number && t.find()
                        && normalizeETag(t.group(1)).equals(eTag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void bindStored(List<CapturedRequest> pieces, String where, SourceBinder binder,
                                   ObservationSink sink) {
        sink.checked(Invariant.STORED_CONTENT_MATCHES_SOURCE);
        String mismatch = binder.mismatch(pieces);
        if (mismatch != null) {
            sink.record(new Observation(Invariant.STORED_CONTENT_MATCHES_SOURCE, "the object " + where
                    + " stored " + mismatch));
        }
    }

    private static boolean isPutObject(CapturedRequest r) {
        if (!S3RequestFilters.simplePut().test(r) || "/".equals(r.path())) {
            return false;
        }
        for (Map.Entry<String, List<String>> q : r.queryParameters().entrySet()) {
            boolean marker = "x-id".equals(q.getKey()) && q.getValue().equals(Immutable.listOf("PutObject"));
            if (!marker && !q.getKey().startsWith("X-Amz-")) {
                return false;
            }
        }
        return true;
    }

    static boolean bodyReadInFull(CapturedExchange e) {
        return e.request().fullBodyDigest() != null && !e.requestAborted();
    }

    private static boolean carriesBody(CapturedExchange e) {
        String method = e.request().method();
        if (!"PUT".equals(method) && !"POST".equals(method)) {
            return false;
        }
        return e.request().fullBodyDigest() != null || (e.requestAborted() && payloadPrefix(e.request()).length > 0);
    }

    static byte[] payloadPrefix(CapturedRequest r) {
        return r.decodedBody() != null ? r.decodedBody() : r.rawBody();
    }

    private static boolean answered2xx(CapturedExchange e) {
        return e.response() != null && e.response().statusCode() / 100 == 2;
    }

    private static boolean isAwsChunked(CapturedRequest r) {
        return r.optionalHeader("Content-Encoding").map(v -> v.toLowerCase(java.util.Locale.ROOT)
                .contains("aws-chunked")).orElse(false);
    }

    private static boolean sameObject(CapturedRequest a, CapturedRequest b) {
        return a.path().equals(b.path()) && host(a).equals(host(b));
    }

    private static String host(CapturedRequest r) {
        return r.optionalHeader("Host").orElse(r.connectHost() == null ? "" : r.connectHost());
    }

    private static String query(CapturedRequest r, String name) {
        List<String> values = r.queryParameters().get(name);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    private static long[] copyRange(CapturedRequest r) {
        String range = r.optionalHeader("x-amz-copy-source-range").orElse(null);
        if (range == null) {
            return null;
        }
        Matcher m = COPY_RANGE.matcher(range);
        return m.find() ? new long[] {Long.parseLong(m.group(1)), Long.parseLong(m.group(2))} : null;
    }

    private static String copyResultETag(CapturedResponse response) {
        Matcher m = ETAG.matcher(new String(responseBody(response), StandardCharsets.UTF_8));
        return m.find() ? normalizeETag(m.group(1)) : null;
    }

    private static byte[] responseBody(CapturedResponse response) {
        return response.decodedBody() != null ? response.decodedBody() : response.body();
    }

    static String normalizeETag(String raw) {
        return raw.replace("&quot;", "").replace("\"", "").trim();
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String firstDifference(byte[] a, byte[] b, int n) {
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return "difference at byte " + i;
            }
        }
        return "no difference";
    }

    static String describe(CapturedExchange e) {
        CapturedRequest r = e.request();
        StringBuilder query = new StringBuilder();
        r.queryParameters().forEach((k, v) -> v.forEach(value ->
                query.append(query.length() == 0 ? '?' : '&').append(k).append(value.isEmpty() ? "" : "=" + value)));
        return r.method() + " " + r.path() + query + " ["
                + r.optionalHeader("amz-sdk-request").orElse("no amz-sdk-request") + ", "
                + (e.response() == null ? "no response" : Integer.toString(e.response().statusCode())) + ", "
                + e.state() + "]";
    }

    private static String bodySummary(CapturedExchange e) {
        CapturedRequest r = e.request();
        return r.optionalHeader("amz-sdk-request").orElse("?") + ": " + r.fullBodyLength() + " bytes, sha256 "
                + Payload.hex(r.fullBodyDigest()).substring(0, 16);
    }
}
