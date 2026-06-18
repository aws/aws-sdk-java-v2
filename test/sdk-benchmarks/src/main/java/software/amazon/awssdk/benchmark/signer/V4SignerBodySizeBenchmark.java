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

import java.net.URI;
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
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Compares the SigV4 fast path against the legacy path across a range of body sizes. The body-hash optimization
 * (Option A: direct {@code ByteBuffer} access via {@link software.amazon.awssdk.http.ByteBufferContentProvider}) is
 * size-sensitive: for very small bodies the per-call wrapper allocation roughly cancels the savings from skipping
 * the {@code InputStream.read(buf)} memcpy; for larger bodies the avoided memcpy dominates.
 *
 * <p>This benchmark varies the body size as a JMH {@code @Param} so a single run sweeps the full curve. For each
 * size it times:
 * <ul>
 *     <li>{@code signFastPath}  — {@link DefaultAwsV4HttpSigner#sign} with all fast-path optimizations on (the
 *         shipping behavior).</li>
 *     <li>{@code signLegacyPath} — {@link DefaultAwsV4HttpSigner#signLegacyPath} for the same input, so the delta
 *         is a controlled experiment, not a comparison against an absent baseline.</li>
 * </ul>
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V4SignerBodySizeBenchmark {

    @Param({"0", "500", "8192", "65536", "1048576"})
    public int bodySize;

    private DefaultAwsV4HttpSigner v2DefaultSigner;
    private SignRequest<AwsCredentialsIdentity> signRequest;
    /**
     * Same payload but wrapped in a {@link ContentStreamProvider} that does <em>not</em> implement
     * {@link software.amazon.awssdk.http.ByteBufferContentProvider}, so the fast path falls back to
     * {@code InputStream.read(buf)}. Together with {@link #signRequest} this isolates the contribution of the
     * Option A direct-{@code ByteBuffer} body-hash path from the rest of the fast path.
     */
    private SignRequest<AwsCredentialsIdentity> signRequestStreamingOnly;

    @Setup
    public void setup() {
        v2DefaultSigner = new DefaultAwsV4HttpSigner();

        byte[] payload = new byte[bodySize];
        // Fill with a deterministic but non-zero pattern so SHA-256 can't short-circuit on all-zero input.
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0x7F);
        }

        SdkHttpRequest httpRequest = SdkHttpRequest.builder()
                                                   .protocol("https")
                                                   .method(SdkHttpMethod.POST)
                                                   .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                   .encodedPath("/")
                                                   .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                   .putHeader("Content-Type", "application/x-amz-json-1.0")
                                                   .putHeader("X-Amz-Target", "Demo.Operation")
                                                   .build();

        signRequest = SignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                                 .request(httpRequest)
                                 .payload(ContentStreamProvider.fromByteArrayUnsafe(payload))
                                 .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                                 .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "demo")
                                 .build();

        // A plain lambda implements only ContentStreamProvider, not ByteBufferContentProvider, so the fast path
        // falls through to its streaming fallback and we can measure Option A's contribution in isolation.
        ContentStreamProvider streamingOnly = () -> new java.io.ByteArrayInputStream(payload);
        signRequestStreamingOnly = SignRequest.builder(AwsCredentialsIdentity.create("access", "secret"))
                                              .request(httpRequest)
                                              .payload(streamingOnly)
                                              .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                                              .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "demo")
                                              .build();
    }

    @Benchmark
    public void signFastPath(Blackhole bh) {
        SignedRequest signed = v2DefaultSigner.sign(signRequest);
        bh.consume(signed);
    }

    @Benchmark
    public void signFastPathStreamingFallback(Blackhole bh) {
        // Same fast path as signFastPath, but the payload provider doesn't implement ByteBufferContentProvider, so
        // the body hash falls back to the InputStream.read(buf) loop. The delta to signFastPath isolates the
        // contribution of the Option A direct-ByteBuffer optimisation.
        SignedRequest signed = v2DefaultSigner.sign(signRequestStreamingOnly);
        bh.consume(signed);
    }

    @Benchmark
    public void signLegacyPath(Blackhole bh) {
        SignedRequest signed = v2DefaultSigner.signLegacyPath(signRequest);
        bh.consume(signed);
    }
}
