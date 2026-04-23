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

package software.amazon.awssdk.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
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
import software.amazon.awssdk.services.endpointauth.EndpointAuthAsyncClient;
import software.amazon.awssdk.services.endpointauth.EndpointAuthClient;
import software.amazon.awssdk.services.endpointauth.auth.scheme.EndpointAuthAuthSchemeProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Functional tests verifying that an {@code AuthSchemeProvider} set on the request-level override configuration
 * takes precedence over the client-level auth scheme provider.
 */
@DisplayName("Request-Override AuthSchemeProvider Tests")
class RequestOverrideAuthSchemeProviderTest {

    private static final String SIGNING_NAME_FROM_CLIENT = "client-signing-name";
    private static final String SIGNING_NAME_FROM_REQUEST = "request-signing-name";
    private static final String REGION_FROM_CLIENT = "us-west-2";
    private static final String REGION_FROM_REQUEST = "eu-west-1";

    @Mock
    private SdkHttpClient mockHttpClient;

    @Mock
    private SdkAsyncHttpClient mockAsyncHttpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockHttpClient.clientName()).thenReturn("MockHttpClient");
        when(mockHttpClient.prepareRequest(any()))
            .thenThrow(new RuntimeException("stop"));
        when(mockAsyncHttpClient.clientName()).thenReturn("MockAsyncHttpClient");
        when(mockAsyncHttpClient.execute(any()))
            .thenThrow(new RuntimeException("stop"));
    }

    @Test
    @DisplayName("Sync: request-level authSchemeProvider overrides client-level provider")
    void sync_requestOverrideAuthSchemeProvider_takesPrecedence() {
        CapturingSigner signer = new CapturingSigner();

        EndpointAuthClient client = EndpointAuthClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .putAuthScheme(authScheme(AwsV4AuthScheme.SCHEME_ID, signer))
            .authSchemeProvider(clientAuthSchemeProvider())
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r
            .stringMember("")
            .overrideConfiguration(c -> c.authSchemeProvider(requestAuthSchemeProvider()))
        )).hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION_FROM_REQUEST);
        assertThat(signer.request.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME_FROM_REQUEST);
    }

    @Test
    @DisplayName("Sync: without request-level override, client-level authSchemeProvider is used")
    void sync_noRequestOverride_usesClientAuthSchemeProvider() {
        CapturingSigner signer = new CapturingSigner();

        EndpointAuthClient client = EndpointAuthClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .putAuthScheme(authScheme(AwsV4AuthScheme.SCHEME_ID, signer))
            .authSchemeProvider(clientAuthSchemeProvider())
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r.stringMember("")))
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION_FROM_CLIENT);
        assertThat(signer.request.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME_FROM_CLIENT);
    }

    @Test
    @DisplayName("Async: request-level authSchemeProvider overrides client-level provider")
    void async_requestOverrideAuthSchemeProvider_takesPrecedence() {
        CapturingSigner signer = new CapturingSigner();

        EndpointAuthAsyncClient client = EndpointAuthAsyncClient.builder()
            .httpClient(mockAsyncHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .putAuthScheme(authScheme(AwsV4AuthScheme.SCHEME_ID, signer))
            .authSchemeProvider(clientAuthSchemeProvider())
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r
            .stringMember("")
            .overrideConfiguration(c -> c.authSchemeProvider(requestAuthSchemeProvider()))
        ).join()).hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION_FROM_REQUEST);
        assertThat(signer.request.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME_FROM_REQUEST);
    }

    @Test
    @DisplayName("Async: without request-level override, client-level authSchemeProvider is used")
    void async_noRequestOverride_usesClientAuthSchemeProvider() {
        CapturingSigner signer = new CapturingSigner();

        EndpointAuthAsyncClient client = EndpointAuthAsyncClient.builder()
            .httpClient(mockAsyncHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .putAuthScheme(authScheme(AwsV4AuthScheme.SCHEME_ID, signer))
            .authSchemeProvider(clientAuthSchemeProvider())
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r.stringMember("")).join())
            .hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION_FROM_CLIENT);
        assertThat(signer.request.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME_FROM_CLIENT);
    }

    @Test
    @DisplayName("Sync: request-level authSchemeProvider set to null falls back to client-level")
    void sync_requestOverrideNull_fallsBackToClientProvider() {
        CapturingSigner signer = new CapturingSigner();

        EndpointAuthClient client = EndpointAuthClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .putAuthScheme(authScheme(AwsV4AuthScheme.SCHEME_ID, signer))
            .authSchemeProvider(clientAuthSchemeProvider())
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r
            .stringMember("")
            .overrideConfiguration(c -> c.authSchemeProvider(null))
        )).hasMessageContaining("stop");

        assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(REGION_FROM_CLIENT);
        assertThat(signer.request.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME_FROM_CLIENT);
    }

    /**
     * Client-level auth scheme provider that sets signing properties to "client" values.
     */
    private static EndpointAuthAuthSchemeProvider clientAuthSchemeProvider() {
        return params -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder()
                    .schemeId(AwsV4AuthScheme.SCHEME_ID)
                    .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME_FROM_CLIENT)
                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, REGION_FROM_CLIENT)
                    .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    /**
     * Request-level auth scheme provider that sets signing properties to "request" values.
     */
    private static EndpointAuthAuthSchemeProvider requestAuthSchemeProvider() {
        return params -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder()
                    .schemeId(AwsV4AuthScheme.SCHEME_ID)
                    .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME_FROM_REQUEST)
                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, REGION_FROM_REQUEST)
                    .build()
            );
            return Collections.unmodifiableList(options);
        };
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

    /**
     * A signer that captures the sign request for later assertion, then throws to stop execution.
     */
    static class CapturingSigner implements HttpSigner<AwsCredentialsIdentity> {
        volatile BaseSignRequest<?, ?> request;

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            this.request = request;
            throw new RuntimeException("stop");
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(
            AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            this.request = request;
            return CompletableFutureUtils.failedFuture(new RuntimeException("stop"));
        }
    }
}
