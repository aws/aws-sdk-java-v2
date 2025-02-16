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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersAsyncClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class EndpointInterceptorTests {

    @Test
    public void sync_hostPrefixInjectDisabled_hostPrefixNotAdded() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}))
            .hasMessageContaining("stop");

        Endpoint endpoint = interceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);

        assertThat(endpoint.url().getHost()).isEqualTo("restjson.us-west-2.amazonaws.com");
    }

    @Test
    public void async_hostPrefixInjectDisabled_hostPrefixNotAdded() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        Endpoint endpoint = interceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);

        assertThat(endpoint.url().getHost()).isEqualTo("restjson.us-west-2.amazonaws.com");
    }

    @Test
    public void sync_clientContextParamsSetOnBuilder_includedInExecutionAttributes() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        })).hasMessageContaining("stop");

        Endpoint endpoint = interceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        assertThat(endpoint).isNotNull();
    }

    @Test
    public void async_clientContextParamsSetOnBuilder_includedInExecutionAttributes() {
        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }).join()).hasMessageContaining("stop");

        Endpoint endpoint = interceptor.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        assertThat(endpoint).isNotNull();
    }

    @Test
    public void sync_endpointProviderReturnsHeaders_includedInHttpRequest() {
        RestJsonEndpointProvidersEndpointProvider defaultProvider = RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultProvider.resolveEndpoint(r)
                                                  .thenApply(e -> e.toBuilder()
                                                                   .putHeader("TestHeader", "TestValue")
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}))
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().matchingHeaders("TestHeader")).containsExactly("TestValue");
    }

    @Test
    public void async_endpointProviderReturnsHeaders_includedInHttpRequest() {
        RestJsonEndpointProvidersEndpointProvider defaultProvider = RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultProvider.resolveEndpoint(r)
                                                  .thenApply(e -> e.toBuilder()
                                                                   .putHeader("TestHeader", "TestValue")
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().matchingHeaders("TestHeader")).containsExactly("TestValue");
    }

    @Test
    public void sync_endpointProviderReturnsHeaders_appendedToExistingRequest() {
        RestJsonEndpointProvidersEndpointProvider defaultProvider = RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultProvider.resolveEndpoint(r)
                                                  .thenApply(e -> e.toBuilder()
                                                                   .putHeader("TestHeader", "TestValue")
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> r.overrideConfiguration(c -> c.putHeader("TestHeader",
                                                                                                              "TestValue0"))))
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().matchingHeaders("TestHeader")).containsExactly("TestValue", "TestValue0");
    }

    @Test
    public void async_endpointProviderReturnsHeaders_appendedToExistingRequest() {
        RestJsonEndpointProvidersEndpointProvider defaultProvider = RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingInterceptor interceptor = new CapturingInterceptor();
        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultProvider.resolveEndpoint(r)
                                                  .thenApply(e -> e.toBuilder()
                                                                   .putHeader("TestHeader", "TestValue")
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> r.overrideConfiguration(c -> c.putHeader("TestHeader",
                                                                                                              "TestValue0")))
                                       .join())
            .hasMessageContaining("stop");

        assertThat(interceptor.context.httpRequest().matchingHeaders("TestHeader")).containsExactly("TestValue", "TestValue0");
    }


    @Test
    public void sync_endpointProviderReturnsSignerProperties_overridesV4AuthSchemeResolverProperties() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingSigner signer = new CapturingSigner();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4AuthScheme.builder()
                                               .signingRegion("region-from-ep")
                                               .signingName("name-from-ep")
                                               .disableDoubleEncoding(true)
                                               .build());

        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4", signer))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}))
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo("region-from-ep");
        assertThat(signer.request.property(AwsV4HttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(signer.request.property(AwsV4HttpSigner.DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void async_endpointProviderReturnsSignerProperties_overridesV4AuthSchemeResolverProperties() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingSigner signer = new CapturingSigner();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4AuthScheme.builder()
                                               .signingRegion("region-from-ep")
                                               .signingName("name-from-ep")
                                               .disableDoubleEncoding(true)
                                               .build());

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4", signer))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo("region-from-ep");
        assertThat(signer.request.property(AwsV4HttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(signer.request.property(AwsV4HttpSigner.DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void sync_endpointProviderReturnsSignerProperties_overridesV4AAuthSchemeResolverProperties() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingSigner signer = new CapturingSigner();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4aAuthScheme.builder()
                                                .addSigningRegion("region-1-from-ep")
                                                .signingName("name-from-ep")
                                                .disableDoubleEncoding(true)
                                                .build());

        RestJsonEndpointProvidersClient client = syncClientBuilder()
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4a")
                                                                   .putSignerProperty(
                                                                       AwsV4aHttpSigner.REGION_SET, RegionSet.create("us-east-1"))
                                                                   .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, true)
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}))
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET)).isEqualTo(RegionSet.create("region-1-from-ep"));
        assertThat(signer.request.property(AwsV4aHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(signer.request.property(AwsV4aHttpSigner.DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void async_endpointProviderReturnsSignerProperties_overridesV4AAuthSchemeResolverProperties() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();

        CapturingSigner signer = new CapturingSigner();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4aAuthScheme.builder()
                                                .addSigningRegion("region-1-from-ep")
                                                .signingName("name-from-ep")
                                                .disableDoubleEncoding(true)
                                                .build());

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4a")
                                                                   .putSignerProperty(AwsV4aHttpSigner.REGION_SET,
                                                                                      RegionSet.create("us-east-1"))
                                                                   .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, true)
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET)).isEqualTo(RegionSet.create("region-1-from-ep"));
        assertThat(signer.request.property(AwsV4aHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(signer.request.property(AwsV4aHttpSigner.DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void sync_endpointProviderDoesNotReturnV4SignerProperties_executionAttributesFromAuthSchemeOption() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();
        CapturingInterceptor interceptor = new CapturingInterceptor();

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4")
                                                                   .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "X")
                                                                   .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, true)
                                                                   .build()))
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, emptyList())
                                                                           .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("X"));
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("Y");
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }

    @Test
    public void sync_endpointProviderReturnsV4SignerProperties_executionAttributesFromEndpointProvider() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();
        CapturingInterceptor interceptor = new CapturingInterceptor();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4AuthScheme.builder()
                                               .signingRegion("region-from-ep")
                                               .signingName("name-from-ep")
                                               .disableDoubleEncoding(false)
                                               .build());

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4")
                                                                   .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "X")
                                                                   .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                                                                   .build()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("region-from-ep"));
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }

    @Test
    public void sync_endpointProviderDoesNotReturnV4aSignerProperties_executionAttributesFromAuthSchemeOption() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();
        CapturingInterceptor interceptor = new CapturingInterceptor();

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4a")
                                                                   .putSignerProperty(AwsV4aHttpSigner.REGION_SET,
                                                                                      RegionSet.create("region-from-ap"))
                                                                   .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, true)
                                                                   .build()))
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, emptyList())
                                                                           .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4a", AwsV4HttpSigner.create()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.create("region-from-ap"));
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("Y");
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }

    @Test
    public void sync_endpointProviderReturnsV4aSignerProperties_executionAttributesFromEndpointProvider() {
        RestJsonEndpointProvidersEndpointProvider defaultEndpointProvider =
            RestJsonEndpointProvidersEndpointProvider.defaultProvider();
        CapturingInterceptor interceptor = new CapturingInterceptor();

        List<EndpointAuthScheme> endpointAuthSchemes = new ArrayList<>();
        endpointAuthSchemes.add(SigV4aAuthScheme.builder()
                                               .addSigningRegion("region-from-ep")
                                               .signingName("name-from-ep")
                                               .disableDoubleEncoding(false)
                                               .build());

        RestJsonEndpointProvidersAsyncClient client = asyncClientBuilder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .endpointProvider(r -> defaultEndpointProvider.resolveEndpoint(r)
                                                          .thenApply(e -> e.toBuilder()
                                                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, endpointAuthSchemes)
                                                                           .build()))
            .authSchemeProvider(p -> singletonList(AuthSchemeOption.builder()
                                                                   .schemeId("aws.auth#sigv4a")
                                                                   .putSignerProperty(AwsV4aHttpSigner.REGION_SET,
                                                                                      RegionSet.create("us-east-1"))
                                                                   .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "Y")
                                                                   .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false)
                                                                   .build()))
            .putAuthScheme(authScheme("aws.auth#sigv4a", AwsV4HttpSigner.create()))
            .build();

        assertThatThrownBy(() -> client.operationWithHostPrefix(r -> {}).join())
            .hasMessageContaining("stop");

        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.create("region-from-ep"));
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("name-from-ep");
        assertThat(interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }


    private static AuthScheme<?> authScheme(String schemeId, HttpSigner<AwsCredentialsIdentity> signer) {
        return new AuthScheme<AwsCredentialsIdentity>() {
            @Override
            public String schemeId() {
                return schemeId;
            }

            @Override
            public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
                return providers.identityProvider(AwsCredentialsIdentity.class);
            }

            @Override
            public HttpSigner<AwsCredentialsIdentity> signer() {
                return signer;
            }
        };
    }

    public static class CapturingSigner implements HttpSigner<AwsCredentialsIdentity> {
        private BaseSignRequest<?, ?> request;

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            this.request = request;
            throw new CaptureCompletedException("stop");
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            this.request = request;
            return CompletableFutureUtils.failedFuture(new CaptureCompletedException("stop"));
        }
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {

        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new CaptureCompletedException("stop");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }

    public static class CaptureCompletedException extends RuntimeException {
        CaptureCompletedException(String message) {
            super(message);
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
}
