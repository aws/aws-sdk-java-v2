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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SplitAsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;
import software.amazon.awssdk.utils.Pair;

@WireMockTest
class MultipartDownloaderSubscriberWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";

    private S3AsyncClient s3AsyncClient;
    private Random random;
    private String eTag;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .pathStyleAccessEnabled(true)
                                                                          .build())
                                     .overrideConfiguration(b -> b.retryPolicy(p -> p.numRetries(5)))
                                     .build();
        random = new Random();
        eTag = UUID.randomUUID().toString();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void happyPath_shouldReceiveAllBodyPArtInCorrectOrder(AsyncResponseTransformerTestSupplier<T> supplier,
                                                   int amountOfPartToTest,
                                                   int partSize) {
        byte[] expectedBody = new byte[amountOfPartToTest * partSize];
        for (int i = 0; i < amountOfPartToTest; i++) {
            byte[] individualBody = stubForPart(i + 1, amountOfPartToTest, partSize);
            System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
        }

        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        SplitAsyncResponseTransformer<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSize(1024 * 1024 * 64L)
                                             .build());
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        split.publisher().subscribe(subscriber);
        T response = split.preparedFuture().join();

        byte[] body = supplier.body(response);
        assertArrayEquals(expectedBody, body);
        verifyCorrectAmountOfRequestsMade(amountOfPartToTest);
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void retryableErrorOnFirstRequest_shouldRetryAndSucceed(AsyncResponseTransformerTestSupplier<T> supplier,
                                                                int amountOfPartToTest,
                                                                int partSize) {

        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .inScenario("RETRY")
                    .whenScenarioStateIs(STARTED)
                    .willSetStateTo("SUCCESS")
                    .willReturn(
                        aResponse()
                            .withStatus(500)
                            .withBody(" <Error><Code>InternalError</Code><Message>test error msg</Message></Error>")
                            .withHeader("x-amzn-ErrorType", "InternalError")));

        byte[] expectedBody = new byte[amountOfPartToTest * partSize];
        for (int i = 0; i < amountOfPartToTest; i++) {
            byte[] individualBody = stubForPartSuccess(i + 1, amountOfPartToTest, partSize);
            System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
        }

        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        SplitAsyncResponseTransformer<GetObjectResponse,T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSize(1024 * 1024 * 64L)
                                             .build());
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        split.publisher().subscribe(subscriber);
        T response = split.preparedFuture().join();
        byte[] body = supplier.body(response);
        assertArrayEquals(expectedBody, body);
        verify(exactly(amountOfPartToTest + 1), getRequestedFor(urlMatching(".*partNumber=\\d+.*")));
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void nonRetryableErrorOnFirstRequest_shouldCompleteExceptionally(AsyncResponseTransformerTestSupplier<T> supplier,
                                                                         int amountOfPartToTest,
                                                                         int partSize) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey)))
                    .willReturn(
                        aResponse()
                            .withStatus(409)
                            .withBody(" <Error><Code>OperationAborted</Code><Message>test error msg</Message></Error>")
                            .withHeader("x-amzn-ErrorType", "OperationAborted")));

        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        SplitAsyncResponseTransformer<GetObjectResponse,T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSize(1024 * 1024 * 64L)
                                             .build());
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());
        split.publisher().subscribe(subscriber);

        split.preparedFuture().join();

        assertThat(split.preparedFuture())
            .isCompletedExceptionally()
            .withFailMessage("test error msg");

        verify(exactly(1), getRequestedFor(urlMatching(".*partNumber=\\d+.*")));
        verify(exactly(0), getRequestedFor(urlMatching(".*partNumber=[^1].*")));

    }

    private byte[] stubForPartSuccess(int part, int totalPart, int partSize) {
        byte[] body = new byte[partSize];
        random.nextBytes(body);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, part)))
                    .inScenario("RETRY")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(
                        aResponse()
                            .withHeader("x-amz-mp-parts-count", totalPart + "")
                            .withHeader("ETag", eTag)
                            .withBody(body)));
        return body;
    }

    private byte[] stubForPart(int part, int totalPart, int partSize) {
        byte[] body = new byte[partSize];
        random.nextBytes(body);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, part))).willReturn(
            aResponse()
                .withHeader("x-amz-mp-parts-count", totalPart + "")
                .withHeader("ETag", eTag)
                .withBody(body)));
        return body;
    }

    private static List<AsyncResponseTransformerTestSupplier<?>> transformersSuppliers() {
        return Arrays.asList(
            new AsyncResponseTransformerTestSupplier.ByteTestArtSupplier(),
            new AsyncResponseTransformerTestSupplier.InputStreamArtSupplier(),
            new AsyncResponseTransformerTestSupplier.PublisherArtSupplier(),
            new AsyncResponseTransformerTestSupplier.FileArtSupplier()
        );
    }

    private static Stream<Arguments> transformerArguments() {
        return transformersSuppliers().stream().map(Arguments::arguments);
    }

    private static Stream<Arguments> argumentsProvider() {
        // amount of part, individual part size
        List<Pair<Integer, Integer>> partSizes = Arrays.asList(
            Pair.of(4, 16),
            Pair.of(1, 1024),
            Pair.of(31, 1243),
            Pair.of(16, 16 * 1024),
            Pair.of(1, 1024 * 1024),
            Pair.of(4, 1024 * 1024),
            Pair.of(1, 4 * 1024 * 1024),
            Pair.of(16, 16 * 1024 * 1024),
            Pair.of(7, 5 * 3752)
        );

        Stream.Builder<Arguments> sb = Stream.builder();
        transformersSuppliers().forEach(tr -> partSizes.forEach(p -> sb.accept(arguments(tr, p.left(), p.right()))));
        return sb.build();
    }

    private void verifyCorrectAmountOfRequestsMade(int amountOfPartToTest) {
        String urlTemplate = ".*partNumber=%d.*";
        for (int i = 1; i <= amountOfPartToTest; i++) {
            verify(getRequestedFor(urlMatching(String.format(urlTemplate, i))));
        }
        verify(0, getRequestedFor(urlMatching(String.format(urlTemplate, amountOfPartToTest + 1))));
    }
}