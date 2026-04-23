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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Verifies GetObject response stream connection management. Tests that:
 * <ul>
 *     <li>Aborting a stream before fully consuming it does not leak connections.</li>
 *     <li>Fully consuming and closing a stream properly releases the connection back to the pool.</li>
 * </ul>
 * All tests use maxConcurrency=1 so that a leaked connection causes the next request to time out.
 */
@WireMockTest
class GetObjectResponseInputStreamConnectionManagementTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final byte[] LARGE_BODY = new byte[24 * 1024 * 1024];
    private static final byte[] SMALL_BODY = "hello".getBytes();
    private static final Duration CONNECTION_ACQUIRE_TIMEOUT = Duration.ofSeconds(5);

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret"));
    }

    private static void stubSlowGetAndHead() {
        // Drip-feed the response body over 10 seconds so that abort() is called while the body is still in-flight.
        // Without this, localhost is fast enough that the full body arrives before abort(), masking the leak.
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withBody(LARGE_BODY)
                                                    .withChunkedDribbleDelay(100, 10_000)));
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private static void stubGetAndHead() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(SMALL_BODY)));
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private static String previousMaxConnections;

    @BeforeAll
    static void setUpMaxConnections() {
        previousMaxConnections = System.getProperty("http.maxConnections");
        System.setProperty("http.maxConnections", "1");
    }

    @AfterAll
    static void restoreMaxConnections() {
        if (previousMaxConnections == null) {
            System.clearProperty("http.maxConnections");
        } else {
            System.setProperty("http.maxConnections", previousMaxConnections);
        }
    }

    static Stream<Arguments> syncHttpClients() {
        return Stream.of(
            Arguments.of("Apache",
                         ApacheHttpClient.builder().connectionAcquisitionTimeout(CONNECTION_ACQUIRE_TIMEOUT)
                                         .maxConnections(1).build()),
            Arguments.of("UrlConnection",
                         UrlConnectionHttpClient.builder().build()),
            Arguments.of("CrtSync",
                         AwsCrtHttpClient.builder().connectionAcquisitionTimeout(CONNECTION_ACQUIRE_TIMEOUT)
                                         .maxConcurrency(1).build())
        );
    }

    static Stream<Arguments> asyncHttpClients() {
        return Stream.of(
            Arguments.of("Netty",
                         NettyNioAsyncHttpClient.builder().connectionAcquisitionTimeout(CONNECTION_ACQUIRE_TIMEOUT)
                                                .maxConcurrency(1).build()),
            Arguments.of("CrtAsync",
                         AwsCrtAsyncHttpClient.builder().connectionAcquisitionTimeout(CONNECTION_ACQUIRE_TIMEOUT)
                                              .maxConcurrency(1).build())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncHttpClients")
    void syncGetObject_abortBeforeFullyConsumed_doesNotLeakConnection(
            String name, SdkHttpClient httpClient, WireMockRuntimeInfo wm) throws Exception {
        stubSlowGetAndHead();

        try (S3Client s3 = S3Client.builder()
                                   .httpClient(httpClient)
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                   .forcePathStyle(true)
                                   .credentialsProvider(credentials())
                                   .build()) {

            ResponseInputStream<GetObjectResponse> response = s3.getObject(r -> r.bucket(BUCKET).key(KEY));
            response.read();
            response.abort();

            HeadObjectResponse headResponse = s3.headObject(r -> r.bucket(BUCKET).key(KEY));
            assertThat(headResponse.sdkHttpResponse().isSuccessful()).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asyncHttpClients")
    void asyncGetObject_abortBeforeFullyConsumed_doesNotLeakConnection(
            String name, SdkAsyncHttpClient httpClient, WireMockRuntimeInfo wm) throws Exception {
        stubSlowGetAndHead();

        try (S3AsyncClient s3 = S3AsyncClient.builder()
                                             .httpClient(httpClient)
                                             .region(Region.US_EAST_1)
                                             .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                             .forcePathStyle(true)
                                             .credentialsProvider(credentials())
                                             .build()) {

            ResponseInputStream<GetObjectResponse> response =
                s3.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join();
            response.read();
            response.abort();

            HeadObjectResponse headResponse = s3.headObject(r -> r.bucket(BUCKET).key(KEY)).join();
            assertThat(headResponse.sdkHttpResponse().isSuccessful()).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncHttpClients")
    void syncGetObject_fullyConsumedAndClosed_connectionIsReused(
            String name, SdkHttpClient httpClient, WireMockRuntimeInfo wm) throws Exception {
        stubGetAndHead();

        try (S3Client s3 = S3Client.builder()
                                   .httpClient(httpClient)
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                   .forcePathStyle(true)
                                   .credentialsProvider(credentials())
                                   .build()) {

            ResponseInputStream<GetObjectResponse> response = s3.getObject(r -> r.bucket(BUCKET).key(KEY));
            IoUtils.drainInputStream(response);
            response.close();

            HeadObjectResponse headResponse = s3.headObject(r -> r.bucket(BUCKET).key(KEY));
            assertThat(headResponse.sdkHttpResponse().isSuccessful()).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asyncHttpClients")
    void asyncGetObject_fullyConsumedAndClosed_connectionIsReused(
            String name, SdkAsyncHttpClient httpClient, WireMockRuntimeInfo wm) throws Exception {
        stubGetAndHead();

        try (S3AsyncClient s3 = S3AsyncClient.builder()
                                             .httpClient(httpClient)
                                             .region(Region.US_EAST_1)
                                             .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                             .forcePathStyle(true)
                                             .credentialsProvider(credentials())
                                             .build()) {

            ResponseInputStream<GetObjectResponse> response =
                s3.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join();
            IoUtils.drainInputStream(response);
            response.close();

            HeadObjectResponse headResponse = s3.headObject(r -> r.bucket(BUCKET).key(KEY)).join();
            assertThat(headResponse.sdkHttpResponse().isSuccessful()).isTrue();
        }
    }
}
