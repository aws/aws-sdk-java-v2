package software.amazon.awssdk.wirediff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

/**
 * The S3 operations this harness diffs, and the client they run against.
 *
 * <p>Chosen for coverage of the HTTP/XML binding features that a bridge can silently get wrong rather
 * than for being commonly used. Each case names the feature it is there to catch, because a case whose
 * purpose is not written down gets deleted the first time it is inconvenient:
 *
 * <ul>
 *   <li>{@code list-objects-v2} — a literal query parameter baked into the URI pattern
 *       ({@code ?list-type=2}) alongside bound {@code @httpQuery} members.</li>
 *   <li>{@code delete-object} — a greedy path label ({@code /{Key+}}), which must not percent-encode
 *       its slashes, plus a modeled 204 success code.</li>
 *   <li>{@code head-object} — no body in either direction; everything is headers.</li>
 *   <li>{@code complete-multipart-upload} — an XML request body containing a list, exercising
 *       {@code @xmlName} on members and whether the list is flattened.</li>
 *   <li>{@code put-object-tagging} — an XML request body plus a literal query parameter plus a
 *       content MD5 computed over that body.</li>
 *   <li>{@code copy-object} — a wide spread of {@code x-amz-*} headers, and user metadata, which binds
 *       through {@code @httpPrefixHeaders} rather than a named header.</li>
 *   <li>{@code put-object} — a streaming request body, which v2's generator strips from the shape
 *       entirely, so the bridge has to carry it around the schema layer and re-attach it before
 *       signing.</li>
 *   <li>{@code get-object} — a streaming response body; the request itself is small, and what is under
 *       test is that adding a {@code ResponseTransformer} does not change it.</li>
 *   <li>{@code write-get-object-response} — the {@code @endpoint} trait: the request goes to
 *       {@code <RequestRoute>.<endpoint>}, a host prefix built from an input member (ledger 5.4).</li>
 *   <li>{@code upload-part} — a streaming body <em>plus</em> bound query parameters, the combination
 *       every multipart upload sends thousands of times.</li>
 * </ul>
 */
public final class S3Cases {

    /** Fixed so the signature's credential scope, and therefore the golden file, is stable. */
    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDWIREDIFFEXAMPLE", "wirediff/secret/key"));

    private S3Cases() {
    }

    /** A body small enough to read in a diff, and not a round number of anything. */
    static final String PAYLOAD = "wire-diff streaming payload: 38 bytes.";

