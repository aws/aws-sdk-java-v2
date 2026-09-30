package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.client.metrics.otel.OperationMetricsPlugin;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.StandardRetryStrategy;

/**
 * One workload per SDK variant: a single client pointed at the mock server, prebuilt request
 * objects, and one method per scenario.
 *
 * <p>Retry parity (fairness fix #1): every client is explicitly configured for standard,
 * token-bucket retries with a maximum of 3 attempts, so all SDKs pay comparable per-call retry
 * accounting (token acquisition/release) while none actually retries (the server always
 * returns 200):
 * <ul>
 *   <li>V2: {@code RetryMode.STANDARD} (3 attempts)</li>
 *   <li>V1: default DynamoDB retry policy capped at {@code maxErrorRetry=2} (3 attempts)</li>
 *   <li>smithy-java: {@code StandardRetryStrategy} with {@code maxAttempts(3)}</li>
 * </ul>
 */
interface Workloads {

    String ACCESS_KEY = "benchmarkAccessKey";
    String SECRET_KEY = "benchmarkSecretKey";

    interface Workload extends AutoCloseable {
        void smallGet() throws Exception;

        void smallPut() throws Exception;

        void batchGet() throws Exception;

        void batchPut() throws Exception;

        /**
         * S3 {@code GetObject} of {@code bytes} bytes, streamed to the caller and discarded.
         *
         * <p>Defaulted rather than added to every arm: the DynamoDB arms are four clients against one
         * mock service, and only the {@code s3-*} arms have an S3 client. The message names the arms
         * so a mistyped {@code --client} says what to use instead of failing abstractly.
         */
        default void getObject(int bytes) throws Exception {
            throw new UnsupportedOperationException(
                "the S3 scenarios need an s3-* client arm (s3-v2-sync, s3-v2-sync-stripped, ...)");
        }

        /** S3 {@code PutObject} of {@code bytes} bytes, streamed from a preallocated body. */
        default void putObject(int bytes) throws Exception {
            throw new UnsupportedOperationException(
                "the S3 scenarios need an s3-* client arm (s3-v2-sync, s3-v2-sync-stripped, ...)");
        }

        /**
         * Human-readable transport identity for the run header and the {@code transport} results
         * column. Reported because it has been a silent variable: with several {@code SdkHttpService}
         * implementations on the classpath, V2's default resolution picks by an internal priority
         * table, and this benchmark's classpath resolved to Apache5 while its README claimed Apache
         * 4.x. Every client pins its transport explicitly and records which one it used, so a results
         * file can never leave the question open.
         */
        String transport();

        /**
         * Whether {@code *Async} methods are usable. Async clients can hold many operations in
         * flight from a single thread, which is the workload shape they exist for; a blocking
         * {@code join()} per call measures something else.
         */
        default boolean supportsAsync() {
            return false;
        }

        default CompletableFuture<?> smallGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> smallPutAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchPutAsync() {
            throw new UnsupportedOperationException();
        }

        default void resetMetrics() {
        }

        default void printMetrics(PrintStream out) {
        }
    }

