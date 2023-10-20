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
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersServiceClientConfiguration;
import software.amazon.awssdk.utils.Validate;

public class OverrideConfigurationPluginsTest {
    private CapturingInterceptor interceptor;

    @BeforeEach
    void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @Test
    void sync_pluginsClientOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                                                                    .putHeader("K1", "V1")))
            .build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> {
        }))
            .hasMessageContaining("boom!");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    @Test
    void sync_pluginsRequestOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder().build();
        SdkPlugin plugin = config -> config.overrideConfiguration(c -> c.putHeader("K1", "V1"));
        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(c -> c.addPlugin(plugin))))
            .hasMessageContaining("boom!");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    @Test
    void async_pluginsClientOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersAsyncClient syncClient = asyncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)
                                                                    .putHeader("K1", "V1")))
            .build();
        assertThatThrownBy(() -> syncClient.allTypes(r -> {
        }).join())
            .hasMessageContaining("boom!");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    @Test
    void async_pluginsRequestOverrideConfiguration_isAddedToRequest() {
        RestJsonEndpointProvidersAsyncClient syncClient = asyncClientBuilder().build();
        SdkPlugin plugin = config -> config.overrideConfiguration(c -> c.putHeader("K1", "V1"));
        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(c -> c.addPlugin(plugin)))
                                           .join())
            .hasMessageContaining("boom!");

        assertThat(interceptor.context.httpRequest().headers()).containsEntry("K1", singletonList("V1"));
    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return RestJsonEndpointProvidersClient
            .builder()
            .addPlugin(c -> {
                RestJsonEndpointProvidersServiceClientConfiguration.Builder config =
                    Validate.isInstanceOf(RestJsonEndpointProvidersServiceClientConfiguration.Builder.class, c, "\uD83E\uDD14");
                config.region(Region.US_WEST_2)
                      .credentialsProvider(
                          StaticCredentialsProvider.create(
                              AwsBasicCredentials.create("akid", "skid")))
                      .overrideConfiguration(oc -> oc.addExecutionInterceptor(interceptor));
            });
    }

    private RestJsonEndpointProvidersAsyncClientBuilder asyncClientBuilder() {
        return RestJsonEndpointProvidersAsyncClient
            .builder()
            .addPlugin(c -> {
                RestJsonEndpointProvidersServiceClientConfiguration.Builder config =
                    Validate.isInstanceOf(RestJsonEndpointProvidersServiceClientConfiguration.Builder.class, c, "\uD83E\uDD14");
                config.region(Region.US_WEST_2)
                      .credentialsProvider(
                          StaticCredentialsProvider.create(
                              AwsBasicCredentials.create("akid", "skid")))
                      .overrideConfiguration(oc -> oc.addExecutionInterceptor(interceptor));
            });
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("boom!");
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
