package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * {@link DynamoDbBehaviorProbe} on the bridge must print what it printed on published 2.46.10
 * ({@code golden/dynamodb-behavior.txt}): {@code x-amz-crc32} validation on both client types (ledger 6.1),
 * idempotency-token auto-fill (ledger 1.8), and — because the token case fails its first attempt with a 500 —
 * that a server error is retried at all.
 *
 * <p>That last property is the reason this test exists. It regressed once, silently, when an interceptor was
 * added ahead of {@code V2ErrorEnricher} (ledger 3.7), and no other suite in this module noticed, because
 * every other one uses a transport that always succeeds.
 */
class DynamoDbBehaviorTest {

    @Test
    void behaviorMatchesStockV2() throws IOException {
        String golden;
        try (InputStream in = getClass().getResourceAsStream("/golden/dynamodb-behavior.txt")) {
            golden = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        assertEquals(golden, String.join("\n", DynamoDbBehaviorProbe.run()));
    }
}
