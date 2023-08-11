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

package software.amazon.awssdk.services.s3.internal.crossregion;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class S3CrossRegionAsyncClientRedirectTest extends S3DecoratorRedirectTestBase {
    private static S3AsyncClient mockDelegateAsyncClient;
    private S3AsyncClient decoratedS3AsyncClient;

    @BeforeEach
    public void setup() {
        mockDelegateAsyncClient = Mockito.mock(S3AsyncClient.class);
        decoratedS3AsyncClient = new S3CrossRegionAsyncClient(mockDelegateAsyncClient);
    }

    @Override
    protected void stubRedirectSuccessSuccess(Integer redirect) {
        when(mockDelegateAsyncClient.listObjects(any(ListObjectsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(redirect, CROSS_REGION.id(),
                                                                                                      null, null))))
            .thenReturn(CompletableFuture.completedFuture(ListObjectsResponse.builder().contents(S3_OBJECTS).build()))
            .thenReturn(CompletableFuture.completedFuture(ListObjectsResponse.builder().contents(S3_OBJECTS).build()));
    }

    @Override
    protected ListObjectsResponse apiCallToService() throws Throwable {
        try{
           return decoratedS3AsyncClient.listObjects(i -> i.bucket(CROSS_REGION_BUCKET)).join();
        }catch (CompletionException exception){
            throw exception.getCause();
        }
    }

    @Override
    protected void verifyTheApiServiceCall(int times, ArgumentCaptor<ListObjectsRequest> requestArgumentCaptor) {
        verify(mockDelegateAsyncClient, times(times)).listObjects(requestArgumentCaptor.capture());
    }

    @Override
    protected void stubServiceClientConfiguration() {
        when(mockDelegateAsyncClient.serviceClientConfiguration()).thenReturn(CONFIGURED_ENDPOINT_PROVIDER);
    }

    @Override
    protected void stubClientAPICallWithFirstRedirectThenSuccessWithRegionInErrorResponse(Integer redirect) {
        when(mockDelegateAsyncClient.listObjects(any(ListObjectsRequest.class)))
           .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(redirect, CROSS_REGION.id(), null,
                                                                                                     null))))
            .thenReturn(CompletableFuture.completedFuture(ListObjectsResponse.builder().contents(S3_OBJECTS).build()
            ));
    }

    @Override
    protected void verifyNoBucketApiCall(int times, ArgumentCaptor<ListBucketsRequest> requestArgumentCaptor) {
        verify(mockDelegateAsyncClient, times(times)).listBuckets(requestArgumentCaptor.capture());
    }

    @Override
    protected ListBucketsResponse noBucketCallToService() throws Throwable {
        return decoratedS3AsyncClient.listBuckets(ListBucketsRequest.builder().build()).join();
    }

    @Override
    protected void stubApiWithNoBucketField() {
        when(mockDelegateAsyncClient.listBuckets(any(ListBucketsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(301, CROSS_REGION.id(), null,
                                                                                                      "Redirect"))))
            .thenReturn(CompletableFuture.completedFuture(ListBucketsResponse.builder().build()
            ));
    }

    @Override
    protected void stubHeadBucketRedirect() {
        when(mockDelegateAsyncClient.headBucket(any(HeadBucketRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(301,CROSS_REGION.id(), null, null))));
        when(mockDelegateAsyncClient.headBucket(any(Consumer.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(301,CROSS_REGION.id(), null, null))));
    }

    @Override
    protected void stubRedirectWithNoRegionAndThenSuccess(Integer redirect) {
        when(mockDelegateAsyncClient.listObjects(any(ListObjectsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(redirect, null, null, null))))
            .thenReturn(CompletableFuture.completedFuture(ListObjectsResponse.builder().contents(S3_OBJECTS).build()))
            .thenReturn(CompletableFuture.completedFuture(ListObjectsResponse.builder().contents(S3_OBJECTS).build()));
    }

    @Override
    protected void stubRedirectThenError(Integer redirect) {
        when(mockDelegateAsyncClient.listObjects(any(ListObjectsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(redirect, CROSS_REGION.id(), null,
                                                                                                      null))))
            .thenReturn(CompletableFutureUtils.failedFuture(new CompletionException(redirectException(400, null,
                                                                                                       "InvalidArgument", "Invalid id"))));
    }

    @Override
    protected void verifyHeadBucketServiceCall(int times) {
        verify(mockDelegateAsyncClient, times(times)).headBucket(any(Consumer.class));
    }

    @Override
    protected void verifyNoBucketCall() {
        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(
                () -> noBucketCallToService())

            .withCauseInstanceOf(S3Exception.class)
            .withMessage("software.amazon.awssdk.services.s3.model.S3Exception: Redirect (Service: S3, Status Code: 301, Request ID: 1, Extended Request ID: A1)");
    }
}
