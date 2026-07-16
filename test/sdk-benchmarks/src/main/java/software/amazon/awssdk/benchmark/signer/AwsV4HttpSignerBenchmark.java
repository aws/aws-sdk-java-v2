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

import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * JMH benchmark for the full {@link software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner#sign} entry point
 * (i.e., {@link DefaultAwsV4HttpSigner#sign}), unlike {@link Sigv4SignerBenchmark} which measures only the low-level
 * {@link software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigner} component.
 *
 * <p>The request shape mirrors what an end-to-end call like the one exercised by
 * {@code V2JsonRoundtripBenchmark} (DynamoDB {@code PutItem}) actually presents to the signer: a POST to a DDB
 * host with a JSON-1.0 body of roughly the same size as a real marshalled {@code PutItem} payload, credentials
 * for {@code us-east-1}/{@code dynamodb}, and no additional signer properties beyond region and service name.
 *
 * <p>Two benchmark methods are provided to make the fast-path vs legacy-path comparison direct:
 * <ul>
 *     <li>{@link #signFastPath} calls {@link DefaultAwsV4HttpSigner#sign}, which — for this request shape —
 *         dispatches to {@code FastV4HeaderSigner}.</li>
 *     <li>{@link #signLegacyPath} calls {@link DefaultAwsV4HttpSigner#signLegacyPath} on the same request,
 *         forcing the {@code Checksummer} → {@code V4RequestSigner} → {@code V4PayloadSigner} composition
 *         (bypassing the fast-path dispatch).</li>
 * </ul>
 * Both should produce byte-identical signatures for the same input; the delta in ns/op is the fast-path
 * improvement.
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AwsV4HttpSignerBenchmark {

    /**
     * Representative JSON-1.0 body for a DDB {@code PutItem} with a modest item (~500 bytes). Sized similarly to
     * the marshalled body produced by {@code V2JsonRoundtripBenchmark#itemMap}. Kept as a literal so the benchmark
     * doesn't pull in the DDB codegen'd model just to build a request.
     */
    private static final String DDB_PUT_ITEM_BODY_JSON =
        "{\"TableName\":\"benchmark-table\",\"Item\":{"
        + "\"pk\":{\"S\":\"benchmark-key\"},"
        + "\"sk\":{\"N\":\"100\"},"
        + "\"stringField\":{\"S\":\"test-value\"},"
        + "\"numberField\":{\"N\":\"123.456\"},"
        + "\"binaryField\":{\"B\":\"aGVsbG8gd29ybGQ=\"},"
        + "\"stringSetField\":{\"SS\":[\"value1\",\"value2\",\"value3\"]},"
        + "\"numberSetField\":{\"NS\":[\"1.1\",\"2.2\",\"3.3\"]},"
        + "\"boolField\":{\"BOOL\":false},"
        + "\"nullField\":{\"NULL\":true},"
        + "\"mapField\":{\"M\":{"
        +   "\"nested\":{\"S\":\"nested-value\"},"
        +   "\"deepNested\":{\"M\":{\"level2\":{\"N\":\"999\"}}}}},"
        + "\"listField\":{\"L\":["
        +   "{\"S\":\"item1\"},{\"N\":\"42\"},{\"BOOL\":true},{\"NULL\":true}]}"
        + "}}";

    private DefaultAwsV4HttpSigner signer;
    private SignRequest<AwsCredentialsIdentity> ddbPutItemRequest;

    @Setup(Level.Iteration)
    public void setup() {
        signer = new DefaultAwsV4HttpSigner();

        byte[] body = DDB_PUT_ITEM_BODY_JSON.getBytes(StandardCharsets.UTF_8);

        SdkHttpRequest httpRequest =
            SdkHttpRequest.builder()
                          .method(SdkHttpMethod.POST)
                          .uri(URI.create("https://dynamodb.us-east-1.amazonaws.com/"))
                          .encodedPath("/")
                          // Match the header shape a DDB JSON-1.0 client would present to the signer.
                          .putHeader("Content-Type", "application/x-amz-json-1.0")
                          .putHeader("X-Amz-Target", "DynamoDB_20120810.PutItem")
                          .putHeader("Content-Length", Integer.toString(body.length))
                          .build();

        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("access", "secret");

        ddbPutItemRequest =
            SignRequest.builder(credentials)
                       .request(httpRequest)
                       .payload(ContentStreamProvider.fromByteArrayUnsafe(body))
                       .putProperty(REGION_NAME, "us-east-1")
                       .putProperty(SERVICE_SIGNING_NAME, "dynamodb")
                       // Use a real clock so signing-key cache behavior matches production. The cache is keyed on
                       // (secretKey, region, service) with day-scoped validity, so repeat calls within the same
                       // iteration reuse the cached derivation.
                       .putProperty(SIGNING_CLOCK, Clock.systemUTC())
                       .build();
    }

    /**
     * Full sign() dispatch. For this request shape ({@code canUseFastPath} returns true), this calls into
     * {@code FastV4HeaderSigner}.
     */
    @Benchmark
    public void signFastPath(Blackhole blackhole) {
        SignedRequest signed = signer.sign(ddbPutItemRequest);
        blackhole.consume(signed);
    }

    /**
     * Explicit legacy-path invocation for the same request, bypassing dispatch. Measures the {@code Checksummer}
     * → {@code V4RequestSigner} → {@code V4PayloadSigner} composition on the same input.
     */
    @Benchmark
    public void signLegacyPath(Blackhole blackhole) {
        SignedRequest signed = signer.signLegacyPath(ddbPutItemRequest);
        blackhole.consume(signed);
    }
}
