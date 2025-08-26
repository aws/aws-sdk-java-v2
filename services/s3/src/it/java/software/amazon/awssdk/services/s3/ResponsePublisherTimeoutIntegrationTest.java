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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ResponsePublisherTimeoutIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectIntegrationTest.class);
    private static final String KEY = "TestKey";
    private static final String CONTENT = "Hello";
    private static final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                             .bucket(BUCKET)
                                                                             .key(KEY)
                                                                             .build();
    private S3AsyncClient s3AsyncClient;

    @Before
    public void init() {
        s3AsyncClient = s3AsyncClientBuilder().httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(1)).build();
    }

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), RequestBody.fromString(CONTENT));
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void defaultTimeout_firstPublisherNotConsumed_secondRequestTimesOut() {
        getObjectWithDefaultTimeoutPublisher();
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get2 = getObjectWithDefaultTimeoutPublisher();

        assertThatThrownBy(get2::join).hasRootCauseInstanceOf(TimeoutException.class)
                                      .hasMessageContaining("Acquire operation took longer than the configured maximum time. "
                                                            + "This indicates that a request cannot get a connection from the "
                                                            + "pool within the specified maximum time.");
    }

    @Test
    public void defaultTimeout_firstPublisherConsumed_secondRequestSucceeds() {
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get1 = getObjectWithDefaultTimeoutPublisher();
        consumeResponsePublisher(get1.join());
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get2 = getObjectWithDefaultTimeoutPublisher();

        GetObjectResponse getObjectResponse = get2.join().response();
        assertThat(getObjectResponse.contentLength()).isEqualTo(CONTENT.length());
    }

    @Test
    public void defaultTimeout_cancelFirstRequestFuture_secondRequestSucceeds() {
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get1 = getObjectWithDefaultTimeoutPublisher();
        get1.cancel(true);
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get2 = getObjectWithDefaultTimeoutPublisher();

        GetObjectResponse getObjectResponse = get2.join().response();
        assertThat(getObjectResponse.contentLength()).isEqualTo(CONTENT.length());
    }

    @Test
    public void customTimeout_waitForTimeout_secondRequestSucceeds() throws InterruptedException {
        s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toPublisher(Duration.ofSeconds(2)));
        Thread.sleep(3000);
        CompletableFuture<ResponsePublisher<GetObjectResponse>> get2 = getObjectWithDefaultTimeoutPublisher();

        GetObjectResponse getObjectResponse = get2.join().response();
        assertThat(getObjectResponse.contentLength()).isEqualTo(CONTENT.length());
    }

    private CompletableFuture<ResponsePublisher<GetObjectResponse>> getObjectWithDefaultTimeoutPublisher() {
        return s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toPublisher());
    }

    private void consumeResponsePublisher(Publisher<ByteBuffer> responsePublisher) {
        responsePublisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            @Override
            public void onNext(ByteBuffer byteBuffer) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {
            }
        });
    }
}
