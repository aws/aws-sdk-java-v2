package software.amazon.awssdk.wirediff;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

/**
 * A differential probe of request-level {@code overrideConfiguration()}: every field of
 * {@link AwsRequestOverrideConfiguration}, set one at a time on an S3 {@code ListObjectsV2} through the sync
 * and the async client, with what each one observably did. Run on stock and on the bridge and compared line
 * for line, like the other probes, because "is this override honored" is behavior, not bytes.
 *
 * <p>Output: {@code <client> <field> -> <observation>}.
 */
public final class RequestOverrideProbe {

    private static final ExecutionAttribute<String> PROBE_ATTRIBUTE = new ExecutionAttribute<>("RequestOverrideProbe");
    private static final String LISTING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ListBucketResult></ListBucketResult>";
    private static final URI ENDPOINT = URI.create("https://s3.us-east-1.amazonaws.com");

    private RequestOverrideProbe() {
    }

    public static void main(String[] args) {
        run().forEach(System.out::println);
        System.exit(0);
    }

    /** One override field: how to set it, and what to report about the call. */
    private record Case(String name, long delayMillis, Consumer<AwsRequestOverrideConfiguration.Builder> override,
                        Function<Observation, String> observe) {
    }

    /** What a call left behind. */
    private record Observation(CapturingHttpClient.CapturedRequest sent, Throwable failure, long elapsedMillis,
                               String attributeSeen, int metricsPublished) {
        String header(String name) {
            return sent == null ? "<no request>" : sent.request().firstMatchingHeader(name).orElse("-");
        }
    }

