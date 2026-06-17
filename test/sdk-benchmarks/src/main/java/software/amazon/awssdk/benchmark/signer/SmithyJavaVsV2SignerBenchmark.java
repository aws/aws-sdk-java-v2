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

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4AuthScheme;
import software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Settings;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.core.settings.ClockSetting;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Compares the AWS SDK for Java v2 SigV4 signer ({@link AwsV4HttpSigner}) against the smithy-java
 * SigV4 signer ({@link software.amazon.smithy.java.aws.client.auth.scheme.sigv4.SigV4Signer}) on the
 * same DynamoDB {@code PutItem} request used by
 * {@link software.amazon.awssdk.benchmark.apicall.protocol.V2JsonRoundtripBenchmark}.
 *
 * <p>The {@code @Setup} drives a single {@code PutItem} call through the SDK V2 marshaller
 * pipeline. An {@link ExecutionInterceptor} captures the unsigned {@link SdkHttpRequest} at
 * the {@code modifyHttpRequest} stage (the last hook before signing) and then aborts the call
 * by throwing — no signing, transmission, or unmarshalling runs. The captured request is then
 * materialised twice:
 * <ul>
 *   <li>As an SDK V2 {@link SignRequest} fed to {@link AwsV4HttpSigner#sign(SignRequest)}.</li>
 *   <li>As a smithy-java unmodifiable {@link HttpRequest} fed to the smithy-java signer's
 *       {@code Signer#sign(...)} method (returned via {@link SigV4AuthScheme#signer()}).</li>
 * </ul>
 *
 * <p>Both benchmarks include the SHA-256 of the request body in their work, matching what each
 * signer's public entry-point does in production. Neither benchmark passes a payload-checksum
 * cache, so each iteration recomputes the body hash from scratch.
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SmithyJavaVsV2SignerBenchmark {

    private static final String REGION = "us-east-1";
    private static final String SIGNING_NAME = "dynamodb";

    // Inputs shared by both benchmarks.
    private byte[] payloadBytes;
    private String contentType;

    // SDK V2 signer state. The fully-qualified AwsCredentialsIdentity type avoids a name clash
    // with the smithy-java identity type imported below.
    private AwsV4HttpSigner v2Signer;
    // Direct handle to the V2 signer's legacy pipeline so we can benchmark the pre-fast-path implementation
    // alongside the fast path. {@link DefaultAwsV4HttpSigner#sign} dispatches to one or the other based on
    // {@code canUseFastPath}; we want to time both deterministically.
    private software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner v2DefaultSigner;
    private SignRequest<software.amazon.awssdk.identity.spi.AwsCredentialsIdentity> v2SignRequest;

    // smithy-java signer state.
    private software.amazon.smithy.java.auth.api.Signer<HttpRequest, AwsCredentialsIdentity> smithyJavaSigner;
    private HttpRequest smithyJavaRequest;
    private AwsCredentialsIdentity smithyJavaCredentials;
    private Context smithyJavaSigningContext;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        Captured captured = captureMarshalledPutItem();
        this.payloadBytes = captured.bodyBytes;
        this.contentType = captured.contentType;

        // Build SDK V2 sign-once-per-iteration state. No clock override: the benchmark uses the
        // wall clock, matching how the SDK runs in production.
        v2Signer = AwsV4HttpSigner.create();
        v2DefaultSigner = new software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner();
        v2SignRequest = buildV2SignRequest(captured, /* clockOverride */ null);

        // Build smithy-java sign-once-per-iteration state. The captured request is faithfully
        // ported into smithy-java's HttpRequest type: same method, URI, headers and body bytes.
        smithyJavaSigner = new SigV4AuthScheme(SIGNING_NAME).signer();
        smithyJavaCredentials = AwsCredentialsIdentity.create("test", "test");
        smithyJavaRequest = toSmithyJavaRequest(captured.request, payloadBytes, contentType);
        smithyJavaSigningContext = buildSmithyJavaSigningContext(/* clockOverride */ null);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (smithyJavaSigner != null) {
            smithyJavaSigner.close();
        }
    }

    @Benchmark
    public void signWithV2Signer(Blackhole bh) {
        // Goes through DefaultAwsV4HttpSigner.sign, which dispatches to the new fast path for this request shape
        // (header auth, no flexible checksum, no chunk encoding, etc.).
        SignedRequest signed = v2Signer.sign(v2SignRequest);
        bh.consume(signed);
    }

    @Benchmark
    public void signWithV2LegacyPath(Blackhole bh) {
        // Calls the original Checksummer → V4RequestSigner → V4PayloadSigner pipeline directly so we can measure
        // the fast-path delta as a controlled experiment instead of relying on historical numbers.
        SignedRequest signed = v2DefaultSigner.signLegacyPath(v2SignRequest);
        bh.consume(signed);
    }

    @Benchmark
    public void signWithSmithyJavaSigner(Blackhole bh) {
        // Pass the unmodifiable request: the smithy-java signer calls toModifiable() internally,
        // which on an unmodifiable HttpRequest returns a fresh modifiable copy each call.
        bh.consume(smithyJavaSigner.sign(smithyJavaRequest, smithyJavaCredentials, smithyJavaSigningContext));
    }

    /**
     * Drives a single PutItem call through the SDK V2 marshaller pipeline and captures the
     * unsigned, marshalled {@link SdkHttpRequest} plus body bytes. The capture happens at the
     * {@code modifyHttpRequest} interceptor stage, which is the last pipeline hook before
     * signing.
     */
    private static Captured captureMarshalledPutItem() {
        CapturingInterceptor capturingInterceptor = new CapturingInterceptor();
        try (DynamoDbClient client = DynamoDbClient.builder()
                                                   .region(Region.of(REGION))
                                                   .credentialsProvider(
                                                       StaticCredentialsProvider.create(
                                                           AwsBasicCredentials.create("test", "test")))
                                                   .httpClient(UrlConnectionHttpClient.create())
                                                   .overrideConfiguration(c -> c.addExecutionInterceptor(
                                                       capturingInterceptor))
                                                   .build()) {
            try {
                client.putItem(PutItemRequest.builder()
                                             .tableName("benchmark-table")
                                             .item(itemMap())
                                             .build());
                throw new IllegalStateException("Expected the capture interceptor to abort the call");
            } catch (RuntimeException e) {
                if (!isCaptureCompleteSignal(e)) {
                    throw e;
                }
            }
        }

        SdkHttpRequest capturedRequest = capturingInterceptor.capturedRequest;
        RequestBody capturedBody = capturingInterceptor.capturedBody;
        if (capturedRequest == null || capturedBody == null) {
            throw new IllegalStateException("Failed to capture marshalled PutItem request");
        }
        return new Captured(capturedRequest, readAll(capturedBody.contentStreamProvider()), capturedBody.contentType());
    }

    /**
     * Builds the V2 SignRequest from a captured marshalled request. {@code clockOverride} pins
     * the signing instant when non-null; otherwise the signer falls back to the wall clock.
     */
    private static SignRequest<software.amazon.awssdk.identity.spi.AwsCredentialsIdentity>
            buildV2SignRequest(Captured captured, Clock clockOverride) {
        software.amazon.awssdk.identity.spi.AwsCredentialsIdentity v2Credentials =
            software.amazon.awssdk.identity.spi.AwsCredentialsIdentity.create("test", "test");
        SignRequest.Builder<software.amazon.awssdk.identity.spi.AwsCredentialsIdentity> builder =
            SignRequest.builder(v2Credentials)
                       .request(captured.request)
                       .payload(ContentStreamProvider.fromByteArrayUnsafe(captured.bodyBytes))
                       .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME)
                       .putProperty(AwsV4HttpSigner.REGION_NAME, REGION);
        if (clockOverride != null) {
            builder.putProperty(HttpSigner.SIGNING_CLOCK, clockOverride);
        }
        return builder.build();
    }

    private static HttpRequest toSmithyJavaRequest(SdkHttpRequest captured, byte[] payload, String contentType) {
        ModifiableHttpRequest modifiable = HttpRequest.create()
                                                     .setMethod(captured.method().name())
                                                     .setUri(captured.getUri())
                                                     .setBody(DataStream.ofBytes(payload, contentType));
        // Carry every marshaller-emitted header through verbatim. The signer is responsible for
        // adding x-amz-date / authorization on its own pass.
        for (Map.Entry<String, List<String>> entry : captured.headers().entrySet()) {
            modifiable.headers().setHeader(entry.getKey(), entry.getValue());
        }
        return modifiable.toUnmodifiable();
    }

    /**
     * Builds the smithy-java signing context. {@code clockOverride} pins the signing instant
     * when non-null; otherwise smithy-java's signer falls back to {@link Clock#systemUTC()}.
     */
    private static Context buildSmithyJavaSigningContext(Clock clockOverride) {
        Context ctx = Context.create();
        ctx.put(SigV4Settings.SIGNING_NAME, SIGNING_NAME);
        ctx.put(RegionSetting.REGION, REGION);
        if (clockOverride != null) {
            ctx.put(ClockSetting.CLOCK, clockOverride);
        }
        return Context.unmodifiableView(ctx);
    }

    private static byte[] readAll(ContentStreamProvider provider) {
        try (InputStream is = provider.newStream()) {
            return IoUtils.toByteArray(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to materialise request payload", e);
        }
    }

    /** Walks the cause chain looking for our capture marker, since the SDK may wrap it. */
    private static boolean isCaptureCompleteSignal(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof CaptureCompleteSignal) {
                return true;
            }
            if (cur.getCause() == cur) {
                break;
            }
        }
        return false;
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.builder().ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return item;
    }

    /**
     * Captured input shared by the V2 and smithy-java signing paths.
     */
    private static final class Captured {
        final SdkHttpRequest request;
        final byte[] bodyBytes;
        final String contentType;

        Captured(SdkHttpRequest request, byte[] bodyBytes, String contentType) {
            this.request = request;
            this.bodyBytes = bodyBytes;
            this.contentType = contentType;
        }
    }

    /**
     * Standalone entry point that signs the same captured PutItem request with both signers
     * under a fixed clock and reports whether the resulting Authorization headers are
     * byte-identical. The two signers don't produce identical Authorization headers in
     * production by design: the SDK V2 signer always emits an {@code x-amz-content-sha256}
     * header (via its Checksummer) and includes {@code content-length} in
     * {@code SignedHeaders}, whereas smithy-java's signer ignores {@code content-length} and
     * only emits {@code x-amz-content-sha256} for S3-family services. This entry point makes
     * that divergence explicit so the benchmark numbers can be interpreted accurately.
     *
     * <p>Run with the shaded benchmark jar:
     * {@code java -cp test/sdk-benchmarks/target/benchmarks.jar
     * software.amazon.awssdk.benchmark.signer.SmithyJavaVsV2SignerBenchmark}.
     */
    public static void main(String[] args) throws Exception {
        // Pin a fixed instant so the only sources of divergence between the two signed outputs
        // are the signers themselves. Without this both signers would each call
        // Clock.systemUTC().instant() at slightly different times and disagree on x-amz-date
        // and signing-key derivation as well.
        Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

        Captured captured = captureMarshalledPutItem();
        System.out.println("Captured PutItem marshalled request:");
        System.out.println("  method:       " + captured.request.method());
        System.out.println("  uri:          " + captured.request.getUri());
        System.out.println("  body bytes:   " + captured.bodyBytes.length);
        System.out.println("  content-type: " + captured.contentType);
        System.out.println("  headers:");
        for (Map.Entry<String, List<String>> entry : captured.request.headers().entrySet()) {
            System.out.println("    " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
        }
        System.out.println();

        // Sign with the SDK V2 signer.
        AwsV4HttpSigner v2Signer = AwsV4HttpSigner.create();
        SignedRequest v2Signed = v2Signer.sign(buildV2SignRequest(captured, fixedClock));
        String v2Authorization = v2Signed.request().firstMatchingHeader("Authorization").orElse("");
        String v2AmzDate = v2Signed.request().firstMatchingHeader("X-Amz-Date").orElse("");

        // Sign with the smithy-java signer.
        String smithyAuthorization;
        String smithyAmzDate;
        try (software.amazon.smithy.java.auth.api.Signer<HttpRequest, AwsCredentialsIdentity> smithySigner =
                 new SigV4AuthScheme(SIGNING_NAME).signer()) {
            HttpRequest smithyReq =
                toSmithyJavaRequest(captured.request, captured.bodyBytes, captured.contentType);
            HttpRequest smithySigned = smithySigner.sign(smithyReq,
                                                         AwsCredentialsIdentity.create("test", "test"),
                                                         buildSmithyJavaSigningContext(fixedClock))
                                                   .signedRequest();
            smithyAuthorization = nullToEmpty(smithySigned.headers().firstValue("authorization"));
            smithyAmzDate = nullToEmpty(smithySigned.headers().firstValue("x-amz-date"));
        }

        System.out.println("V2     X-Amz-Date:    " + v2AmzDate);
        System.out.println("smithy X-Amz-Date:    " + smithyAmzDate);
        System.out.println("V2     Authorization: " + v2Authorization);
        System.out.println("smithy Authorization: " + smithyAuthorization);
        System.out.println();

        // Decompose the Authorization headers to make the divergence specific. SignedHeaders is
        // the canonical-request projection each signer applied; comparing the two sets is more
        // informative than just comparing the whole header.
        String v2SignedHeaders = extractSignedHeadersField(v2Authorization);
        String smithySignedHeaders = extractSignedHeadersField(smithyAuthorization);
        java.util.Set<String> v2Headers = new java.util.TreeSet<>(java.util.Arrays.asList(v2SignedHeaders.split(";")));
        java.util.Set<String> smithyHeaders =
            new java.util.TreeSet<>(java.util.Arrays.asList(smithySignedHeaders.split(";")));
        java.util.Set<String> v2Only = new java.util.TreeSet<>(v2Headers);
        v2Only.removeAll(smithyHeaders);
        java.util.Set<String> smithyOnly = new java.util.TreeSet<>(smithyHeaders);
        smithyOnly.removeAll(v2Headers);
        java.util.Set<String> common = new java.util.TreeSet<>(v2Headers);
        common.retainAll(smithyHeaders);
        System.out.println("V2     SignedHeaders: " + v2SignedHeaders);
        System.out.println("smithy SignedHeaders: " + smithySignedHeaders);
        System.out.println("Common headers:    " + common);
        System.out.println("V2-only headers:   " + v2Only);
        System.out.println("smithy-only headers: " + smithyOnly);
        System.out.println();

        boolean datesMatch = v2AmzDate.equals(smithyAmzDate);
        boolean signedHeadersIdentical = v2Headers.equals(smithyHeaders);
        boolean authorizationsIdentical = v2Authorization.equals(smithyAuthorization);

        System.out.println("X-Amz-Date match:           " + datesMatch);
        System.out.println("SignedHeaders set match:    " + signedHeadersIdentical);
        System.out.println("Authorization byte-equal:   " + authorizationsIdentical);
        if (authorizationsIdentical) {
            System.out.println("RESULT: BYTE-EQUIVALENT (signers produced identical Authorization headers)");
        } else if (signedHeadersIdentical && datesMatch) {
            // Same signed-headers set + same date but different signature would be a real bug.
            System.out.println("RESULT: SIGNATURES DIVERGE for identical SignedHeaders/X-Amz-Date — investigate.");
            System.exit(1);
        } else {
            System.out.println("RESULT: INTENTIONAL DIVERGENCE");
            System.out.println("  V2 always emits an x-amz-content-sha256 header and signs content-length;");
            System.out.println("  smithy-java only emits x-amz-content-sha256 for S3 and ignores content-length.");
            System.out.println("  Each signature is valid over its respective canonical request — both will");
            System.out.println("  validate against AWS service-side verification. The benchmark numbers compare");
            System.out.println("  the cost of each signer's natural production behaviour for a DDB PutItem.");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String extractSignedHeadersField(String authorization) {
        // Authorization format: "AWS4-HMAC-SHA256 Credential=..., SignedHeaders=a;b;c, Signature=..."
        String marker = "SignedHeaders=";
        int start = authorization.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = authorization.indexOf(',', start);
        return end < 0 ? authorization.substring(start) : authorization.substring(start, end);
    }

    /**
     * Captures the SdkHttpRequest emitted by the marshaller (and the synchronous request body)
     * before signing, then aborts the call. Lives at the {@code modifyHttpRequest} stage, which
     * is the latest hook in the pipeline that runs before the signing stage.
     */
    private static final class CapturingInterceptor implements ExecutionInterceptor {
        volatile SdkHttpRequest capturedRequest;
        volatile RequestBody capturedBody;

        @Override
        public SdkHttpRequest modifyHttpRequest(
            software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest context,
            ExecutionAttributes executionAttributes) {
            this.capturedRequest = context.httpRequest();
            this.capturedBody = context.requestBody().orElse(null);
            throw new CaptureCompleteSignal();
        }
    }

    /**
     * Marker thrown by {@link CapturingInterceptor} to abort the {@code PutItem} call once the
     * unsigned marshalled request has been captured. Caught in {@link #setup()} via
     * {@link #isCaptureCompleteSignal(Throwable)} so the SDK is free to wrap it on the way out.
     */
    private static final class CaptureCompleteSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CaptureCompleteSignal() {
            super("captured marshalled request; aborting call before signing");
        }
    }
}
