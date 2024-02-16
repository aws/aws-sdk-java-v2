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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;
import software.amazon.awssdk.utils.IoUtils;
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
                                     .build();
        random = new Random();
        eTag = UUID.randomUUID().toString();
    }

    @ParameterizedTest
    @MethodSource("argumentsProvider")
    <T> void happyPath_allAsyncResponseTransformer(AsyncResponseTransformerTestSupplier<T> supplier,
                                                   int amountOfPartToTest,
                                                   int partSize) {
        byte[] expectedBody = new byte[amountOfPartToTest * partSize];
        for (int i = 0; i < amountOfPartToTest; i++) {
            byte[] individualBody = stubForPart(i + 1, amountOfPartToTest, partSize);
            System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
        }

        AsyncResponseTransformer<GetObjectResponse, T> transformer;
        if (supplier.requiresJimfs()) {
            FileSystem jimfs = Jimfs.newFileSystem();
            String filePath = "/tmp-file-" + UUID.randomUUID();
            Path inMemoryFilePath = jimfs.getPath(filePath);
            transformer = supplier.transformer(inMemoryFilePath);
        } else {
            transformer = supplier.transformer(null);
        }

        SplitAsyncResponseTransformer<GetObjectResponse, T> split = transformer.split(1024 * 1024 * 64);
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
            new ByteTestArtSupplier(),
            new InputStreamArtSupplier(),
            new PublisherArtSupplier(),
            new FileArtSupplier()
        );
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

    private static byte[] unbox(Byte[] arr) {
        byte[] bb = new byte[arr.length];
        int i = 0;
        for (Byte b : arr) {
            bb[i] = b;
            i++;
        }
        return bb;
    }

    private static class ByteTestArtSupplier implements AsyncResponseTransformerTestSupplier<ResponseBytes<GetObjectResponse>> {
        @Override
        public byte[] body(ResponseBytes<GetObjectResponse> response) {
            return response.asByteArray();
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> transformer(Path elem) {
            return AsyncResponseTransformer.toBytes();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toBytes";
        }
    }

    private static class InputStreamArtSupplier
        implements AsyncResponseTransformerTestSupplier<ResponseInputStream<GetObjectResponse>> {
        @Override
        public byte[] body(ResponseInputStream<GetObjectResponse> response) {
            try {
                return IoUtils.toByteArray(response);
            } catch (IOException ioe) {
                fail("unexpected IOE during test", ioe);
                return null;
            }
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> transformer(Path elem) {
            return AsyncResponseTransformer.toBlockingInputStream();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toBlockingInputStream";
        }
    }

    private static class FileArtSupplier implements AsyncResponseTransformerTestSupplier<GetObjectResponse> {
        private Path path;

        @Override
        public byte[] body(GetObjectResponse response) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException ioe) {
                fail("unexpected IOE during test", ioe);
                return new byte[0];
            }
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer(Path path) {
            this.path = path;
            return AsyncResponseTransformer.toFile(path);
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toFile";
        }

        @Override
        public boolean requiresJimfs() {
            return true;
        }
    }

    private static class PublisherArtSupplier implements AsyncResponseTransformerTestSupplier<ResponsePublisher<GetObjectResponse>> {
        @Override
        public byte[] body(ResponsePublisher<GetObjectResponse> response) {
            List<Byte> buffer = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            response.subscribe(new Subscriber<ByteBuffer>() {
                Subscription s;

                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    while (byteBuffer.remaining() > 0) {
                        buffer.add(byteBuffer.get());
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
            return unbox(buffer.toArray(new Byte[0]));
        }

        @Override
        public AsyncResponseTransformer<GetObjectResponse, ResponsePublisher<GetObjectResponse>> transformer(Path elem) {
            return AsyncResponseTransformer.toPublisher();
        }

        @Override
        public String toString() {
            return "AsyncResponseTransformer.toPublisher";
        }
    }
}