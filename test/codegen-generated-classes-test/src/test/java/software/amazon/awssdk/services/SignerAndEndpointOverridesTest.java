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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryAsyncClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.auth.scheme.ProtocolQueryAuthSchemeProvider;
import software.amazon.awssdk.services.protocolquery.endpoints.ProtocolQueryEndpointProvider;

/**
 * Tests to ensure that parameters set when endpoint and auth-scheme resolution occurs get propagated to the overriden
 * signer (i.e. pre-SRA signers). These tests also test that a different type of signer from the auth-scheme still has params
 * propagated to it.
 */
public class SignerAndEndpointOverridesTest {

    private CapturingInterceptor interceptor;

    @BeforeEach
    void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @ParameterizedTest
    @MethodSource("provideSigners")
    void test_whenV4EndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner(Signer signer) {
        ProtocolQueryClient client = ProtocolQueryClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, signer).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("boom!");
        assertEquals("us-west-1", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id());
        assertEquals("query-test-v4", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        assertEquals(Boolean.FALSE, interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
    }

    @ParameterizedTest
    @MethodSource("provideSigners")
    void testAsync_whenV4EndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner(Signer signer) {
        ProtocolQueryAsyncClient client = ProtocolQueryAsyncClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, signer).addExecutionInterceptor(interceptor))
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {}).join()).hasMessageContaining("boom!");
        assertEquals("us-west-1", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id());
        assertEquals("query-test-v4", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        assertEquals(Boolean.FALSE, interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
    }

    @ParameterizedTest
    @MethodSource("provideSigners")
    void test_whenV4aEndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner(Signer signer) {
        ProtocolQueryClient client = ProtocolQueryClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, signer)
                      .addExecutionInterceptor(interceptor)
                      .putExecutionAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, Collections.singletonMap(
                          "aws.auth#sigv4a", AwsV4aAuthScheme.create()
                      )))
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("boom!");
        assertEquals("us-east-1", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE).id());
        assertEquals("query-test-v4a",
                     interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        assertEquals(Boolean.FALSE, interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
    }

    @ParameterizedTest
    @MethodSource("provideSigners")
    void testAsync_whenV4aEndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner(Signer signer) {
        ProtocolQueryAsyncClient client = ProtocolQueryAsyncClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .overrideConfiguration(
                o -> o.putAdvancedOption(SIGNER, signer)
                      .addExecutionInterceptor(interceptor)
                      .putExecutionAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, Collections.singletonMap(
                          "aws.auth#sigv4a", AwsV4aAuthScheme.create()
                      )))
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {}).join()).hasMessageContaining("boom!");
        assertEquals("us-east-1", interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE).id());
        assertEquals("query-test-v4a",
                     interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        assertEquals(Boolean.FALSE, interceptor.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
    }

    private static List<Signer> provideSigners() {
        return Arrays.asList(Aws4Signer.create(), AwsCrtV4aSigner.create());
    }

    private static ProtocolQueryAuthSchemeProvider v4AuthSchemeProviderOverride() {
        return x -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "query-will-be-overriden")
                                .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "region-will-be-overriden")
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static ProtocolQueryAuthSchemeProvider v4aAuthSchemeProviderOverride() {
        return x -> {
            List<AuthSchemeOption> options = new ArrayList<>();
            options.add(
                AuthSchemeOption.builder().schemeId("aws.auth#sigv4a")
                                .putSignerProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "query-will-be-overriden")
                                .putSignerProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create("region-will-be-overriden"))
                                .build()
            );
            return Collections.unmodifiableList(options);
        };
    }

    private static ProtocolQueryEndpointProvider v4EndpointProviderOverride() {
        return x -> {
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

    private static ProtocolQueryEndpointProvider v4aEndpointProviderOverride() {
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

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("boom!");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }

        public class CaptureCompletedException extends RuntimeException {
            CaptureCompletedException(String message) {
                super(message);
            }
        }
    }
}
