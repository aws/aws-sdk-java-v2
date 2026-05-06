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

package software.amazon.awssdk.services.s3.apache5;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.apache5.Apache5NioAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Manual integration test comparing performance of Apache5NioAsyncHttpClient, NettyNioAsyncHttpClient,
 * and AwsCrtAsyncHttpClient for common S3 operations (listBuckets/GET, PUT, GET, HEAD, DELETE).
 *
 * <p>Each measured operation runs {@value #WARMUP_ITERATIONS} warmup call(s) to prime the connection
 * pool (DNS, TLS handshake, JIT compilation), followed by a single timed measurement call.
 *
 * <p>Requires valid AWS credentials and uses a user-specific bucket (configurable via
 * the {@code TEST_BUCKET} environment variable).
 */
public class S3Apache5NioAsyncClientTest {

    private static final String DEFAULT_BUCKET = "aws-sdk-java-test-bucket-joviegas";
    private static final String TEST_KEY = "apache5-nio-perftest/shared-object.txt";
    private static final byte[] TEST_PAYLOAD = "Hello from Apache5 NIO performance test!".getBytes();
    private static final int TIMEOUT_SECONDS = 30;
    private static final int WARMUP_ITERATIONS = 1;

    private static S3AsyncClient apache5Client;
    private static S3AsyncClient nettyClient;
    private static S3AsyncClient crtClient;
    private static String bucketName;

    @BeforeAll
    static void setup() throws Exception {
        bucketName = System.getenv().getOrDefault("TEST_BUCKET", DEFAULT_BUCKET);

        apache5Client = buildClient(Apache5NioAsyncHttpClient.create());
        nettyClient = buildClient(NettyNioAsyncHttpClient.create());
        crtClient = buildClient(AwsCrtAsyncHttpClient.create());

        // Seed the shared object used by GET/HEAD tests.
        apache5Client.putObject(r -> r.bucket(bucketName).key(TEST_KEY),
                                AsyncRequestBody.fromBytes(TEST_PAYLOAD))
                     .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @AfterAll
    static void teardown() throws Exception {
        try {
            apache5Client.deleteObject(r -> r.bucket(bucketName).key(TEST_KEY))
                         .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Best-effort cleanup.
        }
        close(apache5Client);
        close(nettyClient);
        close(crtClient);
    }

    // ---- listBuckets (GET, empty body) ----

    @Test
    void apache5_listBuckets() throws Exception {
        runListBuckets("Apache5", apache5Client);
    }

    @Test
    void netty_listBuckets() throws Exception {
        runListBuckets("Netty", nettyClient);
    }

    @Test
    void crt_listBuckets() throws Exception {
        runListBuckets("CRT", crtClient);
    }

    // ---- putObject (PUT with body) ----

    @Test
    void apache5_putObject() throws Exception {
        runPutObject("Apache5", apache5Client);
    }

    @Test
    void netty_putObject() throws Exception {
        runPutObject("Netty", nettyClient);
    }

    @Test
    void crt_putObject() throws Exception {
        runPutObject("CRT", crtClient);
    }

    // ---- getObject (GET with response body) ----

    @Test
    void apache5_getObject() throws Exception {
        runGetObject("Apache5", apache5Client);
    }

    @Test
    void netty_getObject() throws Exception {
        runGetObject("Netty", nettyClient);
    }

    @Test
    void crt_getObject() throws Exception {
        runGetObject("CRT", crtClient);
    }

    // ---- headObject (HEAD) ----

    @Test
    void apache5_headObject() throws Exception {
        runHeadObject("Apache5", apache5Client);
    }

    @Test
    void netty_headObject() throws Exception {
        runHeadObject("Netty", nettyClient);
    }

    @Test
    void crt_headObject() throws Exception {
        runHeadObject("CRT", crtClient);
    }

    // ---- deleteObject (DELETE) ----

    @Test
    void apache5_deleteObject() throws Exception {
        runDeleteObject("Apache5", apache5Client);
    }

    @Test
    void netty_deleteObject() throws Exception {
        runDeleteObject("Netty", nettyClient);
    }

    @Test
    void crt_deleteObject() throws Exception {
        runDeleteObject("CRT", crtClient);
    }

    // ---- Helpers ----

    private static S3AsyncClient buildClient(SdkAsyncHttpClient httpClient) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .httpClient(httpClient)
                            .build();
    }

    private static void runListBuckets(String label, S3AsyncClient client) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            client.listBuckets().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        // Timed
        long start = System.currentTimeMillis();
        ListBucketsResponse response = client.listBuckets().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        List<Bucket> buckets = response.buckets();
        System.out.printf("[%-7s] listBuckets : %4d ms (found %d buckets, warmup=%d)%n",
                          label, elapsed, buckets.size(), WARMUP_ITERATIONS);
    }

    private static void runPutObject(String label, S3AsyncClient client) throws Exception {
        // Warmup - each iteration uses a unique key so we do real PUTs.
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String warmupKey = "apache5-nio-perftest/put-warmup-" + label.toLowerCase() + "-" + i + "-"
                               + System.nanoTime();
            client.putObject(r -> r.bucket(bucketName).key(warmupKey),
                             AsyncRequestBody.fromBytes(TEST_PAYLOAD))
                  .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client.deleteObject(r -> r.bucket(bucketName).key(warmupKey))
                  .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Timed
        String key = "apache5-nio-perftest/put-" + label.toLowerCase() + "-" + System.nanoTime();
        long start = System.currentTimeMillis();
        PutObjectResponse response = client.putObject(r -> r.bucket(bucketName).key(key),
                                                      AsyncRequestBody.fromBytes(TEST_PAYLOAD))
                                           .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        System.out.printf("[%-7s] putObject   : %4d ms (%d bytes, warmup=%d)%n",
                          label, elapsed, TEST_PAYLOAD.length, WARMUP_ITERATIONS);

        // Cleanup
        try {
            client.deleteObject(r -> r.bucket(bucketName).key(key)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static void runGetObject(String label, S3AsyncClient client) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            client.getObject(r -> r.bucket(bucketName).key(TEST_KEY), AsyncResponseTransformer.toBytes())
                  .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Timed
        long start = System.currentTimeMillis();
        byte[] content = client.getObject(r -> r.bucket(bucketName).key(TEST_KEY),
                                          AsyncResponseTransformer.toBytes())
                               .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                               .asByteArray();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(content).isEqualTo(TEST_PAYLOAD);
        System.out.printf("[%-7s] getObject   : %4d ms (%d bytes, warmup=%d)%n",
                          label, elapsed, content.length, WARMUP_ITERATIONS);
    }

    private static void runHeadObject(String label, S3AsyncClient client) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            client.headObject(r -> r.bucket(bucketName).key(TEST_KEY)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Timed
        long start = System.currentTimeMillis();
        HeadObjectResponse response = client.headObject(r -> r.bucket(bucketName).key(TEST_KEY))
                                            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        System.out.printf("[%-7s] headObject  : %4d ms (content-length=%d, warmup=%d)%n",
                          label, elapsed, response.contentLength(), WARMUP_ITERATIONS);
    }

    private static void runDeleteObject(String label, S3AsyncClient client) throws Exception {
        // Warmup - create and delete a dedicated object each iteration.
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String warmupKey = "apache5-nio-perftest/del-warmup-" + label.toLowerCase() + "-" + i + "-"
                               + System.nanoTime();
            client.putObject(r -> r.bucket(bucketName).key(warmupKey),
                             AsyncRequestBody.fromBytes(TEST_PAYLOAD))
                  .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client.deleteObject(r -> r.bucket(bucketName).key(warmupKey))
                  .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Pre-create target object (not timed).
        String key = "apache5-nio-perftest/del-" + label.toLowerCase() + "-" + System.nanoTime();
        client.putObject(r -> r.bucket(bucketName).key(key),
                         AsyncRequestBody.fromBytes(TEST_PAYLOAD))
              .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Timed DELETE
        long start = System.currentTimeMillis();
        DeleteObjectResponse response = client.deleteObject(r -> r.bucket(bucketName).key(key))
                                              .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        System.out.printf("[%-7s] deleteObject: %4d ms (warmup=%d)%n", label, elapsed, WARMUP_ITERATIONS);
    }

    private static void close(S3AsyncClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
