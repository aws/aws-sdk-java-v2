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

package software.amazon.awssdk.services.multiauth;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
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
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@DisplayName("Multi-Auth Tests")
class MultiAuthSigningPropertiesTest {

    private static final String MOCK_HTTP_CLIENT_NAME = "MockHttpClient";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "expected exception";
    private static final String CRT_DEPENDENCY_ERROR_MESSAGE =
        "You must add a dependency on the 'software.amazon.awssdk:http-auth-aws-crt' module to enable the CRT-V4a signing feature";

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @Mock
    private SdkHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockHttpClient.clientName()).thenReturn(MOCK_HTTP_CLIENT_NAME);
        when(mockHttpClient.prepareRequest(any())).thenThrow(new RuntimeException(EXPECTED_EXCEPTION_MESSAGE));
    }

    @AfterEach
    void tearDown() {
        environmentVariableHelper.reset();
    }


    @Nested
    @DisplayName("Region Set Configuration Tests")
    class RegionSetConfigurationTests {

        @Test
        @DisplayName("Should use environment variable region set when provided")
        void shouldUseEnvironmentVariableRegionSet() {
            environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");
            CapturingSigner signer = new CapturingSigner();

            MultiauthClient client = createMultiauthClient(signer);

            assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.create(Arrays.asList("us-west-2", "us-west-1")));
        }

        @Test
        @DisplayName("Should fall back to client region when no environment variable is set")
        void shouldFallBackToClientRegion() {
            CapturingSigner signer = new CapturingSigner();
            MultiauthClient client = createMultiauthClient(signer);

            assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.create(Region.US_WEST_2.toString()));
        }

        @Test
        @DisplayName("Should use GLOBAL Regionset as defined in the endpoint rule set when no region set defined on client")
        void endpointParamsDefinedAsGlobalUsedWhenNoRegionSetConfigured() {
            CapturingSigner signer = new CapturingSigner();

            MultiauthClient client = createMultiauthClient(signer);

            assertThatThrownBy(() -> client.multiAuthWithRegionSetInEndpointParams(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.GLOBAL);
        }

        @Test
        @DisplayName("Should use the Region set from Endpoint RuleSet when no RegionSet configured")
        void clientApiConfiguredRegionSetTakePrecedenceOverEndpointRulesRegionSet() {
            CapturingSigner signer = new CapturingSigner();
            MultiauthClient client = MultiauthClient.builder()
                                                    .httpClient(mockHttpClient)
                                                    .region(Region.US_WEST_2)
                                                    .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
                                                    .sigv4aRegionSet(RegionSet.create(new StringJoiner(",")
                                                                                          .add(Region.US_WEST_2.id())
                                                                                          .add(Region.US_GOV_EAST_1.id())
                                                                                          .toString()))
                                                    .build();

            assertThatThrownBy(() -> client.multiAuthWithRegionSetInEndpointParams(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.create("us-west-2,us-gov-east-1"));
        }
    }

    @Nested
    @DisplayName("Fallback Behavior Tests")
    class FallbackBehaviorTests {

        @Test
        @DisplayName("Should throw error when Sigv4a has no fallback to Sigv4")
        void shouldThrowErrorWhenNoFallback() {
            MultiauthClient client = MultiauthClient.builder()
                                                    .httpClient(mockHttpClient)
                                                    .region(Region.US_WEST_2)
                                                    .build();

            assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> r.stringMember("")))
                .hasMessageContaining(CRT_DEPENDENCY_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should fall back to Sigv4 when Sigv4a is not available")
        void shouldFallBackToSigv4() {
            MultiauthClient client = createMultiauthClient(null);

            assertThatThrownBy(() -> client.multiAuthWithOnlySigv4aAndSigv4(r -> r.stringMember("")))
                .hasMessageContaining(EXPECTED_EXCEPTION_MESSAGE);

            verify(mockHttpClient).prepareRequest(
                argThat(request -> request.httpRequest().firstMatchingHeader("Authorization").isPresent()));
        }
    }

    @Nested
    @DisplayName("Region Configuration Tests")
    class RegionConfigurationTests {
        @Test
        @DisplayName("Endpoint Rules Auth Scheme Region take highest precedence ")
        void authSchemesParamsUpdatedWithStaticContextAndDefaultEndpointParams() {
            environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");

            CapturingSigner signer = new CapturingSigner();

            MultiauthClient multiauthClient = MultiauthClient.builder()
                                                             .httpClient(mockHttpClient)
                                                             .putAuthScheme(authScheme("aws.auth#sigv4", signer))
                                                             .region(Region.EU_CENTRAL_1)
                                                             .build();

            Assertions.assertThatThrownBy(() -> multiauthClient.legacySigv4WithEndpointsRules(r -> r.stringMember("")))
                      .hasMessageContaining("stop");

            Assertions.assertThat(signer.request.property(AwsV4HttpSigner.REGION_NAME)).isEqualTo(Region.US_GOV_EAST_1.id());
        }
    }


    private MultiauthClient createMultiauthClient(CapturingSigner signer) {
        MultiauthClientBuilder builder = MultiauthClient.builder()
                                                       .httpClient(mockHttpClient)
                                                       .region(Region.US_WEST_2);

        if (signer != null) {
            builder.putAuthScheme(authScheme("aws.auth#sigv4a", signer));
        }

        return builder.build();
    }



    public static class CapturingSigner implements HttpSigner<AwsCredentialsIdentity> {
        private BaseSignRequest<?, ?> request;

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

}