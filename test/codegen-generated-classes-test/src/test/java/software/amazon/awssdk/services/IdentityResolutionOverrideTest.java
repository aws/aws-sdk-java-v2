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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class IdentityResolutionOverrideTest {

    private static final AwsCredentials BASE_CREDENTIALS =
        AwsBasicCredentials.create("akid", "skid");

    private static final AwsCredentials OVERRIDE_CREDENTIALS =
        AwsBasicCredentials.create("akidOverride", "skidOverride");

    private CapturingInterceptor capturingInterceptor;

    @BeforeEach
    public void setup() {
        this.capturingInterceptor = new CapturingInterceptor();
    }

    @Test
    void when_credentialsProviderIsOverridden_atRequestCreateTime_itIsUsed() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder().build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(
            c -> c.credentialsProvider(StaticCredentialsProvider.create(OVERRIDE_CREDENTIALS)))))
            .hasMessageContaining("stop");

        assertSelectedAuthSchemeBeforeTransmissionContains(OVERRIDE_CREDENTIALS);
    }

    // Changing the credentials provider in modifyRequest does not work in SRA identity resolution
    // Identity is resolved in beforeExecution (and happens before user applied interceptors) and cannot
    // be affected by execution interceptors.
    @Test
    void when_credentialsProviderIsOverridden_inExecutionInterceptor_modifyRequest_itIsNotUsed() {
        ExecutionInterceptor overridingInterceptor =
            new OverrideInterceptor(b -> b.credentialsProvider(StaticCredentialsProvider.create(OVERRIDE_CREDENTIALS)));

        RestJsonEndpointProvidersClient syncClient = syncClientBuilder(overridingInterceptor).build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> {})).hasMessageContaining("stop");

        assertSelectedAuthSchemeBeforeTransmissionContains(BASE_CREDENTIALS);
    }

    @Test
    void when_credentialsAreOverridden_atRequestCreateTime_theyAreNotUsed() {
        RestJsonEndpointProvidersClient syncClient = syncClientBuilder().build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> r.overrideConfiguration(
            c -> c.putExecutionAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, OVERRIDE_CREDENTIALS))))
            .hasMessageContaining("stop");

        assertSelectedAuthSchemeBeforeTransmissionContains(BASE_CREDENTIALS);
    }

    // Updating an execution attribute inside the request override, in an interceptor doesn't work
    // Execution attributes are merged before interceptor hooks
    @Test
    void when_credentialsAreOverridden_inInterceptor_throughOverride_theyAreNotUsed() {
        ExecutionInterceptor overridingInterceptor =
            new OverrideInterceptor(b -> b.putExecutionAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, OVERRIDE_CREDENTIALS));

        RestJsonEndpointProvidersClient syncClient = syncClientBuilder(overridingInterceptor).build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> {})).hasMessageContaining("stop");

        assertSelectedAuthSchemeBeforeTransmissionContains(BASE_CREDENTIALS);
    }

    // Updating the AWS_CREDENTIALS pre-SRA credentials execution attribute in an interceptor works
    // Resolved credentials are synced both ways with AWS_CREDENTIALS.
    @Test
    void when_credentialsAreOverridden_inInterceptor_throughExecutionAttributes_theyAreUsed() {
        ExecutionInterceptor overridingInterceptor = new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, OVERRIDE_CREDENTIALS);
                return ExecutionInterceptor.super.modifyRequest(context, executionAttributes);
            }
        };

        RestJsonEndpointProvidersClient syncClient = syncClientBuilder(overridingInterceptor).build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> {})).hasMessageContaining("stop");

        assertSelectedAuthSchemeBeforeTransmissionContains(OVERRIDE_CREDENTIALS);
    }

    private void assertSelectedAuthSchemeBeforeTransmissionContains(AwsCredentials overriddenCredentials) {
        SelectedAuthScheme<?> auth = capturingInterceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        Optional<AwsCredentials> requestCredentials = Optional.empty();
        try {
            requestCredentials = providerNameFromIdentity(auth);
        } catch (Exception e) {
            fail("Failed to resolve identity", e);
        }
        assertThat(requestCredentials).isPresent().contains(overriddenCredentials);
    }

    private static <T extends Identity> Optional<AwsCredentials> providerNameFromIdentity(SelectedAuthScheme<T> selectedAuthScheme) {
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);
        if (identity instanceof AwsBasicCredentials) {
            return Optional.of((AwsBasicCredentials) identity);
        }
        return Optional.empty();
    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return syncClientBuilder(null);
    }
    private RestJsonEndpointProvidersClientBuilder syncClientBuilder(ExecutionInterceptor additionalInterceptor) {
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        interceptors.add(capturingInterceptor);
        if (additionalInterceptor != null) {
            interceptors.add(additionalInterceptor);
        }
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(StaticCredentialsProvider.create(BASE_CREDENTIALS))
                                              .overrideConfiguration(c -> c.executionInterceptors(interceptors));
    }

    public static class OverrideInterceptor implements ExecutionInterceptor {

        private final Consumer<AwsRequestOverrideConfiguration.Builder> modifier;

        public OverrideInterceptor(Consumer<AwsRequestOverrideConfiguration.Builder> m) {
            this.modifier = m;
        }

        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
            SdkRequest request = context.request();
            AwsRequest awsRequest = (AwsRequest) request;

            AwsRequestOverrideConfiguration.Builder overrideConfigBuilder = getOrCreateOverrideConfig(request);
            modifier.accept(overrideConfigBuilder);

            //overrideConfigBuilder.putExecutionAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, overrideCredentials);
            //     executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, overriddenCredentials);
            //      overrideConfigBuilder.credentialsProvider(provider);
            return awsRequest.toBuilder().overrideConfiguration(overrideConfigBuilder.build()).build();
        }

        private AwsRequestOverrideConfiguration.Builder getOrCreateOverrideConfig(SdkRequest request) {
            Optional<? extends RequestOverrideConfiguration> requestOverrideConfiguration = request.overrideConfiguration();
            if (requestOverrideConfiguration.isPresent()) {
                return ((AwsRequestOverrideConfiguration) requestOverrideConfiguration.get()).toBuilder();
            }
            return AwsRequestOverrideConfiguration.builder();
        }
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
    }
}
