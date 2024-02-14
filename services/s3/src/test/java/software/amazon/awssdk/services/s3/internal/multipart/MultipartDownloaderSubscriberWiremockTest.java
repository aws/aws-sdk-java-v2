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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SplitAsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.InputStreamUtils;

@WireMockTest
class MultipartDownloaderSubscriberWiremockTest {

    private S3AsyncClient s3AsyncClient;
    private String testBucket = "test-bucket";
    private String testKey = "test-key";

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
                                     .build();
    }

    @ParameterizedTest
    @MethodSource("partsParamProvider")
    void happyPath_ByteArrayAsyncResponseTransformer(int amountOfPartToTest, int partSize) {
        for (int i = 1; i <= amountOfPartToTest; i++) {
            stubForPart(i, amountOfPartToTest, partSize);
        }

        MultipartDownloaderSubscriber subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        SplitAsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> split =
            AsyncResponseTransformer.<GetObjectResponse>toBytes().split(1024 * 16);

        split.publisher().subscribe(subscriber);
        ResponseBytes<GetObjectResponse> responseBytes = split.preparedFuture().join();

        byte[] fullBody = responseBytes.asByteArray();
        byte[] expectedBody = expectedBody(amountOfPartToTest, partSize);
        assertArrayEquals(expectedBody, fullBody);

        verifyCorrectAmountOfRequestsMade(amountOfPartToTest);
    }

    @ParameterizedTest
    @MethodSource("partsParamProvider")
    void happyPath_InputStreamResponseTransformer(int amountOfPartToTest, int partSize) {
        for (int i = 1; i <= amountOfPartToTest; i++) {
            stubForPart(i, amountOfPartToTest, partSize);
        }

        MultipartDownloaderSubscriber subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        SplitAsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> split =
            AsyncResponseTransformer.<GetObjectResponse>toBlockingInputStream().split(1024 * 16);

        split.publisher().subscribe(subscriber);
        ResponseInputStream<GetObjectResponse> responseInputStream = split.preparedFuture().join();

        byte[] fullBody = InputStreamUtils.drainInputStream(responseInputStream);
        byte[] expectedBody = expectedBody(amountOfPartToTest, partSize);
        assertArrayEquals(expectedBody, fullBody);

        verifyCorrectAmountOfRequestsMade(amountOfPartToTest);

    }

    @ParameterizedTest
    @MethodSource("partsParamProvider")
    void happyPath_PublisherAsyncResponseTransformer(int amountOfPartToTest, int partSize) {
        for (int i = 1; i <= amountOfPartToTest; i++) {
            stubForPart(i, amountOfPartToTest, partSize);
        }

        MultipartDownloaderSubscriber subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        SplitAsyncResponseTransformer<GetObjectResponse, ResponsePublisher<GetObjectResponse>> split =
            AsyncResponseTransformer.<GetObjectResponse>toPublisher().split(1024 * 16);

        split.publisher().subscribe(subscriber);
        ResponsePublisher<GetObjectResponse> responsePublisher = split.preparedFuture().join();

        List<Byte> bodyBytes = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        responsePublisher.subscribe(new Subscriber<ByteBuffer>() {
            Subscription s;
            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                while (byteBuffer.remaining() > 0) {
                    bodyBytes.add(byteBuffer.get());
                }
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
                fail("Unexpected onError during test", t);
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("Unexpected thread interruption during test", e);
        }

        byte[] fullBody = unbox(bodyBytes.toArray(new Byte[0]));
        byte[] expectedBody = expectedBody(amountOfPartToTest, partSize);
        assertArrayEquals(expectedBody, fullBody);

        verifyCorrectAmountOfRequestsMade(amountOfPartToTest);
    }

    @ParameterizedTest
    @MethodSource("partsParamProvider")
    void happyPath_FileAsyncResponseTransformer(int amountOfPartToTest, int partSize) {
        for (int i = 1; i <= amountOfPartToTest; i++) {
            stubForPart(i, amountOfPartToTest, partSize);
        }

        MultipartDownloaderSubscriber subscriber = new MultipartDownloaderSubscriber(
            s3AsyncClient,
            GetObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testKey)
                            .build());

        FileSystem fs = Jimfs.newFileSystem();
        String filePath = "/tmp-file-" + UUID.randomUUID();
        Path inMemoryFilePath = fs.getPath(filePath);
        SplitAsyncResponseTransformer<GetObjectResponse, GetObjectResponse> split =
            AsyncResponseTransformer.<GetObjectResponse>toFile(inMemoryFilePath).split(1024 * 16);

        split.publisher().subscribe(subscriber);
        GetObjectResponse response = split.preparedFuture().join();

        Path savedFile = fs.getPath(filePath);
        byte[] expectedBody = expectedBody(amountOfPartToTest, partSize);
        byte[] fullBody = null;
        try {
            fullBody = Files.readAllBytes(savedFile);
        } catch (IOException e) {
            fail("Unexpected IOException during test", e);
        }

        assertArrayEquals(expectedBody, fullBody);
        verifyCorrectAmountOfRequestsMade(amountOfPartToTest);

    }

    private static Stream<Arguments> partsParamProvider() {
        return Stream.of(
            arguments(4, 16),
            arguments(1, 1024),
            arguments(31, 1243)
        );
    }

    private void verifyCorrectAmountOfRequestsMade(int amountOfPartToTest) {
        String urlTemplate = ".*partNumber=%d.*";
        for (int i = 1; i <= amountOfPartToTest; i++) {
            verify(getRequestedFor(urlMatching(String.format(urlTemplate, i))));
        }
        verify(0, getRequestedFor(urlMatching(String.format(urlTemplate, amountOfPartToTest + 1))));
    }

    private void stubForPart(int part, int totalPart, int partSize) {
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, part))).willReturn(
            aResponse()
                .withHeader("x-amz-mp-parts-count", totalPart + "")
                .withBody(byteArrayOfLength(partSize, (byte) part))));
    }

    private byte[] expectedBody(int totalPart, int partSize) {
        Stream.Builder<Stream<Byte>> s = Stream.builder();
        for (int i = 1; i <= totalPart; i++) {
            int j = i;
            s.add(Stream.generate(() -> (byte) j).limit(partSize));
        }
        return unbox(s.build().flatMap(Function.identity()).toArray(Byte[]::new));
    }

    private byte[] unbox(Byte[] arr) {
        byte[] bb = new byte[arr.length];
        int i = 0;
        for (Byte b : arr) {
            bb[i] = b.byteValue();
            i++;
        }
        return bb;
    }

    private byte[] byteArrayOfLength(int length, byte value) {
        byte[] b = new byte[length];
        Arrays.fill(b, value);
        return b;
    }

}