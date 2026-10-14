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

package software.amazon.awssdk.s3.wiretap.server.helpers.put;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.s3.wiretap.server.CapturedBody;
import software.amazon.awssdk.s3.wiretap.server.MockResponder;
import software.amazon.awssdk.s3.wiretap.server.Request;
import software.amazon.awssdk.s3.wiretap.server.RequestMatchers;
import software.amazon.awssdk.s3.wiretap.server.Response;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;
import software.amazon.awssdk.s3.wiretap.server.helpers.S3ServiceHeader;

public final class PutMpuMockResponders {

    private static final List<String> CHECKSUM_ALGORITHMS =
            List.of("crc32", "crc32c", "crc64nvme", "sha1", "sha256");

    private static final Pattern PART_CHECKSUM = Pattern.compile(
            "<Checksum(CRC32|CRC32C|CRC64NVME|SHA1|SHA256)>([^<]+)</Checksum(?:CRC32|CRC32C|CRC64NVME|SHA1|SHA256)>");
    private static final Pattern PART_ELEMENT = Pattern.compile("<Part>");
    private static final Pattern VIRTUAL_HOSTED_HOST =
            Pattern.compile("^([^.]+)\\.s3(?:[.-][a-z0-9-]+)?\\.amazonaws\\.com$");

    private static final String DEFAULT_BUCKET = "wiretap-test-1";
    private static final String DEFAULT_REGION = "us-west-2";

    private static final DateTimeFormatter XML_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    private static final String MOCK_INITIATOR_ARN =
            "arn:aws:sts::000000000000:assumed-role/WiretapMock/wiretap-mock";
    private static final String MOCK_INITIATOR_NAME = "WiretapMock/wiretap-mock";

    private PutMpuMockResponders() {
    }

    public static void registerMultipartUpload(S3Wiretap wiretap) {
        registerMultipartUpload(wiretap, false);
    }

    public static void registerMultipartUpload(S3Wiretap wiretap, boolean versioned) {
        wiretap.enqueueResponder(responder(
                RequestMatchers.method("POST").and(RequestMatchers.pathContains("uploads")),
                PutMpuMockResponders::createMultipartUpload));

        wiretap.enqueueResponder(responder(
                RequestMatchers.method("PUT").and(RequestMatchers.pathContains("partNumber")),
                PutMpuMockResponders::uploadPart));

        wiretap.enqueueResponder(responder(
                RequestMatchers.method("POST").and(RequestMatchers.pathContains("uploadId")),
                (request, body) -> completeMultipartUpload(request, body, versioned)));

        wiretap.enqueueResponder(responder(
                RequestMatchers.method("DELETE").and(RequestMatchers.pathContains("uploadId")),
                PutMpuMockResponders::abortMultipartUpload));
    }

    public static void registerAll(S3Wiretap wiretap) {
        registerAll(wiretap, false);
    }

    public static void registerAll(S3Wiretap wiretap, boolean versioned) {
        registerMultipartUpload(wiretap, versioned);

        wiretap.enqueueResponder(responder(
                RequestMatchers.method("PUT"),
                (request, body) -> putObject(request, body, versioned)));

        wiretap.enqueueResponder(responder(
                RequestMatchers.method("GET"),
                (request, body) -> S3ServiceHeader.apply(Response.builder())
                        .header("ETag", syntheticETag(request, "get", 0))
                        .body("mock-object-body")
                        .build()));
    }

    public static void registerBadDigestOn(S3Wiretap wiretap, Predicate<Request> where) {
        wiretap.enqueueResponder(responder(where, (request, body) -> badDigest(request)));
    }

