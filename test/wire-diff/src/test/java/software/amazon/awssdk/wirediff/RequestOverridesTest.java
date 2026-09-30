package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Every field of a request's {@code overrideConfiguration()} does on the bridge what it does on stock v2
 * (ledger 2.3, 2.4, 8.1, 8.2): {@link RequestOverrideProbe} was run on published 2.46.10 and committed as
 * {@code golden/request-overrides.txt}, and the bridge must print the same, sync and async.
 *
 * <p>The attempt-timeout case also covers ledger 3.6: stock retries a timed-out attempt, which the bridge can
 * only do because it defers transport failures into smithy-java's retry loop ({@code V2DeferredTransportFailure}).
 */
class RequestOverridesTest {

    @Test
    void everyOverrideFieldBehavesAsOnStockV2() throws IOException {
        String golden;
        try (InputStream in = getClass().getResourceAsStream("/golden/request-overrides.txt")) {
            golden = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        assertEquals(golden, String.join("\n", RequestOverrideProbe.run()));
    }
}
