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

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlClient;

public class PayloadSigningInterceptorTest {

    private SdkHttpRequest request;

    @Before
    public void setup() {
        request = SdkHttpFullRequest.builder()
                                    .protocol(Protocol.HTTPS.toString())
                                    .method(SdkHttpMethod.POST)
                                    .host(S3ControlClient.serviceMetadata().endpointFor(Region.US_EAST_1).toString())
                                    .build();
    }

    @Test
    public void modifyHttpContent_AddsExecutionAttributeAndPayload() {
        PayloadSigningInterceptor interceptor = new PayloadSigningInterceptor();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        Optional<RequestBody> modified = interceptor.modifyHttpContent(new Context(request, null),
                                                                       executionAttributes);

        assertThat(modified.isPresent()).isTrue();
        assertThat(modified.get().contentLength()).isEqualTo(0);
        assertThat(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING)).isTrue();
    }

    @Test
    public void modifyHttpContent_DoesNotReplaceBody() {
        PayloadSigningInterceptor interceptor = new PayloadSigningInterceptor();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        Optional<RequestBody> modified = interceptor.modifyHttpContent(new Context(request, RequestBody.fromString("hello")),
                                                                       executionAttributes);

        assertThat(modified.isPresent()).isTrue();
        assertThat(modified.get().contentLength()).isEqualTo(5);
        assertThat(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING)).isTrue();
    }

    public final class Context implements software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest {

        private final SdkHttpRequest request;
        private final RequestBody requestBody;

        public Context(SdkHttpRequest request,
                       RequestBody requestBody) {
            this.request = request;
            this.requestBody = requestBody;
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
            return Optional.ofNullable(requestBody);
        }

        @Override
        public Optional<AsyncRequestBody> asyncRequestBody() {
            return Optional.empty();
        }
    }
}
