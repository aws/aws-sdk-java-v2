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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.defaultendpointprovider.DefaultEndpointProviderAsyncClient;
import software.amazon.awssdk.services.defaultendpointprovider.DefaultEndpointProviderClient;
import software.amazon.awssdk.services.defaultendpointprovider.auth.scheme.DefaultEndpointProviderAuthSchemeProvider;
import software.amazon.awssdk.services.endpointauth.EndpointAuthAsyncClient;
import software.amazon.awssdk.services.endpointauth.EndpointAuthClient;
import software.amazon.awssdk.services.endpointauth.endpoints.EndpointAuthEndpointProvider;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.sigv4aauth.Sigv4AauthAsyncClient;
import software.amazon.awssdk.services.sigv4aauth.Sigv4AauthClient;
import software.amazon.awssdk.services.sigv4aauth.auth.scheme.Sigv4AauthAuthSchemeProvider;
import software.amazon.awssdk.services.testutil.ValidSdkObjects;

/**
 * Tests to ensure that parameters set on either endpoints-based (legacy) or model-based auth schemes get
 * propagated to the legacy signer (i.e., pre-SRA signers).
 */
public class LegacySignerOverrideTest {
    private static final String REGION_FROM_EP = "region-from-ep";
    private static final String SIGNING_NAME_FROM_EP = "signing-name-from-ep";
    private static final String REGION_FROM_SERVICE = "region-from-service";
    private static final String SIGNING_NAME_FROM_SERVICE = "signing-name-from-service";

    private Signer mockSigner;

    private FailRequestInterceptor interceptor = new FailRequestInterceptor();

    @BeforeEach
    public void setup() {
        mockSigner = Mockito.mock(Signer.class);
        when(mockSigner.sign(any(), any())).thenReturn(ValidSdkObjects.sdkHttpFullRequest().build());
    }

