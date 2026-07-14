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

public class ParallelPresignedUrlMultipartDownloaderSubscriberTckTest
    extends SubscriberWhiteboxVerification<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {

    private final S3AsyncClient s3mock;

    public ParallelPresignedUrlMultipartDownloaderSubscriberTckTest() {
        super(new TestEnvironment());
        this.s3mock = Mockito.mock(S3AsyncClient.class);
        AsyncPresignedUrlExtension presignedUrlExtension = Mockito.mock(AsyncPresignedUrlExtension.class);
        when(s3mock.presignedUrlExtension()).thenReturn(presignedUrlExtension);

        when(presignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetObjectResponse.builder()
                                 .contentRange("bytes 0-8388607/33554432")
                                 .contentLength(8388608L)
                                 .eTag("\"test-etag\"")
                                 .build()));
    }

    @Override
    public Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> createSubscriber(
        WhiteboxSubscriberProbe<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> probe) {

        return new ParallelPresignedUrlMultipartDownloaderSubscriber(
            s3mock,
            createTestPresignedUrlRequest(),
            8 * 1024 * 1024L,
            new CompletableFuture<>(),
            10
        ) {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        s.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> item) {
                super.onNext(item);
                probe.registerOnNext(item);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                probe.registerOnError(t);
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
        return new AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>() {
            @Override
            public CompletableFuture<GetObjectResponse> prepare() {
                return new CompletableFuture<>();
            }

            @Override
            public void onResponse(GetObjectResponse response) {
            }

            @Override
            public void onStream(SdkPublisher<ByteBuffer> publisher) {
            }

            @Override
            public void exceptionOccurred(Throwable error) {
            }
        };
    }

    private PresignedUrlDownloadRequest createTestPresignedUrlRequest() {
        try {
            return PresignedUrlDownloadRequest.builder()
                .presignedUrl(java.net.URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL())
                .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