    /**
     * @param concurrency operations the driver will keep in flight. Every client's connection pool is
     *                    sized to exactly this, so no client is measured waiting on its own pool and
     *                    none is given a larger pool than another.
     */
    static Workload create(String client, URI endpoint, boolean metrics, int concurrency) {
        switch (client) {
            case "v1":
                return v1(endpoint, metrics, concurrency);
            case "v2-sync":
                return v2Sync(endpoint, metrics, concurrency);
            case "v2-sync-stripped":
                // Only meaningful on a jar built against the smithy-java bridge SDK: it asks the
                // bridge to use smithy-java's own endpoint, retry, interceptor and error components
                // instead of the v2 ones, leaving the HTTP transport as the only bridged piece. On a
                // baseline (unbridged) SDK the properties are simply unread, so this arm measures the
                // same thing as v2-sync — which is the point: it makes the bridging tax the only
                // difference between the two arms.
                return strippedV2Sync("awssdk.bridge.stripAll", endpoint, metrics, concurrency);
            case "v2-sync-strip-endpoints":
                return strippedV2Sync("awssdk.bridge.stripEndpoints", endpoint, metrics, concurrency);
            case "v2-sync-strip-retries":
                return strippedV2Sync("awssdk.bridge.stripRetries", endpoint, metrics, concurrency);
            case "v2-sync-strip-interceptors":
                return strippedV2Sync("awssdk.bridge.stripInterceptors", endpoint, metrics, concurrency);
            case "v2-sync-strip-errors":
                return strippedV2Sync("awssdk.bridge.stripErrorEnricher", endpoint, metrics, concurrency);
            case "v2-sync-inert-interceptors":
                // The one arm that adds cost rather than removing it: it restores the pre-filter
                // behavior, bridging the three interceptors AwsDefaultClientBuilder puts on every AWS
                // client even though none of them can act. v2-sync minus this arm is what the filter
                // in V2ConfigTranslator.inert() recovers.
                return strippedV2Sync("awssdk.bridge.keepInertInterceptors", endpoint, metrics, concurrency);
            case "s3-v2-sync":
            case "s3-v2-sync-stripped":
            case "s3-v2-sync-strip-endpoints":
            case "s3-v2-sync-strip-retries":
            case "s3-v2-sync-strip-interceptors":
            case "s3-v2-sync-strip-errors":
                // The S3 arms exist for the streaming scenarios; they serve the DynamoDB scenarios not
                // at all. strip-interceptors is the interesting one here rather than a formality: S3
                // installs a dozen of its own interceptors, so it is the first service where bridging
                // the v2 chain is a real cost rather than three inert defaults.
                return s3Sync(client, endpoint, metrics, concurrency);
            case "v2-async":
                return v2Async(endpoint, metrics, concurrency, false);
            case "v2-async-netty":
                // The other v2 async transport. A bridged async client has to drive both, and they park
                // and wake differently (a Java event loop versus a native one), so a transport-specific
                // cost in the bridge would show up in one arm and not the other.
                return v2Async(endpoint, metrics, concurrency, true);
            case "smithy":
                return smithy(endpoint, metrics, concurrency);
            default:
                throw new IllegalArgumentException("unknown client: " + client);
        }
    }

    /**
     * A V2 sync client with one bridge (or all of them, with {@code stripAll}) replaced by
     * smithy-java's own component, so the delta against the {@code v2-sync} arm attributes the
     * bridging tax to that component.
     *
     * <p>The property has to be set before the client is built, since the bridge reads these once at
     * construction. Each one changes behavior as well as cost — see {@code compatability_issues.md}
     * 10.6 for exactly what each gives up.
     *
     * <p>{@code stripInterceptors} was added on the assumption that it would measure nothing — DynamoDB
     * ships no classpath-discovered interceptors and this benchmark configures none. It measured the
     * most of any single component (~6–8% of app CPU on a small operation), because
     * {@code AwsDefaultClientBuilder.awsInterceptors()} puts three interceptors on <em>every</em> AWS
     * client, none of which the bridge treated as replaced. {@code V2ConfigTranslator.inert()} now
     * filters those three, so on a current jar this arm should measure close to nothing and
     * {@code v2-sync-inert-interceptors} measures what the filter recovered instead. Note that the
     * three also run on stock v2, so neither arm is pure bridging tax: they remove the bridge's
     * per-hook context building <em>and</em> the interceptors' own work.
     */
    private static Workload strippedV2Sync(String property, URI endpoint, boolean metrics, int concurrency) {
        System.setProperty(property, "true");
        return v2Sync(endpoint, metrics, concurrency);
    }

    // ==================== V1 ====================

