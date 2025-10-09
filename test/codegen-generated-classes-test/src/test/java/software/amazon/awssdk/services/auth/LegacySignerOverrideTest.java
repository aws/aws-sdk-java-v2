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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
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
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.MultiauthAsyncClient;
import software.amazon.awssdk.services.multiauth.MultiauthClient;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeProvider;
import software.amazon.awssdk.services.multiauth.endpoints.MultiauthEndpointProvider;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.testutil.ValidSdkObjects;

/**
 * Tests to ensure that parameters set when endpoint and auth-scheme resolution occurs get propagated to the legacy signer (i.e.
 * pre-SRA signers).
 */
public class LegacySignerOverrideTest {
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
    public void syncClient_oldSignerOverriddenInExecutionInterceptor_takesPrecedence() {
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
        MultiauthClient client = MultiauthClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner);
    }

    @Test
    void v4EndpointAuthSchemeAsync_signerOverride_endpointParamsShouldPropagateToSigner() {
        MultiauthAsyncClient client = MultiauthAsyncClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4SignerAttributes(mockSigner);
    }

    @Test
    void v4aEndpointAuthSchemeSync_signerOverride_thenEndpointParamsShouldPropagateToSigner() {
        MultiauthClient client = MultiauthClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, mockSigner)
                      .addExecutionInterceptor(interceptor))
            .build();


        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> {
        })).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner);
    }

    @Test
    void v4aEndpointAuthSchemeAsync_signerOverride_thenEndpointParamsShouldPropagateToSigner() {
        MultiauthAsyncClient client = MultiauthAsyncClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, mockSigner)
                      .addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> {
        }).join()).hasMessageContaining("boom!");
        verifySigV4aSignerAttributes(mockSigner);
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

    private static void verifySigV4SignerAttributes(Signer signer) {
        ArgumentCaptor<SdkHttpFullRequest> httpRequest = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(signer).sign(httpRequest.capture(), attributes.capture());

        ExecutionAttributes actualAttributes = attributes.getValue();
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()).isEqualTo("us-west-1");
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("query-test-v4");
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isFalse();
    }

    private static void verifySigV4aSignerAttributes(Signer signer) {
        ArgumentCaptor<SdkHttpFullRequest> httpRequest = ArgumentCaptor.forClass(SdkHttpFullRequest.class);
        ArgumentCaptor<ExecutionAttributes> attributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(signer).sign(httpRequest.capture(), attributes.capture());

        ExecutionAttributes actualAttributes = attributes.getValue();

        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE).id()).isEqualTo("us-east-1");
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("query-test-v4a");
        assertThat(actualAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isFalse();
    }

    private static MultiauthAuthSchemeProvider v4AuthSchemeProviderOverride() {
        return x -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "query-will-be-overridden")
                                .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "region-will-be-overridden")
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static MultiauthAuthSchemeProvider v4aAuthSchemeProviderOverride() {
        return i -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "query-will-be-overridden")
                                .putSignerProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create("region-will-be-overridden"))
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static MultiauthEndpointProvider v4EndpointProviderOverride() {
        return i -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4.query.us-west-1"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4AuthScheme.builder()
                                                                     .signingRegion("us-west-1")
                                                                     .signingName("query-test-v4")
                                                                     .disableDoubleEncoding(true)
                                                                     .build()))
                        .build();

            return CompletableFuture.completedFuture(endpoint);
        };
    }

    private static MultiauthEndpointProvider v4aEndpointProviderOverride() {
        return x -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4a.query.us-east-1"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4aAuthScheme.builder()
                                                                      .addSigningRegion("us-east-1")
                                                                      .signingName("query-test-v4a")
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
