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

package software.amazon.awssdk.s3.wiretap.server.helpers.list;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.s3.wiretap.server.MockResponder;
import software.amazon.awssdk.s3.wiretap.server.Request;
import software.amazon.awssdk.s3.wiretap.server.Response;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;
import software.amazon.awssdk.s3.wiretap.server.helpers.S3ServiceHeader;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;

public final class ListObjectsMockResponders {

    static final int MAX_KEYS = 1000;

    private static final String TOKEN_TAG = "wiretap-mock-list:";

    private static final DateTimeFormatter XML_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    private static final Pattern VIRTUAL_HOSTED_HOST =
            Pattern.compile("^([^.]+)\\.s3(?:[.-][a-z0-9-]+)?\\.amazonaws\\.com$");
    private static final Pattern REGION_LABEL =
            Pattern.compile("[.-]([a-z]{2}(?:-gov)?-[a-z]+-\\d+)\\.amazonaws\\.com$");
    private static final Pattern GLOBAL_ENDPOINT = Pattern.compile("(?:^|\\.)s3\\.amazonaws\\.com$");

    private ListObjectsMockResponders() {
    }

    public static void register(S3Wiretap wiretap, BucketListing listing) {
        wiretap.enqueueResponder(responderFor(listing));
        listing.objects().forEach((key, object) ->
                wiretap.enqueueResponder(GetMockResponders.responderFor(key, object)));
    }

    public static MockResponder responderFor(BucketListing listing) {
        return MockResponder.of(ListObjectsMockResponders::isListObjectsV2,
                (request, body) -> listObjectsV2(request, listing));
    }

    public static boolean isListObjectsV2(Request request) {
        return "GET".equalsIgnoreCase(request.method()) && "2".equals(queryParam(request, "list-type"));
    }

    private record Entry(String position, StoredObject object) {
        boolean commonPrefix() {
            return object == null;
        }
    }

    public static Response listObjectsV2(Request request, BucketListing listing) {
        if (queryParam(request, "encoding-type") != null) {
            throw new IllegalArgumentException("encoding-type is not modelled by this mock");
        }
        if ("true".equalsIgnoreCase(queryParam(request, "fetch-owner"))) {
            throw new IllegalArgumentException("fetch-owner=true is not modelled by this mock");
        }
        String prefix = orEmpty(queryParam(request, "prefix"));
        String delimiter = queryParam(request, "delimiter");
        if (delimiter != null && delimiter.isEmpty()) {
            throw new IllegalArgumentException("an empty delimiter is not modelled by this mock");
        }
        String startAfter = queryParam(request, "start-after");
        String token = queryParam(request, "continuation-token");
        String resumeAfter = token == null ? null : decodeToken(token);
        int maxKeys = maxKeys(request);

        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, StoredObject> stored : listing.objects().entrySet()) {
            String key = stored.getKey();
            if (!key.startsWith(prefix)) {
                continue;
            }
            if (startAfter != null && BucketListing.UTF8_ORDER.compare(key, startAfter) <= 0) {
                continue;
            }
            int cut = delimiter == null ? -1 : key.indexOf(delimiter, prefix.length());
            if (cut >= 0) {
                String commonPrefix = key.substring(0, cut + delimiter.length());
                if (entries.isEmpty() || !entries.get(entries.size() - 1).position().equals(commonPrefix)) {
                    entries.add(new Entry(commonPrefix, null));
                }
            } else {
                entries.add(new Entry(key, stored.getValue()));
            }
        }
        if (resumeAfter != null) {
            entries.removeIf(e -> BucketListing.UTF8_ORDER.compare(e.position(), resumeAfter) <= 0);
        }

        boolean truncated = maxKeys > 0 && entries.size() > maxKeys;
        List<Entry> page = entries.subList(0, Math.min(maxKeys, entries.size()));

