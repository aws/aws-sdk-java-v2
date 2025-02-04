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

package software.amazon.awssdk.services.endpointauth;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.SdkHttpClient;
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
class EndpointAuthSigningPropertiesTest {

    private static final String MOCK_HTTP_CLIENT_NAME = "MockHttpClient";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "expected exception";

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @Mock
    private SdkHttpClient mockHttpClient;

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

    private EndpointAuthClientBuilder createEndpointAuthClient() {
        return EndpointAuthClient.builder()
                                 .httpClient(mockHttpClient)
                                 .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("akid", "skid")))
                                 .region(Region.US_WEST_2);
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

    @Nested
    @DisplayName("Region Set Configuration Tests")
    class RegionSetConfigurationTests {

        @Test
        @DisplayName("Should use environment variable region set when provided")
        void shouldNotUseRegionSetForOperationWhichIsNotSigv4a() {
            environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");
            CapturingSigner signer = new CapturingSigner();

            EndpointAuthClient client = createEndpointAuthClient()
                .putAuthScheme(authScheme("aws.auth#sigv4", signer))
                .build();

            assertThatThrownBy(() -> client.noSigv4aPropertiesInEndpointRules(r -> r.stringMember("")))
                .hasMessageContaining("stop");
            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isNull();
        }

        @Test
        @DisplayName("Should fall back to client region when no environment variable is set")
        void shouldFallBackToClientRegion() {
            CapturingSigner signer = new CapturingSigner();
            EndpointAuthClient client = createEndpointAuthClient()
                .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
                .build();

            assertThatThrownBy(() -> client.regionsetAbsentInSigv4aPropertiesInEndpointRules(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.create(Region.US_WEST_2.toString()));
        }

        @Test
        @DisplayName("Should use RegionSet as defined in the endpoint rule set when no region set defined on client")
        void authSchemePickedFromEndpointParamsAndNotFromClientConfig() {
            environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");
            CapturingSigner signer = new CapturingSigner();

            EndpointAuthClient client = createEndpointAuthClient()
                .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
                .build();

            assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            RegionSet property = signer.request.property(AwsV4aHttpSigner.REGION_SET);

            assertThat(property)
                .isEqualTo(RegionSet.create("us-seattle,us-west-2"));
        }

        @Test
        @DisplayName("Should use from configurations from env variable if EndpointAuth Property not present")
        void envVariableConfiguredRegionSetTakePrecedenceOverEndpointRulesRegionSet() {
            environmentVariableHelper.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, "us-west-2,us-west-1");

            CapturingSigner signer = new CapturingSigner();
            EndpointAuthClient client = createEndpointAuthClient()
                .putAuthScheme(authScheme("aws.auth#sigv4a", signer))
                .build();

            assertThatThrownBy(() -> client.regionsetAbsentInSigv4aPropertiesInEndpointRules(r -> r.stringMember("")))
                .hasMessageContaining("stop");

            assertThat(signer.request.property(AwsV4aHttpSigner.REGION_SET))
                .isEqualTo(RegionSet.create("us-west-1, us-west-2"));
        }
    }
}