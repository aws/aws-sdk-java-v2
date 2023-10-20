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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.mediastore.MediaStoreClient;
import software.amazon.awssdk.services.mediastore.model.Container;
import software.amazon.awssdk.services.mediastore.model.ContainerStatus;
import software.amazon.awssdk.services.mediastore.model.DescribeContainerResponse;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

/**
 * Base class for MediaStoreData integration tests. Used for Transfer-Encoding and Request Compression testing.
 */
public class MediaStoreDataIntegrationTestBase extends AwsIntegrationTestBase {
    protected static AwsCredentialsProvider credentialsProvider;
    protected static MediaStoreClient mediaStoreClient;
    protected static URI uri;

    @BeforeAll
    public static void init() {
        credentialsProvider = getCredentialsProvider();
        mediaStoreClient = MediaStoreClient.builder()
                                           .credentialsProvider(credentialsProvider)
                                           .httpClient(ApacheHttpClient.builder().build())
                                           .build();
    }

    @AfterEach
    public void reset() {
        CaptureTransferEncodingHeaderInterceptor.reset();
    }

    protected static Container createContainer(String containerName) {
        mediaStoreClient.createContainer(r -> r.containerName(containerName));
        DescribeContainerResponse response = waitContainerToBeActive(containerName);
        return response.container();
    }

    private static DescribeContainerResponse waitContainerToBeActive(String containerName) {
        return Waiter.run(() -> mediaStoreClient.describeContainer(r -> r.containerName(containerName)))
                     .until(r -> r.container().status() == ContainerStatus.ACTIVE)
                     .orFailAfter(Duration.ofMinutes(3));
    }

    protected AsyncRequestBody customAsyncRequestBodyWithoutContentLength(byte[] body) {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes(body))
                        .subscribe(s);
            }
        };
    }

    protected static class CaptureTransferEncodingHeaderInterceptor implements ExecutionInterceptor {
        public static boolean isChunked;

        public static void reset() {
            isChunked = false;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            isChunked = context.httpRequest().matchingHeaders("Transfer-Encoding").contains("chunked");
        }
    }

    protected static class TestContentProvider implements ContentStreamProvider {
        private final byte[] content;
        private final List<CloseTrackingInputStream> createdStreams = new ArrayList<>();
        private CloseTrackingInputStream currentStream;

        protected TestContentProvider(byte[] content) {
            this.content = content.clone();
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
            return Collections.unmodifiableList(createdStreams);
        }
    }

    protected static class CloseTrackingInputStream extends FilterInputStream {
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
