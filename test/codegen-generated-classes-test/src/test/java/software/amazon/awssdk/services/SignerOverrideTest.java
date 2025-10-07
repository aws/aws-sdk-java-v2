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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.testutil.ValidSdkObjects;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

@RunWith(MockitoJUnitRunner.class)
public class SignerOverrideTest {
    @Mock
    public Signer mockSigner;

    @Mock
    public static AwsV4HttpSigner mockHttpSigner;

    @Mock
    public SignedRequest signedRequest;
    @Mock
    public AsyncSignedRequest asyncSignedRequest;

    @BeforeEach
    public void setup() {
        SdkHttpRequest sdkHttpRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        Publisher<ByteBuffer> signedPayload = AsyncRequestBody.fromString("signed async request body");

        when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(SignedRequest.builder().build());

        CompletableFuture<AsyncSignedRequest> requestFuture = new CompletableFuture<>();
        requestFuture.complete(asyncSignedRequest);
        when(mockHttpSigner.signAsync(any(AsyncSignRequest.class)))
            .thenReturn(
                CompletableFuture.completedFuture(AsyncSignedRequest.builder()
                                                                    .request(sdkHttpRequest)
                                                                    .payload(signedPayload)
                                                                    .build()));
    }

    /**
     * Test to ensure that operations that use the {@link software.amazon.awssdk.auth.signer.AsyncAws4Signer} don't apply
     * the override when the signer is overridden by the customer.
     */
    @Test
    public void test_signerOverriddenForStreamingInput_takesPrecedence() {
        ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
                .build();

        try {
            asyncClient.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                    AsyncRequestBody.fromString("test")).join();
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void asyncClient_oldSignerOverriddenInExecutionInterceptor_takesPrecedence() {
        try (ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                                                  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                                  .region(Region.US_WEST_2)
                                                                                  .endpointOverride(URI.create("http://localhost:8080"))
                                                                                  .overrideConfiguration(o -> o.addExecutionInterceptor(signerOverrideExecutionInterceptor(mockSigner)))
                                                                                  .build()) {
            asyncClient.allTypes(AllTypesRequest.builder().build()).join();
        } catch (Exception expected) {
            // Doesn't matter if the request succeeds or not
        }

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void syncClient_oldSignerOverriddenInExecutionInterceptor_takesPrecedence() {
        try (ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                   .region(Region.US_WEST_2)
                                                                   .endpointOverride(URI.create("http://localhost:8080"))
                                                                   .overrideConfiguration(o -> o.addExecutionInterceptor(signerOverrideExecutionInterceptor(mockSigner)))
                                                                   .build()) {
            client.allTypes(AllTypesRequest.builder().build());
        } catch (Exception expected) {
            // Doesn't matter if the request succeeds or not
        }

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    public void sync_httpSignerOverride_takesPrecedence() {
        try (ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                   .region(Region.US_WEST_2)
                                                                   .putAuthScheme(new MockAuthScheme())
                                                                   .build()) {

            assertThatThrownBy(() -> client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                                                    RequestBody.fromString("test"))).isInstanceOf(NullPointerException.class);
            verify(mockHttpSigner).sign(any(SignRequest.class));
        }
    }

    @Test
    public void async_httpSignerOverride_takesPrecedence() {
        try(ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                                             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                             .region(Region.US_WEST_2)
                                                                             .putAuthScheme(new MockAuthScheme())
                                                                             .build()) {
            assertThatThrownBy(() -> asyncClient.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                                AsyncRequestBody.fromString("test")).join()).hasRootCauseInstanceOf(NullPointerException.class);
        }
        verify(mockHttpSigner).signAsync(any(AsyncSignRequest.class));
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

    private static class MockAuthScheme implements AwsV4AuthScheme {
        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public AwsV4HttpSigner signer() {
            return mockHttpSigner;
        }

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }
    }
}
