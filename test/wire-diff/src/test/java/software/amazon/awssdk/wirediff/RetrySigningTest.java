package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * A retried request is signed exactly like its first attempt.
 *
 * <p>smithy-java hands the signer the previous attempt's signed request on a retry. The v2 signer bridge used
 * to sign that as it was: the stale {@code Authorization} header went into {@code SignedHeaders}, so every
 * retry carried a signature a real service rejects, and an {@code aws-chunked} trailer body would have been
 * framed twice. A mock server does not check signatures, so nothing noticed until signer verification
 * compared the two signers on a retry. Both signing paths are covered: S3 with a checksum trailer (v2's
 * signer) and DynamoDB (smithy-java's signer, which the bridge uses when v2's would add nothing).
 */
class RetrySigningTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDRETRY", "retry/secret"));

    @Test
    void retriedS3UploadWithTrailerIsSignedLikeTheFirstAttempt() {
        CapturingHttpClient transport = CapturingHttpClient.xml("").failFirst(1);
        try (S3Client s3 = S3ChecksumCases.client(transport, URI.create("https://s3.us-east-1.amazonaws.com"))) {
            s3.putObject(r -> r.bucket("b").key("k"), RequestBody.fromString(S3Cases.PAYLOAD));
        }
        assertAttemptsSignedAlike(transport.captured());
    }

    @Test
    void retriedDynamoDbCallIsSignedLikeTheFirstAttempt() {
        CapturingHttpClient transport = new CapturingHttpClient(200, "{}", "application/x-amz-json-1.0").failFirst(1);
        try (DynamoDbClient ddb = DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                                                .endpointOverride(URI.create("https://dynamodb.us-east-1.amazonaws.com"))
                                                .httpClient(transport).build()) {
            ddb.getItem(r -> r.tableName("t").key(Map.of("id", AttributeValue.fromS("a"))));
        }
        assertAttemptsSignedAlike(transport.captured());
    }

    private static void assertAttemptsSignedAlike(List<CapturingHttpClient.CapturedRequest> attempts) {
        assertEquals(2, attempts.size(), "expected the 500 to be retried once");
        String first = signedHeaders(attempts.get(0));
        String second = signedHeaders(attempts.get(1));
        assertFalse(second.contains("authorization"), "the retry signed the previous attempt's Authorization: " + second);
        assertEquals(first, second, "the retry signed a different set of headers");
        assertEquals(new String(attempts.get(0).body()), new String(attempts.get(1).body()),
                     "the retry sent a different body (a trailer framed twice?)");
    }

    private static String signedHeaders(CapturingHttpClient.CapturedRequest request) {
        String authorization = request.request().firstMatchingHeader("Authorization").orElse("");
        int start = authorization.indexOf("SignedHeaders=");
        return authorization.substring(start, authorization.indexOf(',', start));
    }
}
