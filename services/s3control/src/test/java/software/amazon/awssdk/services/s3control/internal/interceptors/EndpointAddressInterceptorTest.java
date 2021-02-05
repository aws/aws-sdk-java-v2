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
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.SIGNING_REGION;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.services.s3control.internal.S3ControlInternalExecutionAttribute.S3_ARNABLE_FIELD;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;
import software.amazon.awssdk.services.s3control.internal.S3ArnableField;
import software.amazon.awssdk.services.s3control.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest;

public class EndpointAddressInterceptorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String X_AMZ_ACCOUNT_ID = "x-amz-account-id";
    private static final String ACCOUNT_ID = "123456789012";

    private SdkHttpRequest request;
    private S3ControlConfiguration configuration;
    private ExecutionAttributes executionAttributes;

    @Before
    public void setup() {
        request = SdkHttpFullRequest.builder()
                                    .appendHeader(X_AMZ_ACCOUNT_ID, ACCOUNT_ID)
                                    .protocol(Protocol.HTTPS.toString())
                                    .method(SdkHttpMethod.POST)
                                    .host(S3ControlClient.serviceMetadata().endpointFor(Region.US_EAST_1).toString())
                                    .build();
        configuration = S3ControlConfiguration.builder().build();
        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_SIGNING_NAME, "s3-control");
        executionAttributes.putAttribute(SIGNING_REGION, Region.of("us-east-1"));
        executionAttributes.putAttribute(SERVICE_CONFIG, configuration);
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
                                                               executionAttributes))
            .hasMessageContaining("Dual stack");
    }

    @Test(expected = IllegalArgumentException.class)
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

    @Test
    public void outpostBucketArn_shouldResolveHost() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        executionAttributes.putAttribute(S3_ARNABLE_FIELD, S3ArnableField.builder().arn(arn).build());
        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(new Context(request), executionAttributes);

        assertThat(modifiedRequest.host()).isEqualTo("s3-outposts.us-east-1.amazonaws.com");
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3-outposts");
        assertThat(modifiedRequest.headers().get("x-amz-outpost-id").get(0)).isEqualTo("op-01234567890123456");
        assertThat(modifiedRequest.headers().get("x-amz-account-id").get(0)).isEqualTo(ACCOUNT_ID);
    }

    @Test
    public void outpostAccessPointArn_shouldResolveHost() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint");
        executionAttributes.putAttribute(S3_ARNABLE_FIELD, S3ArnableField.builder().arn(arn).build());
        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(new Context(request), executionAttributes);

        assertThat(modifiedRequest.host()).isEqualTo("s3-outposts.us-east-1.amazonaws.com");
        assertThat(executionAttributes.getAttribute(SERVICE_SIGNING_NAME)).isEqualTo("s3-outposts");
        assertThat(modifiedRequest.headers().get("x-amz-outpost-id").get(0)).isEqualTo("op-01234567890123456");
        assertThat(modifiedRequest.headers().get("x-amz-account-id").get(0)).isEqualTo(ACCOUNT_ID);
    }

    @Test
    public void outpostArnWithFipsEnabled_shouldThrowException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("FIPS");

        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        executionAttributes.putAttribute(S3_ARNABLE_FIELD, S3ArnableField.builder().arn(arn).build());
        executionAttributes.putAttribute(SERVICE_CONFIG, enableFips());
        interceptor.modifyHttpRequest(new Context(request), executionAttributes);
    }

    @Test
    public void outpostArnWithDualstackEnabled_shouldThrowException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Dual stack");

        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        Arn arn = Arn.fromString("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:bucket:mybucket");
        executionAttributes.putAttribute(S3_ARNABLE_FIELD, S3ArnableField.builder().arn(arn).build());
        executionAttributes.putAttribute(SERVICE_CONFIG, enableDualstack());
        interceptor.modifyHttpRequest(new Context(request), executionAttributes);
    }

    private S3ControlConfiguration enableDualstack() {
        return S3ControlConfiguration.builder()
                                     .dualstackEnabled(true)
                                     .build();
    }

    private S3ControlConfiguration enableFips() {
        return S3ControlConfiguration.builder()
                                     .fipsModeEnabled(true)
                                     .build();
    }

    public final class Context implements software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest {

        private final SdkHttpRequest request;
        private SdkRequest sdkRequest = CreateBucketRequest.builder().build();

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
