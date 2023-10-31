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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryAsyncClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.auth.scheme.ProtocolQueryAuthSchemeProvider;
import software.amazon.awssdk.services.protocolquery.endpoints.ProtocolQueryEndpointProvider;

/**
 * Tests to ensure that parameters set when endpoint and auth-scheme resolution occurs get propagated to the overriden
 * signer (i.e. pre-SRA signers).
 */
public class SignerAndEndpointOverridesTest {

    public Signer mockSigner = mock(Signer.class);

    @Test
    public void test_whenV4EndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner() {
        ProtocolQueryClient client = ProtocolQueryClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .region(Region.US_WEST_2)
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
            .build();

        try {
            client.streamingInputOperation(r -> {}, RequestBody.fromString("test"));
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(
            any(SdkHttpFullRequest.class),
            argThat(attrs ->
                        "us-west-2".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()) &&
                        "query-test-v4".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) &&
                        !attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE))
        );
    }

    @Test
    public void testAsync_whenV4EndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner() {
        ProtocolQueryAsyncClient client = ProtocolQueryAsyncClient
            .builder()
            .authSchemeProvider(v4AuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4EndpointProviderOverride())
            .region(Region.US_WEST_2)
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
            .build();

        try {
            client.streamingInputOperation(r -> {}, AsyncRequestBody.fromString("test")).join();
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(
            any(SdkHttpFullRequest.class),
            argThat(attrs ->
                        "us-west-2".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()) &&
                        "query-test-v4".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) &&
                        !attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE))
        );
    }

    // TODO(sra-identity-and-auth): Enable this test once an auth-scheme override is respected when resolving the auth-scheme -
    //  the V4 tests pass right now only because the V4 auth-scheme is the default for this client
    @Disabled("Expected to fail as of now.")
    @Test
    public void test_whenV4aEndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner() {
        ProtocolQueryClient client = ProtocolQueryClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .region(Region.US_EAST_1)
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
            .build();

        try {
            client.streamingInputOperation(r -> {}, RequestBody.fromString("test"));
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(
            any(SdkHttpFullRequest.class),
            argThat(attrs ->
                        "us-east-1".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()) &&
                        "query-test-v4a".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) &&
                        !attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE))
        );
    }

    // TODO(sra-identity-and-auth): Enable this test once an auth-scheme override is respected when resolving the auth-scheme -
    //  the V4 tests pass right now only because the V4 auth-scheme is the default for this client
    @Disabled("Expected to fail as of now.")
    @Test
    public void testAsync_whenV4aEndpointAuthSchemeWithSignerOverride_thenEndpointParamsShouldPropagateToSigner() {
        ProtocolQueryAsyncClient client = ProtocolQueryAsyncClient
            .builder()
            .authSchemeProvider(v4aAuthSchemeProviderOverride())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .endpointProvider(v4aEndpointProviderOverride())
            .region(Region.US_EAST_1)
            .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
            .build();

        try {
            client.streamingInputOperation(r -> {}, AsyncRequestBody.fromString("test")).join();
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(
            any(SdkHttpFullRequest.class),
            argThat(attrs ->
                        "us-east-1".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id()) &&
                        "query-test-v4a".equals(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) &&
                        !attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE))
        );
    }

    private static ProtocolQueryAuthSchemeProvider v4AuthSchemeProviderOverride() {
        return __ -> {
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
        return __ -> {
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
        return __ -> {
            Endpoint endpoint =
                Endpoint.builder()
                        .url(URI.create("https://testv4.query.us-west-2"))
                        .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Collections.singletonList(SigV4AuthScheme.builder()
                                                                     .signingRegion("us-west-2")
                                                                     .signingName("query-test-v4")
                                                                     .disableDoubleEncoding(true)
                                                                     .build()))
                        .build();

            return CompletableFuture.completedFuture(endpoint);
        };
    }

    private static ProtocolQueryEndpointProvider v4aEndpointProviderOverride() {
        return __ -> {
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
}