    public static List<String> run() {
        List<Case> cases = List.of(
            new Case("headers", 0, o -> o.putHeader("x-probe-header", "h1"), ob -> "x-probe-header=" + ob.header("x-probe-header")),
            new Case("rawQueryParameters", 0, o -> o.putRawQueryParameter("probe", "q1"),
                     ob -> "probe=" + (ob.sent == null ? "<no request>" : ob.sent.request().rawQueryParameters().get("probe"))),
            new Case("apiNames", 0, o -> o.addApiName(b -> b.name("probeApi").version("1.0")),
                     ob -> "user-agent has probeApi/1.0=" + ob.header("User-Agent").contains("probeApi/1.0")),
            new Case("credentialsProvider", 0,
                     o -> o.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDOVERRIDE", "s"))),
                     ob -> "signed-with-override=" + ob.header("Authorization").contains("Credential=AKIDOVERRIDE/")),
            new Case("endpointProvider", 0,
                     o -> o.endpointProvider((S3EndpointProvider) p -> CompletableFuture.completedFuture(
                         Endpoint.builder().url(URI.create("https://override.example.com")).build())),
                     ob -> "host=" + (ob.sent == null ? "<no request>" : ob.sent.request().host())),
            new Case("authSchemeProvider", 0,
                     o -> o.authSchemeProvider((S3AuthSchemeProvider) p -> List.of(
                         AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                         .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "probesvc")
                                         .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "us-west-2").build())),
                     ob -> "scope-from-override=" + ob.header("Authorization").contains("/us-west-2/probesvc/")),
            new Case("executionAttributes", 0, o -> o.putExecutionAttribute(PROBE_ATTRIBUTE, "ea1"),
                     ob -> "interceptor-saw=" + ob.attributeSeen),
            new Case("signer", 0, o -> o.signer(new LegacySigner()),
                     ob -> "legacy-signer-ran=" + "yes".equals(ob.header("x-legacy-signer"))),
            new Case("plugins", 0, o -> o.addPlugin(config -> {
                         S3ServiceClientConfiguration.Builder s3 = (S3ServiceClientConfiguration.Builder) config;
                         s3.region(Region.EU_WEST_1);
                     }),
                     ob -> "signed-for-eu-west-1=" + ob.header("Authorization").contains("/eu-west-1/s3/")),
            new Case("metricPublishers", 0, o -> o.addMetricPublisher(new CountingPublisher()),
                     ob -> "published=" + ob.metricsPublished));

        List<String> out = new ArrayList<>();
        for (Case c : cases) {
            out.add("sync  " + c.name() + " -> " + c.observe().apply(sync(c)));
            out.add("async " + c.name() + " -> " + c.observe().apply(async(c)));
        }
        // Client-level counterparts: the same mechanisms, configured on the client builder.
        List<ClientCase> clientCases = List.of(
            new ClientCase("client headers", o -> o.putHeader("x-client-header", "c1"),
                           r -> "x-client-header=" + header(r, "x-client-header")),
            new ClientCase("client header under request header", o -> o.putHeader("x-probe-header", "client"),
                           r -> "x-probe-header=" + header(r, "x-probe-header")),
            new ClientCase("client signer", o -> o.putAdvancedOption(
                               software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER, new LegacySigner()),
                           r -> "legacy-signer-ran=" + "yes".equals(header(r, "x-legacy-signer"))),
            new ClientCase("user agent prefix/suffix", o -> o
                               .putAdvancedOption(software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_PREFIX,
                                                  "probe-prefix")
                               .putAdvancedOption(software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                                  "probe-suffix"),
                           r -> {
                               String ua = header(r, "User-Agent");
                               return "prefix-first=" + ua.startsWith("probe-prefix") + " suffix-last=" + ua.endsWith("probe-suffix");
                           }));
        for (ClientCase c : clientCases) {
            out.add("sync  " + c.name() + " -> " + c.observe().apply(syncWithClient(c)));
            out.add("async " + c.name() + " -> " + c.observe().apply(asyncWithClient(c)));
        }
        // Timeouts need a real socket: whether a timed-out attempt is aborted and retried is transport
        // behavior, which a stub transport cannot show.
        for (String field : List.of("apiCallTimeout", "apiCallAttemptTimeout")) {
            out.add("sync  " + field + " -> " + timeout(false, field));
            out.add("async " + field + " -> " + timeout(true, field));
        }
        return out;
    }

    private static String timeout(boolean async, String field) {
        Consumer<AwsRequestOverrideConfiguration.Builder> override = "apiCallTimeout".equals(field)
            ? o -> o.apiCallTimeout(Duration.ofMillis(300))
            : o -> o.apiCallAttemptTimeout(Duration.ofMillis(300));
        AtomicInteger attempts = new AtomicInteger();
        try (LocalHttpServer server = LocalHttpServer.start()) {
            server.route(r -> {
                attempts.incrementAndGet();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return LocalHttpServer.Response.ok(LISTING.getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/xml");
            });
            long[] elapsed = {0};
            Throwable failure = null;
            try {
                if (async) {
                    try (S3AsyncClient s3 = S3AsyncClient.builder().region(Region.US_EAST_1)
                                                         .credentialsProvider(defaultCredentials())
                                                         .endpointOverride(server.uri("")).forcePathStyle(true).build()) {
                        timed(elapsed, () -> s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(override)).join());
                    }
                } else {
                    try (S3Client s3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(defaultCredentials())
                                               .endpointOverride(server.uri("")).forcePathStyle(true).build()) {
                        timed(elapsed, () -> s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(override)));
                    }
                }
            } catch (RuntimeException e) {
                failure = e;
            }
            return outcome(new Observation(null, failure, elapsed[0], null, 0)) + " attempts=" + attempts.get();
        }
    }

    /** Times the call alone -- not the client's close(), which try-with-resources runs before any catch. */
    private static void timed(long[] elapsed, Runnable call) {
        long start = System.nanoTime();
        try {
            call.run();
        } finally {
            elapsed[0] = (System.nanoTime() - start) / 1_000_000;
        }
    }

    /** A client-level setting, and what to report about a call made with it. */
    private record ClientCase(String name,
                              Consumer<software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder> config,
                              Function<CapturingHttpClient.CapturedRequest, String> observe) {
    }

    private static String header(CapturingHttpClient.CapturedRequest sent, String name) {
        return sent == null ? "<no request>" : sent.request().firstMatchingHeader(name).orElse("-");
    }

    // The "under request header" case also sets a request-level x-probe-header, to show which wins.
    private static CapturingHttpClient.CapturedRequest syncWithClient(ClientCase c) {
        CapturingHttpClient transport = CapturingHttpClient.xml(LISTING);
        try (S3Client s3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(defaultCredentials())
                                   .endpointOverride(ENDPOINT).forcePathStyle(true).httpClient(transport)
                                   .overrideConfiguration(c.config()).build()) {
            s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(o -> o.putHeader("x-probe-header", "request")));
        } catch (RuntimeException e) {
            // Reported through the captured request, or its absence.
        }
        return transport.captured().isEmpty() ? null : transport.captured().get(0);
    }

    private static CapturingHttpClient.CapturedRequest asyncWithClient(ClientCase c) {
        CapturingAsyncHttpClient transport = CapturingAsyncHttpClient.xml(LISTING);
        try (S3AsyncClient s3 = S3AsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(defaultCredentials())
                                             .endpointOverride(ENDPOINT).forcePathStyle(true).httpClient(transport)
                                             .overrideConfiguration(c.config()).build()) {
            s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(o -> o.putHeader("x-probe-header", "request"))).join();
        } catch (RuntimeException e) {
            // Reported through the captured request, or its absence.
        }
        return transport.captured().isEmpty() ? null : transport.captured().get(0);
    }

    private static String outcome(Observation ob) {
        if (ob.failure == null) {
            return "OK";
        }
        Throwable t = ob.failure;
        while (t.getCause() != null && !(t instanceof software.amazon.awssdk.core.exception.SdkException)) {
            t = t.getCause();
        }
        return "FAIL " + t.getClass().getSimpleName() + (ob.elapsedMillis < 1200 ? " (early)" : " (after the delay)");
    }

    private static final AtomicReference<String> ATTRIBUTE_SEEN = new AtomicReference<>();
    private static final AtomicInteger METRICS = new AtomicInteger();

    private static final ExecutionInterceptor ATTRIBUTE_READER = new ExecutionInterceptor() {
        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            ATTRIBUTE_SEEN.set(executionAttributes.getAttribute(PROBE_ATTRIBUTE));
        }
    };

    private static Observation sync(Case c) {
        ATTRIBUTE_SEEN.set("<not called>");
        METRICS.set(0);
        CapturingHttpClient transport = CapturingHttpClient.xml(LISTING).withDelay(c.delayMillis());
        long start = System.nanoTime();
        Throwable failure = null;
        try (S3Client s3 = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(defaultCredentials())
                                   .endpointOverride(ENDPOINT).forcePathStyle(true).httpClient(transport)
                                   .overrideConfiguration(o -> o.addExecutionInterceptor(ATTRIBUTE_READER)).build()) {
            s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(c.override()));
        } catch (RuntimeException e) {
            failure = e;
        }
        return new Observation(transport.captured().isEmpty() ? null : transport.captured().get(0), failure,
                               (System.nanoTime() - start) / 1_000_000, ATTRIBUTE_SEEN.get(), METRICS.get());
    }

    private static Observation async(Case c) {
        ATTRIBUTE_SEEN.set("<not called>");
        METRICS.set(0);
        CapturingAsyncHttpClient transport = CapturingAsyncHttpClient.xml(LISTING).withDelay(c.delayMillis());
        long start = System.nanoTime();
        Throwable failure = null;
        try (S3AsyncClient s3 = S3AsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(defaultCredentials())
                                             .endpointOverride(ENDPOINT).forcePathStyle(true).httpClient(transport)
                                             .overrideConfiguration(o -> o.addExecutionInterceptor(ATTRIBUTE_READER))
                                             .build()) {
            s3.listObjectsV2(r -> r.bucket("b").overrideConfiguration(c.override())).join();
        } catch (RuntimeException e) {
            failure = e;
        }
        return new Observation(transport.captured().isEmpty() ? null : transport.captured().get(0), failure,
                               (System.nanoTime() - start) / 1_000_000, ATTRIBUTE_SEEN.get(), METRICS.get());
    }

    private static StaticCredentialsProvider defaultCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIDDEFAULT", "s"));
    }

    /** A legacy {@code Signer} that marks the request, so it is observable whether it replaced SigV4. */
    @SuppressWarnings("deprecation")
    private static final class LegacySigner implements Signer {
        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            return request.toBuilder().putHeader("x-legacy-signer", "yes").build();
        }
    }

    private static final class CountingPublisher implements MetricPublisher {
        @Override
        public void publish(MetricCollection metricCollection) {
            METRICS.incrementAndGet();
        }

        @Override
        public void close() {
        }
    }
}
