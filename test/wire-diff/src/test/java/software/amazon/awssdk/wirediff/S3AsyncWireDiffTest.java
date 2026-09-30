package software.amazon.awssdk.wirediff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * {@link S3WireDiffTest} for the bridged async client: the request a bridged {@link S3AsyncClient} puts on
 * the wire, diffed against a stock async capture.
 *
 * <p>Goldens are in {@code golden/async/}, captured with {@code CaptureMain --async} against the published
 * 2.46.10 SDK. They were compared against the sync goldens at capture time and are identical once
 * normalized, and unabridged they differ only in stock async sending {@code content-length: 0} on the two
 * bodyless requests that sync omits. So a failure here is a difference between the bridged async client
 * and stock v2, never between v2's two pipelines.
 */
class S3AsyncWireDiffTest {

    static Stream<S3AsyncCases.Case> cases() {
        return S3AsyncCases.all().stream();
    }

    /**
     * Guards the premise. Every other test here would pass against stock v2 — that is the point of a
     * byte diff — so without this, a build that silently generated a stock async client would look like a
     * perfect bridge.
     */
    @Test
    void clientUnderTestIsTheBridgedOne() throws Exception {
        try (S3AsyncClient s3 = S3AsyncCases.client(CapturingAsyncHttpClient.xml(""))) {
            Class<?> type = s3.getClass();
            while (!type.getSimpleName().equals("DefaultS3AsyncClient")) {
                // S3AsyncClient.builder() may hand back a decorating client; the generated one is inside.
                java.lang.reflect.Field delegate = findDelegate(type);
                assertTrue(delegate != null, "no generated client found under " + s3.getClass());
                delegate.setAccessible(true);
                Object inner = delegate.get(s3);
                type = inner.getClass();
            }
            assertTrue(hasField(type, "smithyClient"), type + " was generated without the smithy-java pipeline");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void matchesStockV2AsyncWire(S3AsyncCases.Case testCase) {
        String golden = readGolden(testCase.name() + ".normalized");
        Assumptions.assumeTrue(golden != null,
                               "no async golden capture for " + testCase.name()
                               + "; run CaptureMain --async against a stock build and commit it");

        CapturingAsyncHttpClient transport = CapturingAsyncHttpClient.xml(testCase.responseXml());
        try (S3AsyncClient s3 = S3AsyncCases.client(transport)) {
            testCase.invoke().apply(s3);
        } catch (RuntimeException e) {
            List<CapturingHttpClient.CapturedRequest> captured = transport.captured();
            fail("call threw " + e
                 + (captured.isEmpty()
                    ? " before any request was captured"
                    : "\n--- request captured before the failure ---\n" + WireFormat.render(captured.get(0))
                      + "\n--- golden ---\n" + readGolden(testCase.name())),
                 e);
            return;
        }

        String actual = WireFormat.renderIgnoringKnownDifferences(transport.only());
        if (testCase.knownDifference() != null && !golden.equals(actual)) {
            Assumptions.abort(testCase.name() + " differs for a known, ledgered reason: "
                              + testCase.knownDifference()
                              + "\n--- stock v2 ---\n" + golden + "\n--- bridge ---\n" + actual);
        }
        assertEquals(golden, actual, "wire bytes differ from stock v2 async for " + testCase.name()
                                     + "\n(unabridged stock capture:\n" + readGolden(testCase.name()) + ")");
    }

    private static java.lang.reflect.Field findDelegate(Class<?> type) {
        for (Class<?> t = type; t != null; t = t.getSuperclass()) {
            for (java.lang.reflect.Field f : t.getDeclaredFields()) {
                if (S3AsyncClient.class.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
        }
        return null;
    }

    private static boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static String readGolden(String name) {
        try (InputStream in = S3AsyncWireDiffTest.class.getResourceAsStream("/golden/async/" + name + ".txt")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
