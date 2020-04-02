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

import java.util.Optional;
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
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;

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
    public void modifyHttpRequest_illegalCharacterInAccountId_throwsException() {
        SdkHttpRequest modifiedRequest = SdkHttpFullRequest.builder()
                                                           .appendHeader(X_AMZ_ACCOUNT_ID, "1234/#")
                                                           .protocol(Protocol.HTTPS.toString())
                                                           .method(SdkHttpMethod.POST)
                                                           .host(S3ControlClient.serviceMetadata().endpointFor(Region.US_EAST_1).toString())
                                                           .build();
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        assertThatThrownBy(() -> interceptor.modifyHttpRequest(new Context(modifiedRequest), new ExecutionAttributes()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("account id");
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_StandardSettings() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();
        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), new ExecutionAttributes());
        assertThat(modified.host()).isEqualTo(ACCOUNT_ID + ".s3-control.us-east-1.amazonaws.com");
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_Dualstack() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().dualstackEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), executionAttributes);
        assertThat(modified.host()).isEqualTo(ACCOUNT_ID + ".s3-control.dualstack.us-east-1.amazonaws.com");
    }

    @Test
    public void modifyHttpRequest_ResolvesCorrectHost_Fips() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder().fipsModeEnabled(true).build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        SdkHttpRequest modified = interceptor.modifyHttpRequest(new Context(request), executionAttributes);
        assertThat(modified.host()).isEqualTo(ACCOUNT_ID + ".s3-control-fips.us-east-1.amazonaws.com");
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

    @Test(expected = SdkClientException.class)
    public void modifyHttpRequest_ThrowsException_NoAccountId() {
        EndpointAddressInterceptor interceptor = new EndpointAddressInterceptor();

        S3ControlConfiguration controlConfiguration = S3ControlConfiguration.builder()
                                                                            .dualstackEnabled(true)
                                                                            .build();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, controlConfiguration);

        interceptor.modifyHttpRequest(new Context(request.toBuilder().removeHeader(X_AMZ_ACCOUNT_ID).build()),
                                      executionAttributes);
    }

    public final class Context implements software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest {

        private final SdkHttpRequest request;

        public Context(SdkHttpRequest request) {
            this.request = request;
        }

        @Override
        public SdkRequest request() {
            return null;
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
