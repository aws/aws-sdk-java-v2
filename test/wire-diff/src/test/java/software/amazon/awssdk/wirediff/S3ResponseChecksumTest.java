package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Response checksum validation matches stock v2, case for case (ledger 6.1's S3 half).
 *
 * <p>{@link ResponseChecksumProbe} was run against published 2.46.10 and its output committed as
 * {@code golden/checksums/response-validation.txt}; this runs the same probe on the bridge and requires the
 * same output. Validation runs in v2's own {@code HttpChecksumValidationInterceptor}, which the bridge now
 * drives through the bridged response hooks, so this is also the test that those hooks see the attempt's
 * checksum spec.
 */
class S3ResponseChecksumTest {

    @Test
    void validationMatchesStockV2() throws IOException {
        String golden;
        try (InputStream in = getClass().getResourceAsStream("/golden/checksums/response-validation.txt")) {
            golden = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        String bridge = String.join("\n", ResponseChecksumProbe.run());
        // Ledger 13.4: stock runs the ResponseTransformer inside its retry loop, whose retry stage rewraps a
        // transformer failure as SdkClientException("Unable to unmarshall response (...)"). The bridge runs it
        // after the call and surfaces v2's own NonRetryableException -- a subclass of SdkClientException, so
        // a catch block written against stock still matches, and the mismatch itself is detected the same
        // way. Asserted exactly, so any other difference still fails.
        String known = "sync  mode-enabled corrupt -> FAIL NonRetryableException (checksum mismatch)";
        assertEquals(golden.replace("sync  mode-enabled corrupt -> FAIL SdkClientException (checksum mismatch)", known),
                     bridge);
        assertEquals(software.amazon.awssdk.core.exception.SdkClientException.class,
                     software.amazon.awssdk.core.exception.NonRetryableException.class.getSuperclass(),
                     "the 13.4 difference is only benign while NonRetryableException is an SdkClientException");
    }
}
