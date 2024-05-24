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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeParams;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeProvider;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.internal.ProtocolRestJsonAuthSchemeInterceptor;

public class AuthSchemeInterceptorTest {
    private static final ProtocolRestJsonAuthSchemeInterceptor INTERCEPTOR = new ProtocolRestJsonAuthSchemeInterceptor();

    private Context.ModifyRequest mockContext;

    @BeforeEach
    public void setup() {
        mockContext = mock(Context.ModifyRequest.class);
    }

    @Test
    public void resolveAuthScheme_authSchemeSignerThrows_continuesToNextAuthScheme() {
        ProtocolRestJsonAuthSchemeProvider mockAuthSchemeProvider = mock(ProtocolRestJsonAuthSchemeProvider.class);
        List<AuthSchemeOption> authSchemeOptions = Arrays.asList(
            AuthSchemeOption.builder().schemeId(TestAuthScheme.SCHEME_ID).build(),
            AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()
        );
        when(mockAuthSchemeProvider.resolveAuthScheme(any(ProtocolRestJsonAuthSchemeParams.class))).thenReturn(authSchemeOptions);

        IdentityProviders mockIdentityProviders = mock(IdentityProviders.class);
        when(mockIdentityProviders.identityProvider(any(Class.class))).thenReturn(AnonymousCredentialsProvider.create());

        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(AwsV4AuthScheme.SCHEME_ID, AwsV4AuthScheme.create());

        TestAuthScheme notProvidedAuthScheme = spy(new TestAuthScheme());
        authSchemes.put(TestAuthScheme.SCHEME_ID, notProvidedAuthScheme);

        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "GetFoo");
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER, mockAuthSchemeProvider);
        attributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, mockIdentityProviders);
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes);

        INTERCEPTOR.modifyRequest(mockContext, attributes);

        SelectedAuthScheme<?> selectedAuthScheme = attributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        verify(notProvidedAuthScheme).signer();
        assertThat(selectedAuthScheme.authSchemeOption().schemeId()).isEqualTo(AwsV4AuthScheme.SCHEME_ID);
    }

    private static class TestAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        public static final String SCHEME_ID = "codegen-test-scheme";

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            throw new RuntimeException("Not on classpath");
        }
    }
}
