/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.awscore.AwsExecutionAttribute.AWS_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;

import java.net.URI;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class EndpointAddressInterceptorTest {

    private final EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

    @Test
    public void traditionalEndpoint_shouldNotConvertEndpoint() {
        verifyEndpoint("http", "http://s3-test.com",
                       S3Configuration.builder());

        verifyEndpoint("https", "https://s3-test.com",
                       S3Configuration.builder());
    }

    @Test
    public void accelerateEnabled_shouldConvertToAccelerateEndpoint() {
        verifyEndpoint("http", "http://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
        verifyEndpoint("https", "https://s3-accelerate.amazonaws.com",
                       S3Configuration.builder().accelerateModeEnabled(true));
    }

    @Test
    public void bothAccelerateDualstackEnabled_shouldConvertToAccelerateDualstackEndpoint() {
        S3Configuration.Builder configurationBuilder = S3Configuration.builder()
                                                                      .dualstackEnabled(true)
                                                                      .accelerateModeEnabled(true);
        verifyEndpoint("http",
                       "http://s3-accelerate.dualstack.amazonaws.com",
                       S3Configuration.builder()
                                                                    .accelerateModeEnabled(true)
                                                                    .dualstackEnabled(true)
        );
        verifyEndpoint("https",
                       "https://s3-accelerate.dualstack.amazonaws.com",
                       configurationBuilder);
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
    public void dualstackEnabled_shouldConvertToDualstackEndpoint() {
        verifyEndpoint("http", "http://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
        verifyEndpoint("https", "https://s3.dualstack.us-east-1.amazonaws.com",
                       S3Configuration.builder().dualstackEnabled(true));
    }

    @Test
    public void virtualStyle_shouldConvertToDnsEndpoint() {
        verifyVirtualStyleConvertDnsEndpoint("https");
        verifyVirtualStyleConvertDnsEndpoint("http");
    }

    @Test
    public void pathStyleAccessEnabled_shouldNotConvertToDnsEndpoint() {
        verifyEndpoint("http", "http://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
        verifyEndpoint("https", "https://s3-test.com",
                       S3Configuration.builder().pathStyleAccessEnabled(true));
    }

    private void verifyVirtualStyleConvertDnsEndpoint(String protocol) {
        URI customUri = URI.create(String.format("%s://s3-test.com", protocol));
        String bucketName = "some-bucket";
        URI expectedUri = URI.create(String.format("%s://%s.s3.dualstack.us-east-1.amazonaws.com", protocol, bucketName));


        Context.ModifyHttpRequest ctx = context(ListObjectsV2Request.builder().bucket(bucketName).build(),
                                                sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = S3Configuration.builder().dualstackEnabled(true).build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(expectedUri);
    }

    private SdkHttpRequest sdkHttpRequest(URI customUri) {
        return SdkHttpFullRequest.builder()
                                 .protocol(customUri.getScheme())
                                 .host(customUri.getHost())
                                 .port(customUri.getPort())
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }

    private void verifyAccelerateDisabledOperationsEndpointNotConverted(SdkRequest request) {
        URI customUri = URI.create("http://s3-test.com");
        Context.ModifyHttpRequest ctx = context(request, sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = S3Configuration.builder().accelerateModeEnabled(true).build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(customUri);
    }

    private void verifyEndpoint(String protocol, String expectedEndpoint,
                                S3Configuration.Builder builder) {
        URI customUri = URI.create(String.format("%s://s3-test.com", protocol));
        URI expectedUri = URI.create(expectedEndpoint);
        Context.ModifyHttpRequest ctx = context(PutObjectRequest.builder().build(), sdkHttpRequest(customUri));
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        S3Configuration s3Configuration = builder.build();

        executionAttributes.putAttribute(SERVICE_CONFIG, s3Configuration);
        executionAttributes.putAttribute(AWS_REGION, Region.US_EAST_1);

        SdkHttpRequest sdkHttpFullRequest = interceptor.modifyHttpRequest(ctx, executionAttributes);

        assertThat(sdkHttpFullRequest.getUri()).isEqualTo(expectedUri);
    }

    private Context.ModifyHttpRequest context(SdkRequest request, SdkHttpRequest sdkHttpRequest) {
        return new Context.ModifyHttpRequest() {
            @Override
            public SdkHttpRequest httpRequest() {
                return sdkHttpRequest;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return null;
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return request;
            }
        };
    }
}
