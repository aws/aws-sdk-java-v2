package software.amazon.awssdk.wirediff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

/**
 * Cases for the checksum goldens: the same client as {@link S3Cases}, but with v2's <b>default</b>
 * checksum settings, {@code WHEN_SUPPORTED} for both calculation and validation.
 *
 * <p>{@link S3Cases} turns checksums off on both arms so that a byte diff is about the HTTP binding. That
 * was necessary while the bridge computed no checksums; it also meant nothing compared the checksum
 * behavior itself. These cases are that comparison, one per decision v2's checksum logic makes:
 *
 * <ul>
 *   <li>{@code put-object-tagging} — checksum <em>required</em>, non-streaming: a CRC32 header.</li>
 *   <li>{@code delete-objects} — required, with an XML list body, the other common required case.</li>
 *   <li>{@code put-object} — optional and streaming, over HTTPS: an unsigned {@code aws-chunked} body with
 *       the CRC32 in a trailer.</li>
 *   <li>{@code put-object-sha256} — the caller chose the algorithm, via the request's
 *       {@code ChecksumAlgorithm} member.</li>
 *   <li>{@code put-object-precomputed} — the caller supplied the checksum value, so nothing may be
 *       computed.</li>
 *   <li>{@code upload-part} — streaming plus bound query parameters.</li>
 *   <li>{@code put-object-http} — streaming over plain <b>HTTP</b>: chunk-signed, with a signed trailer.</li>
 *   <li>{@code get-object-checksum-mode} — response validation requested; only the request is diffed.</li>
 * </ul>
 *
 * <p>Each case exists for the sync and the async client, since v2 signs them through different code
 * ({@code sign} with a content provider, {@code signAsync} with a publisher).
 */
public final class S3ChecksumCases {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDWIREDIFFEXAMPLE", "wirediff/secret/key"));
    private static final URI HTTPS = URI.create("https://s3.us-east-1.amazonaws.com");
    private static final URI HTTP = URI.create("http://s3.us-east-1.amazonaws.com");

    private S3ChecksumCases() {
    }

    public static S3Client client(SdkHttpClient transport, URI endpoint) {
        return S3Client.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                       .endpointOverride(endpoint).forcePathStyle(true).httpClient(transport).build();
    }

    public static S3AsyncClient asyncClient(SdkAsyncHttpClient transport, URI endpoint) {
        return S3AsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                            .endpointOverride(endpoint).forcePathStyle(true).httpClient(transport).build();
    }

    private static Tagging tagging() {
        return Tagging.builder().tagSet(Tag.builder().key("env").value("test").build()).build();
    }

    private static Delete delete() {
        return Delete.builder().objects(ObjectIdentifier.builder().key("a.txt").build(),
                                        ObjectIdentifier.builder().key("b.txt").build()).build();
    }

    private static final String DELETE_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DeleteResult></DeleteResult>";

    public static List<Case> all() {
        return List.of(
            new Case("put-object-tagging", "", HTTPS,
                     s3 -> s3.putObjectTagging(r -> r.bucket("wirediff-bucket").key("k").tagging(tagging())),
                     s3 -> s3.putObjectTagging(r -> r.bucket("wirediff-bucket").key("k").tagging(tagging())).join()),
            new Case("delete-objects", DELETE_RESULT, HTTPS,
                     s3 -> s3.deleteObjects(r -> r.bucket("wirediff-bucket").delete(delete())),
                     s3 -> s3.deleteObjects(r -> r.bucket("wirediff-bucket").delete(delete())).join()),
            new Case("put-object", "", HTTPS,
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k"), RequestBody.fromString(S3Cases.PAYLOAD)),
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k"),
                                        AsyncRequestBody.fromString(S3Cases.PAYLOAD)).join()),
            new Case("put-object-sha256", "", HTTPS,
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k").checksumAlgorithm(ChecksumAlgorithm.SHA256),
                                        RequestBody.fromString(S3Cases.PAYLOAD)),
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k").checksumAlgorithm(ChecksumAlgorithm.SHA256),
                                        AsyncRequestBody.fromString(S3Cases.PAYLOAD)).join()),
            new Case("put-object-precomputed", "", HTTPS,
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k").checksumCRC32("AAAAAA=="),
                                        RequestBody.fromString(S3Cases.PAYLOAD)),
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k").checksumCRC32("AAAAAA=="),
                                        AsyncRequestBody.fromString(S3Cases.PAYLOAD)).join()),
            new Case("upload-part", "", HTTPS,
                     s3 -> s3.uploadPart(r -> r.bucket("wirediff-bucket").key("big.bin").uploadId("u").partNumber(2),
                                         RequestBody.fromString(S3Cases.PAYLOAD)),
                     s3 -> s3.uploadPart(r -> r.bucket("wirediff-bucket").key("big.bin").uploadId("u").partNumber(2),
                                         AsyncRequestBody.fromString(S3Cases.PAYLOAD)).join()),
            new Case("put-object-http", "", HTTP,
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k"), RequestBody.fromString(S3Cases.PAYLOAD)),
                     s3 -> s3.putObject(r -> r.bucket("wirediff-bucket").key("k"),
                                        AsyncRequestBody.fromString(S3Cases.PAYLOAD)).join()),
            new Case("get-object-checksum-mode", S3Cases.PAYLOAD, HTTPS,
                     s3 -> s3.getObject(r -> r.bucket("wirediff-bucket").key("k").checksumMode(ChecksumMode.ENABLED),
                                        ResponseTransformer.toBytes()),
                     s3 -> s3.getObject(r -> r.bucket("wirediff-bucket").key("k").checksumMode(ChecksumMode.ENABLED),
                                        AsyncResponseTransformer.toBytes()).join()));
    }

    /** One checksum case, with its sync and async forms. */
    public record Case(String name, String response, URI endpoint,
                       Function<S3Client, Object> sync, Function<S3AsyncClient, Object> async) {
        @Override
        public String toString() {
            return name;
        }

        public byte[] responseBytes() {
            return response.getBytes(StandardCharsets.UTF_8);
        }
    }
}