        StringBuilder xml = new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
                .append("<Name>").append(bucket(request)).append("</Name>")
                .append("<Prefix>").append(escape(prefix)).append("</Prefix>");
        if (startAfter != null) {
            xml.append("<StartAfter>").append(escape(startAfter)).append("</StartAfter>");
        }
        if (token != null) {
            xml.append("<ContinuationToken>").append(token).append("</ContinuationToken>");
        }
        if (truncated) {
            xml.append("<NextContinuationToken>")
                    .append(encodeToken(page.get(page.size() - 1).position()))
                    .append("</NextContinuationToken>");
        }
        xml.append("<KeyCount>").append(page.size()).append("</KeyCount>")
                .append("<MaxKeys>").append(maxKeys).append("</MaxKeys>");
        if (delimiter != null) {
            xml.append("<Delimiter>").append(escape(delimiter)).append("</Delimiter>");
        }
        xml.append("<IsTruncated>").append(truncated).append("</IsTruncated>");
        for (Entry entry : page) {
            if (!entry.commonPrefix()) {
                appendContents(xml, entry.position(), entry.object());
            }
        }
        for (Entry entry : page) {
            if (entry.commonPrefix()) {
                xml.append("<CommonPrefixes><Prefix>").append(escape(entry.position()))
                        .append("</Prefix></CommonPrefixes>");
            }
        }
        xml.append("</ListBucketResult>");

        return S3ServiceHeader.apply(Response.builder())
                .header("Content-Type", "application/xml")
                .header("x-amz-bucket-region", region(request))
                .body(xml.toString())
                .build();
    }

    private static void appendContents(StringBuilder xml, String key, StoredObject object) {
        xml.append("<Contents>")
                .append("<Key>").append(escape(key)).append("</Key>")
                .append("<LastModified>").append(XML_DATE.format(S3ServiceHeader.LAST_MODIFIED))
                .append("</LastModified>")
                .append("<ETag>").append(escape(object.eTag())).append("</ETag>")
                .append("<ChecksumAlgorithm>").append(object.algorithm().name())
                .append("</ChecksumAlgorithm>")
                .append("<ChecksumType>").append(object.checksumType().name()).append("</ChecksumType>")
                .append("<Size>").append(object.size()).append("</Size>")
                .append("<StorageClass>STANDARD</StorageClass>")
                .append("</Contents>");
    }

    private static int maxKeys(Request request) {
        String raw = queryParam(request, "max-keys");
        if (raw == null) {
            return MAX_KEYS;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("max-keys must be an integer, was '" + raw
                    + "' — real S3 answers InvalidArgument here, which this mock does not model", e);
        }
        if (parsed < 0 || parsed > MAX_KEYS) {
            throw new IllegalArgumentException("max-keys outside 0.." + MAX_KEYS + " (" + parsed
                    + ") is not modelled by this mock");
        }
        return parsed;
    }

    static String encodeToken(String position) {
        return Base64.getEncoder().encodeToString((TOKEN_TAG + position).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeToken(String token) {
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = "";
        }
        if (!decoded.startsWith(TOKEN_TAG)) {
            throw new IllegalArgumentException("continuation-token " + token + " was not minted by "
                    + "this mock; only S3 can resume from its own tokens");
        }
        return decoded.substring(TOKEN_TAG.length());
    }

    private static String bucket(Request request) {
        String host = host(request);
        Matcher matcher = VIRTUAL_HOSTED_HOST.matcher(host);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        String path = request.path();
        int q = path.indexOf('?');
        String noQuery = q < 0 ? path : path.substring(0, q);
        String segment = noQuery.startsWith("/") ? noQuery.substring(1) : noQuery;
        int slash = segment.indexOf('/');
        segment = slash < 0 ? segment : segment.substring(0, slash);
        if (segment.isEmpty()) {
            throw new IllegalArgumentException("cannot tell which bucket " + host + " " + path
                    + " lists");
        }
        return segment;
    }

    private static String region(Request request) {
        String host = host(request);
        Matcher matcher = REGION_LABEL.matcher(host);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (GLOBAL_ENDPOINT.matcher(host).find()) {
            return "us-east-1";
        }
        throw new IllegalArgumentException("cannot tell the region of endpoint " + host);
    }

    private static String host(Request request) {
        List<String> values = request.headers().get("Host");
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("a ListObjectsV2 with no Host header");
        }
        String host = values.get(0);
        int colon = host.indexOf(':');
        return colon < 0 ? host : host.substring(0, colon);
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
                String raw = eq < 0 ? "" : pair.substring(eq + 1).replace("+", "%2B");
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
