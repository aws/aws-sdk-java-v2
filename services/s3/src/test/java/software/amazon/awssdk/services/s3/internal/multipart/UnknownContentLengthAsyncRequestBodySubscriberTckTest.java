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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class UnknownContentLengthAsyncRequestBodySubscriberTckTest
    extends SubscriberWhiteboxVerification<CloseableAsyncRequestBody> {
    private static final long PART_SIZE = 1024;

    private final MultipartUploadHelper multipartUploadHelper;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    @SuppressWarnings("unchecked")
    public UnknownContentLengthAsyncRequestBodySubscriberTckTest() {
        super(new TestEnvironment());
        multipartUploadHelper = mock(MultipartUploadHelper.class);
        genericMultipartHelper = mock(GenericMultipartHelper.class);
        when(multipartUploadHelper.createMultipartUpload(any()))
            .thenAnswer(i -> CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder()
                                                                                            .uploadId("uploadId")
                                                                                            .build()));
        when(multipartUploadHelper.sendIndividualUploadPartRequest(any(), any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(CompletedPart.builder().build()));
    }

    @Override
    public Subscriber<CloseableAsyncRequestBody> createSubscriber(WhiteboxSubscriberProbe<CloseableAsyncRequestBody> probe) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("bucket").key("key").build();

        return new UnknownContentLengthAsyncRequestBodySubscriber(PART_SIZE, putObjectRequest, new CompletableFuture<>(),
                                                                  multipartUploadHelper, genericMultipartHelper, 50) {
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
            public void onNext(CloseableAsyncRequestBody item) {
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
    public CloseableAsyncRequestBody createElement(int element) {
        return new CloseableAsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(PART_SIZE);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
            }

            @Override
            public void close() {
            }
        };
    }
}
