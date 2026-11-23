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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.utils.ControlledSubscription;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Timeout(10)
class UnknownContentLengthAsyncRequestBodySubscriberLifecycleTest {
    private static final long PART_SIZE = 1024;
    private static final String UPLOAD_ID = "upload-id";
    private static final RuntimeException BODY_ERROR = new RuntimeException("body failed");
    private static final Consumer<Fixture> NO_EXTRA_CHECK = f -> { };
    private static final Consumer<Fixture> PART_1_CANCELLED = f -> assertThat(f.parts.get(1)).isCancelled();

    enum FailurePoint {
        CMPU_IN_FLIGHT(f -> {
            f.deliverParts(2);
            f.subscriber.onError(BODY_ERROR);
            f.completeCreateMultipartUpload();
            f.completeSentParts();
        }),
        BEFORE_PARTS_REGISTERED(f -> {
            f.deliverParts(2);
            f.onUploadPart = partNumber -> {
                if (partNumber == 1) {
                    f.subscriber.onError(BODY_ERROR);
                }
            };
            f.completeCreateMultipartUpload();
            f.completeSentParts();
        }, PART_1_CANCELLED),
        WHILE_PART_RESPONSE_ARRIVES(f -> {
            f.deliverParts(2);
            f.completeCreateMultipartUpload();
            f.onReturnFutureFailing = f::completeSentParts;
            f.subscriber.onError(BODY_ERROR);
        }),
        PARTS_IN_FLIGHT(f -> {
            f.deliverParts(2);
            f.completeCreateMultipartUpload();
            f.subscriber.onError(BODY_ERROR);
            f.completeSentParts();
        }),
        PART_FAILS(f -> {
            f.deliverParts(2);
            f.completeCreateMultipartUpload();
            f.parts.get(2).completeExceptionally(S3Exception.builder().message("part failed").build());
        });

        private final Consumer<Fixture> scenario;
        private final Consumer<Fixture> extraCheck;

        FailurePoint(Consumer<Fixture> scenario) {
            this(scenario, NO_EXTRA_CHECK);
        }

        FailurePoint(Consumer<Fixture> scenario, Consumer<Fixture> extraCheck) {
            this.scenario = scenario;
            this.extraCheck = extraCheck;
        }
    }

    enum CancelPoint {
        CMPU_IN_FLIGHT(f -> {
            f.deliverParts(2);
            f.returnFuture.cancel(true);
            f.completeCreateMultipartUpload();
        }),
        BETWEEN_CMPU_RESPONSE_AND_PARTS(f -> {
            f.deliverParts(2);
            f.onUploadPart = partNumber -> {
                if (partNumber == 1) {
                    f.returnFuture.cancel(true);
                }
            };
            f.completeCreateMultipartUpload();
            f.completeSentParts();
        }, PART_1_CANCELLED),
        PART_IN_FLIGHT(f -> {
            f.deliverParts(2);
            f.completeCreateMultipartUpload();
            f.completePart(1);
            f.returnFuture.cancel(true);
        }),
        ALL_PARTS_ANSWERED(f -> {
            f.deliverParts(2);
            f.completeCreateMultipartUpload();
            f.completeSentParts();
            f.returnFuture.cancel(true);
        });

        private final Consumer<Fixture> scenario;
        private final Consumer<Fixture> extraCheck;

        CancelPoint(Consumer<Fixture> scenario) {
            this(scenario, NO_EXTRA_CHECK);
        }

        CancelPoint(Consumer<Fixture> scenario, Consumer<Fixture> extraCheck) {
            this.scenario = scenario;
            this.extraCheck = extraCheck;
        }
    }

    enum CallerFailure {
        CANCEL(future -> future.cancel(true)),
        TIMEOUT(future -> future.completeExceptionally(new TimeoutException("caller timed out")));

        private final Consumer<CompletableFuture<PutObjectResponse>> failure;

