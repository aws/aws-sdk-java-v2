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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class S3BucketEndpointResolverTest {

    S3BucketEndpointResolver endpointResolver;

    @Before
    public void setUp() throws Exception {
        endpointResolver = S3BucketEndpointResolver.create();
    }

    @Test
    public void traditionalEndpoint_shouldNotConvertEndpoint() {
        verifyEndpoint("http", "http://s3-test.com", S3Configuration.builder());
        verifyEndpoint("https", "https://s3-test.com", S3Configuration.builder());
    }

    @Test
    public void accelerateEnabled_shouldConvertToAccelerateEndpoint() {
        verifyEndpoint("http",
                       "http://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
        verifyEndpoint("https", "https://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
    }

    @Test
    public void bothAccelerateDualstackEnabled_shouldConvertToAccelerateDualstackEndpoint() {
        verifyEndpoint("http",
                       "http://s3-accelerate.dualstack.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true).dualstackEnabled(true)
        );
        verifyEndpoint("https",
                       "https://s3-accelerate.dualstack.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true).dualstackEnabled(true));
    }

    @Test
    public void pathStyleAccessEnabled_shouldNotConvertToDnsEndpoint() {
        verifyEndpoint("http",
                       "http://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
        verifyEndpoint("https",
                       "https://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
    }

    @Test
    public void dualstackEnabled_shouldConvertToDualstackEndpoint() {
        verifyEndpoint("http", "http://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
        verifyEndpoint("https", "https://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
    }

    @Test
    public void accelerateEnabled_ListBucketRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(ListBucketsRequest.builder().build());
    }

    @Test
    public void accelerateEnabled_CreateBucketsRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(CreateBucketRequest.builder().build());
    }

    @Test
    public void accelerateEnabled_DeleteBucketRequest_shouldNotConvertToAccelerateEndpoint() {
        verifyAccelerateDisabledOperationsEndpointNotConverted(DeleteBucketRequest.builder().build());
    }

    @Test
    public void virtualStyle_shouldConvertToDnsEndpoint() {
        verifyVirtualStyleConvertDnsEndpoint("https");
        verifyVirtualStyleConvertDnsEndpoint("http");
    }

    private void verifyVirtualStyleConvertDnsEndpoint(String protocol) {
        String bucketName = "test-bucket";
        String key = "test-key";
        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, bucketName, key));
        URI expectedUri = URI.create(String.format("%s://%s.s3.dualstack.us-east-1.amazonaws.com/%s", protocol,
                                                   bucketName, key));
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                                                                     .request(InterceptorTestUtils.sdkHttpRequest(customUri))
                                                                     .originalRequest(ListObjectsV2Request.builder().bucket(bucketName).build())
                                                                     .region(Region.US_EAST_1)
                                                                     .serviceConfiguration(S3Configuration.builder().dualstackEnabled(true).build())
                                                                     .build();
        ConfiguredS3SdkHttpRequest sdkHttpFullRequest = endpointResolver.applyEndpointConfiguration(context);

        assertThat(sdkHttpFullRequest.sdkHttpRequest().getUri()).isEqualTo(expectedUri);
    }

    private void verifyAccelerateDisabledOperationsEndpointNotConverted(SdkRequest request) {
        URI customUri = URI.create("http://s3-test.com");
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                                                                     .request(InterceptorTestUtils.sdkHttpRequest(customUri))
                                                                     .originalRequest(request)
                                                                     .region(Region.US_EAST_1)
                                                                     .serviceConfiguration(S3Configuration.builder().accelerateModeEnabled(true).build())
                                                                     .build();
        ConfiguredS3SdkHttpRequest sdkHttpFullRequest = endpointResolver.applyEndpointConfiguration(context);
        assertThat(sdkHttpFullRequest.sdkHttpRequest().getUri()).isEqualTo(customUri);
    }

    private void verifyEndpoint(String protocol,
                                String expectedEndpoint,
                                S3Configuration.Builder configBuilder) {
        String bucket = "test-bucket";
        String key = "test-key";
        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, bucket, key));
        URI expectedUri = URI.create(String.format("%s/%s/%s", expectedEndpoint, bucket, key));

        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                                                                     .request(InterceptorTestUtils.sdkHttpRequest(customUri))
                                                                     .originalRequest(PutObjectRequest.builder().build())
                                                                     .region(Region.US_EAST_1)
                                                                     .serviceConfiguration(configBuilder.build())
                                                                     .build();

        ConfiguredS3SdkHttpRequest sdkHttpFullRequest = endpointResolver.applyEndpointConfiguration(context);
        assertThat(sdkHttpFullRequest.sdkHttpRequest().getUri()).isEqualTo(expectedUri);
    }

}
