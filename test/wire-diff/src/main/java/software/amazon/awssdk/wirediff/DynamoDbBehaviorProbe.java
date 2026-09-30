package software.amazon.awssdk.wirediff;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A differential probe of two DynamoDB behaviors that are driven by model metadata rather than by the
 * wire binding, run on a stock SDK and on the bridge and compared line for line:
 *
 * <ul>
 *   <li><b>{@code x-amz-crc32} response validation</b> (ledger 6.1) — a correct and a corrupted header, on
 *       the sync and async clients, with the server's attempt count, since v2 retries a mismatch.</li>
 *   <li><b>{@code @idempotencyToken} auto-fill</b> — {@code TransactWriteItems.ClientRequestToken}, left
 *       unset by the caller, against a server that fails the first attempt with a 500: whether a token is
 *       sent, and whether the retry sends the <em>same</em> one, which is the point of the token.</li>
 * </ul>
 */
public final class DynamoDbBehaviorProbe {

    private static final String ITEM = "{\"Item\":{\"id\":{\"S\":\"a\"}}}";
    private static final Pattern TOKEN = Pattern.compile("\"ClientRequestToken\":\"([^\"]+)\"");

    private DynamoDbBehaviorProbe() {
    }

    public static void main(String[] args) {
        run().forEach(System.out::println);
    }

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        for (boolean corrupt : new boolean[] {false, true}) {
            out.add("sync  crc32 " + (corrupt ? "corrupt" : "correct") + " -> " + crc32(false, corrupt));
            out.add("async crc32 " + (corrupt ? "corrupt" : "correct") + " -> " + crc32(true, corrupt));
        }
        out.add("sync  idempotency-token -> " + idempotencyToken(false));
        out.add("async idempotency-token -> " + idempotencyToken(true));
        return out;
    }

    private static String crc32(boolean async, boolean corrupt) {
        CRC32 crc = new CRC32();
        crc.update(ITEM.getBytes(StandardCharsets.UTF_8));
        String header = String.valueOf(corrupt ? crc.getValue() + 1 : crc.getValue());
        AtomicInteger attempts = new AtomicInteger();
        try (LocalHttpServer server = LocalHttpServer.start()) {
            server.route(r -> {
                attempts.incrementAndGet();
                return LocalHttpServer.Response.ok(ITEM.getBytes(StandardCharsets.UTF_8), "application/x-amz-json-1.0")
                                               .header("x-amz-crc32", header);
            });
            String outcome;
            try {
                Map<String, AttributeValue> key = Map.of("id", AttributeValue.fromS("a"));
                if (async) {
                    try (DynamoDbAsyncClient ddb = asyncClient(server)) {
                        ddb.getItem(r -> r.tableName("t").key(key)).join();
                    }
                } else {
                    try (DynamoDbClient ddb = syncClient(server)) {
                        ddb.getItem(r -> r.tableName("t").key(key));
                    }
                }
                outcome = "OK";
            } catch (RuntimeException e) {
                outcome = "FAIL " + innermostSdkType(e);
            }
            return outcome + ", attempts=" + attempts.get();
        }
    }

    private static String idempotencyToken(boolean async) {
        List<String> tokens = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        try (LocalHttpServer server = LocalHttpServer.start()) {
            server.route(r -> {
                Matcher m = TOKEN.matcher(r.bodyAsString());
                synchronized (tokens) {
                    tokens.add(m.find() ? m.group(1) : null);
                }
                if (attempts.incrementAndGet() == 1) {
                    return LocalHttpServer.Response.of(500, "{\"__type\":\"InternalServerError\"}".getBytes(StandardCharsets.UTF_8),
                                                       "application/x-amz-json-1.0");
                }
                return LocalHttpServer.Response.ok("{}".getBytes(StandardCharsets.UTF_8), "application/x-amz-json-1.0");
            });
            try {
                if (async) {
                    try (DynamoDbAsyncClient ddb = asyncClient(server)) {
                        ddb.transactWriteItems(r -> r.transactItems(i -> i.conditionCheck(
                            c -> c.tableName("t").key(Map.of("id", AttributeValue.fromS("a")))
                                  .conditionExpression("attribute_exists(id)")))).join();
                    }
                } else {
                    try (DynamoDbClient ddb = syncClient(server)) {
                        ddb.transactWriteItems(r -> r.transactItems(i -> i.conditionCheck(
                            c -> c.tableName("t").key(Map.of("id", AttributeValue.fromS("a")))
                                  .conditionExpression("attribute_exists(id)"))));
                    }
                }
            } catch (RuntimeException e) {
                if (Boolean.getBoolean("probe.trace")) { e.printStackTrace(); }
                return "FAIL " + innermostSdkType(e);
            }
        }
        boolean present = !tokens.isEmpty() && tokens.stream().allMatch(t -> t != null && t.length() == 36);
        boolean stable = tokens.stream().distinct().count() == 1;
        return "attempts=" + tokens.size() + " token-present=" + present + " same-token-on-retry=" + stable;
    }

    private static DynamoDbClient syncClient(LocalHttpServer server) {
        return DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                             .endpointOverride(server.uri("")).build();
    }

    private static DynamoDbAsyncClient asyncClient(LocalHttpServer server) {
        return DynamoDbAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                  .endpointOverride(server.uri("")).build();
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDPROBE", "probe/secret"));
    }

    private static String innermostSdkType(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && !(t instanceof software.amazon.awssdk.core.exception.SdkException)) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName();
    }
}
