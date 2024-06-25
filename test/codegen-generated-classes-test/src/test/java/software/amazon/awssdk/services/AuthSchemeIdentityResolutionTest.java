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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

/**
 * This test ensures that any updates made to IDENTITY_PROVIDERS is respected by the generated service auth scheme interceptor.
 */
public class AuthSchemeIdentityResolutionTest {
    private SdkHttpClient mockClient;

    @BeforeEach
    public void setup() throws IOException {
        mockClient = mock(SdkHttpClient.class);
        ExecutableHttpRequest executableRequest = mock(ExecutableHttpRequest.class);
        when(executableRequest.call()).thenThrow(new RuntimeException("boom"));
        when(mockClient.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(executableRequest);
    }
    @Test
    public void identityProviders_modifiedByInterceptor_overrideUsedInAuth() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("akid", "skid");
        TestIdentityProvider clientCredentialsProvider = spy(new TestIdentityProvider(credentials));
        TestIdentityProvider interceptorCredentialsProvider = spy(new TestIdentityProvider(credentials));

        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .credentialsProvider(clientCredentialsProvider)
            .region(Region.US_WEST_2)
            .overrideConfiguration(o -> o.addExecutionInterceptor(new OverrideIdentityInterceptor(
                interceptorCredentialsProvider)))
            .httpClient(mockClient)
            .build();

        assertThatThrownBy(client::deleteOperation).hasMessageContaining("boom");

        verify(interceptorCredentialsProvider).resolveIdentity(any(ResolveIdentityRequest.class));
        verify(clientCredentialsProvider, times(0)).resolveIdentity(any(ResolveIdentityRequest.class));
    }

    private static class OverrideIdentityInterceptor implements ExecutionInterceptor {
        private final IdentityProvider<AwsCredentialsIdentity> identityIdentityProvider;

        public OverrideIdentityInterceptor(IdentityProvider<AwsCredentialsIdentity> identityIdentityProvider) {
            this.identityIdentityProvider = identityIdentityProvider;
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            IdentityProviders identityProviders = executionAttributes.getAttribute(
                SdkInternalExecutionAttribute.IDENTITY_PROVIDERS);
            identityProviders = identityProviders.toBuilder()
                                                 .putIdentityProvider(identityIdentityProvider)
                                                 .build();
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);
        }
    }

    private static class TestIdentityProvider implements IdentityProvider<AwsCredentialsIdentity> {
        private final AwsCredentialsIdentity identity;

        private TestIdentityProvider(AwsCredentialsIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(identity);
        }
    }
}
