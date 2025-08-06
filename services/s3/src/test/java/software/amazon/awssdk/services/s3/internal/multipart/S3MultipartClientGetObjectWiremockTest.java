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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadTestUtil.transformersSuppliers;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;
import software.amazon.awssdk.utils.Pair;

@WireMockTest
class S3MultipartClientGetObjectWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";
    private final static int MAX_ATTEMPT = 3;

    private S3AsyncClient s3AsyncClient;
    private MultipartDownloadTestUtil util;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        wiremock.getWireMock().resetMappings();
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .multipartEnabled(true)
            .overrideConfiguration(o -> o.retryStrategy(b -> b.maxAttempts(MAX_ATTEMPT)))
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .pathStyleAccessEnabled(true)
                                                                          .build())
                                     .build();
        util = new MultipartDownloadTestUtil(testBucket, testKey, UUID.randomUUID().toString());
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void happyPath_shouldReceiveAllBodyPartInCorrectOrder(AsyncResponseTransformerTestSupplier<T> supplier,
                                                              int amountOfPartToTest,
                                                              int partSize) {
        byte[] expectedBody = util.stubAllParts(testBucket, testKey, amountOfPartToTest, partSize);
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        T response = s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join();

        byte[] body = supplier.body(response);
        assertArrayEquals(expectedBody, body);
        util.verifyCorrectAmountOfRequestsMade(amountOfPartToTest);
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void nonRetryableErrorOnFirstPart_shouldFail(AsyncResponseTransformerTestSupplier<T> supplier,
                                                             int amountOfPartToTest,
                                                             int partSize) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=1", testBucket, testKey))).willReturn(
            aResponse()
                .withStatus(400)
                .withBody("<Error><Code>400</Code><Message>test error message</Message></Error>")));
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());

        assertThatThrownBy(() -> s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join())
            .hasMessageContaining("test error message");
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void nonRetryableErrorOnThirdPart_shouldCompleteExceptionallyOnlyPartsGreaterThanTwo(
        AsyncResponseTransformerTestSupplier<T> supplier,
        int amountOfPartToTest,
        int partSize) {
        util.stubForPart(testBucket, testKey, 1, 3, partSize);
        util.stubForPart(testBucket, testKey, 2, 3, partSize);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=3", testBucket, testKey))).willReturn(
            aResponse()
                .withStatus(400)
                .withBody("<Error><Code>400</Code><Message>test error message</Message></Error>")));
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        if (partSize > 1) {
            assertThatThrownBy(() -> {
                T res = s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join();
                supplier.body(res);
            }).hasMessageContaining("test error message");
        } else {
            T res = split.resultFuture().join();
            assertNotNull(supplier.body(res));
        }
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void ioError_retryExhausted_shouldFail(AsyncResponseTransformerTestSupplier<T> supplier,
                                                     int amountOfPartToTest,
                                                     int partSize) {
        util.stubIoError( 1);
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();

        assertThatThrownBy(() -> s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join())
            .hasMessageContaining("The connection was closed during the request");
        verify(MAX_ATTEMPT, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1",
                                                                     testBucket, testKey))));
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void serverError_retryExhausted_shouldFail(AsyncResponseTransformerTestSupplier<T> supplier,
                                               int amountOfPartToTest,
                                               int partSize) {
        util.stubSeverError(1, util.internalErrorBody(), amountOfPartToTest);
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();


        // Only enable this test for ByteArrayAsyncResponseTransformer because only ByteArrayAsyncResponseTransformer supports
        // retry
        Assumptions.assumeTrue(transformer instanceof ByteArrayAsyncResponseTransformer);

        assertThatThrownBy(() -> s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join())
            .hasMessageContaining(" We encountered an internal error");
        verify(MAX_ATTEMPT, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber=1",
                                                                     testBucket, testKey))));
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void serverError_retrySucceeds_shouldSucceed(AsyncResponseTransformerTestSupplier<T> supplier,
                                                   int amountOfPartToTest,
                                                   int partSize) {

        byte[] expectedBody = util.stubFirst503Second200AllParts(amountOfPartToTest, partSize);
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();

        // Only enable this test for ByteArrayAsyncResponseTransformer because only ByteArrayAsyncResponseTransformer supports
        // retry
        Assumptions.assumeTrue(transformer instanceof ByteArrayAsyncResponseTransformer);

        T response = s3AsyncClient.getObject(b -> b.bucket(testBucket).key(testKey), transformer).join();

        byte[] body = supplier.body(response);
        assertArrayEquals(expectedBody, body);
        util.verifyCorrectAmountOfRequestsMade(amountOfPartToTest);

        IntStream.range(1, amountOfPartToTest)
                 .forEach(index -> verify(2, getRequestedFor(urlEqualTo(String.format("/%s/%s?partNumber="+ index,
                                                                                                testBucket, testKey)))));
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
            Pair.of(4, 6 * 1024 * 1024),
            Pair.of(7, 5 * 3752)
        );

        Stream.Builder<Arguments> sb = Stream.builder();
        transformersSuppliers().forEach(tr -> partSizes.forEach(p -> sb.accept(arguments(tr, p.left(), p.right()))));
        return sb.build();
    }

}