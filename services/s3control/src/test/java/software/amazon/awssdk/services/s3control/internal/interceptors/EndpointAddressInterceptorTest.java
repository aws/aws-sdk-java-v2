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
package software.amazon.awssdk.services.s3control.internal.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyZeroInteractions;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;

import java.net.URI;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.presigner.DefaultS3Presigner;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;
import software.amazon.awssdk.services.s3control.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest;

public class EndpointAddressInterceptorTest {

    private static final String X_AMZ_ACCOUNT_ID = "x-amz-account-id";
    private static final String ACCOUNT_ID = "123456789012";

    private SdkHttpRequest request;

    @Before
    public void setup() {
        request = SdkHttpFullRequest.builder()
                                    .appendHeader(X_AMZ_ACCOUNT_ID, ACCOUNT_ID)
                                    .protocol(Protocol.HTTPS.toString())
                                    .method(SdkHttpMethod.POST)
                                    .host(S3ControlClient.serviceMetadata().endpointFor(Region.US_EAST_1).toString())
                                    .build();
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_StandardSettings() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), new ExecutionAttributes());
        assertThat(modified.host()).isEqualTo("s3-control.us-east-1.amazonaws.com");
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_Dualstack() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().dualstackEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), executionAttributes);
        assertThat(modified.host()).isEqualTo("s3-control.dualstack.us-east-1.amazonaws.com");
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_Fips() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().fipsModeEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), executionAttributes);
        assertThat(modified.host()).isEqualTo("s3-control-fips.us-east-1.amazonaws.com");
    }

    @Test
    public void createBucketRequestWithOutpostId_shouldRedirect() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().outpostId("1234").build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request).request(createBucketRequest),
                                                                executionAttributes);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3-outposts");
        assertThat(modified.host()).isEqualTo("s3-outposts.us-east-1.amazonaws.com");
    }

    @Test
    public void listRegionalBucketsRequestsWithOutpostId_shouldRedirect() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        ListRegionalBucketsRequest sdkRequest = ListRegionalBucketsRequest.builder().outpostId("1234").build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3");

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request).request(sdkRequest),
                                                                executionAttributes);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3-outposts");
        assertThat(modified.host()).isEqualTo("s3-outposts.us-east-1.amazonaws.com");
    }

    @Test
    public void listRegionalBucketsRequestsWithoutOutpostId_shouldNotRedirect() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        ListRegionalBucketsRequest sdkRequest = ListRegionalBucketsRequest.builder().build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder()
                                                                            .dualstackEnabled(true)
                                                                            .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3");

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request).request(sdkRequest),
                                                                executionAttributes);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3");
        assertThat(modified.host()).isEqualTo("s3-control.dualstack.us-east-1.amazonaws.com");
    }

    @Test
    public void createBucketRequestsWithoutOutpostId_shouldNotRedirect() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        ListRegionalBucketsRequest sdkRequest = ListRegionalBucketsRequest.builder()
                                                                          .build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder()
                                                                            .fipsModeEnabled(true)
                                                                            .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3");

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request).request(sdkRequest),
                                                                executionAttributes);
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3");
        assertThat(modified.host()).isEqualTo("s3-control-fips.us-east-1.amazonaws.com");
    }

    @Test
    public void listRegionalBucketsRequestWithOutpostId_fipsEnabled_shouldThrowException() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        ListRegionalBucketsRequest sdkRequest = ListRegionalBucketsRequest.builder()
                                                                          .outpostId("123")
                                                                          .build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().fipsModeEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3");

        assertThatThrownBy(() -> interceptor.modifyHttpRequest(new Context(request).request(sdkRequest),
                                                               executionAttributes)).hasMessageContaining("FIPS endpoints are "
                                                                                                          + "not supported");
    }

    @Test
    public void listRegionalBucketsRequestWithOutpostId_fipsDualsackEnabled_shouldThrowException() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        ListRegionalBucketsRequest sdkRequest = ListRegionalBucketsRequest.builder()
                                                                          .outpostId("123")
                                                                          .build();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().dualstackEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);
        executionAttributes.putAttribute(SIGNING_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3");

        assertThatThrownBy(() -> interceptor.modifyHttpRequest(new Context(request).request(sdkRequest),
                                                               executionAttributes)).hasMessageContaining("Dualstack endpoints are "
                                                                                                          + "not supported");
    }

    @Test(expected = SdkClientException.class)
    public void modifyHttpRequest_ThrowsException_FipsAndDualstack() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder()
                                                                            .fipsModeEnabled(true)
                                                                            .dualstackEnabled(true)
                                                                            .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        interceptor.modifyHttpRequest(new Context(request), executionAttributes);
    }

    @Test(expected = SdkClientException.class)
    public void modifyHttpRequest_ThrowsException_NonStandardEndpoint() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder()
                                                                            .dualstackEnabled(true)
                                                                            .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        interceptor.modifyHttpRequest(new Context(request.toBuilder().host("some-garbage").build()),
                                      executionAttributes);
    }

    public final class Context implements software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest {

        private final SdkHttpRequest request;
        private SdkRequest sdkRequest;

        public Context(SdkHttpRequest request) {
            this.request = request;
        }

        public Context request(SdkRequest sdkRequest) {
            this.sdkRequest = sdkRequest;
            return this;
        }

        @Override
        public SdkRequest request() {
            return sdkRequest;
        }

        @Override
        public SdkHttpRequest httpRequest() {
            return request;
        }

        @Override
        public Optional<RequestBody> requestBody() {
            return Optional.empty();
        }

        @Override
        public Optional<AsyncRequestBody> asyncRequestBody() {
            return Optional.empty();
        }
    }
}