    public static Response badDigest(Request request) {
        return S3ServiceHeader.apply(Response.builder())
                .status(400)
                .header("Content-Type", "application/xml")
                .body("<Error><Code>BadDigest</Code>"
                        + "<Message>The Content-MD5 or checksum value that you specified did not "
                        + "match what the server received.</Message>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    public static void registerRetryableErrorOnce(S3Wiretap wiretap, Predicate<Request> where) {
        wiretap.enqueueResponder(new MockResponder() {
            private final AtomicBoolean fired = new AtomicBoolean();

            @Override
            public boolean matches(Request request) {
                return where.test(request) && fired.compareAndSet(false, true);
            }

            @Override
            public Response respond(Request request, byte[] body) {
                return internalError();
            }
        });
    }

    public static Response internalError() {
        return S3ServiceHeader.apply(Response.builder())
                .status(500)
                .header("Content-Type", "application/xml")
                .body("<Error><Code>InternalError</Code>"
                        + "<Message>We encountered an internal error. Please try again.</Message>"
                        + "<RequestId>WIRETAPMOCKREQID0</RequestId>"
                        + "<HostId>aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVy</HostId></Error>")
                .build();
    }

    public static void registerListParts(S3Wiretap wiretap, UploadedParts uploaded) {
        wiretap.enqueueResponder(responder(
                RequestMatchers.method("GET").and(RequestMatchers.pathContains("uploadId")),
                (request, body) -> listParts(request, uploaded)));
    }

    public static Response listParts(Request request, UploadedParts uploaded) {
        String key = key(request);
        int marker = intQueryParam(request, "part-number-marker", 0, 0);
        int maxParts = intQueryParam(request, "max-parts", 1000, 1);

        List<UploadedParts.Part> remaining = uploaded.parts().stream()
                .filter(part -> part.partNumber() > marker)
                .toList();
        int limit = Math.min(maxParts, uploaded.pageSize());
        boolean truncated = remaining.size() > limit;
        List<UploadedParts.Part> page = truncated ? remaining.subList(0, limit) : remaining;

        int nextMarker = page.isEmpty() ? 0 : page.get(page.size() - 1).partNumber();

        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<ListPartsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
                .append("<Bucket>").append(bucket(request)).append("</Bucket>")
                .append("<Key>").append(key).append("</Key>")
                .append("<UploadId>").append(queryParam(request, "uploadId")).append("</UploadId>")
                .append("<Initiator><ID>").append(MOCK_INITIATOR_ARN).append("</ID>")
                .append("<DisplayName>").append(MOCK_INITIATOR_NAME).append("</DisplayName>")
                .append("</Initiator>")
                .append("<Owner><ID>").append(hex32(key) + hex32(key + "|owner"))
                .append("</ID></Owner>")
                .append("<StorageClass>STANDARD</StorageClass>")
                .append("<ChecksumAlgorithm>CRC32</ChecksumAlgorithm>")
                .append("<ChecksumType>COMPOSITE</ChecksumType>")
                .append("<PartNumberMarker>").append(marker).append("</PartNumberMarker>")
                .append("<NextPartNumberMarker>").append(nextMarker)
                .append("</NextPartNumberMarker>")
                .append("<MaxParts>").append(maxParts).append("</MaxParts>")
                .append("<IsTruncated>").append(truncated).append("</IsTruncated>");
        for (UploadedParts.Part part : page) {
            xml.append("<Part>")
                    .append("<PartNumber>").append(part.partNumber()).append("</PartNumber>")
                    .append("<LastModified>").append(XML_DATE.format(S3ServiceHeader.LAST_MODIFIED))
                    .append("</LastModified>")
                    .append("<ETag>&quot;")
                    .append(eTagBody(key, "part-" + part.partNumber(), part.size()))
                    .append("&quot;</ETag>")
                    .append("<Size>").append(part.size()).append("</Size>")
                    .append("<ChecksumCRC32>").append(part.crc32Base64()).append("</ChecksumCRC32>")
                    .append("</Part>");
        }
        xml.append("</ListPartsResult>");

        return S3ServiceHeader.apply(Response.builder())
                .header("Content-Type", "application/xml")
                .body(xml.toString())
                .build();
    }

    public static Response putObject(Request request, CapturedBody body) {
        return putObject(request, body, false);
    }

    public static Response putObject(Request request, CapturedBody body, boolean versioned) {
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder())
                .header("ETag", syntheticETag(request, "put", body.fullLength()));
        if (versioned) {
            builder.header("x-amz-version-id",
                    syntheticVersionId(request, "put", body.fullLength()));
        }
        Checksum checksum = clientChecksum(request, body);
        if (checksum == null) {
            checksum = serviceComputedChecksum(request, body);
        }
        if (checksum != null) {
            builder.header(checksum.header(), checksum.value());
            builder.header("x-amz-checksum-type", "FULL_OBJECT");
        }
        return builder.build();
    }

