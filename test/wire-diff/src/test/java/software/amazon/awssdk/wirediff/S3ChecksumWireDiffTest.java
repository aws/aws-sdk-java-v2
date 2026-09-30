package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The checksum decisions of stock v2, diffed: every {@link S3ChecksumCases} case on the bridged sync and
 * async clients against goldens captured from published 2.46.10 with {@code CaptureMain --checksums}.
 *
 * <p>Unlike {@link S3WireDiffTest} this runs with v2's default checksum settings, and compares checksum
 * headers, {@code x-amz-content-sha256}, and the {@code aws-chunked} body — trailer included — verbatim
 * ({@link WireFormat#renderChecksumAware}). It is the test for ledger 12.8, 12.9, 13.2 and 13.3, which
 * the bridge now answers by signing with v2's own signer.
 */
class S3ChecksumWireDiffTest {

    static Stream<S3ChecksumCases.Case> cases() {
        return S3ChecksumCases.all().stream();
    }

    @ParameterizedTest(name = "sync {0}")
    @MethodSource("cases")
    void syncMatchesStock(S3ChecksumCases.Case testCase) {
        String golden = golden("sync", testCase.name());
        CapturingHttpClient transport = new CapturingHttpClient(200, testCase.responseBytes(), "application/xml");
        try (S3Client s3 = S3ChecksumCases.client(transport, testCase.endpoint())) {
            testCase.sync().apply(s3);
        } catch (RuntimeException e) {
            fail("sync " + testCase.name() + " threw " + e, e);
        }
        assertMatches(golden, transport.only(), "sync " + testCase.name());
    }

    @ParameterizedTest(name = "async {0}")
    @MethodSource("cases")
    void asyncMatchesStock(S3ChecksumCases.Case testCase) {
        String golden = golden("async", testCase.name());
        CapturingAsyncHttpClient transport = new CapturingAsyncHttpClient(200, testCase.responseBytes(), "application/xml");
        try (S3AsyncClient s3 = S3ChecksumCases.asyncClient(transport, testCase.endpoint())) {
            testCase.async().apply(s3);
        } catch (RuntimeException e) {
            fail("async " + testCase.name() + " threw " + e, e);
        }
        assertMatches(golden, transport.only(), "async " + testCase.name());
    }

    /**
     * Byte-identical, or identical except for a header checksum value that is correct for the bytes the
     * bridge actually sent.
     *
     * <p>The exception is ledger 12.13: for an XML request body the bridge's codec omits the prolog and root
     * namespace and does not escape {@code "}, so the body differs from stock's in bytes S3 does not care
     * about — and a checksum over those bytes differs with them. Comparing the value against stock's would
     * report 12.13 a second time; comparing it against the bridge's own body is the check that matters,
     * because a wrong checksum is a rejected request. So the value is recomputed over the sent body and
     * must match, and the rest of the request must still equal stock's.
     */
    private static void assertMatches(String golden, CapturingHttpClient.CapturedRequest sent, String label) {
        String actual = WireFormat.renderChecksumAware(sent);
        if (golden.equals(actual)) {
            return;
        }
        String header = sent.request().firstMatchingHeader("x-amz-checksum-crc32").orElse(null);
        if (header != null) {
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(sent.body());
            byte[] value = java.nio.ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
            assertEquals(java.util.Base64.getEncoder().encodeToString(value), header,
                         label + ": the CRC32 header is not the checksum of the body that was sent");
            String masked = "x-amz-checksum-crc32: <checksum of sent body>";
            assertEquals(golden.replaceAll("x-amz-checksum-crc32: \\S+", masked),
                         actual.replaceAll("x-amz-checksum-crc32: \\S+", masked),
                         label + " differs from stock v2 beyond the checksum value");
            return;
        }
        assertEquals(golden, actual, label + " differs from stock v2");
    }

    private static String golden(String flavor, String name) {
        try (InputStream in = S3ChecksumWireDiffTest.class.getResourceAsStream(
                 "/golden/checksums/" + flavor + "/" + name + ".txt")) {
            Assumptions.assumeTrue(in != null, "no golden for " + flavor + "/" + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
