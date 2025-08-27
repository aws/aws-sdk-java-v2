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

package software.amazon.awssdk.services.s3.internal.multipart;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadTestUtil.contentRangeHeader;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
class S3MultipartFileDownloadWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";

    private S3AsyncClient s3AsyncClient;
    private MultipartDownloadTestUtil util;
    private FileSystem fileSystem;
    private Path testFile;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) throws Exception {
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .multipartEnabled(true)
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .pathStyleAccessEnabled(true)
                                                                          .build())
                                     .httpClient(NettyNioAsyncHttpClient.builder()
                                                                        // .maxConcurrency(10_000)
                                                                        // .connectionAcquisitionTimeout(Duration.ofSeconds(5))
                                                                        // .connectionTimeout(Duration.ofSeconds(5))
                                                                        // .connectionTimeToLive(Duration.ofSeconds(5))
                                                                        .build())
                                     // .httpClient(AwsCrtAsyncHttpClient.create())
                                     .build();
        util = new MultipartDownloadTestUtil(testBucket, testKey, "test-etag");
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        testFile = fileSystem.getPath("/test-file.txt");
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void happyPath_singlePart() throws Exception {
        int partSize = 1024;
        byte[] expectedBody = util.stubSinglePart(testBucket, testKey, partSize);

        CompletableFuture<GetObjectResponse> response = s3AsyncClient.getObject(b -> b
                                                                                    .bucket(testBucket)
                                                                                    .key(testKey)
                                                                                    .build(),
                                                                                AsyncResponseTransformer.toFile(testFile));

        assertThat(response).succeedsWithin(Duration.of(10, ChronoUnit.SECONDS));
        assertThat(Files.exists(testFile)).isTrue();
        byte[] actualBody = Files.readAllBytes(testFile);
        assertThat(actualBody).isEqualTo(expectedBody);
        assertThat(response).isNotNull();
        util.verifyCorrectAmountOfRequestsMade(1);
    }

    @Test
    void happyPath_multipart() throws Exception {
        int numParts = 4;
        int partSize = 1024;
        byte[] expectedBody = util.stubAllParts(testBucket, testKey, numParts, partSize);

        CompletableFuture<GetObjectResponse> response = s3AsyncClient.getObject(b -> b
                                                                                    .bucket(testBucket)
                                                                                    .key(testKey)
                                                                                    .build(),
                                                                                AsyncResponseTransformer.toFile(testFile));

        assertThat(response).succeedsWithin(Duration.of(10, ChronoUnit.SECONDS));
        byte[] actualBody = Files.readAllBytes(testFile);
        assertThat(actualBody).isEqualTo(expectedBody);
        assertThat(response).isNotNull();
        util.verifyCorrectAmountOfRequestsMade(numParts);
    }

    @Test
    void errorOnFirstPart_nonRetryable() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))).willReturn(
            aResponse()
                .withStatus(403)
                .withBody("<Error><Code>AccessDenied</Code><Message>Test: Access denied!</Message></Error>")));

        CompletableFuture<GetObjectResponse> resp = s3AsyncClient.getObject(b -> b
                                                                                .bucket(testBucket)
                                                                                .key(testKey)
                                                                                .build(),
                                                                            AsyncResponseTransformer.toFile(testFile));
        assertThat(resp).failsWithin(Duration.of(10, ChronoUnit.SECONDS))
                        .withThrowableOfType(ExecutionException.class)
                        .withCauseInstanceOf(S3Exception.class)
                        .withMessageContaining("Test: Access denied!");
        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))));
    }

    @Test
    void errorOnFirstPart_retryable() {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 1</Message></Error>"))
                    .willSetStateTo("retry1"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry1")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 2</Message></Error>"))
                    .willSetStateTo("retry2"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 3</Message></Error>"))
                    .willSetStateTo("retry3"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry3")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 4</Message></Error>")));

        CompletableFuture<GetObjectResponse> resp = s3AsyncClient.getObject(b -> b
                                                                                .bucket(testBucket)
                                                                                .key(testKey)
                                                                                .build(),
                                                                            AsyncResponseTransformer.toFile(testFile));
        assertThat(resp).failsWithin(Duration.of(10, ChronoUnit.SECONDS))
                        .withThrowableOfType(ExecutionException.class)
                        .withCauseInstanceOf(S3Exception.class)
                        .withMessageContaining("Internal error 4");
        verify(exactly(4), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))));
    }

    @Test
    void errorOnMiddlePart_nonRetryable() {
        util.stubForPart(testBucket, testKey, 1, 3, 1024);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey))).willReturn(
            aResponse()
                .withStatus(403)
                .withBody("<Error><Code>AccessDenied</Code><Message>Test: Access denied!</Message></Error>")));
        util.stubForPart(testBucket, testKey, 3, 3, 1024);

        CompletableFuture<GetObjectResponse> resp = s3AsyncClient.getObject(b -> b
                                                                                .bucket(testBucket)
                                                                                .key(testKey)
                                                                                .build(),
                                                                            AsyncResponseTransformer.toFile(testFile));

        assertThat(resp).failsWithin(Duration.of(10, ChronoUnit.SECONDS))
                        .withThrowableOfType(ExecutionException.class)
                        .withCauseInstanceOf(S3Exception.class)
                        .withMessageContaining("Test: Access denied!");
        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))));
        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey))));
    }

    @Test
    void errorOnMiddlePart_retryable() {
        util.stubForPart(testBucket, testKey, 1, 3, 1024);
        util.stubForPart(testBucket, testKey, 3, 3, 1024);

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 1</Message></Error>"))
                    .willSetStateTo("retry1"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry1")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 2</Message></Error>"))
                    .willSetStateTo("retry2"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 3</Message></Error>"))
                    .willSetStateTo("retry3")
        );

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry3")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 4</Message></Error>")));

        CompletableFuture<GetObjectResponse> resp = s3AsyncClient.getObject(b -> b
                                                                                .bucket(testBucket)
                                                                                .key(testKey)
                                                                                .build(),
                                                                            AsyncResponseTransformer.toFile(testFile));

        assertThat(resp).failsWithin(Duration.of(10, ChronoUnit.SECONDS))
                        .withThrowableOfType(ExecutionException.class)
                        .withCauseInstanceOf(S3Exception.class)
                        .withMessageContaining("Internal error 4");

        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))));
        verify(exactly(4), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey))));
    }

    @Test
    void errorOnFirstPart_retryable_thenSucceeds() throws Exception {
        int partSize = 1024;
        int totalPart = 3;
        Random random = new Random();

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 1</Message></Error>"))
                    .willSetStateTo("retry1"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry1")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 2</Message></Error>"))
                    .willSetStateTo("retry2"));

        byte[] part1Data = new byte[partSize];
        random.nextBytes(part1Data);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, 1)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", totalPart + "")
                                    .withHeader("x-amz-content-range", contentRangeHeader(1, totalPart, partSize))
                                    .withHeader("ETag", "test-etag")
                                    .withBody(part1Data)));

        byte[] part2Data = new byte[partSize];
        random.nextBytes(part2Data);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, 2)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", totalPart + "")
                                    .withHeader("x-amz-content-range", contentRangeHeader(2, totalPart, partSize))
                                    .withHeader("ETag", "test-etag")
                                    .withBody(part2Data)));

        byte[] part3Data = new byte[partSize];
        random.nextBytes(part3Data);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, 3)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", totalPart + "")
                                    .withHeader("x-amz-content-range", contentRangeHeader(3, totalPart, partSize))
                                    .withHeader("ETag", "test-etag")
                                    .withBody(part3Data)));


    }

    @Test
    void errorOnMiddlePart_retryable_thenSucceeds() throws Exception {
        int partSize = 1024;
        int totalPart = 3;
        Random random = new Random();
        byte[] part1Data = util.stubForPart(testBucket, testKey, 1, totalPart, partSize);

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 1</Message></Error>"))
                    .willSetStateTo("retry1"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry1")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 2</Message></Error>"))
                    .willSetStateTo("retry2"));

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry2")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("<Error><Code>InternalError</Code><Message>Internal error 3</Message></Error>"))
                    .willSetStateTo("retry3")
        );

        byte[] part2Data = new byte[partSize];
        random.nextBytes(part2Data);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, 2)))
                    .inScenario("retry")
                    .whenScenarioStateIs("retry3")
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", totalPart + "")
                                    .withHeader("x-amz-content-range", contentRangeHeader(2, totalPart, partSize))
                                    .withHeader("ETag", "test-etag")
                                    .withBody(part2Data)));

        byte[] part3Data = new byte[partSize];
        random.nextBytes(part3Data);

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, 3)))
                    .willReturn(aResponse()
                                    .withHeader("x-amz-mp-parts-count", totalPart + "")
                                    .withHeader("x-amz-content-range", contentRangeHeader(3, totalPart, partSize))
                                    .withHeader("ETag", "test-etag")
                                    .withBody(part3Data)));

        CompletableFuture<GetObjectResponse> resp = s3AsyncClient.getObject(b -> b
                                                                                .bucket(testBucket)
                                                                                .key(testKey)
                                                                                .build(),
                                                                            AsyncResponseTransformer.toFile(testFile));

        assertThat(resp).succeedsWithin(Duration.of(10, ChronoUnit.SECONDS));

        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))));
        verify(exactly(4), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=2", testBucket, testKey))));
        verify(exactly(1), getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=3", testBucket, testKey))));

        assertThat(Files.exists(testFile)).isTrue();
        byte[] actualBody = Files.readAllBytes(testFile);
        byte[] expectedBody = new byte[partSize * totalPart];
        System.arraycopy(part1Data, 0, expectedBody, 0, partSize);
        System.arraycopy(part2Data, 0, expectedBody, partSize, partSize);
        System.arraycopy(part3Data, 0, expectedBody, partSize * 2, partSize);
        assertThat(actualBody).isEqualTo(expectedBody);

    }

    @Test
    void veryHighPartCount_shouldSucceed() throws Exception {
        int numParts = 5000;
        int partSize = 100;

        byte[] expectedBody = util.stubAllParts(testBucket, testKey, numParts, partSize);

        CompletableFuture<GetObjectResponse> response = s3AsyncClient.getObject(b -> b
                                                                                    .bucket(testBucket)
                                                                                    .key(testKey)
                                                                                    .build(),
                                                                                AsyncResponseTransformer.toFile(testFile));

        assertThat(response).succeedsWithin(Duration.of(5, ChronoUnit.MINUTES));
        response.join();
        byte[] actualBody = Files.readAllBytes(testFile);
        assertThat(actualBody).isEqualTo(expectedBody);
        assertThat(response).isNotNull();
        util.verifyCorrectAmountOfRequestsMade(numParts);

    }
}