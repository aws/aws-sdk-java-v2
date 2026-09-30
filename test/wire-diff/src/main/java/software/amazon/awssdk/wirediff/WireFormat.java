package software.amazon.awssdk.wirediff;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Renders a captured request as stable, reviewable text.
 *
 * <p>The output is the artifact that gets committed as a golden file and the thing a failure diff is
 * shown in, so two properties matter more than compactness: it must be byte-identical across runs of
 * the same code, and a human must be able to see what changed. Hence sorted headers and query
 * parameters, and a body printed as text when it is text.
 *
 * <h2>What is normalized, and why that is not cheating</h2>
 *
 * <p>Four header values cannot be stable across runs and are replaced by placeholders:
 *
 * <ul>
 *   <li>{@code X-Amz-Date} — wall clock.</li>
 *   <li>{@code amz-sdk-invocation-id} — a fresh UUID per call.</li>
 *   <li>{@code User-Agent} — carries versions and JVM details. Deliberately excluded rather than
 *       normalized-in-place, because it legitimately differs between a bridged and a stock client and
 *       that difference belongs in the compatibility ledger, not in every single wire diff.</li>
 *   <li>{@code Authorization} — reduced to its {@code SignedHeaders} list. The signature itself is a
 *       function of the clock and so cannot be compared, but <em>which</em> headers were signed is a
 *       real behavioral fact and the part a bridge can plausibly get wrong. Keeping it means this
 *       harness does check signing coverage; it just cannot check the signature bytes.</li>
 * </ul>
 *
 * <p>Everything else — method, path, every query parameter, every other header including
 * {@code x-amz-content-sha256} and {@code Content-Length}, and the entire body — is compared verbatim.
 *
 * <h2>Known differences</h2>
 *
 * <p>{@link #renderIgnoringKnownDifferences} additionally erases the pipeline-level differences already
 * recorded in {@code compatability_issues.md} §12.8-12.13. Those are uniform per operation and
 * not what this harness exists to watch, so leaving them in makes all six cases fail forever and the
 * gate stops catching new regressions. Each erasure names its ledger entry; nothing is normalized away
 * that is not written down there, and {@link #render} still produces the unabridged text for the
 * message on a failure.
 */
public final class WireFormat {

    private static final String PLACEHOLDER = "<normalized>";

    /**
     * Headers the bridge does not send at all, or sends where v2 does not. Dropped from both sides —
     * including from the {@code SignedHeaders} list, which would otherwise report the same difference a
     * second time.
     */
    private static final Set<String> KNOWN_DIFFERENT_HEADERS = Set.of(
        "amz-sdk-invocation-id",        // 12.10 retry telemetry, absent from the bridge
        "amz-sdk-request",              // 12.10
        "host",                         // 12.11 not in the bridge's header map; added by the transport
        "content-length",               // 12.11
        "x-amz-sdk-checksum-algorithm", // 12.9  request checksums are not computed
        "x-amz-checksum-crc32",         // 12.9
        "x-amz-checksum-crc32c",        // 12.9
        "x-amz-checksum-sha1",          // 12.9
        "x-amz-checksum-sha256",        // 12.9
        "content-md5");                 // 12.9

    /**
     * What the checksum-aware render drops: the {@link #KNOWN_DIFFERENT_HEADERS} that are not about
     * checksums. Everything a checksum changes — the checksum headers themselves, the algorithm header,
     * {@code x-amz-trailer}, {@code content-encoding: aws-chunked}, {@code x-amz-decoded-content-length},
     * and {@code x-amz-content-sha256} — is compared verbatim, because that is what those cases are for.
     */
    private static final Set<String> CHECKSUM_MODE_DROPPED = Set.of(
        "amz-sdk-invocation-id",        // 12.10
        "amz-sdk-request",              // 12.10
        "host",                         // 12.11
        "content-length");              // 12.11

    /** A chunk or trailer signature: a function of the clock, like the Authorization signature. */
    private static final java.util.regex.Pattern CHUNK_SIGNATURE =
        java.util.regex.Pattern.compile("(chunk-signature=|x-amz-trailer-signature:)[0-9a-f]{64}");

    private enum Mode { RAW, KNOWN, CHECKSUM }

    private WireFormat() {
    }

    /**
     * For the checksum goldens: {@link #renderIgnoringKnownDifferences}, but keeping every header and
     * body byte a checksum decision changes. Chunk and trailer signatures — present only when a streaming
     * body is chunk-signed over plain HTTP — are placeholdered, since they depend on the clock.
     */
    public static String renderChecksumAware(CapturingHttpClient.CapturedRequest captured) {
        return render(captured, Mode.CHECKSUM);
    }

    /** The full text, with only the four unstable values placeholdered. Used for golden capture. */
    public static String render(CapturingHttpClient.CapturedRequest captured) {
        return render(captured, Mode.RAW);
    }

    /** As {@link #render}, minus the differences ledgered in §12.8-12.13. Used for the assertion. */
    public static String renderIgnoringKnownDifferences(CapturingHttpClient.CapturedRequest captured) {
        return render(captured, Mode.KNOWN);
    }

    private static String render(CapturingHttpClient.CapturedRequest captured, Mode mode) {
        boolean ignoreKnown = mode != Mode.RAW;
        Set<String> dropped = mode == Mode.CHECKSUM ? CHECKSUM_MODE_DROPPED : KNOWN_DIFFERENT_HEADERS;
        SdkHttpFullRequest request = captured.request();
        StringBuilder sb = new StringBuilder(1024);

        // 12.12: an empty URI pattern leaves a trailing slash the v2 marshallers do not produce.
        String path = request.encodedPath();
        if (ignoreKnown && path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        sb.append(request.method()).append(' ').append(path).append('\n');
        sb.append("host: ").append(request.host()).append('\n');
        sb.append("protocol: ").append(request.protocol()).append('\n');

        sb.append("--- query ---\n");
        new TreeMap<>(request.rawQueryParameters()).forEach((name, values) -> {
            for (String value : values) {
                // A null value is a valueless parameter -- "?tagging", not "?tagging=". That is a real
                // byte difference on the wire, so the two cases must not both print as "name=".
                sb.append(name).append(value == null ? " <valueless>" : "=" + value).append('\n');
            }
        });

        sb.append("--- headers ---\n");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(request.headers());
        headers.forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (ignoreKnown && dropped.contains(lower)) {
                return;
            }
            for (String value : normalize(name, values, mode)) {
                sb.append(lower).append(": ").append(value).append('\n');
            }
        });

        byte[] body = ignoreKnown ? normalizeXmlBody(captured.body()) : captured.body();
        if (mode == Mode.CHECKSUM) {
            body = CHUNK_SIGNATURE.matcher(new String(body, StandardCharsets.ISO_8859_1))
                                  .replaceAll("$1" + PLACEHOLDER)
                                  .getBytes(StandardCharsets.ISO_8859_1);
        }
        sb.append("--- body (").append(body.length).append(" bytes) ---\n");
        sb.append(renderBody(body));
        if (body.length > 0) {
            sb.append('\n');
        }
        return sb.toString();
    }

    private static List<String> normalize(String name, List<String> values, Mode mode) {
        boolean ignoreKnown = mode != Mode.RAW;
        if (name.equalsIgnoreCase("X-Amz-Date")
            || name.equalsIgnoreCase("amz-sdk-invocation-id")
            || name.equalsIgnoreCase("User-Agent")) {
            return List.of(PLACEHOLDER);
        }
        // 12.8: v2's S3 signer sends UNSIGNED-PAYLOAD over HTTPS; smithy-java hashes the body on
        // non-streaming operations (streaming ones send UNSIGNED-PAYLOAD too -- 13.1). Both values are
        // accepted, and the cost -- not the value -- is the finding, so compare only that it was set.
        // S3StreamingTest asserts the literal value where it matters, which is where a regression to
        // hashing would mean buffering the object.
        if (mode == Mode.KNOWN && name.equalsIgnoreCase("x-amz-content-sha256")) {
            return List.of("<payload-hash-or-unsigned>");
        }
        if (name.equalsIgnoreCase("Authorization")) {
            List<String> out = new ArrayList<>(values.size());
            for (String value : values) {
                out.add(ignoreKnown ? signedHeadersIgnoring(value, mode == Mode.CHECKSUM ? CHECKSUM_MODE_DROPPED
                                                                                          : KNOWN_DIFFERENT_HEADERS)
                                    : signedHeadersOnly(value));
            }
            return out;
        }
        return values;
    }

    /**
     * Extracts {@code SignedHeaders=a;b;c} from a SigV4 Authorization header.
     *
     * <p>Falls back to the placeholder rather than the raw value if the header is not in the expected
     * form — a raw value would embed a signature and make the golden file unusable, which is a worse
     * failure than losing one assertion.
     */
    private static String signedHeadersOnly(String authorization) {
        int start = authorization.indexOf("SignedHeaders=");
        if (start < 0) {
            return PLACEHOLDER;
        }
        int end = authorization.indexOf(',', start);
        return end < 0 ? authorization.substring(start) : authorization.substring(start, end);
    }

    /**
     * {@link #signedHeadersOnly} with the headers of {@code dropped} removed.
     *
     * <p>Without this the signed-headers list restates every header difference, so a single ledgered
     * gap fails the comparison twice and a genuinely new signing difference is hard to spot among them.
     */
    private static String signedHeadersIgnoring(String authorization, Set<String> dropped) {
        String signed = signedHeadersOnly(authorization);
        if (!signed.startsWith("SignedHeaders=")) {
            return signed;
        }
        Set<String> kept = new LinkedHashSet<>();
        for (String header : signed.substring("SignedHeaders=".length()).split(";")) {
            if (!dropped.contains(header.toLowerCase(Locale.ROOT))) {
                kept.add(header);
            }
        }
        return "SignedHeaders=" + String.join(";", kept);
    }

    /**
     * Erases the three ledgered XML codec differences (12.13): the prolog, the root namespace, and
     * {@code &quot;} for a literal quote in element text.
     *
     * <p>Textual rather than parsed on purpose. A real XML comparison would also hide differences this
     * harness is meant to catch — element order, and whether a list is flattened — because those are
     * differences an XML model happily normalizes away.
     */
    private static byte[] normalizeXmlBody(byte[] body) {
        if (body.length == 0) {
            return body;
        }
        String text = new String(body, StandardCharsets.UTF_8);
        if (!text.startsWith("<")) {
            return body;
        }
        text = text.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        text = text.replace(" xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"", "");
        text = text.replace("&quot;", "\"");
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Text bodies verbatim, binary bodies as hex.
     *
     * <p>XML and form-encoded payloads are the point of this harness, so they must be readable in a
     * diff. The hex fallback exists so a checksum-trailer or event-stream body cannot silently render
     * as mojibake and compare equal by accident.
     */
    private static String renderBody(byte[] body) {
        if (body.length == 0) {
            return "";
        }
        String text = new String(body, StandardCharsets.UTF_8);
        if (text.getBytes(StandardCharsets.UTF_8).length == body.length && text.chars().noneMatch(c -> c < 9)) {
            return text;
        }
        StringBuilder hex = new StringBuilder(body.length * 2);
        for (byte b : body) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
