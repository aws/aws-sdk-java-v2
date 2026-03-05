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

package software.amazon.awssdk.core.internal.http.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

class AuthSchemeResolverTest {

    private static final String SCHEME_A = "schemeA";
    private static final String SCHEME_B = "schemeB";

    @Test
    void selectAuthScheme_firstOptionSucceeds_returnsFirstScheme() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);

        List<AuthSchemeOption> options = Collections.singletonList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_A);
    }

    @Test
    void selectAuthScheme_firstOptionNoScheme_fallsBackToSecond() {
        AuthScheme<Identity> schemeB = createMockAuthScheme();
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_firstOptionNoIdentityProvider_fallsBackToSecond() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        when(schemeA.identityProvider(any())).thenReturn(null);

        AuthScheme<Identity> schemeB = createMockAuthScheme();

        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_signerThrows_fallsBackToSecond() {
        AuthScheme<Identity> schemeA = createMockAuthScheme();
        when(schemeA.signer()).thenThrow(new RuntimeException("Signer not available"));

        AuthScheme<Identity> schemeB = createMockAuthScheme();

        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();
        authSchemes.put(SCHEME_A, schemeA);
        authSchemes.put(SCHEME_B, schemeB);

        List<AuthSchemeOption> options = Arrays.asList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build(),
            AuthSchemeOption.builder().schemeId(SCHEME_B).build()
        );

        SelectedAuthScheme<?> result = AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null);

        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_B);
    }

    @Test
    void selectAuthScheme_allOptionsFail_throwsException() {
        Map<String, AuthScheme<?>> authSchemes = new HashMap<>();

        List<AuthSchemeOption> options = Collections.singletonList(
            AuthSchemeOption.builder().schemeId(SCHEME_A).build()
        );

        assertThatThrownBy(() -> AuthSchemeResolver.selectAuthScheme(
            options, authSchemes, mock(IdentityProviders.class), null))
            .isInstanceOf(SdkException.class)
            .hasMessageContaining("Failed to determine how to authenticate");
    }

    @Test
    void mergeProperties_noExistingScheme_returnsOriginal() {
        SelectedAuthScheme<Identity> selected = createSelectedAuthScheme(SCHEME_A);
        ExecutionAttributes attributes = new ExecutionAttributes();

        SelectedAuthScheme<Identity> result = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(
            selected, attributes);

        assertThat(result).isSameAs(selected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeProperties_withExistingScheme_returnsNewInstance() {
        SelectedAuthScheme<Identity> selected = createSelectedAuthScheme(SCHEME_A);
        SelectedAuthScheme<Identity> existing = createSelectedAuthScheme(SCHEME_B);

        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, existing);

        SelectedAuthScheme<Identity> result = AuthSchemeResolver.mergePreExistingAuthSchemeProperties(
            selected, attributes);

        assertThat(result).isNotSameAs(selected);
        assertThat(result.authSchemeOption().schemeId()).isEqualTo(SCHEME_A);
    }

    @SuppressWarnings("unchecked")
    private AuthScheme<Identity> createMockAuthScheme() {
        AuthScheme<Identity> scheme = mock(AuthScheme.class);
        IdentityProvider<Identity> identityProvider = mock(IdentityProvider.class);
        Identity mockIdentity = mock(Identity.class);
        doReturn(CompletableFuture.completedFuture(mockIdentity))
            .when(identityProvider).resolveIdentity(any(ResolveIdentityRequest.class));
        when(scheme.identityProvider(any())).thenReturn(identityProvider);
        when(scheme.signer()).thenReturn(mock(HttpSigner.class));
        return scheme;
    }

    @SuppressWarnings("unchecked")
    private SelectedAuthScheme<Identity> createSelectedAuthScheme(String schemeId) {
        Identity mockIdentity = mock(Identity.class);
        HttpSigner<Identity> mockSigner = mock(HttpSigner.class);
        return new SelectedAuthScheme<>(
            CompletableFuture.completedFuture(mockIdentity),
            mockSigner,
            AuthSchemeOption.builder().schemeId(schemeId).build()
        );
    }
}
