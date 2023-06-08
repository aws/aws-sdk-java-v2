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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3CrossRegionSyncClientRedirectTest {


    public static final String CROSS_REGION_BUCKET = "anyBucket";
    public static final String X_AMZ_BUCKET_REGION = "x-amz-bucket-region";
    private static final String CROSS_REGION = "us-east-1";
    private static final String CHANGED_CROSS_REGION = "us-west-1";
    private static S3Client mockDelegateClient;
    private static final List<S3Object> S3_OBJECTS = Collections.singletonList(S3Object.builder().key("keyObject").build());
    private static final S3ServiceClientConfiguration ENDPOINT_CONFIGURED =
        S3ServiceClientConfiguration.builder().endpointProvider(S3EndpointProvider.defaultProvider()).build();
    private static final AwsServiceException SERVICE_EXCEPTION =
        S3Exception.builder()
                   .statusCode(400)
                   .requestId("1")
                   .extendedRequestId("A1")
                   .awsErrorDetails(
                       AwsErrorDetails.builder()
                                      .errorMessage("Invalid id")
                                      .serviceName("S3")
                                      .errorCode("InvalidArgument")
                                      .sdkHttpResponse(SdkHttpFullResponse.builder().appendHeader(X_AMZ_BUCKET_REGION,
                                                                                                  CROSS_REGION)
                                                                          .build()).build()).build();
    S3Client decoratedS3Client;


    @BeforeEach
    public void setup() {
        mockDelegateClient = Mockito.mock(S3Client.class);
        decoratedS3Client = new S3CrossRegionSyncClient(mockDelegateClient);
    }

    @Test
    void decoratorAttemptsToRetry_withRegionNameInErrorResponse() throws Exception {

        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());

        // Assert retrieved listObject
        ListObjectsResponse listObjectsResponse = decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
        assertThat(listObjectsResponse.contents()).isEqualTo(S3_OBJECTS);

        ArgumentCaptor<ListObjectsRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(mockDelegateClient, times(2)).listObjects(requestArgumentCaptor.capture());
        assertThat(requestArgumentCaptor.getAllValues().get(0).overrideConfiguration()).isNotPresent();

        EndpointProvider overridenEndpointProvider =
            requestArgumentCaptor.getAllValues().get(1).overrideConfiguration().get().endpointProvider().get();
        assertThat(overridenEndpointProvider).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overridenEndpointProvider)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CROSS_REGION + ".amazonaws.com");
    }

    @Test
    void decoratorUsesCache_IfCrossRegionAlreadyPresent() throws Exception {
        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build())
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());

        // first call to update the local cache
        ListObjectsResponse preCacheCall = decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
        assertThat(preCacheCall.contents()).isEqualTo(S3_OBJECTS);
        // Second call to use the local cache
        ListObjectsResponse postCacheCall = decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
        assertThat(postCacheCall.contents()).isEqualTo(S3_OBJECTS);

        ArgumentCaptor<ListObjectsRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(mockDelegateClient, times(3)).listObjects(requestArgumentCaptor.capture());
        assertThat(requestArgumentCaptor.getAllValues().get(0).overrideConfiguration()).isNotPresent();

        EndpointProvider overridenEndpointProviderPreCache =
            requestArgumentCaptor.getAllValues().get(1).overrideConfiguration().get().endpointProvider().get();
        assertThat(overridenEndpointProviderPreCache).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overridenEndpointProviderPreCache)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CROSS_REGION + ".amazonaws.com");

        EndpointProvider overriddenEndpointProviderPostCache =
            requestArgumentCaptor.getAllValues().get(1).overrideConfiguration().get().endpointProvider().get();
        assertThat(overriddenEndpointProviderPostCache).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overriddenEndpointProviderPostCache)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CROSS_REGION + ".amazonaws.com");
        verify(mockDelegateClient, times(0)).headBucket(any(HeadBucketRequest.class));

    }

    /**
     * Call is redirected to actual end point
     * The redirected call fails because of incorrect parameters passed
     * This exception should be reported correctly
     */
    @Test
    void apiCallFailure_afterRedirect_isPropagated() {

        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);

        when(mockDelegateClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(redirectException(301, CROSS_REGION))
            .thenThrow(SERVICE_EXCEPTION);
        assertThatExceptionOfType(S3Exception.class).isThrownBy(() -> decoratedS3Client.putObject(p -> p.key("Key").bucket(CROSS_REGION_BUCKET), RequestBody.empty()))
                                                    .withMessage("Invalid id (Service: S3, Status Code: 400, Request ID: 1, "
                                                                 + "Extended Request ID: A1)");
    }

    @Test
    void headBucketCallMade_WhenRedirectMessageDoesNotHaveBucketRegion() throws Exception {

        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);
        when(mockDelegateClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(redirectException(301, null))
            .thenReturn(PutObjectResponse.builder().eTag("ABCDEFGH").build());

        when(mockDelegateClient.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION));

        PutObjectResponse putObjectResponse = decoratedS3Client.putObject(p -> p.key("Key").bucket(CROSS_REGION_BUCKET),
                                                                          RequestBody.empty());
        assertThat(putObjectResponse.eTag()).isEqualTo("ABCDEFGH");
        ArgumentCaptor<PutObjectRequest> requestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockDelegateClient, times(2)).putObject(requestArgumentCaptor.capture(), any(RequestBody.class));

        assertThat(requestArgumentCaptor.getAllValues().get(0).overrideConfiguration()).isNotPresent();
        EndpointProvider overriddenEndpointProviderPreCache =
            requestArgumentCaptor.getAllValues().get(1).overrideConfiguration().get().endpointProvider().get();
        assertThat(overriddenEndpointProviderPreCache).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overriddenEndpointProviderPreCache)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CROSS_REGION + ".amazonaws.com");
    }

    @Test
    void requestsWithNoBucketsDoesnotOverrideEndpoints() {
        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);

        when(mockDelegateClient.listBuckets(any(ListBucketsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION));

        assertThatExceptionOfType(S3Exception.class)
            .isThrownBy(() -> decoratedS3Client.listBuckets(ListBucketsRequest.builder().build()))
            .withMessageContaining("Status Code: 301");

        ArgumentCaptor<ListBucketsRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ListBucketsRequest.class);
        verify(mockDelegateClient, times(1)).listBuckets(requestArgumentCaptor.capture());
        assertThat(requestArgumentCaptor.getAllValues().get(0).overrideConfiguration()).isNotPresent();
    }

    @Test
    void bucketRegionChanged_updatesCache_AndDoesNotError() throws ExecutionException, InterruptedException {
        when(mockDelegateClient.serviceClientConfiguration()).thenReturn(ENDPOINT_CONFIGURED);
        when(mockDelegateClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(redirectException(301, CROSS_REGION))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build())
            .thenThrow(redirectException(301, CHANGED_CROSS_REGION))
            .thenReturn(ListObjectsResponse.builder().contents(S3_OBJECTS).build());

        // first call to update the local cache
        ListObjectsResponse preCacheCall = decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
        assertThat(preCacheCall.contents()).isEqualTo(S3_OBJECTS);

        ArgumentCaptor<ListObjectsRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(mockDelegateClient, times(2)).listObjects(requestArgumentCaptor.capture());
        // Verification for first call
        assertThat(requestArgumentCaptor.getAllValues().get(0).overrideConfiguration()).isNotPresent();

        // Verification of fall back for  first call
        EndpointProvider overriddenEndpointProviderPreCache =
            requestArgumentCaptor.getAllValues().get(1).overrideConfiguration().get().endpointProvider().get();
        assertThat(overriddenEndpointProviderPreCache).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overriddenEndpointProviderPreCache)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CROSS_REGION + ".amazonaws.com");
        // Second call to use the local cache
        ListObjectsResponse afterRegionUpdate = decoratedS3Client.listObjects(i -> i.bucket(CROSS_REGION_BUCKET));
        assertThat(afterRegionUpdate.contents()).isEqualTo(S3_OBJECTS);
        requestArgumentCaptor = ArgumentCaptor.forClass(ListObjectsRequest.class);
        verify(mockDelegateClient, times(4)).listObjects(requestArgumentCaptor.capture());
        // Called from Cached region
        EndpointProvider overridenEndpointProviderPostCache =
            requestArgumentCaptor.getAllValues().get(2).overrideConfiguration().get().endpointProvider().get();
        assertThat(overridenEndpointProviderPostCache).isInstanceOf(BucketEndpointProvider.class);
        EndpointProvider overriddenEndpointProviderPostRegionUpdate =
            requestArgumentCaptor.getAllValues().get(3).overrideConfiguration().get().endpointProvider().get();
        assertThat(overriddenEndpointProviderPostRegionUpdate).isInstanceOf(BucketEndpointProvider.class);

        assertThat(((S3EndpointProvider) overriddenEndpointProviderPostRegionUpdate)
                       .resolveEndpoint(e -> e.region(Region.US_WEST_2).bucket(CROSS_REGION_BUCKET).build()).get().url().getHost())
            .isEqualTo("s3." + CHANGED_CROSS_REGION + ".amazonaws.com");

        verify(mockDelegateClient, times(0)).headBucket(any(HeadBucketRequest.class));


    }

    private AwsServiceException redirectException(int statusCode, String region) {
        SdkHttpFullResponse.Builder sdkHttpFullResponseBuilder = SdkHttpFullResponse.builder();
        if (region != null) {
            sdkHttpFullResponseBuilder.appendHeader(X_AMZ_BUCKET_REGION, region);
        }
        return S3Exception.builder().
                          statusCode(statusCode)
                          .awsErrorDetails(
                              AwsErrorDetails.builder()
                                             .sdkHttpResponse(sdkHttpFullResponseBuilder.build())
                                             .build())
                          .build();
    }
}
