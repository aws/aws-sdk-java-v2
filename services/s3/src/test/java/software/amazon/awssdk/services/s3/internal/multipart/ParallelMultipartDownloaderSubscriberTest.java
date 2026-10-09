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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Unit tests for {@link ParallelMultipartDownloaderSubscriber}.
 */
class ParallelMultipartDownloaderSubscriberTest {

    @Test
    void onError_withInFlightRequests_shouldCompleteResultFutureWithOriginalCause() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        CompletableFuture<GetObjectResponse> resultFuture = new CompletableFuture<>();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("test-bucket")
                                                   .key("test-key")
                                                   .build();
        ParallelMultipartDownloaderSubscriber subscriber =
            new ParallelMultipartDownloaderSubscriber(s3, request, resultFuture, 10);

        CompletableFuture<GetObjectResponse> firstPartFuture = new CompletableFuture<>();
        CompletableFuture<GetObjectResponse> secondPartFuture = new CompletableFuture<>();
        when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(firstPartFuture, secondPartFuture);

        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(mock(AsyncResponseTransformer.class));
        firstPartFuture.complete(GetObjectResponse.builder()
                                                  .partsCount(3)
                                                  .eTag("etag")
                                                  .build());
        // Second part is now in flight and never completes on its own.
        subscriber.onNext(mock(AsyncResponseTransformer.class));

        RuntimeException cause = new RuntimeException("original failure");
        subscriber.onError(cause);

        // The caller-facing future must carry the original cause, not a CancellationException
        // raced onto it by the in-flight part cancellation.
        Throwable thrown = resultFuture.handle((r, t) -> t).join();
        assertThat(thrown).isNotInstanceOf(CancellationException.class);
        assertThat(thrown).isSameAs(cause);
    }
}
