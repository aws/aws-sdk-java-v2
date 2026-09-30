package software.amazon.awssdk.wirediff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

/**
 * {@link S3Cases}, through {@link S3AsyncClient}.
 *
 * <p>Same nine operations, same inputs, same canned responses, and the same client settings (path-style,
 * {@code WHEN_REQUIRED} checksums — see {@link S3Cases#client} for why), so that a case here and its sync
 * twin differ only in which client surface made the call. The goldens are separate all the same
 * ({@code golden/async/}), captured from a stock async client, because stock v2's async and sync
 * pipelines are not required to produce identical bytes, and assuming they do would turn a legitimate
 * stock difference into a false bridge failure.
 *
 * <p>Each call is joined, so a case is a synchronous function of the client like its sync twin, and a
 * failure surfaces as the exception the future completed with.
 */
public final class S3AsyncCases {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDWIREDIFFEXAMPLE", "wirediff/secret/key"));

    private S3AsyncCases() {
    }

    public static S3AsyncClient client(SdkAsyncHttpClient transport) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .credentialsProvider(CREDENTIALS)
                            .endpointOverride(URI.create("https://s3.us-east-1.amazonaws.com"))
                            .forcePathStyle(true)
                            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                            .httpClient(transport)
                            .build();
    }

    public static List<Case> all() {
        Map<String, S3Cases.Case> sync = new HashMap<>();
        S3Cases.all().forEach(c -> sync.put(c.name(), c));

        return List.of(
            twin(sync, "list-objects-v2",
                 s3 -> s3.listObjectsV2(r -> r.bucket("wirediff-bucket").prefix("logs/").maxKeys(10).delimiter("/")).join()),

            twin(sync, "delete-object",
                 s3 -> s3.deleteObject(r -> r.bucket("wirediff-bucket").key("nested/path/with spaces/object.txt")).join()),

            twin(sync, "head-object",
                 s3 -> s3.headObject(r -> r.bucket("wirediff-bucket").key("logs/a.txt").ifMatch("\"etag-value\"")).join()),

            twin(sync, "complete-multipart-upload",
                 s3 -> s3.completeMultipartUpload(
                     r -> r.bucket("wirediff-bucket")
                           .key("big.bin")
                           .uploadId("upload-id-1")
                           .multipartUpload(CompletedMultipartUpload.builder()
                                                                    .parts(CompletedPart.builder().partNumber(1)
                                                                                        .eTag("\"part-1\"").build(),
                                                                           CompletedPart.builder().partNumber(2)
                                                                                        .eTag("\"part-2\"").build())
                                                                    .build())).join()),

            twin(sync, "put-object-tagging",
                 s3 -> s3.putObjectTagging(
                     r -> r.bucket("wirediff-bucket")
                           .key("logs/a.txt")
                           .tagging(Tagging.builder()
                                           .tagSet(Tag.builder().key("env").value("test").build(),
                                                   Tag.builder().key("team").value("bridge").build())
                                           .build())).join()),

            twin(sync, "copy-object",
                 s3 -> s3.copyObject(r -> r.sourceBucket("src-bucket")
                                           .sourceKey("src/key.txt")
                                           .destinationBucket("wirediff-bucket")
                                           .destinationKey("dst/key.txt")
                                           .cacheControl("max-age=60")
                                           .contentType("text/plain")
                                           .metadataDirective("REPLACE")
                                           .metadata(Map.of("owner", "wirediff", "purpose", "byte-diff"))).join()),

            twin(sync, "put-object",
                 s3 -> s3.putObject(r -> r.bucket("wirediff-bucket")
                                          .key("logs/a.txt")
                                          .contentType("text/plain")
                                          .metadata(Map.of("origin", "wirediff")),
                                    AsyncRequestBody.fromString(S3Cases.PAYLOAD, StandardCharsets.UTF_8)).join()),

            twin(sync, "get-object",
                 s3 -> s3.getObject(r -> r.bucket("wirediff-bucket").key("logs/a.txt").range("bytes=0-37"),
                                    AsyncResponseTransformer.toBytes()).join()),

            twin(sync, "write-get-object-response",
                 s3 -> s3.writeGetObjectResponse(r -> r.requestRoute("route-1").requestToken("token-1"),
                                                 AsyncRequestBody.fromString(S3Cases.PAYLOAD, StandardCharsets.UTF_8)).join()),

            twin(sync, "upload-part",
                 s3 -> s3.uploadPart(r -> r.bucket("wirediff-bucket").key("big.bin").uploadId("upload-id-1").partNumber(2),
                                     AsyncRequestBody.fromString(S3Cases.PAYLOAD, StandardCharsets.UTF_8)).join())
        );
    }

    /** Takes the response and the known difference from the sync twin, so the two lists cannot drift. */
    private static Case twin(Map<String, S3Cases.Case> sync, String name, Function<S3AsyncClient, Object> invoke) {
        S3Cases.Case syncCase = sync.get(name);
        if (syncCase == null) {
            throw new IllegalStateException("no sync twin for async case " + name);
        }
        return new Case(name, syncCase.responseXml(), invoke, syncCase.knownDifference());
    }

    /** One async case; the fields mean what they mean on {@link S3Cases.Case}. */
    public record Case(String name, String responseXml, Function<S3AsyncClient, Object> invoke,
                       String knownDifference) {
        @Override
        public String toString() {
            return name;
        }
    }
}
