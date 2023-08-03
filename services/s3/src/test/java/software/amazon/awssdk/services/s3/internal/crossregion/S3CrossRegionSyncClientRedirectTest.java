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

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3CrossRegionSyncClientRedirectTest extends S3DecoratorRedirectTestBase {

    private static S3Client mockDelegateClient;
    private S3Client decoratedS3Client;

    @BeforeEach
    public void setup() {
        mockDelegateClient = Mockito.mock(S3Client.class);
        decoratedS3Client = new S3CrossRegionSyncClient(mockDelegateClient);
    }

    @Override
    protected void verifyNoBucketCall() {
        assertThatExceptionOfType(S3Exception.class)
            .isThrownBy(
                () -> noBucketCallToService())
            .withMessage("Redirect (Service: S3, Status Code: 301, Request ID: 1, "
                         + "Extended Request ID: A1)");
    }

    @Override
    protected void verifyNoBucketApiCall(int times, ArgumentCaptor<ListBucketsRequest> requestArgumentCaptor) {
        verify(mockDelegateClient, times(times)).listBuckets(requestArgumentCaptor.capture());
    }

    @Override
    protected ListBucketsResponse noBucketCallToService() {
        return decoratedS3Client.listBuckets(ListBucketsRequest.builder().build());
    }

    @Override
    protected void stubApiWithNoBucketField() {
        when(mockDelegateClient.listBuckets(any(ListBucketsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION.id(), null, "Redirect"))
            .thenReturn(ListBucketsResponse.builder().build());
    }

    @Override
    protected void stubHeadBucketRedirect() {
        when(mockDelegateClient.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION.id(), null, null))
            .thenReturn(HeadBucketResponse.builder().build());
    }

    @Override
    protected void stubRedirectWithNoRegionAndThenSuccess(Integer redirect) {
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(redirect, null, null, null))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());
    }

    @Override
    protected void stubRedirectThenError(Integer redirect) {
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(redirect, CROSS_REGION.id(), null, null))
            .thenThrow(redirectException(400, null, "InvalidArgument", "Invalid id"));
    }

    @Override
    protected void stubRedirectSuccessSuccess(Integer redirect) {
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(redirect, CROSS_REGION.id(), null, null))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build())
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());
    }

    @Override
    protected ListObjectsResponse apiCallToService() {
        return decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
    }

    @Override
    protected void verifyTheApiServiceCall(int times, ArgumentCaptor<ListObjectsRequest> requestArgumentCaptor) {
        verify(mockDelegateClient, times(times)).listObjects(requestArgumentCaptor.capture());
    }

    @Override
    protected void verifyHeadBucketServiceCall(int times) {
        verify(mockDelegateClient, times(times)).headBucket(any(HeadBucketRequest.class));
    }

    @Override
    protected void stubServiceClientConfiguration() {
        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(CONFIGURED_ENDPOINT_PROVIDER);
    }

    @Override
    protected void stubClientAPICallWithFirstRedirectThenSuccessWithRegionInErrorResponse(Integer redirect) {
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION.id(), null, null))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());
    }
}