        CallerFailure(Consumer<CompletableFuture<PutObjectResponse>> failure) {
            this.failure = failure;
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(FailurePoint.class)
    void onError_whenMultipartUploadStarted_shouldAbortOnceAndNeverComplete(FailurePoint failurePoint) {
        Fixture f = new Fixture();
        failurePoint.scenario.accept(f);
        assertAbortedOnceAndNeverCompleted(f);
        failurePoint.extraCheck.accept(f);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CancelPoint.class)
    void cancel_whenMultipartUploadStarted_shouldAbortOnceAndNeverComplete(CancelPoint cancelPoint) {
        Fixture f = new Fixture();
        cancelPoint.scenario.accept(f);
        assertAbortedOnceAndNeverCompleted(f);
        assertThat(f.subscription.isCancelled()).isTrue();
        cancelPoint.extraCheck.accept(f);
    }

    @Test
    void cancel_whenOnlyFirstPartBuffered_shouldNotCallMultipartApis() {
        Fixture f = new Fixture();
        f.deliverParts(1);
        f.returnFuture.cancel(true);

        verify(f.s3, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        verify(f.s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        verify(f.s3, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void cancel_whenCreateMultipartUploadFailsAfterCancel_shouldNotAbortAndShouldCloseBodies() {
        Fixture f = new Fixture();
        f.deliverParts(2);
        f.returnFuture.cancel(true);
        assertThat(f.createMultipartUploadFuture).isNotDone();

        f.createMultipartUploadFuture.completeExceptionally(
            S3Exception.builder().message("CreateMultipartUpload failed").build());

        assertThat(f.returnFuture).isCancelled();
        verify(f.s3, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        verify(f.s3, never()).uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class));
        f.bodies.forEach(body -> verify(body).close());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CallerFailure.class)
    void failReturnFuture_whenCompleteMultipartUploadInFlight_shouldAbort(CallerFailure callerFailure) {
        Fixture f = new Fixture();
        when(f.s3.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(new CompletableFuture<>());
        f.deliverParts(2);
        f.completeCreateMultipartUpload();
        f.completeSentParts();
        f.subscriber.onComplete();

        callerFailure.failure.accept(f.returnFuture);

        verify(f.s3, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        ArgumentCaptor<AbortMultipartUploadRequest> abort = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(f.s3, times(1)).abortMultipartUpload(abort.capture());
        assertThat(abort.getValue().uploadId()).isEqualTo(UPLOAD_ID);
    }

    @Test
    void onComplete_whenCompleteMultipartUploadFails_shouldAbortOnce() {
        Fixture f = new Fixture();
        CompletableFuture<CompleteMultipartUploadResponse> complete = new CompletableFuture<>();
        when(f.s3.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(complete);
        f.deliverParts(2);
        f.completeCreateMultipartUpload();
        f.completeSentParts();
        f.subscriber.onComplete();

        complete.completeExceptionally(S3Exception.builder().message("CompleteMultipartUpload failed").build());

        assertThat(f.returnFuture).isCompletedExceptionally();
        ArgumentCaptor<AbortMultipartUploadRequest> abort = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(f.s3, times(1)).abortMultipartUpload(abort.capture());
        assertThat(abort.getValue().uploadId()).isEqualTo(UPLOAD_ID);
    }

    @Test
    void onComplete_whenCompleteMultipartUploadAlreadyFailed_shouldAbortOnce() {
        Fixture f = new Fixture();
        CompletableFuture<CompleteMultipartUploadResponse> failed = new CompletableFuture<>();
        failed.completeExceptionally(S3Exception.builder().message("CompleteMultipartUpload failed").build());
        when(f.s3.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(failed);
        f.deliverParts(2);
        f.completeCreateMultipartUpload();
        f.completeSentParts();

        f.subscriber.onComplete();

        assertThat(f.returnFuture).isCompletedExceptionally();
        ArgumentCaptor<AbortMultipartUploadRequest> abort = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(f.s3, times(1)).abortMultipartUpload(abort.capture());
        assertThat(abort.getValue().uploadId()).isEqualTo(UPLOAD_ID);
    }

    @Test
    void onComplete_whenSinglePartUploadFails_shouldLeaveExceptionUnchanged() {
        Fixture f = new Fixture();
        CompletableFuture<PutObjectResponse> putObject = new CompletableFuture<>();
        when(f.s3.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class))).thenReturn(putObject);
        S3Exception accessDenied = (S3Exception) S3Exception.builder().message("Access Denied").build();

        f.deliverParts(1);
        f.subscriber.onComplete();
        putObject.completeExceptionally(accessDenied);

        assertThat(f.returnFuture).isCompletedExceptionally();
        assertThat(accessDenied.getSuppressed()).isEmpty();
    }

    private static void assertAbortedOnceAndNeverCompleted(Fixture f) {
        assertThat(f.returnFuture).isCompletedExceptionally();
        verify(f.s3, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        ArgumentCaptor<AbortMultipartUploadRequest> abort = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(f.s3, times(1)).abortMultipartUpload(abort.capture());
        assertThat(abort.getValue().uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(f.partsStartedAfterFailure).hasValue(0);
        f.bodies.forEach(body -> verify(body).close());
    }

    private static final class Fixture {
        private final S3AsyncClient s3 = mock(S3AsyncClient.class);
        private final CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            new CompletableFuture<>();
        private final Map<Integer, CompletableFuture<UploadPartResponse>> parts = new ConcurrentHashMap<>();
        private final List<CloseableAsyncRequestBody> bodies = new ArrayList<>();
        private final AtomicInteger partsStartedAfterFailure = new AtomicInteger();
        private IntConsumer onUploadPart = partNumber -> { };
        private Runnable onReturnFutureFailing = () -> { };

        private final CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<PutObjectResponse>() {
            @Override
            public boolean completeExceptionally(Throwable ex) {
                onReturnFutureFailing.run();
                return super.completeExceptionally(ex);
            }
        };
        private final UnknownContentLengthAsyncRequestBodySubscriber subscriber;
        private final ControlledSubscription subscription;

        Fixture() {
            when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(createMultipartUploadFuture);
            when(s3.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class))).thenAnswer(invocation -> {
                if (returnFuture.isDone()) {
                    partsStartedAfterFailure.incrementAndGet();
                }
                int partNumber = invocation.getArgument(0, UploadPartRequest.class).partNumber();
                CompletableFuture<UploadPartResponse> part = new CompletableFuture<>();
                parts.put(partNumber, part);
                onUploadPart.accept(partNumber);
                return part;
            });
            when(s3.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(completedFuture(AbortMultipartUploadResponse.builder().build()));
            when(s3.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(completedFuture(CompleteMultipartUploadResponse.builder().build()));

            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("bucket").key("key").build();
            subscriber = new UnknownContentLengthAsyncRequestBodySubscriber(
                PART_SIZE, putObjectRequest, returnFuture,
                new MultipartUploadHelper(s3, PART_SIZE, Long.MAX_VALUE),
                new GenericMultipartHelper<>(s3, SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                             SdkPojoConversionUtils::toPutObjectResponse),
                4);
            subscription = new ControlledSubscription(subscriber);
            subscriber.onSubscribe(subscription);
        }

        void deliverParts(int count) {
            for (int i = 0; i < count; i++) {
                CloseableAsyncRequestBody body = mock(CloseableAsyncRequestBody.class);
                when(body.contentLength()).thenReturn(Optional.of(PART_SIZE));
                bodies.add(body);
                subscription.enqueueBodyAndDeliver(body);
            }
        }

        void completeCreateMultipartUpload() {
            createMultipartUploadFuture.complete(CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build());
        }

        void completePart(int partNumber) {
            parts.get(partNumber).complete(UploadPartResponse.builder().eTag("etag-" + partNumber).build());
        }

        void completeSentParts() {
            parts.keySet().forEach(this::completePart);
        }
    }
}