    /**
     * A client wired to the capturing transport.
     *
     * <p>Path-style addressing is forced. Virtual-host addressing puts the bucket in the host, which
     * both arms get from the same v2 endpoint provider, so it would compare equal while hiding whether
     * the path was built correctly — the thing actually under test.
     *
     * <p>Request checksums are set to {@code WHEN_REQUIRED}, against v2's {@code WHEN_SUPPORTED}
     * default, and that changes what this harness measures for streaming operations. On the default,
     * stock v2 does not send the caller's bytes at all: it adds {@code x-amz-checksum-crc32},
     * {@code content-encoding: aws-chunked} and {@code x-amz-decoded-content-length}, and rewrites the
     * body into chunk framing with a trailing checksum. The bridge computes no checksums (§12.9), so a
     * {@code put-object} diff on the default would compare aws-chunked framing against raw bytes and
     * report a single enormous difference that says nothing about the HTTP binding. Turning checksums
     * off makes both arms send the raw body, so the diff is about the binding again — at the cost of
     * not exercising trailing checksums here at all. §13.3 records that; it needs its own comparison,
     * not this one.
     */
    public static S3Client client(SdkHttpClient transport) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .credentialsProvider(CREDENTIALS)
                       .endpointOverride(URI.create("https://s3.us-east-1.amazonaws.com"))
                       .forcePathStyle(true)
                       .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                       .httpClient(transport)
                       .build();
    }

    public static List<Case> all() {
        return List.of(
            new Case("list-objects-v2",
                     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<ListBucketResult><Name>wirediff-bucket</Name><Prefix>logs/</Prefix>"
                     + "<KeyCount>2</KeyCount><MaxKeys>10</MaxKeys><IsTruncated>false</IsTruncated>"
                     + "<Contents><Key>logs/a.txt</Key><Size>11</Size><StorageClass>STANDARD</StorageClass></Contents>"
                     + "<Contents><Key>logs/b.txt</Key><Size>22</Size><StorageClass>STANDARD</StorageClass></Contents>"
                     + "</ListBucketResult>",
                     s3 -> s3.listObjectsV2(r -> r.bucket("wirediff-bucket")
                                                  .prefix("logs/")
                                                  .maxKeys(10)
                                                  .delimiter("/"))),

            new Case("delete-object",
                     "",
                     s3 -> s3.deleteObject(r -> r.bucket("wirediff-bucket")
                                                 .key("nested/path/with spaces/object.txt"))),

            new Case("head-object",
                     "",
                     s3 -> s3.headObject(r -> r.bucket("wirediff-bucket")
                                               .key("logs/a.txt")
                                               .ifMatch("\"etag-value\""))),

            new Case("complete-multipart-upload",
                     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<CompleteMultipartUploadResult><Location>https://example/o</Location>"
                     + "<Bucket>wirediff-bucket</Bucket><Key>big.bin</Key><ETag>\"final\"</ETag>"
                     + "</CompleteMultipartUploadResult>",
                     s3 -> s3.completeMultipartUpload(
                         r -> r.bucket("wirediff-bucket")
                               .key("big.bin")
                               .uploadId("upload-id-1")
                               .multipartUpload(CompletedMultipartUpload.builder()
                                                                        .parts(CompletedPart.builder()
                                                                                            .partNumber(1)
                                                                                            .eTag("\"part-1\"")
                                                                                            .build(),
                                                                               CompletedPart.builder()
                                                                                            .partNumber(2)
                                                                                            .eTag("\"part-2\"")
                                                                                            .build())
                                                                        .build()))),

            new Case("put-object-tagging",
                     "",
                     s3 -> s3.putObjectTagging(
                         r -> r.bucket("wirediff-bucket")
                               .key("logs/a.txt")
                               .tagging(Tagging.builder()
                                               .tagSet(Tag.builder().key("env").value("test").build(),
                                                       Tag.builder().key("team").value("bridge").build())
                                               .build()))),

            new Case("copy-object",
                     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                     + "<CopyObjectResult><ETag>\"copied\"</ETag>"
                     + "<LastModified>2026-01-01T00:00:00.000Z</LastModified></CopyObjectResult>",
                     s3 -> s3.copyObject(r -> r.sourceBucket("src-bucket")
                                               .sourceKey("src/key.txt")
                                               .destinationBucket("wirediff-bucket")
                                               .destinationKey("dst/key.txt")
                                               .cacheControl("max-age=60")
                                               .contentType("text/plain")
                                               .metadataDirective("REPLACE")
                                               .metadata(Map.of("owner", "wirediff", "purpose", "byte-diff")))),

            new Case("put-object",
                     "",
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket")
                                              .key("logs/a.txt")
                                              .contentType("text/plain")
                                              .metadata(Map.of("origin", "wirediff")),
                                        RequestBody.fromString(PAYLOAD, StandardCharsets.UTF_8))),

            new Case("get-object",
                     PAYLOAD,
                     s3 -> s3.getObject(r -> r.bucket("wirediff-bucket")
                                              .key("logs/a.txt")
                                              .range("bytes=0-37"),
                                        ResponseTransformer.toBytes())),

            new Case("write-get-object-response",
                     "",
                     s3 -> s3.writeGetObjectResponse(r -> r.requestRoute("route-1").requestToken("token-1"),
                                                     RequestBody.fromString(PAYLOAD, StandardCharsets.UTF_8))),

            new Case("upload-part",
                     "",
                     s3 -> s3.uploadPart(r -> r.bucket("wirediff-bucket")
                                               .key("big.bin")
                                               .uploadId("upload-id-1")
                                               .partNumber(2),
                                         RequestBody.fromString(PAYLOAD, StandardCharsets.UTF_8)))
        );
    }

    /**
     * One diffable operation: a name for the golden file, the response the transport should reply with,
     * and the call itself.
     *
     * <p>The response body matters even though this harness diffs <em>requests</em>: a client that
     * cannot deserialize the reply throws before the assertion runs, so an unparseable canned response
     * turns a wire diff into a deserialization test with a confusing message.
     *
     * <p>{@code knownDifference} names the ledger entries that make a case differ for a reason specific
     * to it (as opposed to the pipeline-level differences {@code WireFormat} erases for every case). Such
     * a case reports as skipped-with-a-reason rather than failing. That is a deliberate compromise: a
     * permanent red gate stops being read, and silently passing would lose the finding, so the third
     * option is to stay visible and name the entry. It is a promise about the ledger, not an excuse —
     * remove the field when the entry is fixed.
     */
    public record Case(String name, String responseXml, Function<S3Client, Object> invoke,
                       String knownDifference) {

        public Case(String name, String responseXml, Function<S3Client, Object> invoke) {
            this(name, responseXml, invoke, null);
        }
    }
}
