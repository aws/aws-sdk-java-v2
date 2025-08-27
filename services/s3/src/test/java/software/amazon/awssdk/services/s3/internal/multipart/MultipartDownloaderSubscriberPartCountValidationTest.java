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


import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class MultipartDownloaderSubscriberPartCountValidationTest {
    @Mock
    private S3AsyncClient s3Client;

    @Mock
    private Subscription subscription;

    @Mock
    private AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer;

    private GetObjectRequest getObjectRequest;
    private MultipartDownloaderSubscriber subscriber;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        getObjectRequest = GetObjectRequest.builder()
                                           .bucket("test-bucket")
                                           .key("test-key")
                                           .build();
    }

    @Test
    void callCountMatchesTotalParts_shouldPass() throws InterruptedException {
        subscriber = new MultipartDownloaderSubscriber(s3Client, getObjectRequest);
        GetObjectResponse response1 = createMockResponse(3, "etag1");
        GetObjectResponse response2 = createMockResponse(3, "etag2");
        GetObjectResponse response3 = createMockResponse(3, "etag3");

        CompletableFuture<GetObjectResponse> future1 = CompletableFuture.completedFuture(response1);
        CompletableFuture<GetObjectResponse> future2 = CompletableFuture.completedFuture(response2);
        CompletableFuture<GetObjectResponse> future3 = CompletableFuture.completedFuture(response3);

        when(s3Client.getObject(any(GetObjectRequest.class), eq(responseTransformer)))
            .thenReturn(future1, future2, future3);

        subscriber.onSubscribe(subscription);
        subscriber.onNext(responseTransformer);
        subscriber.onNext(responseTransformer);
        subscriber.onNext(responseTransformer);
        Thread.sleep(100);

        subscriber.onComplete();

        assertDoesNotThrow(() -> subscriber.future().get(1, TimeUnit.SECONDS));
    }

    @Test
    void callCountLessThanTotalParts_shouldThrowException() throws InterruptedException {
        subscriber = new MultipartDownloaderSubscriber(s3Client, getObjectRequest);
        GetObjectResponse response1 = createMockResponse(3, "etag1");
        GetObjectResponse response2 = createMockResponse(3, "etag2");

        CompletableFuture<GetObjectResponse> future1 = CompletableFuture.completedFuture(response1);
        CompletableFuture<GetObjectResponse> future2 = CompletableFuture.completedFuture(response2);

        when(s3Client.getObject(any(GetObjectRequest.class), eq(responseTransformer)))
            .thenReturn(future1, future2);

        subscriber.onSubscribe(subscription);
        subscriber.onNext(responseTransformer);
        subscriber.onNext(responseTransformer);
        Thread.sleep(100);

        subscriber.onComplete();

        ExecutionException exception = assertThrows(ExecutionException.class,
                                                    () -> subscriber.future().get(1, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof SdkClientException);
        assertTrue(exception.getCause().getMessage().contains("PartsCount validation failed"));
        assertTrue(exception.getCause().getMessage().contains("Expected 3, downloaded 2 parts"));

    }

    private GetObjectResponse createMockResponse(int partsCount, String etag) {
        GetObjectResponse.Builder builder = GetObjectResponse.builder()
                                                             .eTag(etag)
                                                             .contentLength(1024L);

        builder.partsCount(partsCount);
        return builder.build();
    }

}
