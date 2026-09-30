package software.amazon.awssdk.wirediff;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;

/**
 * A differential probe of response checksum validation: run on a stock SDK and on the bridge, and the two
 * outputs are compared line for line.
 *
 * <p>Response validation is behavior rather than bytes on the wire, so the golden-file harness cannot see
 * it; and reading v2's rules off its source (validation mode, {@code WHEN_SUPPORTED}, which algorithms, what
 * a missing header does) is exactly the kind of inference that has been wrong on this branch before. So
 * this probe records what a stock client actually does in each case, and the bridge is held to that.
 *
 * <p>Output is one line per case: {@code <client> <mode> <header> -> OK <n bytes> | FAIL <exception type>}.
 */
public final class ResponseChecksumProbe {

    /** CRC32 of {@link S3Cases#PAYLOAD}, as the stock trailer golden shows it. */
    static final String CORRECT = "KQA2vw==";
    static final String CORRUPT = "AAAAAA==";

    private ResponseChecksumProbe() {
    }

    public static void main(String[] args) {
        run().forEach(System.out::println);
    }

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        for (String mode : List.of("mode-enabled", "mode-unset")) {
            for (String header : List.of("correct", "corrupt", "absent")) {
                out.add("sync  " + mode + " " + header + " -> " + sync(mode, header));
                out.add("async " + mode + " " + header + " -> " + async(mode, header));
            }
        }
        // S3's legacy trailing MD5: the body carries 16 extra bytes and the response says so.
        for (String trailer : List.of("md5-correct", "md5-corrupt")) {
            out.add("sync  append-md5 " + trailer + " -> " + syncTrailer(trailer));
            out.add("async append-md5 " + trailer + " -> " + asyncTrailer(trailer));
        }
        return out;
    }

    private static byte[] withMd5Trailer(boolean corrupt) {
        byte[] payload = S3Cases.PAYLOAD.getBytes(StandardCharsets.UTF_8);
        byte[] md5;
        try {
            md5 = java.security.MessageDigest.getInstance("MD5").digest(payload);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        if (corrupt) {
            md5[0] ^= 1;
        }
        byte[] body = java.util.Arrays.copyOf(payload, payload.length + md5.length);
        System.arraycopy(md5, 0, body, payload.length, md5.length);
        return body;
    }

    private static String syncTrailer(String trailer) {
        CapturingHttpClient transport = new CapturingHttpClient(200, withMd5Trailer(trailer.endsWith("corrupt")),
                                                                "application/octet-stream")
            .withResponseHeader("x-amz-transfer-encoding", "append-md5");
        try (S3Client s3 = S3ChecksumCases.client(transport, URI.create("https://s3.us-east-1.amazonaws.com"))) {
            byte[] got = s3.getObject(r -> r.bucket("b").key("k"), ResponseTransformer.toBytes()).asByteArray();
            return "OK " + got.length + " bytes, payload-intact=" + java.util.Arrays.equals(
                got, S3Cases.PAYLOAD.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return "FAIL " + describe(e);
        }
    }

    private static String asyncTrailer(String trailer) {
        CapturingAsyncHttpClient transport = new CapturingAsyncHttpClient(200, withMd5Trailer(trailer.endsWith("corrupt")),
                                                                          "application/octet-stream")
            .withResponseHeader("x-amz-transfer-encoding", "append-md5");
        try (S3AsyncClient s3 = S3ChecksumCases.asyncClient(transport, URI.create("https://s3.us-east-1.amazonaws.com"))) {
            byte[] got = s3.getObject(r -> r.bucket("b").key("k"), AsyncResponseTransformer.toBytes()).join().asByteArray();
            return "OK " + got.length + " bytes, payload-intact=" + java.util.Arrays.equals(
                got, S3Cases.PAYLOAD.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return "FAIL " + describe(e);
        }
    }

    private static String headerValue(String header) {
        return "correct".equals(header) ? CORRECT : "corrupt".equals(header) ? CORRUPT : null;
    }

    private static String sync(String mode, String header) {
        CapturingHttpClient transport = new CapturingHttpClient(200, S3Cases.PAYLOAD.getBytes(StandardCharsets.UTF_8),
                                                                "application/octet-stream");
        if (headerValue(header) != null) {
            transport.withResponseHeader("x-amz-checksum-crc32", headerValue(header));
        }
        try (S3Client s3 = S3ChecksumCases.client(transport, URI.create("https://s3.us-east-1.amazonaws.com"))) {
            byte[] got = s3.getObject(r -> r.bucket("b").key("k")
                                            .checksumMode("mode-enabled".equals(mode) ? ChecksumMode.ENABLED : null),
                                      ResponseTransformer.toBytes()).asByteArray();
            return "OK " + got.length + " bytes" + modeHeader(transport.only());
        } catch (RuntimeException e) {
            if (Boolean.getBoolean("probe.trace")) {
                e.printStackTrace();
            }
            return "FAIL " + describe(e);
        }
    }

    private static String async(String mode, String header) {
        CapturingAsyncHttpClient transport =
            new CapturingAsyncHttpClient(200, S3Cases.PAYLOAD.getBytes(StandardCharsets.UTF_8), "application/octet-stream");
        if (headerValue(header) != null) {
            transport.withResponseHeader("x-amz-checksum-crc32", headerValue(header));
        }
        try (S3AsyncClient s3 = S3ChecksumCases.asyncClient(transport, URI.create("https://s3.us-east-1.amazonaws.com"))) {
            byte[] got = s3.getObject(r -> r.bucket("b").key("k")
                                            .checksumMode("mode-enabled".equals(mode) ? ChecksumMode.ENABLED : null),
                                      AsyncResponseTransformer.toBytes()).join().asByteArray();
            return "OK " + got.length + " bytes" + modeHeader(transport.only());
        } catch (RuntimeException e) {
            return "FAIL " + describe(e);
        }
    }

    private static String modeHeader(CapturingHttpClient.CapturedRequest sent) {
        return " (sent x-amz-checksum-mode: " + sent.request().firstMatchingHeader("x-amz-checksum-mode").orElse("-") + ")";
    }

    /** The innermost SDK exception type and whether its message is v2's checksum-mismatch message. */
    private static String describe(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && (t instanceof java.util.concurrent.CompletionException
                                        || !(t instanceof software.amazon.awssdk.core.exception.SdkException))) {
            t = t.getCause();
        }
        String message = String.valueOf(t.getMessage());
        return t.getClass().getSimpleName()
               + (message.contains("different checksum than expected") ? " (checksum mismatch)" : " (" + message + ")");
    }
}
