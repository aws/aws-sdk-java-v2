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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class MultipartDownloaderSubscriberTckTest
    extends SubscriberWhiteboxVerification<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private S3AsyncClient s3mock;

    public MultipartDownloaderSubscriberTckTest() {
        super(new TestEnvironment());
        this.s3mock = Mockito.mock(S3AsyncClient.class);
    }

    @Override
    public Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>
    createSubscriber(WhiteboxSubscriberProbe<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> probe) {
        CompletableFuture<GetObjectResponse> responseFuture =
            CompletableFuture.completedFuture(GetObjectResponse.builder().partsCount(4).build());
        when(s3mock.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class))).thenReturn(responseFuture);
        return new MultipartDownloaderSubscriber(s3mock, GetObjectRequest.builder()
                                                                         .bucket("test-bucket-unused")
                                                                         .key("test-key-unused")
                                                                         .build()) {
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
            // do nothing, test
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            future.completeExceptionally(error);
        }
    }
}