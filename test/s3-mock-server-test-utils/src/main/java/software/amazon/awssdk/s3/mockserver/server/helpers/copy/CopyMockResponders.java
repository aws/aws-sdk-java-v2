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

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.CapturedBody;
import software.amazon.awssdk.s3.mockserver.server.MockResponder;
import software.amazon.awssdk.s3.mockserver.server.Request;
import software.amazon.awssdk.s3.mockserver.server.Response;
import software.amazon.awssdk.s3.mockserver.server.S3Wiretap;
import software.amazon.awssdk.s3.mockserver.server.helpers.S3ServiceHeader;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.mockserver.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.mockserver.server.helpers.put.PutMpuMockResponders;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public final class CopyMockResponders {

    private static final List<String> UNMODELLED_CONDITIONALS = Immutable.listOf(
            "x-amz-copy-source-if-none-match",
            "x-amz-copy-source-if-modified-since",
            "x-amz-copy-source-if-unmodified-since");

    private static final List<String> UNMODELLED_HEADER_PREFIXES = Immutable.listOf(
            "x-amz-checksum-algorithm",
            "x-amz-server-side-encryption",
            "x-amz-copy-source-server-side-encryption");

    private static final Pattern LISTED_ETAG =
            Pattern.compile("<ETag>(?:\"|&quot;)?([0-9a-fA-F]{32})(?:\"|&quot;)?</ETag>");

    private static final DateTimeFormatter XML_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    private CopyMockResponders() {
    }

    public static void register(S3Wiretap wiretap, String sourceKey, StoredObject source) {
        wiretap.enqueueResponder(responderFor(sourceKey, source));
        wiretap.enqueueResponder(GetMockResponders.responderFor(sourceKey, source));
    }

    public static void register(S3Wiretap wiretap, String sourceKey, StoredObject source,
                                String destinationKey) {
        wiretap.enqueueResponder(new MockResponder() {
            @Override
            public boolean matches(Request request) {
                return isCompleteMultipartUpload(request)
                        && GetMockResponders.matchesKey(request, destinationKey);
            }

            @Override
            public Response respond(Request request, byte[] body) {
                return completeMultipartCopy(request, CapturedBody.of(body), source);
            }

            @Override
            public Response respond(Request request, CapturedBody body) {
                return completeMultipartCopy(request, body, source);
            }
        });
        register(wiretap, sourceKey, source);
    }

    public static MockResponder responderFor(String sourceKey, StoredObject source) {
        return MockResponder.of(
                request -> isCopy(request)
                        && (namesAnArn(request) || sourceKey.equals(matchableSourceKey(request))),
                (request, body) -> isUploadPartCopy(request)
                        ? uploadPartCopy(request, source)
                        : copyObject(request, source));
    }

    public static boolean isCopy(Request request) {
        return "PUT".equalsIgnoreCase(request.method()) && header(request, CopyHeader.COPY_SOURCE) != null;
    }

    public static boolean isUploadPartCopy(Request request) {
        return isCopy(request) && queryParam(request, "partNumber") != null;
    }

    public static Response copyObject(Request request, StoredObject source) {
        sourceKey(request);
        checkModelled(request);
        if (header(request, CopyHeader.COPY_SOURCE_RANGE) != null) {
            throw new IllegalArgumentException("a CopyObject carrying " + CopyHeader.COPY_SOURCE_RANGE
                    + " is not modelled by this mock");
        }
        Response refused = evaluateConditionals(request, source);
        if (refused != null) {
            return refused;
        }
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<CopyObjectResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
                .append("<LastModified>").append(XML_DATE.format(S3ServiceHeader.LAST_MODIFIED))
                .append("</LastModified>")
                .append("<ETag>\"").append(source.payload().md5Hex(0, source.size()))
                .append("\"</ETag>")
                .append('<').append(checksumElement(source)).append('>')
                .append(source.wholeObjectChecksum())
                .append("</").append(checksumElement(source)).append('>')
                .append("<ChecksumType>FULL_OBJECT</ChecksumType>")
                .append("</CopyObjectResult>");
        return S3ServiceHeader.apply(Response.builder())
                .header("Content-Type", "application/xml")
                .body(xml.toString())
                .build();
    }

    public static Response uploadPartCopy(Request request, StoredObject source) {
        sourceKey(request);
        checkModelled(request);
        long[] window = window(request, source);
        Response refused = evaluateConditionals(request, source);
        if (refused != null) {
            return refused;
        }
        int off = (int) window[0];
        int len = (int) window[1];
        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<CopyPartResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
                .append("<LastModified>").append(XML_DATE.format(S3ServiceHeader.LAST_MODIFIED))
                .append("</LastModified>")
                .append("<ETag>\"").append(source.payload().md5Hex(off, len))
                .append("\"</ETag>")
                .append("</CopyPartResult>");
        return S3ServiceHeader.apply(Response.builder())
                .header("Content-Type", "application/xml")
                .body(xml.toString())
                .build();
    }

    public static Response completeMultipartCopy(Request request, CapturedBody body, StoredObject source) {
        Response complete = PutMpuMockResponders.completeMultipartUpload(request, body);
        String xml = new String(complete.body(), StandardCharsets.UTF_8);
        xml = xml.replaceFirst("<ETag>\"[^<]*\"</ETag>",
                Matcher.quoteReplacement("<ETag>\"" + multipartETag(body) + "\"</ETag>"));
        if (!xml.contains("<Checksum")) {
            String end = "</CompleteMultipartUploadResult>";
            xml = xml.replace(end, "<ChecksumCRC64NVME>" + source.payload().crc64NvmeBase64()
                    + "</ChecksumCRC64NVME><ChecksumType>FULL_OBJECT</ChecksumType>" + end);
        }
        return complete.toBuilder().body(xml).build();
    }

    private static String multipartETag(CapturedBody body) {
        String requestXml = new String(body.captured(), StandardCharsets.UTF_8);
        Matcher matcher = LISTED_ETAG.matcher(requestXml);
        java.io.ByteArrayOutputStream md5s = new java.io.ByteArrayOutputStream();
        int parts = 0;
        while (matcher.find()) {
            byte[] raw = ByteArrays.parseHex(matcher.group(1));
            md5s.write(raw, 0, raw.length);
            parts++;
        }
        if (parts == 0) {
            throw new IllegalArgumentException("a CompleteMultipartUpload listing no part ETags");
        }
        return Payload.hex(Payload.of(md5s.toByteArray()).md5Bytes()) + "-" + parts;
    }

    private static boolean isCompleteMultipartUpload(Request request) {
        return "POST".equalsIgnoreCase(request.method()) && queryParam(request, "uploadId") != null;
    }

    public static String sourceKey(Request request) {
        String raw = header(request, CopyHeader.COPY_SOURCE);
        if (raw == null) {
            return null;
        }
        String value = raw.startsWith("/") ? raw.substring(1) : raw;
        int q = value.indexOf('?');
        if (q >= 0) {
            throw new IllegalArgumentException("a versioned copy source (" + raw
                    + ") is not modelled by this mock");
        }
        String decoded = SdkHttpUtils.urlDecode(value.replace("+", "%2B"));
        if (decoded.startsWith("arn:")) {
            throw new IllegalArgumentException("an ARN copy source (" + raw
                    + ") is not modelled by this mock");
        }
        int slash = decoded.indexOf('/');
        if (slash <= 0 || slash == decoded.length() - 1) {
            throw new IllegalArgumentException("cannot read a bucket and key out of " + CopyHeader.COPY_SOURCE
                    + ": " + raw);
        }
        return decoded.substring(slash + 1);
    }

    private static String matchableSourceKey(Request request) {
        String decoded = decodedCopySource(request);
        if (decoded == null) {
            return null;
        }
        int slash = decoded.indexOf('/');
        return slash < 0 ? null : decoded.substring(slash + 1);
    }

    private static boolean namesAnArn(Request request) {
        String decoded = decodedCopySource(request);
        return decoded != null && decoded.startsWith("arn:");
    }

    private static String decodedCopySource(Request request) {
        String raw = header(request, CopyHeader.COPY_SOURCE);
        if (raw == null) {
            return null;
        }
        int q = raw.indexOf('?');
        String unversioned = q < 0 ? raw : raw.substring(0, q);
        String value = unversioned.startsWith("/") ? unversioned.substring(1) : unversioned;
        try {
            return SdkHttpUtils.urlDecode(value.replace("+", "%2B"));
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static void checkModelled(Request request) {
        for (String name : UNMODELLED_CONDITIONALS) {
            if (header(request, name) != null) {
                throw new IllegalArgumentException(name + " is not modelled by this mock");
            }
        }
        for (String name : request.headers().keySet()) {
            String lower = name.toLowerCase(Locale.ROOT);
            for (String prefix : UNMODELLED_HEADER_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    throw new IllegalArgumentException("a copy carrying " + name
                            + " is not modelled by this mock: it changes what S3 stores");
                }
            }
        }
    }

    private static Response evaluateConditionals(Request request, StoredObject source) {
        String ifMatch = header(request, CopyHeader.COPY_SOURCE_IF_MATCH);
        if (ifMatch == null || GetMockResponders.eTagMatches(ifMatch, source.eTag())) {
            return null;
        }
        return S3ServiceHeader.apply(Response.builder())
                .status(412)
                .header("Content-Type", "application/xml")
                .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<Error><Code>PreconditionFailed</Code>"
                        + "<Message>At least one of the pre-conditions you specified did not hold"
                        + "</Message>"
                        + "<Condition>x-amz-copy-source-If-Match</Condition>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    private static long[] window(Request request, StoredObject source) {
        String range = header(request, CopyHeader.COPY_SOURCE_RANGE);
        if (range == null) {
            return new long[] {0, source.size()};
        }
        String spec = range.trim();
        if (!spec.startsWith("bytes=") || spec.contains(",")) {
            throw new IllegalArgumentException(CopyHeader.COPY_SOURCE_RANGE + " " + range
                    + " is not one bytes=a-b range; not modelled by this mock");
        }
        String[] bounds = spec.substring("bytes=".length()).split("-", -1);
        if (bounds.length != 2 || bounds[0].isEmpty() || bounds[1].isEmpty()) {
            throw new IllegalArgumentException(CopyHeader.COPY_SOURCE_RANGE + " " + range
                    + " is not a closed bytes=a-b range; not modelled by this mock");
        }
        long first = Long.parseLong(bounds[0]);
        long last = Long.parseLong(bounds[1]);
        if (first > last || last >= source.size()) {
            throw new IllegalArgumentException(CopyHeader.COPY_SOURCE_RANGE + " " + range + " is not "
                    + "satisfiable against a " + source.size() + "-byte source; S3 answers "
                    + "InvalidArgument here, which this mock does not model");
        }
        return new long[] {first, last - first + 1};
    }

    private static String checksumElement(StoredObject source) {
        return "Checksum" + source.algorithm().name();
    }

    private static String header(Request request, String name) {
        List<String> values = request.headers().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
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
            if (k.equals(name)) {
                return eq < 0 ? "" : pair.substring(eq + 1);
            }
        }
        return null;
    }
}
