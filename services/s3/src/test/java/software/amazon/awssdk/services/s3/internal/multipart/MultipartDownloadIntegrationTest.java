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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SplitTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SplitAsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Logger;

// WIP - please ignore for now, only used in manually testing
class MultipartDownloadIntegrationTest {
    private static final Logger log = Logger.loggerFor(MultipartDownloadIntegrationTest.class);

    static final int fileTestSize = 128;
    static final String bucket = "olapplin-test-bucket";
    static final String key = String.format("debug-test-%smb", fileTestSize);

    private S3AsyncClient s3;
    private final SplitTransformerConfiguration splitConfig = SplitTransformerConfiguration.builder()
                                                                                           .bufferSize(1024 * 1024 * 32L)
                                                                                           .build();

    @BeforeEach
    void init() {
        this.s3 = S3AsyncClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(ProfileCredentialsProvider.create())
                               .httpClient(NettyNioAsyncHttpClient.create())
                               .build();
    }

    @Test
    void testByteAsyncResponseTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> transformer =
            AsyncResponseTransformer.toBytes();
        MultipartDownloaderSubscriber downloaderSubscriber = new MultipartDownloaderSubscriber(
            s3, GetObjectRequest.builder().bucket(bucket).key(key).build());

        SplitAsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> split =
            transformer.split(splitConfig);
        split.publisher().subscribe(downloaderSubscriber);
        ResponseBytes<GetObjectResponse> res = split.preparedFuture().join();
        log.info(() -> "complete");
        byte[] bytes = res.asByteArray();
        log.info(() -> String.format("Byte len: %s", bytes.length));
        assertThat(bytes.length).isEqualTo(fileTestSize * 1024 * 1024);
    }

    @Test
    void testFileAsyncResponseTransformer() {
        Path path = Paths.get("/Users/olapplin/Develop/tmp/" + key);
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer =
            AsyncResponseTransformer.toFile(path);

        MultipartDownloaderSubscriber downloaderSubscriber = new MultipartDownloaderSubscriber(
            s3, GetObjectRequest.builder().bucket(bucket).key(key).build());

        SplitAsyncResponseTransformer<GetObjectResponse, GetObjectResponse> split = transformer.split(splitConfig);
        split.publisher().subscribe(downloaderSubscriber);
        GetObjectResponse res = split.preparedFuture().join();
        log.info(() -> "complete");
        assertTrue(path.toFile().exists());
        assertThat(path.toFile().length()).isEqualTo(fileTestSize * 1024 * 1024);
    }

    // @Test
    void testPublisherAsyncResponseTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ResponsePublisher<GetObjectResponse>> transformer =
            AsyncResponseTransformer.toPublisher();

        MultipartDownloaderSubscriber downloaderSubscriber = new MultipartDownloaderSubscriber(
            s3, GetObjectRequest.builder().bucket(bucket).key(key).build());
        SplitAsyncResponseTransformer<GetObjectResponse, ResponsePublisher<GetObjectResponse>> split =
            transformer.split(splitConfig);
        split.publisher().subscribe(downloaderSubscriber);
        split.preparedFuture().whenComplete((res, e) -> {
            log.info(() -> "complete");
            res.subscribe(new Subscriber<ByteBuffer>() {
                Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    log.info(() -> "received " + byteBuffer.remaining());
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onError(Throwable t) {

                }

                @Override
                public void onComplete() {

                }
            });
        });
        split.preparedFuture().join();
    }

    // @Test
    void testBlockingInputStreamResponseTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> transformer =
            AsyncResponseTransformer.toBlockingInputStream();

        MultipartDownloaderSubscriber downloaderSubscriber = new MultipartDownloaderSubscriber(
            s3, GetObjectRequest.builder().bucket(bucket).key(key).build());

        SplitAsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> split =
            transformer.split(splitConfig);
        split.publisher().subscribe(downloaderSubscriber);
        ResponseInputStream<GetObjectResponse> res = split.preparedFuture().join();
        log.info(() -> "complete");
        int total = 0;
        try {
            while (res.read() != -1) {
                total++;
            }
        } catch (IOException e) {
            fail(e);
        }
        assertThat(total).isEqualTo(fileTestSize * 1024 * 1024);
    }
}
