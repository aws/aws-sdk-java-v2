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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadTestUtil.transformersSuppliers;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;

@WireMockTest
class PresignedUrlMultipartDownloaderSubscriberWiremockTest {

    private final String testBucket = "test-bucket";
    private final String testKey = "test-key";
    private final String basePresignedUrl = "http://localhost:%d/presigned-url";

    private S3AsyncClient s3AsyncClient;
    private PresignedUrlMultipartDownloadTestUtil util;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("key", "secret")))
                                     .region(Region.US_WEST_2)
                                     .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                     .serviceConfiguration(S3Configuration.builder()
                                                                          .pathStyleAccessEnabled(true)
                                                                          .checksumValidationEnabled(false)
                                                                          .build())
                                     .build();
        util = new PresignedUrlMultipartDownloadTestUtil(
            String.format(basePresignedUrl, wiremock.getHttpPort()),
            UUID.randomUUID().toString()
        );
    }

    @ParameterizedTest(name = "multipart download: {1} parts of {2} bytes each")
    @MethodSource("argumentsProvider")
    <T> void onNext_withMultipleParts_shouldReceiveAllBodyPartsInCorrectOrder(
        AsyncResponseTransformerTestSupplier<T> supplier,
        int amountOfPartsToTest,
        int partSize) {
        byte[] expectedBody = util.stubAllRangeParts(amountOfPartsToTest, partSize);
        URL presignedUrl = createPresignedUrl(util.getPresignedUrl());
        
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());
        
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = 
            new PresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                PresignedUrlDownloadRequest.builder()
                                          .presignedUrl(presignedUrl)
                                          .build(),
                partSize);

        split.publisher().subscribe(subscriber);
        T response = split.resultFuture().join();

        byte[] actualBody = supplier.body(response);
        assertArrayEquals(expectedBody, actualBody);
        util.verifyCorrectAmountOfRangeRequestsMade(amountOfPartsToTest);
    }

    @ParameterizedTest(name = "single part download: {1} bytes")
    @MethodSource("singlePartArgumentsProvider")
    <T> void onNext_withSinglePart_shouldReceiveCompleteBody(
        AsyncResponseTransformerTestSupplier<T> supplier,
        int partSize) {

        int actualPartSize = partSize * 2; // Larger part size to ensure single part
        byte[] expectedBody = util.stubSingleRangePart(actualPartSize);
        URL presignedUrl = createPresignedUrl(util.getPresignedUrl());
        
        AsyncResponseTransformer<GetObjectResponse, T> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());
        
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = 
            new PresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                PresignedUrlDownloadRequest.builder()
                                          .presignedUrl(presignedUrl)
                                          .build(),
                actualPartSize);

        split.publisher().subscribe(subscriber);
        T response = split.resultFuture().join();

        byte[] actualBody = supplier.body(response);
        assertArrayEquals(expectedBody, actualBody);
        util.verifyCorrectAmountOfRangeRequestsMade(1);
    }

    @Test
    void onNext_whenFirstRequestFails_shouldCompleteExceptionally() {
        AsyncResponseTransformerTestSupplier<?> supplier = transformersSuppliers().get(0);
        int partSize = 1024;
        
        URL presignedUrl = createPresignedUrl(util.getPresignedUrl());
        util.stubFirstRangeRequestWithError();
        
        AsyncResponseTransformer<GetObjectResponse, ?> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, ?> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());
        
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = 
            new PresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                PresignedUrlDownloadRequest.builder()
                                          .presignedUrl(presignedUrl)
                                          .build(),
                partSize);

        long startTime = System.currentTimeMillis();
        split.publisher().subscribe(subscriber);
        
        assertThatThrownBy(() -> split.resultFuture().join())
            .satisfiesAnyOf(
                ex -> assertThat(ex).isInstanceOf(RuntimeException.class).hasMessageContaining("test error message"),
                ex -> assertThat(ex).hasRootCauseInstanceOf(RuntimeException.class).hasMessageContaining("test error message")
            );

        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration).isLessThan(2000);

        util.verifyCorrectAmountOfRangeRequestsMade(1);
    }

    @Test
    void onNext_whenSecondRequestFails_shouldCompleteExceptionally() {
        AsyncResponseTransformerTestSupplier<?> supplier = transformersSuppliers().get(0);
        int partSize = 1024;
        int amountOfParts = 3;
        
        URL presignedUrl = createPresignedUrl(util.getPresignedUrl());
        util.stubFirstRangePartForSizeDiscovery(amountOfParts, partSize);
        util.stubSecondRangeRequestWithError(partSize);
        
        AsyncResponseTransformer<GetObjectResponse, ?> transformer = supplier.transformer();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, ?> split = transformer.split(
            SplittingTransformerConfiguration.builder()
                                             .bufferSizeInBytes(1024 * 32L)
                                             .build());
        
        Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> subscriber = 
            new PresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                PresignedUrlDownloadRequest.builder()
                                          .presignedUrl(presignedUrl)
                                          .build(),
                partSize);

        long startTime = System.currentTimeMillis();
        split.publisher().subscribe(subscriber);
        
        assertThatThrownBy(() -> split.resultFuture().join())
            .satisfiesAnyOf(
                ex -> assertThat(ex).isInstanceOf(RuntimeException.class).hasMessageContaining("test error message"),
                ex -> assertThat(ex).hasRootCauseInstanceOf(RuntimeException.class).hasMessageContaining("test error message")
            );
        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration).isLessThan(3000);

        util.verifyCorrectAmountOfRangeRequestsMade(2);
        util.verifyNoRequestMadeForRange(2 * partSize, 3 * partSize - 1);
    }



    private static Stream<Arguments> argumentsProvider() {
        List<AsyncResponseTransformerTestSupplier<?>> transformers = transformersSuppliers();
        
        return Stream.of(
            arguments(transformers.get(0), 2, 1024),
            arguments(transformers.get(0), 4, 16 * 1024),
            arguments(transformers.get(0), 7, 1024 * 1024),
            arguments(transformers.get(1), 3, 8192),
            arguments(transformers.get(0), 10, 512),
            arguments(transformers.get(0), 1, 4 * 1024 * 1024)
        );
    }

    private static Stream<Arguments> singlePartArgumentsProvider() {
        List<AsyncResponseTransformerTestSupplier<?>> transformers = transformersSuppliers();
        
        return Stream.of(
            arguments(transformers.get(0), 1024),
            arguments(transformers.get(0), 1024 * 1024),
            arguments(transformers.get(1), 16 * 1024)
        );
    }

    URL createPresignedUrl(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid presigned URL: " + urlString, e);
        }
    }
}
