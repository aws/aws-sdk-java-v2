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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.spi.identity.RequestIdentityProviderResolver;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

class AuthSchemeResolutionStageTest {

    private static final String SCHEME_ID = "test.scheme";

    private AuthSchemeResolutionStage stage;
    private SdkHttpFullRequest.Builder httpRequestBuilder;
    private RequestExecutionContext context;
    private ExecutionAttributes executionAttributes;
    private SdkRequest sdkRequest;

    @BeforeEach
    void setup() {
        stage = new AuthSchemeResolutionStage(null);
        httpRequestBuilder = mock(SdkHttpFullRequest.Builder.class);
        sdkRequest = mock(SdkRequest.class);
        executionAttributes = new ExecutionAttributes();

        InterceptorContext interceptorContext = InterceptorContext.builder()
            .request(sdkRequest)
            .build();
        ExecutionContext executionContext = ExecutionContext.builder()
            .interceptorContext(interceptorContext)
            .executionAttributes(executionAttributes)
            .build();
        context = RequestExecutionContext.builder()
            .executionContext(executionContext)
            .originalRequest(sdkRequest)
            .build();
    }

    @Test
    void execute_noAuthSchemes_returnsRequestUnchanged() throws Exception {
        // AUTH_SCHEMES is null
        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isNull();
    }

    @Test
    void execute_noResolver_returnsRequestUnchanged() throws Exception {
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        // AUTH_SCHEME_OPTIONS_RESOLVER is null

        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isNull();
    }

    @Test
    void execute_resolverReturnsEmpty_returnsRequestUnchanged() throws Exception {
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> Collections.emptyList());

        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isNull();
    }

    @Test
    void execute_resolverReceivesRequestFromInterceptorContext() throws Exception {
        SdkRequest modifiedRequest = mock(SdkRequest.class);
        
        // Setup interceptor context with a DIFFERENT request than originalRequest
        InterceptorContext interceptorContext = InterceptorContext.builder()
            .request(modifiedRequest)
            .build();
        ExecutionContext executionContext = ExecutionContext.builder()
            .interceptorContext(interceptorContext)
            .executionAttributes(executionAttributes)
            .build();
        context = RequestExecutionContext.builder()
            .executionContext(executionContext)
            .originalRequest(sdkRequest)  // Different from modifiedRequest
            .build();

        AuthSchemeOptionsResolver resolver = mock(AuthSchemeOptionsResolver.class);
        doReturn(createAuthOptions()).when(resolver).resolve(eq(modifiedRequest), any(ExecutionAttributes.class));

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER, resolver);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, createIdentityProviders());

        stage.execute(httpRequestBuilder, context);

        // Verify resolver was called with the MODIFIED request, not originalRequest
        verify(resolver).resolve(eq(modifiedRequest), any(ExecutionAttributes.class));
    }

    @Test
    void execute_withRequestIdentityProviderResolver_callsUpdaterWithRequest() throws Exception {
        // Create mocks first before any stubbing
        IdentityProvider<Identity> identityProvider = createMockIdentityProvider();
        Map<String, AuthScheme<?>> authSchemes = createAuthSchemes();
        IdentityProviders baseProviders = mock(IdentityProviders.class);
        IdentityProviders updatedProviders = mock(IdentityProviders.class);

        RequestIdentityProviderResolver resolver = mock(RequestIdentityProviderResolver.class);
        doReturn(updatedProviders).when(resolver).resolve(sdkRequest, baseProviders, executionAttributes);

        // Setup so that auth scheme uses the updated providers
        @SuppressWarnings("unchecked")
        AuthScheme<Identity> scheme = (AuthScheme<Identity>) authSchemes.get(SCHEME_ID);
        doReturn(identityProvider).when(scheme).identityProvider(updatedProviders);

        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> createAuthOptions());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, baseProviders);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_RESOLVER, resolver);

        stage.execute(httpRequestBuilder, context);

        verify(resolver).resolve(sdkRequest, baseProviders, executionAttributes);
    }

    @Test
    void execute_withoutRequestIdentityProviderResolver_doesNotFail() throws Exception {
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> createAuthOptions());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, createIdentityProviders());
        // No IDENTITY_PROVIDER_RESOLVER set

        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        assertThat(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)).isNotNull();
    }

    @Test
    void execute_happyPath_setsSelectedAuthScheme() throws Exception {
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> createAuthOptions());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, createIdentityProviders());

        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        SelectedAuthScheme<?> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(selectedAuthScheme).isNotNull();
        assertThat(selectedAuthScheme.authSchemeOption().schemeId()).isEqualTo(SCHEME_ID);
    }

    @SuppressWarnings("unchecked")
    private Map<String, AuthScheme<?>> createAuthSchemes() {
        IdentityProvider<Identity> identityProvider = createMockIdentityProvider();
        HttpSigner<Identity> signer = mock(HttpSigner.class);

        AuthScheme<Identity> scheme = mock(AuthScheme.class);
        doReturn(identityProvider).when(scheme).identityProvider(any());
        doReturn(signer).when(scheme).signer();

        Map<String, AuthScheme<?>> schemes = new HashMap<>();
        schemes.put(SCHEME_ID, scheme);
        return schemes;
    }

    @SuppressWarnings("unchecked")
    private IdentityProvider<Identity> createMockIdentityProvider() {
        IdentityProvider<Identity> provider = mock(IdentityProvider.class);
        Identity mockIdentity = mock(Identity.class);
        doReturn(CompletableFuture.completedFuture(mockIdentity))
            .when(provider).resolveIdentity(any(ResolveIdentityRequest.class));
        return provider;
    }

    private IdentityProviders createIdentityProviders() {
        IdentityProviders providers = mock(IdentityProviders.class);
        return providers;
    }

    private List<AuthSchemeOption> createAuthOptions() {
        return Collections.singletonList(
            AuthSchemeOption.builder().schemeId(SCHEME_ID).build()
        );
    }



    @Test
    void execute_authSchemeAlreadyResolved_skipsResolution() throws Exception {
        // Simulate old service interceptor already resolved auth scheme
        SelectedAuthScheme<Identity> alreadyResolved = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(mock(Identity.class)),
            mock(HttpSigner.class),
            AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build()
        );
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, alreadyResolved);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> createAuthOptions());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, createIdentityProviders());

        SdkHttpFullRequest.Builder result = stage.execute(httpRequestBuilder, context);

        assertThat(result).isSameAs(httpRequestBuilder);
        // Auth scheme should still be the one set by the old interceptor (not re-resolved)
        SelectedAuthScheme<?> finalScheme = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(finalScheme.authSchemeOption().schemeId()).isEqualTo("aws.auth#sigv4");
    }

    @Test
    void execute_authSchemeUnset_proceedsWithResolution() throws Exception {
        // "unset" placeholder — pipeline should resolve
        SelectedAuthScheme<Identity> unsetPlaceholder = new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(mock(Identity.class)),
            mock(HttpSigner.class),
            AuthSchemeOption.builder().schemeId("unset").build()
        );
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, unsetPlaceholder);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, createAuthSchemes());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
            (req, attrs) -> createAuthOptions());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, createIdentityProviders());

        stage.execute(httpRequestBuilder, context);

        // Should have resolved to the test scheme
        SelectedAuthScheme<?> finalScheme = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(finalScheme.authSchemeOption().schemeId()).isEqualTo(SCHEME_ID);
    }
}
