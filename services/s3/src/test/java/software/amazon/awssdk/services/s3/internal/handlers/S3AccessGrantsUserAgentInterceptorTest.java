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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class S3AccessGrantsUserAgentInterceptorTest {

    private S3AccessGrantsUserAgentInterceptor interceptor;
    private ExecutionAttributes executionAttributes;
    private Context.ModifyRequest context;

    @BeforeEach
    void setUp() {
        interceptor = new S3AccessGrantsUserAgentInterceptor();
        executionAttributes = new ExecutionAttributes();
        context = mock(Context.ModifyRequest.class);
    }

    @Test
    void modifyRequest_whenS3AccessGrantsPluginActive_shouldAddS3AccessGrantsApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        IdentityProvider<AwsCredentialsIdentity> mockProvider = createMockS3AccessGrantsProvider();
        IdentityProviders identityProviders = IdentityProviders.builder()
                                                               .putIdentityProvider(mockProvider)
                                                               .build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        RequestOverrideConfiguration requestOverrideConfiguration = modifiedRequest.overrideConfiguration().get();
        Predicate<ApiName> apiNamePredicate = a -> a.name().equals("sdk-metrics") &&
                                                   a.version().equals(BusinessMetricFeatureId.S3_ACCESS_GRANTS.value());
        assertThat(requestOverrideConfiguration.apiNames().stream().anyMatch(apiNamePredicate)).isTrue();
    }

    @Test
    void modifyRequest_whenRegularS3Operation_shouldNotAddS3AccessGrantsApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        IdentityProvider<AwsCredentialsIdentity> mockProvider = createMockRegularProvider();
        IdentityProviders identityProviders = IdentityProviders.builder()
                                                               .putIdentityProvider(mockProvider)
                                                               .build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        assertThat(modifiedRequest.overrideConfiguration()).isEmpty();
    }

    @Test
    void modifyRequest_whenNoCredentialsProvider_shouldNotAddS3AccessGrantsApiName() {
        GetObjectRequest s3Request = GetObjectRequest.builder().build();
        when(context.request()).thenReturn(s3Request);

        // Empty identity providers
        IdentityProviders identityProviders = IdentityProviders.builder().build();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);

        SdkRequest modifiedRequest = interceptor.modifyRequest(context, executionAttributes);

        assertThat(modifiedRequest.overrideConfiguration()).isEmpty();
    }

    private IdentityProvider<AwsCredentialsIdentity> createMockS3AccessGrantsProvider() {
        return new MockS3AccessGrantsIdentityProvider();
    }

    private IdentityProvider<AwsCredentialsIdentity> createMockRegularProvider() {
        return mock(IdentityProvider.class);
    }


    /**
     * Mock implementation that simulates S3AccessGrantsIdentityProvider
     */
    private static class MockS3AccessGrantsIdentityProvider implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public java.util.concurrent.CompletableFuture<AwsCredentialsIdentity> resolveIdentity(
            software.amazon.awssdk.identity.spi.ResolveIdentityRequest request) {
            return null;
        }
    }
}
