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

package software.amazon.awssdk.services.mediastoredata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.mediastore.MediaStoreClient;
import software.amazon.awssdk.services.mediastore.model.Container;
import software.amazon.awssdk.services.mediastore.model.ContainerStatus;
import software.amazon.awssdk.services.mediastore.model.DescribeContainerResponse;
import software.amazon.awssdk.services.mediastoredata.model.DeleteObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.ObjectNotFoundException;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectRequest;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

/**
 * Integration test to verify Transfer-Encoding:chunked functionalities for all supported HTTP clients. Do not delete.
 */
public class TransferEncodingChunkedIntegrationTest extends AwsIntegrationTestBase {
    private static final String CONTAINER_NAME = "java-sdk-test-" + Instant.now().toEpochMilli();
    private static MediaStoreClient mediaStoreClient;
    private static MediaStoreDataClient syncClientWithApache;
    private static MediaStoreDataClient syncClientWithUrlConnection;
    private static MediaStoreDataAsyncClient asyncClientWithNetty;
    private static AwsCredentialsProvider credentialsProvider;
    private static Container container;
    private static PutObjectRequest putObjectRequest;
    private static DeleteObjectRequest deleteObjectRequest;

    @BeforeAll
    public static void setup() {
        credentialsProvider = getCredentialsProvider();
        mediaStoreClient = MediaStoreClient.builder()
                                           .credentialsProvider(credentialsProvider)
                                           .httpClient(ApacheHttpClient.builder().build())
                                           .build();
        container = createContainer();
        URI uri = URI.create(container.endpoint());

        syncClientWithApache = MediaStoreDataClient.builder()
                                                   .endpointOverride(uri)
                                                   .credentialsProvider(credentialsProvider)
                                                   .httpClient(ApacheHttpClient.builder().build())
                                                   .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                   .build();

        syncClientWithUrlConnection= MediaStoreDataClient.builder()
                                                         .endpointOverride(uri)
                                                         .credentialsProvider(credentialsProvider)
                                                         .httpClient(UrlConnectionHttpClient.create())
                                                         .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                         .build();

        asyncClientWithNetty = MediaStoreDataAsyncClient.builder()
                                                        .endpointOverride(uri)
                                                        .credentialsProvider(getCredentialsProvider())
                                                        .httpClient(NettyNioAsyncHttpClient.create())
                                                        .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                        .build();

        putObjectRequest = PutObjectRequest.builder()
                                           .contentType("application/octet-stream")
                                           .path("/foo")
                                           .build();

        deleteObjectRequest = DeleteObjectRequest.builder()
                                                 .path("/foo")
                                                 .build();
    }

    @AfterAll
    public static void tearDown() {
        syncClientWithApache.deleteObject(deleteObjectRequest);
        Waiter.run(() -> syncClientWithApache.describeObject(r -> r.path("/foo")))
              .untilException(ObjectNotFoundException.class)
              .orFailAfter(Duration.ofMinutes(1));
        CaptureTransferEncodingHeaderInterceptor.reset();
    }

    @Test
    public void apacheClientPutObject_withoutContentLength_sendsSuccessfully() {
        TestContentProvider provider = new TestContentProvider(RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        syncClientWithApache.putObject(putObjectRequest, RequestBody.fromContentProvider(provider, "binary/octet-stream"));
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }

    @Test
    public void urlConnectionClientPutObject_withoutContentLength_sendsSuccessfully() {
        TestContentProvider provider = new TestContentProvider(RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        syncClientWithUrlConnection.putObject(putObjectRequest, RequestBody.fromContentProvider(provider, "binary/octet-stream"));
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }

    @Test
    public void nettyClientPutObject_withoutContentLength_sendsSuccessfully() {
        asyncClientWithNetty.putObject(putObjectRequest, customAsyncRequestBodyWithoutContentLength()).join();
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }

    private static Container createContainer() {
        mediaStoreClient.createContainer(r -> r.containerName(CONTAINER_NAME));
        DescribeContainerResponse response = waitContainerToBeActive();
        return response.container();
    }

    private static DescribeContainerResponse waitContainerToBeActive() {
        return Waiter.run(() -> mediaStoreClient.describeContainer(r -> r.containerName(CONTAINER_NAME)))
                     .until(r -> ContainerStatus.ACTIVE.equals(r.container().status()))
                     .orFailAfter(Duration.ofMinutes(3));
    }

    private static class CaptureTransferEncodingHeaderInterceptor implements ExecutionInterceptor {
        private static boolean isChunked;

        public static void reset() {
            isChunked = false;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            isChunked = context.httpRequest().matchingHeaders("Transfer-Encoding").contains("chunked");
        }
    }

    private AsyncRequestBody customAsyncRequestBodyWithoutContentLength() {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes("Random text".getBytes()))
                        .subscribe(s);
            }
        };
    }

    private static class TestContentProvider implements ContentStreamProvider {
        private final byte[] content;
        private final List<CloseTrackingInputStream> createdStreams = new ArrayList<>();
        private CloseTrackingInputStream currentStream;

        private TestContentProvider(byte[] content) {
            this.content = content;
        }

        @Override
        public InputStream newStream() {
            if (currentStream != null) {
                invokeSafely(currentStream::close);
            }
            currentStream = new CloseTrackingInputStream(new ByteArrayInputStream(content));
            createdStreams.add(currentStream);
            return currentStream;
        }

        List<CloseTrackingInputStream> getCreatedStreams() {
            return createdStreams;
        }
    }

    private static class CloseTrackingInputStream extends FilterInputStream {
        private boolean isClosed = false;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }

        boolean isClosed() {
            return isClosed;
        }
    }
}
