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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.s3.wiretap.server.MockResponder;
import software.amazon.awssdk.s3.wiretap.server.Request;
import software.amazon.awssdk.s3.wiretap.server.Response;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;
import software.amazon.awssdk.s3.wiretap.server.helpers.S3ServiceHeader;

public final class GetMockResponders {

    private static final int MD5_LENGTH = 16;

    private GetMockResponders() {
    }

    public static void register(S3Wiretap wiretap, String key, StoredObject object) {
        wiretap.enqueueResponder(responderFor(key, object));
    }

    public static MockResponder responderFor(String key, StoredObject object) {
        return MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> respond(request, object));
    }

    public static void register(S3Wiretap wiretap, String key, VersionedObject stack) {
        wiretap.enqueueResponder(responderFor(key, stack));
    }

    public static MockResponder responderFor(String key, VersionedObject stack) {
        return MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> respond(request, stack));
    }

    public static void registerCorrupting(S3Wiretap wiretap, String key, StoredObject object) {
        wiretap.enqueueResponder(corruptingResponderFor(key, object));
    }

    public static void registerSwitchingAfterFirstRequest(S3Wiretap wiretap, String key,
                                                          StoredObject before, StoredObject after) {
        AtomicBoolean served = new AtomicBoolean();
        wiretap.enqueueResponder(MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> respond(request, served.getAndSet(true) ? after : before)));
    }

    public static void registerSwitchingAfterFirstRequest(S3Wiretap wiretap, String key,
                                                          VersionedObject before,
                                                          VersionedObject after) {
        AtomicBoolean served = new AtomicBoolean();
        wiretap.enqueueResponder(MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> respond(request, served.getAndSet(true) ? after : before)));
    }

    public static void registerMissing(S3Wiretap wiretap, String key) {
        wiretap.enqueueResponder(missingResponderFor(key));
    }

    public static MockResponder missingResponderFor(String key) {
        return MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> noSuchKey(request, key));
    }

    private static boolean eTagMatches(String ifMatch, String eTag) {
        String value = ifMatch.trim();
        if ("*".equals(value)) {
            return true;
        }
        for (String candidate : value.split(",")) {
            if (candidate.trim().equals(eTag)) {
                return true;
            }
        }
        return false;
    }

    private static Response evaluateConditionals(Request request, StoredObject object, boolean head,
                                                 String versionId) {
        String ifMatch = headerOrNull(request, "If-Match");
        if (ifMatch != null && !eTagMatches(ifMatch, object.eTag())) {
            return preconditionFailed(head);
        }
        String ifNoneMatch = headerOrNull(request, "If-None-Match");
        if (ifNoneMatch != null && eTagMatches(ifNoneMatch, object.eTag())) {
            return notModified(object, versionId);
        }
        return null;
    }

    private static Response notModified(StoredObject object, String versionId) {
        Response.ResponseBuilder builder =
                S3ServiceHeader.applyWithLastModified(Response.builder())
                        .status(304)
                        .header("ETag", object.eTag());
        return withVersionId(builder, versionId).build();
    }

    private static Response.ResponseBuilder withVersionId(Response.ResponseBuilder builder,
                                                          String versionId) {
        return versionId == null ? builder : builder.header("x-amz-version-id", versionId);
    }

    private static Response preconditionFailed(boolean head) {
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder()).status(412);
        if (head) {
            return builder.build();
        }
        return builder
                .header("Content-Type", "application/xml")
                .body("<Error><Code>PreconditionFailed</Code>"
                        + "<Message>At least one of the pre-conditions you specified did not hold"
                        + "</Message>"
                        + "<Condition>If-Match</Condition>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    private static Response noSuchKey(Request request, String key) {
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder()).status(404);
        if ("HEAD".equalsIgnoreCase(request.method())) {
            return builder.build();
        }
        return builder
                .header("Content-Type", "application/xml")
                .body("<Error><Code>NoSuchKey</Code>"
                        + "<Message>The specified key does not exist.</Message>"
                        + "<Key>" + key + "</Key>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    private static Response noSuchVersion(Request request, String versionId) {
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder()).status(404);
        if ("HEAD".equalsIgnoreCase(request.method())) {
            return builder.build();
        }
        return builder
                .header("Content-Type", "application/xml")
                .body("<Error><Code>NoSuchVersion</Code>"
                        + "<Message>The specified version does not exist.</Message>"
                        + "<VersionId>" + versionId + "</VersionId>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    public static MockResponder corruptingResponderFor(String key, StoredObject object) {
        return MockResponder.of(
                request -> ("GET".equalsIgnoreCase(request.method())
                        || "HEAD".equalsIgnoreCase(request.method()))
                        && matchesKey(request, key),
                (request, body) -> corrupt(respond(request, object)));
    }

    private static Response corrupt(Response response) {
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            return response;
        }
        byte[] flipped = body.clone();
        flipped[0]++;
        return response.toBuilder().body(flipped).build();
    }

    public static Response respond(Request request, VersionedObject stack) {
        String requested = queryParam(request, "versionId");
        StoredObject selected = requested == null ? stack.current() : stack.version(requested);
        if (selected == null) {
            return noSuchVersion(request, requested);
        }
        return respond(request, selected, true);
    }

    public static Response respond(Request request, StoredObject object) {
        return respond(request, object, false);
    }

    private static Response respond(Request request, StoredObject object, boolean versioned) {
        boolean head = "HEAD".equalsIgnoreCase(request.method());
        String versionId = versioned ? object.versionId() : null;

        Response conditional = evaluateConditionals(request, object, head, versionId);
        if (conditional != null) {
            return conditional;
        }

        boolean checksumMode = "ENABLED".equalsIgnoreCase(headerOrNull(request, "x-amz-checksum-mode"));
        boolean appendMd5 = !head && !checksumMode
                && "append-md5".equalsIgnoreCase(headerOrNull(request, "x-amz-te"));

        Integer partNumber = intQueryParam(request, "partNumber");
        String rangeHeader = headerOrNull(request, "Range");

        int off;
        int len;
        if (partNumber != null) {
            off = object.partOffset(partNumber);
            len = object.partLength(partNumber);
        } else if (rangeHeader != null) {
            long[] window = object.windowForRange(rangeHeader);
            off = (int) window[0];
            len = (int) window[1];
        } else {
            off = 0;
            len = object.size();
        }
        boolean scopedRead = partNumber != null || rangeHeader != null;

        Response.ResponseBuilder builder = withVersionId(
                S3ServiceHeader.applyWithLastModified(Response.builder()), versionId)
                .header("Accept-Ranges", "bytes")
                .header("Content-Type", object.contentType())
                .header("ETag", object.eTag());

        if (appendMd5) {
            builder.status(200).header("x-amz-transfer-encoding", "append-md5");
            if (scopedRead) {
                builder.header("x-amz-content-range",
                        "bytes " + off + "-" + (off + len - 1) + "/" + object.size());
            }
            if (object.multipart() && partNumber != null) {
                builder.header("x-amz-mp-parts-count", String.valueOf(object.partCount()));
            }
            return builder.body(bodyWithAppendedMd5(object, off, len)).build();
        }

        boolean wholeObject = off == 0 && len == object.size();
        if (scopedRead) {
            builder.status(206)
                    .header("Content-Range", "bytes " + off + "-" + (off + len - 1) + "/" + object.size());
        } else {
            builder.status(200);
        }

        if (checksumMode) {
            String checksum;
            if (scopedRead) {
                Integer part = partNumber != null
                        ? partNumber
                        : object.partCoveringExactly(off, len);
                String partValue = part == null ? null : object.partChecksum(part);
                checksum = partValue != null
                        ? partValue
                        : (wholeObject ? object.storedChecksum() : null);
            } else {
                checksum = object.storedChecksum();
            }
            if (checksum != null) {
                builder.header(object.algorithm().headerName(), checksum);
                builder.header("x-amz-checksum-type", object.checksumType().name());
            }
        }

        if (object.multipart() && partNumber != null) {
            builder.header("x-amz-mp-parts-count", String.valueOf(object.partCount()));
        }

        if (head) {
            return builder.header("Content-Length", String.valueOf(len)).build();
        }
        return builder.body(rangeBytes(object, off, len)).build();
    }

    private static byte[] rangeBytes(StoredObject object, int off, int len) {
        return object.payload().toByteArray(off, len);
    }

    private static byte[] bodyWithAppendedMd5(StoredObject object, int off, int len) {
        byte[] out = new byte[len + MD5_LENGTH];
        byte[] body = object.payload().toByteArray(off, len);
        System.arraycopy(body, 0, out, 0, len);
        byte[] md5 = object.appendedMd5(off, len);
        System.arraycopy(md5, 0, out, len, md5.length);
        return out;
    }

    private static boolean matchesKey(Request request, String key) {
        String path = request.path();
        int q = path.indexOf('?');
        String raw = q < 0 ? path : path.substring(0, q);
        String noQuery;
        try {
            noQuery = URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformed) {
            noQuery = raw;
        }
        boolean virtualHosted = noQuery.equals("/" + key);
        boolean pathStyle = noQuery.endsWith("/" + key)
                && noQuery.indexOf('/', 1) == noQuery.length() - key.length() - 1;
        String host = headerOrNull(request, "Host");
        if (host == null) {
            return virtualHosted;
        }
        return host.contains(".s3.") || host.contains(".s3-") ? virtualHosted : pathStyle;
    }

    private static String headerOrNull(Request request, String name) {
        var values = request.headers().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static Integer intQueryParam(Request request, String name) {
        String raw = queryParam(request, name);
        return raw == null ? null : Integer.valueOf(raw);
    }

    private static String queryParam(Request request, String name) {
        String path = request.path();
        int q = path.indexOf('?');
        if (q < 0) {
            return null;
        }
        for (String pair : path.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            if (k.equals(name) && eq >= 0) {
                String raw = pair.substring(eq + 1).replace("+", "%2B");
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
