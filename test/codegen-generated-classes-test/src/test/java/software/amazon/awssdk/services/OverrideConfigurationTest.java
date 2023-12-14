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

package software.amazon.awssdk.services;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.endpointproviders.EndpointInterceptorTests;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;

public class OverrideConfigurationTest {
    private CapturingInterceptor interceptor;

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @Test
    public void sync_clientOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                                         .putHeader("K1", "V1"))
            .build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> {}))
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    @Test
    public void sync_requestOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder().build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(c -> c.putHeader("K1", "V1")
                                                                                        .putRawQueryParameter("K2", "V2"))))
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
        assertThat(interceptor.context.httpRequest().rawQueryParameters()).containsEntry("K2", singletonList("V2"));
    }

    @Test
    public void async_clientOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersAsyncClient syncClient = asyncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                                         .putHeader("K1", "V1"))
            .build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    @Test
    public void async_requestOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersAsyncClient syncClient = asyncClientBuilder().build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(c -> c.putHeader("K1", "V1")
                                                                                        .putRawQueryParameter("K2", "V2")))
                                           .join())
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
        assertThat(interceptor.context.httpRequest().rawQueryParameters()).containsEntry("K2", singletonList("V2"));
    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(
                                                  StaticCredentialsProvider.create(
                                                      AwsBasicCredentials.create("akid", "skid")))
                                              .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    private RestJsonEndpointProvidersAsyncClientBuilder asyncClientBuilder() {
        return RestJsonEndpointProvidersAsyncClient.builder()
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(
                                                       StaticCredentialsProvider.create(
                                                           AwsBasicCredentials.create("akid", "skid")))
                                                   .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }

        public class CaptureCompletedException extends RuntimeException {
            CaptureCompletedException(String message) {
                super(message);
            }
        }
    }
}
