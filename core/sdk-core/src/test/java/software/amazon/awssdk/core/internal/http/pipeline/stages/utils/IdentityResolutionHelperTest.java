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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.metrics.MetricCollector;

class IdentityResolutionHelperTest {
    private static final IdentityProperty<String> TEST_PROPERTY =
        IdentityProperty.create(IdentityResolutionHelperTest.class, "TestProperty");

    @Test
    void reResolveIdentityForRetry_whenNoSelectedAuthScheme_doesNothing() {
        ExecutionAttributes attributes = new ExecutionAttributes();

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    void reResolveIdentityForRetry_whenNoIdentityProvider_keepsIdentity() {
        SelectedAuthScheme<TestIdentity> scheme =
            new SelectedAuthScheme<>(CompletableFuture.completedFuture(new TestIdentity("original")),
                                     mockSigner(),
                                     AuthSchemeOption.builder().schemeId("test").build());
        ExecutionAttributes attributes = attributesWith(scheme);

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isSameAs(scheme);
    }

    @Test
    void reResolveIdentityForRetry_replacesIdentityWithNewlyResolvedOne_andPreservesOtherFields() {
        CountingIdentityProvider provider = new CountingIdentityProvider();
        HttpSigner<TestIdentity> signer = mockSigner();
        AuthSchemeOption option = AuthSchemeOption.builder().schemeId("test").build();
        SelectedAuthScheme<TestIdentity> scheme = SelectedAuthScheme.<TestIdentity>builder()
                                                                    .identity(CompletableFuture.completedFuture(
                                                                        new TestIdentity("original")))
                                                                    .signer(signer)
                                                                    .authSchemeOption(option)
                                                                    .identityProvider(provider)
                                                                    .build();
        ExecutionAttributes attributes = attributesWith(scheme);

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        SelectedAuthScheme<?> updated = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(provider.resolveCount()).isEqualTo(1);
        assertThat(((TestIdentity) updated.identity().join()).name()).isEqualTo("resolved-1");
        assertThat(updated.signer()).isSameAs(signer);
        assertThat(updated.authSchemeOption()).isSameAs(option);
        assertThat(updated.identityProvider()).isSameAs(provider);
    }

    @Test
    void reResolveIdentityForRetry_eachCallResolvesAgain() {
        CountingIdentityProvider provider = new CountingIdentityProvider();
        ExecutionAttributes attributes = attributesWith(schemeWith(provider, AuthSchemeOption.builder().schemeId("test").build()));

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);
        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        SelectedAuthScheme<?> updated = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(provider.resolveCount()).isEqualTo(2);
        assertThat(((TestIdentity) updated.identity().join()).name()).isEqualTo("resolved-2");
    }

    @Test
    void reResolveIdentityForRetry_passesIdentityPropertiesOfAuthSchemeOption() {
        CountingIdentityProvider provider = new CountingIdentityProvider();
        AuthSchemeOption option = AuthSchemeOption.builder()
                                                  .schemeId("test")
                                                  .putIdentityProperty(TEST_PROPERTY, "property-value")
                                                  .build();
        ExecutionAttributes attributes = attributesWith(schemeWith(provider, option));

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        assertThat(provider.requests()).hasSize(1);
        assertThat(provider.requests().get(0).property(TEST_PROPERTY)).isEqualTo("property-value");
    }

    @Test
    void reResolveIdentityForRetry_whenProviderThrows_storesFailureAsIdentity() {
        RuntimeException failure = new RuntimeException("resolve failed");
        IdentityProvider<TestIdentity> provider = new CountingIdentityProvider() {
            @Override
            public CompletableFuture<TestIdentity> resolveIdentity(ResolveIdentityRequest request) {
                throw failure;
            }
        };
        ExecutionAttributes attributes = attributesWith(schemeWith(provider, AuthSchemeOption.builder().schemeId("test").build()));

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        SelectedAuthScheme<?> updated = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThatThrownBy(() -> updated.identity().join()).isInstanceOf(CompletionException.class)
                                                           .hasCause(failure);
    }

    @Test
    void reResolveIdentityForRetry_reportsCredentialsFetchDurationToApiCallMetricCollector() {
        IdentityProvider<AwsCredentialsIdentity> provider = new IdentityProvider<AwsCredentialsIdentity>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("akid", "skid"));
            }
        };
        SelectedAuthScheme<AwsCredentialsIdentity> scheme =
            SelectedAuthScheme.<AwsCredentialsIdentity>builder()
                              .identity(CompletableFuture.completedFuture(AwsCredentialsIdentity.create("old", "old")))
                              .signer(mockSigner())
                              .authSchemeOption(AuthSchemeOption.builder().schemeId("test").build())
                              .identityProvider(provider)
                              .build();
        MetricCollector metricCollector = MetricCollector.create("ApiCall");
        ExecutionAttributes attributes = attributesWith(scheme);
        attributes.putAttribute(SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR, metricCollector);

        IdentityResolutionHelper.reResolveIdentityForRetry(attributes);

        assertThat(metricCollector.collect().metricValues(CoreMetric.CREDENTIALS_FETCH_DURATION)).hasSize(1);
    }

    private static SelectedAuthScheme<TestIdentity> schemeWith(IdentityProvider<TestIdentity> provider,
                                                               AuthSchemeOption option) {
        return SelectedAuthScheme.<TestIdentity>builder()
                                 .identity(CompletableFuture.completedFuture(new TestIdentity("original")))
                                 .signer(mockSigner())
                                 .authSchemeOption(option)
                                 .identityProvider(provider)
                                 .build();
    }

    private static ExecutionAttributes attributesWith(SelectedAuthScheme<?> scheme) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, scheme);
        return attributes;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Identity> HttpSigner<T> mockSigner() {
        return (HttpSigner<T>) mock(HttpSigner.class);
    }

    private static final class TestIdentity implements Identity {
        private final String name;

        TestIdentity(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }
    }

    private static class CountingIdentityProvider implements IdentityProvider<TestIdentity> {
        private final AtomicInteger resolveCount = new AtomicInteger();
        private final List<ResolveIdentityRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public Class<TestIdentity> identityType() {
            return TestIdentity.class;
        }

        @Override
        public CompletableFuture<TestIdentity> resolveIdentity(ResolveIdentityRequest request) {
            requests.add(request);
            return CompletableFuture.completedFuture(new TestIdentity("resolved-" + resolveCount.incrementAndGet()));
        }

        int resolveCount() {
            return resolveCount.get();
        }

        List<ResolveIdentityRequest> requests() {
            return requests;
        }
    }
}