    @Test
    public void asyncClient_signerOverriddenInConfig_takesPrecedence() {
        ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                                             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                             .region(Region.US_WEST_2)
                                                                             .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
                                                                             .build();

        assertThatThrownBy(() -> asyncClient.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                                                     AsyncRequestBody.fromString("test")).join()).hasMessageContaining("boom!");

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void asyncClient_signerOverriddenInExecutionInterceptor_takesPrecedence() {
        ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                                             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                             .region(Region.US_WEST_2)
                                                                             .overrideConfiguration(o -> o.addExecutionInterceptor(signerOverrideExecutionInterceptor(mockSigner)).addExecutionInterceptor(interceptor))
                                                                             .build();
        assertThatThrownBy(() -> asyncClient.allTypes(AllTypesRequest.builder().build()).join()).hasMessageContaining("boom!");
        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void syncClient_signerOverriddenInExecutionInterceptor_takesPrecedence() {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                              .region(Region.US_WEST_2)
                                                              .overrideConfiguration(o -> o.addExecutionInterceptor(signerOverrideExecutionInterceptor(mockSigner)).addExecutionInterceptor(interceptor))
                                                              .build();
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build())).hasMessageContaining("boom!");

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void syncClient_signerOverriddenInConfig_takesPrecedence() {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                              .region(Region.US_WEST_2)
                                                              .overrideConfiguration(o -> o.addExecutionInterceptor(signerOverrideExecutionInterceptor(mockSigner)).addExecutionInterceptor(interceptor))
                                                              .build();
        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build())).hasMessageContaining("boom!");

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    void v4EndpointAuthSchemeSync_signerOverride_endpointParamsShouldPropagateToSigner() {
        EndpointAuthClient client = EndpointAuthClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4EndpointAuthSchemeAsync_signerOverride_endpointParamsShouldPropagateToSigner() {
        EndpointAuthAsyncClient client = EndpointAuthAsyncClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4aEndpointAuthSchemeSync_signerOverride_thenEndpointParamsShouldPropagateToSigner() {
        EndpointAuthClient client = EndpointAuthClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, mockSigner)
                      .addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4aEndpointAuthSchemeAsync_signerOverride_thenEndpointParamsShouldPropagateToSigner() {
        EndpointAuthAsyncClient client = EndpointAuthAsyncClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, mockSigner)
                      .addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allAuthPropertiesInEndpointRules(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4ModelAuthSync_signerOverride_signerPropertiesShouldPropagateToSigner() {
        DefaultEndpointProviderClient client = DefaultEndpointProviderClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.oneOperation(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner, AuthType.MODEL);
    }

    @Test
    void v4BothAuthSync_signerOverride_endpointSignerPropertiesShouldPropagateToSigner() {
        DefaultEndpointProviderClient client = DefaultEndpointProviderClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .endpointProvider(x -> {
                Endpoint endpoint =
                    Endpoint.builder()
                            .url(URI.create("https://testv4.query.us-east-1"))
                            .putAttribute(
                                AwsEndpointAttribute.AUTH_SCHEMES,
                                Collections.singletonList(SigV4AuthScheme.builder()
                                                                          .signingRegion(REGION_FROM_EP)
                                                                          .signingName(SIGNING_NAME_FROM_EP)
                                                                          .disableDoubleEncoding(true)
                                                                          .build()))
                            .build();

            return CompletableFuture.completedFuture(endpoint);
            })
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.oneOperation(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4ModelAuthAsync_signerOverride_signerPropertiesShouldPropagateToSigner() {
        DefaultEndpointProviderAsyncClient client = DefaultEndpointProviderAsyncClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.oneOperation(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner, AuthType.MODEL);
    }

    // TODO: fix the logic, tracking in JAVA-8567
    @Disabled("regionSet from EP should be getting used")
    @Test
    void v4aBothAuthProviderAndEndpointAuth_signerOverride_endpointSignerPropertiesShouldPropagateToSigner() {
        Sigv4AauthClient client = Sigv4AauthClient
            .builder()
            .authSchemeProvider(i -> {
                List<AuthSchemeOption> options = new ArrayList<>();
                options.add(
                    AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                    .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME_FROM_SERVICE)
                                    .putSignerProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create(REGION_FROM_SERVICE))
                                    .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false)
                                    .build()
                );
                return Collections.unmodifiableList(options);
            })
            .endpointProvider(x -> {
                Endpoint endpoint =
                    Endpoint.builder()
                            .url(URI.create("https://testv4a.query.us-east-1"))
                            .putAttribute(
                                AwsEndpointAttribute.AUTH_SCHEMES,
                                Collections.singletonList(SigV4aAuthScheme.builder()
                                                                          .signingRegionSet(Arrays.asList(REGION_FROM_EP))
                                                                          .signingName(SIGNING_NAME_FROM_EP)
                                                                          .disableDoubleEncoding(true)
                                                                          .build()))
                            .build();

                return CompletableFuture.completedFuture(endpoint);
            })
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.simpleOperationWithNoEndpointParams(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner, AuthType.EP);
    }

    @Test
    void v4aModelAuthSync_signerOverride_signerPropertiesShouldPropagateToSigner() {
        Sigv4AauthClient client = Sigv4AauthClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))

            .build();

        assertThatThrownBy(() -> client.simpleOperationWithNoEndpointParams(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner, AuthType.MODEL);
    }

    @Test
    void v4aModelAuthAsync_signerOverride_signerPropertiesShouldPropagateToSigner() {
        Sigv4AauthAsyncClient client = Sigv4AauthAsyncClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))

            .build();

        assertThatThrownBy(() -> client.simpleOperationWithNoEndpointParams(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner, AuthType.MODEL);
    }

    private ExecutionInterceptor signerOverrideExecutionInterceptor(Signer signer) {
        return new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                AwsRequest.Builder builder = (AwsRequest.Builder) context.request().toBuilder();
                builder.overrideConfiguration(c -> c.signer(signer)
                                                    .build());

                return builder.build();
            }
        };
    }

    private static void verifySigV4SignerAttributes(Signer signer, AuthType authType) {
        ArgumentCaptor<SdkHttpFullRequest> httpRequest = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(signer).sign(httpRequest.capture(), attributes.capture());

        ExecutionAttributes actualAttributes = attributes.getValue();
        String expectedRegion;
        String expectedSigningName;
        switch (authType) {
            case EP:
                expectedRegion = REGION_FROM_EP;
                expectedSigningName = SIGNING_NAME_FROM_EP;
                break;
            case MODEL:
                expectedRegion = REGION_FROM_SERVICE;
                expectedSigningName = SIGNING_NAME_FROM_SERVICE;
                break;
            default:
                throw new UnsupportedOperationException("unsupported auth type " + authType);
        }

        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()).isEqualTo(expectedRegion);
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo(expectedSigningName);
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isFalse();
    }

    private static void verifySigV4aSignerAttributes(Signer signer, AuthType authType) {
        ArgumentCaptor<SdkHttpFullRequest> httpRequest = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(signer).sign(httpRequest.capture(), attributes.capture());

        ExecutionAttributes actualAttributes = attributes.getValue();
        String expectedRegion;
        String expectedSigningName;
        switch (authType) {
            case EP:
                expectedRegion = REGION_FROM_EP;
                expectedSigningName = SIGNING_NAME_FROM_EP;
                break;
            case MODEL:
                expectedRegion = REGION_FROM_SERVICE;
                expectedSigningName = SIGNING_NAME_FROM_SERVICE;
                break;
            default:
                throw new UnsupportedOperationException("unsupported auth type " + authType);
        }

        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE).id()).isEqualTo(expectedRegion);
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo(expectedSigningName);
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isFalse();
    }

    private enum AuthType {
        EP,
        MODEL
    }

    private static DefaultEndpointProviderAuthSchemeProvider v4AuthSchemeProviderOverride() {
        return x -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID)
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME_FROM_SERVICE)
                                .putSignerProperty(AwsV4HttpSigner.REGION_NAME, REGION_FROM_SERVICE)
                                .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false)
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static Sigv4AauthAuthSchemeProvider v4aAuthSchemeProviderOverride() {
        return i -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME_FROM_SERVICE)
                                .putSignerProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create(REGION_FROM_SERVICE))
                                .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false)
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static EndpointAuthEndpointProvider v4EndpointProviderOverride() {
        return i -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4.query.us-west-1"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4AuthScheme.builder()
                                                                     .signingRegion(REGION_FROM_EP)
                                                                     .signingName(SIGNING_NAME_FROM_EP)
                                                                     .disableDoubleEncoding(true)
                                                                     .build()))
                        .build();

            return CompletableFuture.completedFuture(endpoint);
        };
    }

    private static EndpointAuthEndpointProvider v4aEndpointProviderOverride() {
        return x -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4a.query.us-east-1"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4aAuthScheme.builder()
                                                                      .addSigningRegion(REGION_FROM_EP)
                                                                      .signingName(SIGNING_NAME_FROM_EP)
                                                                      .disableDoubleEncoding(true)
                                                                      .build()))
                        .build();

            return CompletableFuture.completedFuture(endpoint);
        };
    }

    private static EndpointAuthEndpointProvider sigv4aAuthEndpointProvider() {
        return x -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4a.query.us-east-1"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4aAuthScheme.builder()
                                                                      .addSigningRegion(REGION_FROM_EP)
                                                                      .signingName(SIGNING_NAME_FROM_EP)
                                                                      .disableDoubleEncoding(true)
                                                                      .build()))
                        .build();

            return CompletableFuture.completedFuture(endpoint);
        };
    }

    private static class FailRequestInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("boom!");
        }
    }
}
