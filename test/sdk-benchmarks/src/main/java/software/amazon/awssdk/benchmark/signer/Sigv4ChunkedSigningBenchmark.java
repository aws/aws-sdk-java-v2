/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.signer;

import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.CONTENT_ENCODING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Thread)
public class Sigv4ChunkedSigningBenchmark {
    private static final int DRAIN_BUFFER_SIZE = 16 * 1024;
    private static final Clock SIGNING_CLOCK =
        Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final AwsCredentialsIdentity CREDENTIALS =
        AwsCredentialsIdentity.create("access-key", "secret-key");

    @Param({"KIB_64", "MIB_1", "MIB_16"})
    private PayloadSize payloadSize;

    @Param({"SIGNED_PAYLOAD", "SIGNED_CHECKSUM_TRAILER", "UNSIGNED_CHECKSUM_TRAILER"})
    private SigningMode signingMode;

    private final byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];

    private AwsV4HttpSigner signer;
    private SignRequest<AwsCredentialsIdentity> signRequest;

    @Setup
    public void setup() {
        byte[] payload = new byte[payloadSize.bytes];
        Arrays.fill(payload, (byte) 'a');

        ContentStreamProvider content = () -> new ByteArrayInputStream(payload);
        SdkHttpRequest request =
            SdkHttpRequest.builder()
                          .uri(URI.create("https://s3.us-east-1.amazonaws.com/bucket/key"))
                          .method(SdkHttpMethod.PUT)
                          .putHeader(Header.CONTENT_LENGTH, Integer.toString(payload.length))
                          .build();

        SignRequest.Builder<AwsCredentialsIdentity> requestBuilder =
            SignRequest.builder(CREDENTIALS)
                       .request(request)
                       .payload(content)
                       .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                       .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "s3")
                       .putProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                       .putProperty(AwsV4HttpSigner.NORMALIZE_PATH, false)
                       .putProperty(AwsV4HttpSigner.CHUNK_ENCODING_ENABLED, true)
                       .putProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, signingMode.payloadSigning)
                       .putProperty(HttpSigner.SIGNING_CLOCK, SIGNING_CLOCK);

        if (signingMode.checksumAlgorithm != null) {
            requestBuilder.putProperty(AwsV4HttpSigner.CHECKSUM_ALGORITHM, signingMode.checksumAlgorithm);
        }

        signer = AwsV4HttpSigner.create();
        signRequest = requestBuilder.build();
        validateCase(payload.length);
    }

    @Benchmark
    public long signAndConsume() {
        SignedRequest signedRequest = signer.sign(signRequest);
        return drain(signedRequest.payload().orElseThrow(() -> new IllegalStateException("Signed payload is missing"))
                                  .newStream(),
                     drainBuffer);
    }

    private void validateCase(int payloadLength) {
        SignedRequest signed = signer.sign(signRequest);
        long encodedLength =
            drain(signed.payload().orElseThrow(() -> new IllegalStateException("Signed payload is missing")).newStream(),
                  drainBuffer);

        requireHeader(signed, "x-amz-content-sha256", signingMode.contentHash);
        requireHeader(signed, "x-amz-decoded-content-length", Integer.toString(payloadLength));
        requireHeader(signed, CONTENT_ENCODING, "aws-chunked");
        requireHeader(signed, Header.CONTENT_LENGTH, Long.toString(encodedLength));

        require(signed.request().firstMatchingHeader("Authorization").isPresent(),
                "Authorization header was not generated");
    }

    private static void requireHeader(SignedRequest request, String name, String expected) {
        String actual = request.request().firstMatchingHeader(name).orElse(null);
        require(expected.equals(actual), "Unexpected " + name + " header: " + actual);
    }

    private static long drain(InputStream input, byte[] buffer) {
        try (InputStream stream = input) {
            long total = 0;
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                total += read;
            }
            return total;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public enum PayloadSize {
        KIB_64(64 * 1024),
        MIB_1(1024 * 1024),
        MIB_16(16 * 1024 * 1024);

        private final int bytes;

        PayloadSize(int bytes) {
            this.bytes = bytes;
        }
    }

    public enum SigningMode {
        SIGNED_PAYLOAD(true, null, "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"),
        SIGNED_CHECKSUM_TRAILER(true, DefaultChecksumAlgorithm.CRC32,
                                "STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER"),
        UNSIGNED_CHECKSUM_TRAILER(false, DefaultChecksumAlgorithm.CRC32,
                                  "STREAMING-UNSIGNED-PAYLOAD-TRAILER");

        private final boolean payloadSigning;
        private final ChecksumAlgorithm checksumAlgorithm;
        private final String contentHash;

        SigningMode(boolean payloadSigning, ChecksumAlgorithm checksumAlgorithm, String contentHash) {
            this.payloadSigning = payloadSigning;
            this.checksumAlgorithm = checksumAlgorithm;
            this.contentHash = contentHash;
        }
    }
}
