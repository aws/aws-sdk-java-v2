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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Unit tests for {@link ParallelMultipartDownloaderSubscriber} using a mocked {@link S3AsyncClient}, so each test controls
 * when each part request completes. For tests against a real client and HTTP server, see
 * {@link ParallelMultipartDownloaderSubscriberWiremockTest}.
 *
 * <p>Tests complete part 1 themselves, while later parts complete immediately. This makes the remaining parts finish
 * inside part 1's callback, before it returns.
 */
class ParallelMultipartDownloaderSubscriberTest {
    private static final String ETAG = "\"etag\"";

    private S3AsyncClient s3;
    private CompletableFuture<GetObjectResponse> firstPartFuture;
    private List<Integer> requestedParts;

    @BeforeEach
    void setUp() {
        s3 = mock(S3AsyncClient.class);
        firstPartFuture = new CompletableFuture<>();
        requestedParts = new ArrayList<>();
    }

    /**
     * Total parts, and max in-flight parts. When max in-flight is lower than total parts, the later parts' transformers are
     * delivered one at a time from earlier parts' callbacks, rather than all at subscribe time.
     */
    private static Stream<Arguments> multipartConfigurations() {
        return Stream.of(Arguments.of(2, 2),
                         Arguments.of(4, 4),
                         Arguments.of(4, 2));
    }

    @ParameterizedTest(name = "{0} parts, max {1} in flight")
    @MethodSource("multipartConfigurations")
    void remainingPartsFinishBeforeFirstPartCallbackReturns_completesWithFirstPartResponse(int totalParts,
                                                                                           int maxInFlightParts) {
        stubPartResponses(totalParts);
        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        ParallelMultipartDownloaderSubscriber subscriber =
            new ParallelMultipartDownloaderSubscriber(s3, GetObjectRequest.builder().bucket("b").key("k").build(),
                                                      resultFuture, maxInFlightParts);
        subscriber.onSubscribe(new SynchronousTransformerSubscription(subscriber));

        GetObjectResponse firstPartResponse = partResponse(1, totalParts);
        firstPartFuture.complete(firstPartResponse);

        assertThat(requestedParts).containsExactlyInAnyOrder(allParts(totalParts));
        assertThat(resultFuture).isCompletedWithValue(firstPartResponse);
    }

    @ParameterizedTest(name = "{0} parts, max {1} in flight")
    @MethodSource("multipartConfigurations")
    void remainingPartsFinishBeforeFirstPartCallbackReturns_resumeContextHasFirstPartWhenResultCompletes(int totalParts,
                                                                                                         int maxInFlightParts) {
        stubPartResponses(totalParts);
        MultipartDownloadResumeContext ctx = new MultipartDownloadResumeContext();
        GetObjectRequest request =
            GetObjectRequest.builder()
                            .bucket("b")
                            .key("k")
                            .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, ctx))
                            .build();
        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();

        // Snapshot the context at the moment the download completes, which is when a pause would read it.
        AtomicReference<List<Integer>> completedPartsAtCompletion = new AtomicReference<>();
        AtomicReference<GetObjectResponse> contextResponseAtCompletion = new AtomicReference<>();
        AtomicReference<Integer> contextTotalPartsAtCompletion = new AtomicReference<>();
        resultFuture.whenComplete((r, t) -> {
            completedPartsAtCompletion.set(new ArrayList<>(ctx.completedParts()));
            contextResponseAtCompletion.set(ctx.response());
            contextTotalPartsAtCompletion.set(ctx.totalParts());
        });

        ParallelMultipartDownloaderSubscriber subscriber =
            new ParallelMultipartDownloaderSubscriber(s3, request, resultFuture, maxInFlightParts);
        subscriber.onSubscribe(new SynchronousTransformerSubscription(subscriber));

        GetObjectResponse firstPartResponse = partResponse(1, totalParts);
        firstPartFuture.complete(firstPartResponse);

        assertThat(resultFuture).isCompletedWithValue(firstPartResponse);
        assertThat(completedPartsAtCompletion.get()).containsExactlyInAnyOrder(allParts(totalParts));
        assertThat(contextResponseAtCompletion.get()).isSameAs(firstPartResponse);
        assertThat(contextTotalPartsAtCompletion.get()).isEqualTo(totalParts);
    }

    @Test
    void singlePartObject_completesWithResponseAndDoesNotRecordResumeContext() {
        stubPartResponses(1);
        MultipartDownloadResumeContext ctx = new MultipartDownloadResumeContext();
        GetObjectRequest request =
            GetObjectRequest.builder()
                            .bucket("b")
                            .key("k")
                            .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, ctx))
                            .build();
        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        ParallelMultipartDownloaderSubscriber subscriber =
            new ParallelMultipartDownloaderSubscriber(s3, request, resultFuture, 4);
        subscriber.onSubscribe(new SynchronousTransformerSubscription(subscriber));

        GetObjectResponse singlePartResponse = GetObjectResponse.builder().eTag(ETAG).contentLength(10L).build();
        firstPartFuture.complete(singlePartResponse);

        assertThat(requestedParts).containsExactly(1);
        assertThat(resultFuture).isCompletedWithValue(singlePartResponse);
        assertThat(ctx.completedParts()).isEmpty();
        assertThat(ctx.response()).isNull();
        assertThat(ctx.totalParts()).isNull();
    }

    /**
     * Part 1's request returns {@code firstPartFuture}, which the test completes. Every later part completes immediately.
     */
    private void stubPartResponses(int totalParts) {
        when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class))).thenAnswer(invocation -> {
            GetObjectRequest request = invocation.getArgument(0);
            int part = request.partNumber();
            requestedParts.add(part);
            if (part == 1) {
                return firstPartFuture;
            }
            return CompletableFuture.completedFuture(partResponse(part, totalParts));
        });
    }

    private static GetObjectResponse partResponse(int part, int totalParts) {
        return GetObjectResponse.builder()
                                .partsCount(totalParts)
                                .eTag(ETAG)
                                .contentLength(10L)
                                .contentRange("bytes " + (part - 1) * 10 + "-" + (part * 10 - 1) + "/" + totalParts * 10)
                                .build();
    }

    private static Integer[] allParts(int totalParts) {
        return IntStream.rangeClosed(1, totalParts).boxed().toArray(Integer[]::new);
    }

    /**
     * Delivers one transformer per unit of demand, synchronously on the requesting thread.
     */
    private static final class SynchronousTransformerSubscription implements Subscription {
        private final ParallelMultipartDownloaderSubscriber subscriber;
        private boolean cancelled;

        SynchronousTransformerSubscription(ParallelMultipartDownloaderSubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void request(long n) {
            for (long i = 0; i < n && !cancelled; i++) {
                subscriber.onNext(mock(AsyncResponseTransformer.class));
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
