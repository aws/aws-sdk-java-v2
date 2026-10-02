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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class ParallelMultipartDownloaderSubscriberTckTest
    extends SubscriberWhiteboxVerification<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private S3AsyncClient s3Client;
    private CompletableFuture<GetObjectResponse> future;

    public ParallelMultipartDownloaderSubscriberTckTest() {
        super(new TestEnvironment());
        s3Client = mock(S3AsyncClient.class);
        when(s3Client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(GetObjectResponse.builder()
                                                                           .partsCount(10)
                                                                           .eTag("eTag")
                                                                           .build()));
        future = new CompletableFuture<>();
    }

    @Override
    public Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> createSubscriber(
        WhiteboxSubscriberProbe<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> probe) {
        return new ParallelMultipartDownloaderSubscriber(s3Client, GetObjectRequest.builder().build(), future, 50) {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberWhiteboxVerification.SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long l) {
                        s.request(l);
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
                // do nothing, test
            }

            @Override
            public void onStream(SdkPublisher<ByteBuffer> publisher) {
                // do nothing, test
            }

            @Override
            public void exceptionOccurred(Throwable error) {
                // do nothing, test
            }
        };
    }
}