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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;
import org.junit.Test;

public class RequestOverrideEndpointProviderTests {

    @Test
    public void sync_endpointOverridden_equals_requestOverride() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(
            r -> r.overrideConfiguration(o -> o.endpointProvider(new CustomEndpointProvider(Region.AWS_GLOBAL)))))
            .hasMessageContaining("stop");

        assertThat(interceptor.httpRequest.host()).isEqualTo("restjson.aws-global.amazonaws.com");
    }

    @Test
    public void sync_endpointOverridden_equals_ClientsWhenNoRequestOverride() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .endpointProvider(new CustomEndpointProvider(Region.EU_WEST_2))
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {})).hasMessageContaining("stop");

        assertThat(interceptor.httpRequest.host()).isEqualTo("restjson.eu-west-2.amazonaws.com");
    }

    @Test
    public void async_endpointOverridden_equals_requestOverride() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(
            r -> r.overrideConfiguration(o -> o.endpointProvider(new CustomEndpointProvider(Region.AWS_GLOBAL)))
        ).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.httpRequest.host()).isEqualTo("restjson.aws-global.amazonaws.com");
    }

    @Test
    public void async_endpointOverridden_equals_ClientsWhenNoRequestOverride() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .endpointProvider(new CustomEndpointProvider(Region.EU_WEST_2))
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.httpRequest.host()).isEqualTo("restjson.eu-west-2.amazonaws.com");
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private SdkHttpRequest httpRequest;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.httpRequest = context.httpRequest();
            throw new CaptureCompletedException("stop");
        }

        public class CaptureCompletedException extends RuntimeException {
            CaptureCompletedException(String message) {
                super(message);
            }
        }
    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(
                                                  StaticCredentialsProvider.create(
                                                      AwsBasicCredentials.create("akid", "skid")));
    }

    private RestJsonEndpointProvidersAsyncClientBuilder asyncClientBuilder() {
        return RestJsonEndpointProvidersAsyncClient.builder()
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(
                                                       StaticCredentialsProvider.create(
                                                           AwsBasicCredentials.create("akid", "skid")));
    }

    public static class CustomEndpointProvider implements RestJsonEndpointProvidersEndpointProvider {
        private final Region region;

        CustomEndpointProvider(Region region) {
            this.region = region;
        }

        @Override
        public java.util.concurrent.CompletableFuture<Endpoint> resolveEndpoint(RestJsonEndpointProvidersEndpointParams params) {
            return RestJsonEndpointProvidersEndpointProvider.defaultProvider()
                .resolveEndpoint(params.toBuilder().region(region).build());
        }
    }
}
