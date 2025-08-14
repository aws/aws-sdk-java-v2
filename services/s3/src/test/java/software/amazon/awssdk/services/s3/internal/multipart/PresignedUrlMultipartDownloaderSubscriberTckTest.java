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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

public class PresignedUrlMultipartDownloaderSubscriberTckTest
    extends SubscriberWhiteboxVerification<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    
    private S3AsyncClient s3mock;

    public PresignedUrlMultipartDownloaderSubscriberTckTest() {
        super(new TestEnvironment());
        this.s3mock = Mockito.mock(S3AsyncClient.class);
    }

    @Override
    public Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
    createSubscriber(WhiteboxSubscriberProbe<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> probe) {
        AsyncPresignedUrlExtension presignedUrlExtension = Mockito.mock(AsyncPresignedUrlExtension.class);
        when(s3mock.presignedUrlExtension()).thenReturn(presignedUrlExtension);

        CompletableFuture<GetObjectResponse> firstPartResponse = CompletableFuture.completedFuture(
            GetObjectResponse.builder()
                .contentRange("bytes 0-8388607/33554432")
                .contentLength(8388608L) // 8MB
                .eTag("\"test-etag-12345\"")
                .build()
        );

        CompletableFuture<GetObjectResponse> subsequentPartResponse = CompletableFuture.completedFuture(
            GetObjectResponse.builder()
                .contentRange("bytes 8388608-16777215/33554432")
                .contentLength(8388608L) // 8MB
                .eTag("\"test-etag-12345\"")
                .build()
        );
        
        when(presignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(firstPartResponse)
            .thenReturn(subsequentPartResponse)
            .thenReturn(subsequentPartResponse)
            .thenReturn(subsequentPartResponse);
        
        return new PresignedUrlMultipartDownloaderSubscriber(
            s3mock,
            createTestPresignedUrlRequest(),
            8 * 1024 * 1024L
        ) {
            @Override
            public void onError(Throwable throwable) {
                super.onError(throwable);
                probe.registerOnError(throwable);
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                super.onSubscribe(subscription);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> item) {
                super.onNext(item);
                probe.registerOnNext(item);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> createElement(int element) {
        return new TestAsyncResponseTransformer();
    }

    private PresignedUrlDownloadRequest createTestPresignedUrlRequest() {
        try {
            return PresignedUrlDownloadRequest.builder()
                .presignedUrl(java.net.URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL())
                .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create test URL", e);
        }
    }

    private static class TestAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> {
        private CompletableFuture<GetObjectResponse> future;

        @Override
        public CompletableFuture<GetObjectResponse> prepare() {
            this.future = new CompletableFuture<>();
            return this.future;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            this.future.complete(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            future.completeExceptionally(error);
        }
    }
}