    private static Workload v1(URI endpoint, boolean metrics, int concurrency) {
        MetricsSupport.V1Collector collector = new MetricsSupport.V1Collector();
        var builder = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint.toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)))
            // Default DynamoDB policy honors this cap: 2 retries = 3 attempts, matching V2 STANDARD.
            .withClientConfiguration(new ClientConfiguration().withMaxErrorRetry(2)
                                                              .withMaxConnections(concurrency));
        if (metrics) {
            builder.withMetricsCollector(collector);
        }
        var ddb = builder.build();

        var smallItem = BenchmarkItems.toV1(BenchmarkItems.smallItem());
        var getReq = new com.amazonaws.services.dynamodbv2.model.GetItemRequest()
            .withTableName(BenchmarkItems.TABLE_NAME)
            .withKey(Map.of("pk", new com.amazonaws.services.dynamodbv2.model.AttributeValue()
                .withS(BenchmarkItems.SMALL_KEY)));
        var putReq = new com.amazonaws.services.dynamodbv2.model.PutItemRequest()
            .withTableName(BenchmarkItems.TABLE_NAME).withItem(smallItem);

        List<Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue>> keys = new ArrayList<>();
        List<com.amazonaws.services.dynamodbv2.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", new com.amazonaws.services.dynamodbv2.model.AttributeValue()
                .withS(BenchmarkItems.batchKey(i))));
            writes.add(new com.amazonaws.services.dynamodbv2.model.WriteRequest()
                           .withPutRequest(new com.amazonaws.services.dynamodbv2.model.PutRequest()
                                               .withItem(BenchmarkItems.toV1(BenchmarkItems.mediumItem(i)))));
        }
        var batchGetReq = new com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest()
            .withRequestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                     new com.amazonaws.services.dynamodbv2.model.KeysAndAttributes().withKeys(keys)));
        var batchPutReq = new com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest()
            .withRequestItems(Map.of(BenchmarkItems.TABLE_NAME, writes));

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "apache4-v1";
            }

            public void resetMetrics() {
                collector.reset();
            }

            public void printMetrics(PrintStream out) {
                collector.print(out);
            }

            public void close() {
                ddb.shutdown();
            }
        };
    }

    // ==================== V2 shared ====================

    private static StaticCredentialsProvider v2Creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    }

    private static ClientOverrideConfiguration v2Override(boolean metrics, MetricsSupport.V2Publisher publisher) {
        ClientOverrideConfiguration.Builder b = ClientOverrideConfiguration.builder()
                                                                           .retryStrategy(RetryMode.STANDARD);
        if (metrics) {
            b.addMetricPublisher(publisher);
        }
        return b.build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.GetItemRequest v2GetReq() {
        return software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .key(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.SMALL_KEY)))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.PutItemRequest v2PutReq() {
        return software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .item(BenchmarkItems.toV2(BenchmarkItems.smallItem()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest v2BatchGetReq() {
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.batchKey(i))));
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                 software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes.builder()
                                                                                                 .keys(keys).build()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest v2BatchPutReq() {
        List<software.amazon.awssdk.services.dynamodb.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            writes.add(software.amazon.awssdk.services.dynamodb.model.WriteRequest.builder()
                .putRequest(software.amazon.awssdk.services.dynamodb.model.PutRequest.builder()
                                .item(BenchmarkItems.toV2(BenchmarkItems.mediumItem(i))).build())
                .build());
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME, writes))
            .build();
    }

    // ==================== V2 sync (Apache5) ====================
    //
    // Apache5 is the transport V2 sync is standardizing on, so it is the one worth measuring. It is
    // pinned rather than resolved from the classpath: `dynamodb` pulls in apache5-client and
    // netty-nio-client transitively, so several SdkHttpService implementations can be present at
    // once and V2 picks by an internal priority table (ClasspathSdkHttpServiceProvider) instead of
    // failing. That made the transport an invisible variable — this module's README claimed Apache
    // 4.x for `v2-sync` while Apache5 was in fact what ran. Naming it here means the priority table,
    // and anything new that lands on the classpath, cannot change what is under test.

    private static Workload v2Sync(URI endpoint, boolean metrics, int concurrency) {
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClientBuilder(software.amazon.awssdk.http.apache5.Apache5HttpClient.builder()
                                                                                   .maxConnections(concurrency))
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "apache5";
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

    // ==================== S3 sync streaming (Apache5 over TLS) ====================
    //
    // Same transport as the DynamoDB v2-sync arm, so the two are comparable, but over TLS: a streamed
    // request body cannot be compared over plain HTTP, because v2 forces payload signing there and the
    // bridge refuses. See BenchmarkTls.
    //
    // Both checksum knobs are turned down to WHEN_REQUIRED, and that is the one place this arm departs
    // from a default client. With v2's default (WHEN_SUPPORTED) stock v2 computes a CRC32 over every
    // PutObject and rewrites the body into aws-chunked framing with a trailing checksum, while the
    // bridge computes nothing (compatability_issues.md 13.3) -- so the comparison would be measuring a
    // missing feature, at a cost that scales with the object, rather than the pipeline around it. Read
    // these numbers as "the streaming pipeline, with checksums off on both sides".

    private static Workload s3Sync(String client, URI endpoint, boolean metrics, int concurrency) {
        String property = s3StripProperty(client);
        if (property != null) {
            System.setProperty(property, "true");
        }

        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var s3 = software.amazon.awssdk.services.s3.S3Client.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            // Path-style, because the mock server is an IP and a virtual-host bucket name would not
            // resolve; it also keeps the request path identical on both arms.
            .forcePathStyle(true)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClientBuilder(software.amazon.awssdk.http.apache5.Apache5HttpClient
                                   .builder()
                                   .maxConnections(concurrency)
                                   .tlsTrustManagersProvider(BenchmarkTls.trustManagersProvider()))
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        // One request object and one body per size, built here rather than per operation: v2's
        // RequestBody.fromBytes copies the array, and an 8 MiB copy inside the measured loop would be
        // the largest single cost in the 8 MiB scenario.
        Map<Integer, software.amazon.awssdk.services.s3.model.GetObjectRequest> gets = new LinkedHashMap<>();
        Map<Integer, software.amazon.awssdk.services.s3.model.PutObjectRequest> puts = new LinkedHashMap<>();
        Map<Integer, RequestBody> bodies = new LinkedHashMap<>();
        for (int bytes : new int[] {S3Objects.SMALL_BYTES, S3Objects.MEDIUM_BYTES, S3Objects.LARGE_BYTES}) {
            gets.put(bytes, software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                                                                                     .bucket(S3Objects.BUCKET)
                                                                                     .key(S3Objects.key(bytes))
                                                                                     .build());
            puts.put(bytes, software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                                                                     .bucket(S3Objects.BUCKET)
                                                                                     .key(S3Objects.key(bytes))
                                                                                     .build());
            bodies.put(bytes, RequestBody.fromBytes(S3Objects.payload(bytes)));
        }

        return new Workload() {
            public void smallGet() {
                throw new UnsupportedOperationException("the s3-* arms serve only the *-object scenarios");
            }

            public void smallPut() {
                throw new UnsupportedOperationException("the s3-* arms serve only the *-object scenarios");
            }

            public void batchGet() {
                throw new UnsupportedOperationException("the s3-* arms serve only the *-object scenarios");
            }

            public void batchPut() {
                throw new UnsupportedOperationException("the s3-* arms serve only the *-object scenarios");
            }

            public void getObject(int bytes) {
                // Drained into a reused buffer and counted, not collected: ResponseTransformer.toBytes
                // would allocate the whole object per operation, which at 8 MiB would dominate the
                // measurement and make it a test of the allocator. The count is checked because a
                // silent short read is exactly the kind of failure a latency number cannot show.
                long received = s3.getObject(gets.get(bytes), (response, body) -> drain(body));
                if (received != bytes) {
                    throw new IllegalStateException("GetObject returned " + received + " of " + bytes + " bytes");
                }
            }

            public void putObject(int bytes) {
                s3.putObject(puts.get(bytes), bodies.get(bytes));
            }

            public String transport() {
                return "apache5-tls";
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                s3.close();
            }
        };
    }

    /** The bridge property an {@code s3-v2-sync-*} arm asks for, or null for the unmodified arm. */
    private static String s3StripProperty(String client) {
        switch (client) {
            case "s3-v2-sync":                     return null;
            case "s3-v2-sync-stripped":            return "awssdk.bridge.stripAll";
            case "s3-v2-sync-strip-endpoints":     return "awssdk.bridge.stripEndpoints";
            case "s3-v2-sync-strip-retries":       return "awssdk.bridge.stripRetries";
            case "s3-v2-sync-strip-interceptors":  return "awssdk.bridge.stripInterceptors";
            case "s3-v2-sync-strip-errors":        return "awssdk.bridge.stripErrorEnricher";
            default: throw new IllegalArgumentException("unknown S3 client arm: " + client);
        }
    }

    /**
     * Reads a response body to EOF with one reused buffer, returning the byte count.
     *
     * <p>The buffer is per call rather than per client: the workload has to be usable from several
     * driver threads at once, and 64 KiB per operation is an allocation both arms pay identically and
     * which is invisible next to an 8 MiB transfer.
     */
    private static long drain(InputStream body) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        for (int read = body.read(buffer); read >= 0; read = body.read(buffer)) {
            total += read;
        }
        return total;
    }

    // ==================== V2 async (CRT) ====================
    //
    // CRT is the transport V2 async is standardizing on, so it is the one worth measuring, and it is
    // pinned for the same reason as the sync side. Note that V2's *current* default resolution would
    // pick Netty (priority 1 in ClasspathSdkHttpServiceProvider's async table), so this benchmark is
    // deliberately measuring the intended long-term default rather than today's fallback.

    private static Workload v2Async(URI endpoint, boolean metrics, int concurrency, boolean netty) {
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClient(netty
                        ? software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient.builder()
                                                                                     .maxConcurrency(concurrency)
                                                                                     .build()
                        : AwsCrtAsyncHttpClient.builder().maxConcurrency(concurrency).build())
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq).join();
            }

            public void smallPut() {
                ddb.putItem(putReq).join();
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq).join();
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq).join();
            }

            public boolean supportsAsync() {
                return true;
            }

            public CompletableFuture<?> smallGetAsync() {
                return ddb.getItem(getReq);
            }

            public CompletableFuture<?> smallPutAsync() {
                return ddb.putItem(putReq);
            }

            public CompletableFuture<?> batchGetAsync() {
                return ddb.batchGetItem(batchGetReq);
            }

            public CompletableFuture<?> batchPutAsync() {
                return ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return netty ? "netty" : "crt";
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

    // ==================== smithy-java ====================

    private static Workload smithy(URI endpoint, boolean metrics, int concurrency) {
        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
            .create(ACCESS_KEY, SECRET_KEY);
        var resolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
            ctx -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        MetricsSupport.OtelHolder otel = new MetricsSupport.OtelHolder();
        HttpClient http = HttpClient.builder().httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                                    .maxConnectionsPerRoute(concurrency)
                                    .maxTotalConnections(concurrency).build();

        var builder = com.example.demo.dynamodb.client.DynamoDBClient.builder()
            .putConfig(RegionSetting.REGION, "us-east-1")
            .endpoint(Endpoint.builder().uri(endpoint.toString()).build())
            .addIdentityResolver(resolver)
            // Standard token-bucket retries, 3 attempts: equivalent to V2 RetryMode.STANDARD.
            .retryStrategy(StandardRetryStrategy.builder().maxAttempts(3).build())
            .transport(new SmithyHttpClientTransport(http));
        if (metrics) {
            builder.addPlugin(new OperationMetricsPlugin(otel.otel));
        }
        var ddb = builder.build();

        var getReq = com.example.demo.dynamodb.model.GetItemInput.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .key(Map.of("pk", com.example.demo.dynamodb.model.AttributeValue
                .builder().s(BenchmarkItems.SMALL_KEY).build()))
            .build();
        var putReq = com.example.demo.dynamodb.model.PutItemInput.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .item(BenchmarkItems.toSj(BenchmarkItems.smallItem()))
            .build();

        List<Map<String, com.example.demo.dynamodb.model.AttributeValue>> keys =
            new ArrayList<>();
        List<com.example.demo.dynamodb.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", com.example.demo.dynamodb.model.AttributeValue
                .builder().s(BenchmarkItems.batchKey(i)).build()));
            writes.add(com.example.demo.dynamodb.model.WriteRequest.builder()
                .putRequest(com.example.demo.dynamodb.model.PutRequest.builder()
                                .item(BenchmarkItems.toSj(BenchmarkItems.mediumItem(i))).build())
                .build());
        }
        var batchGetReq = com.example.demo.dynamodb.model.BatchGetItemInput.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                 com.example.demo.dynamodb.model.KeysAndAttributes
                                     .builder().keys(keys).build()))
            .build();
        Map<String, List<com.example.demo.dynamodb.model.WriteRequest>> writeMap =
            new LinkedHashMap<>();
        writeMap.put(BenchmarkItems.TABLE_NAME, writes);
        var batchPutReq = com.example.demo.dynamodb.model.BatchWriteItemInput.builder()
            .requestItems(writeMap)
            .build();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "smithy-http1";
            }

            public void resetMetrics() {
                otel.reset();
            }

            public void printMetrics(PrintStream out) {
                otel.print(out);
            }

            public void close() throws Exception {
                ((software.amazon.smithy.java.client.core.Client) ddb).close();
                http.close();
            }
        };
    }
}