    public static Response createMultipartUpload(Request request, CapturedBody body) {
        String key = key(request);
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder()).body(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<InitiateMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                        + "<Bucket>" + bucket(request) + "</Bucket>"
                        + "<Key>" + key + "</Key>"
                        + "<UploadId>" + uploadIdFor(request) + "</UploadId>"
                        + "</InitiateMultipartUploadResult>");
        String algorithm = header(request, "x-amz-checksum-algorithm");
        if (algorithm != null) {
            builder.header("x-amz-checksum-algorithm", algorithm);
            String type = header(request, "x-amz-checksum-type");
            builder.header("x-amz-checksum-type", type != null ? type : "COMPOSITE");
        }
        return builder.build();
    }

    public static Response uploadPart(Request request, CapturedBody body) {
        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder())
                .header("ETag", syntheticETag(request,
                        "part-" + queryParam(request, "partNumber"), body.fullLength()));
        Checksum checksum = clientChecksum(request, body);
        if (checksum != null) {
            builder.header(checksum.header(), checksum.value());
        }
        return builder.build();
    }

    public static Response completeMultipartUpload(Request request, CapturedBody body) {
        return completeMultipartUpload(request, body, false);
    }

    public static Response completeMultipartUpload(Request request, CapturedBody body,
                                                   boolean versioned) {
        String key = key(request);
        String requestXml = new String(body.captured(), StandardCharsets.UTF_8);
        int parts = Math.max(countParts(requestXml), 1);

        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
                .append("<Location>https://").append(endpoint(request)).append('/')
                .append(percentEncode(key))
                .append("</Location>")
                .append("<Bucket>").append(bucket(request)).append("</Bucket>")
                .append("<Key>").append(key).append("</Key>")
                .append("<ETag>&quot;").append(hex32(key + "|complete|" + parts))
                .append('-').append(parts).append("&quot;</ETag>");

        ObjectChecksum checksum = completedObjectChecksum(request, requestXml, parts);
        if (checksum != null) {
            xml.append('<').append(checksum.element()).append('>').append(checksum.value())
                    .append("</").append(checksum.element()).append('>')
                    .append("<ChecksumType>").append(checksum.type()).append("</ChecksumType>");
        }
        xml.append("</CompleteMultipartUploadResult>");

        Response.ResponseBuilder builder = S3ServiceHeader.apply(Response.builder())
                .header("Content-Type", "application/xml")
                .body(xml.toString());
        if (versioned) {
            builder.header("x-amz-version-id",
                    syntheticVersionId(request, "complete-" + parts, body.fullLength()));
        }
        return builder.build();
    }

    public static Response abortMultipartUpload(Request request, CapturedBody body) {
        return S3ServiceHeader.apply(Response.builder()).status(204).build();
    }

    private record Checksum(String header, String value) { }

    private record ObjectChecksum(String element, String value, String type) { }

    private static Checksum clientChecksum(Request request, CapturedBody body) {
        for (String algorithm : CHECKSUM_ALGORITHMS) {
            String name = "x-amz-checksum-" + algorithm;
            String trailer = body.trailer(name);
            if (trailer != null) {
                return new Checksum(name, trailer);
            }
            String precomputed = header(request, name);
            if (precomputed != null) {
                return new Checksum(name, precomputed);
            }
        }
        return null;
    }

    private static Checksum serviceComputedChecksum(Request request, CapturedBody body) {
        String sha256 = header(request, "x-amz-content-sha256");
        boolean chunked = sha256 != null && sha256.startsWith("STREAMING")
                || header(request, "x-amz-trailer") != null;
        if (chunked || !body.complete()) {
            return null;
        }
        SdkChecksum checksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC64NVME);
        checksum.update(body.captured(), 0, body.captured().length);
        return new Checksum("x-amz-checksum-crc64nvme",
                Base64.getEncoder().encodeToString(checksum.getChecksumBytes()));
    }

    private static ObjectChecksum completedObjectChecksum(Request request, String requestXml,
                                                          int parts) {
        String declaredType = header(request, "x-amz-checksum-type");
        for (String algorithm : CHECKSUM_ALGORITHMS) {
            String declared = header(request, "x-amz-checksum-" + algorithm);
            if (declared != null) {
                return new ObjectChecksum(element(algorithm), declared,
                        declaredType != null ? declaredType : "FULL_OBJECT");
            }
        }

        Matcher matcher = PART_CHECKSUM.matcher(requestXml);
        List<String> values = new ArrayList<>();
        String algorithmName = null;
        while (matcher.find()) {
            algorithmName = matcher.group(1);
            values.add(matcher.group(2));
        }
        if (algorithmName == null) {
            return null;
        }
        return new ObjectChecksum("Checksum" + algorithmName,
                composite(algorithmName, values) + "-" + parts,
                declaredType != null ? declaredType : "COMPOSITE");
    }

    private static String composite(String algorithmName, List<String> base64PartChecksums) {
        ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
        for (String value : base64PartChecksums) {
            byte[] raw = Base64.getDecoder().decode(value);
            concatenated.write(raw, 0, raw.length);
        }
        SdkChecksum checksum = sdkChecksum(algorithmName);
        byte[] bytes = concatenated.toByteArray();
        checksum.update(bytes, 0, bytes.length);
        return Base64.getEncoder().encodeToString(checksum.getChecksumBytes());
    }

    private static SdkChecksum sdkChecksum(String algorithmName) {
        return switch (algorithmName) {
            case "CRC32" -> SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);
            case "CRC32C" -> SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32C);
            case "CRC64NVME" -> SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC64NVME);
            case "SHA1" -> SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.SHA1);
            case "SHA256" -> SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.SHA256);
            default -> throw new IllegalArgumentException("unknown checksum algorithm " + algorithmName);
        };
    }

    private static String element(String algorithm) {
        return "Checksum" + algorithm.toUpperCase(Locale.ROOT);
    }

    private static int countParts(String requestXml) {
        Matcher matcher = PART_ELEMENT.matcher(requestXml);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static MockResponder responder(Predicate<Request> filter,
                                           BiFunction<Request, CapturedBody, Response> producer) {
        return new MockResponder() {
            @Override
            public boolean matches(Request request) {
                return filter.test(request);
            }

            @Override
            public Response respond(Request request, byte[] body) {
                return producer.apply(request, CapturedBody.of(body));
            }

            @Override
            public Response respond(Request request, CapturedBody body) {
                return producer.apply(request, body);
            }
        };
    }

    private static String syntheticETag(Request request, String role, long length) {
        return '"' + eTagBody(key(request), role, length) + '"';
    }

    private static String eTagBody(String key, String role, long length) {
        return hex32(key + "|" + role + "|" + length);
    }

    private static String syntheticVersionId(Request request, String role, long length) {
        String hex = hex32(key(request) + "|version|" + role + "|" + length);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hex.substring(0, 16).getBytes(StandardCharsets.UTF_8));
    }

    private static String hex32(String seed) {
        long a = 0xcbf29ce484222325L;
        for (byte b : seed.getBytes(StandardCharsets.UTF_8)) {
            a = (a ^ (b & 0xFF)) * 0x100000001b3L;
        }
        long c = Long.rotateLeft(a, 27) * 0x9E3779B97F4A7C15L;
        return String.format("%016x%016x", a, c);
    }

    private static String key(Request request) {
        String path = request.path();
        int q = path.indexOf('?');
        String noQuery = q < 0 ? path : path.substring(0, q);
        return noQuery.startsWith("/") ? noQuery.substring(1) : noQuery;
    }

    private static String uploadIdFor(Request request) {
        return "mock-upload-" + key(request);
    }

    private static String bucket(Request request) {
        Matcher matcher = hostMatcher(request);
        return matcher != null ? matcher.group(1) : DEFAULT_BUCKET;
    }

    private static String endpoint(Request request) {
        String host = header(request, "Host");
        if (host == null) {
            return DEFAULT_BUCKET + ".s3." + DEFAULT_REGION + ".amazonaws.com";
        }
        int colon = host.indexOf(':');
        return colon < 0 ? host : host.substring(0, colon);
    }

    private static Matcher hostMatcher(Request request) {
        String host = header(request, "Host");
        if (host == null) {
            return null;
        }
        int colon = host.indexOf(':');
        Matcher matcher = VIRTUAL_HOSTED_HOST.matcher(colon < 0 ? host : host.substring(0, colon));
        return matcher.matches() ? matcher : null;
    }

    private static String percentEncode(String key) {
        StringBuilder out = new StringBuilder();
        for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return out.toString();
    }

    private static String header(Request request, String name) {
        List<String> values = request.headers().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String queryParam(Request request, String name) {
        String path = request.path();
        int q = path.indexOf('?');
        if (q < 0) {
            return "";
        }
        for (String pair : path.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            if (k.equals(name)) {
                return eq < 0 ? "" : pair.substring(eq + 1);
            }
        }
        return "";
    }

    private static int intQueryParam(Request request, String name, int fallback, int minimum) {
        String value = queryParam(request, name);
        if (value.isEmpty()) {
            return fallback;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer, was '" + value
                    + "' — real S3 answers InvalidArgument here, which this mock does not model", e);
        }
        if (parsed < minimum) {
            throw new IllegalArgumentException(name + " must be at least " + minimum + ", was "
                    + parsed + " — real S3 answers InvalidArgument here, which this mock does not"
                    + " model");
        }
        return parsed;
    }
}
