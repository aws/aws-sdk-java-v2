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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
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
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Validate that values set to {@link AwsSignerExecutionAttribute}s and {@link S3SignerExecutionAttribute}s from execution
 * interceptors are visible to {@link HttpSigner}s.
 */
public class ExecutionAttributeBackwardsCompatibilityTest {
    private static final AwsCredentials CREDS = AwsBasicCredentials.create("akid", "skid");

    @Test
    public void canSetSignerExecutionAttributes_beforeExecution() {
        test(attributeModifications -> new ExecutionInterceptor() {
                 @Override
                 public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                     attributeModifications.accept(executionAttributes);
                 }
             },
             AwsSignerExecutionAttribute.AWS_CREDENTIALS, // Identity resolution (modifyRequest) overrides credentials
             AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH, // Set in ConfigureSignerInterceptor (beforeExecution)
             S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, // Set in DisablePayloadSigningInterceptor (beforeExecution)
             AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, // Endpoint rules override signing name
             AwsSignerExecutionAttribute.SIGNING_REGION, // Endpoint rules override signing region
             AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE); // Endpoint rules override double-url-encode
    }

    @Test
    public void canSetSignerExecutionAttributes_modifyRequest() {
        test(attributeModifications -> new ExecutionInterceptor() {
             @Override
             public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                 attributeModifications.accept(executionAttributes);
                 return context.request();
            }
        });
    }

    @Test
    public void canSetSignerExecutionAttributes_beforeMarshalling() {
        test(attributeModifications -> new ExecutionInterceptor() {
            @Override
            public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                attributeModifications.accept(executionAttributes);
            }
        });
    }

    @Test
    public void canSetSignerExecutionAttributes_afterMarshalling() {
        test(attributeModifications -> new ExecutionInterceptor() {
            @Override
            public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
                attributeModifications.accept(executionAttributes);
            }
        });
    }

    @Test
    public void canSetSignerExecutionAttributes_modifyHttpRequest() {
        test(attributeModifications -> new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
                attributeModifications.accept(executionAttributes);
                return context.httpRequest();
            }
        });
    }

    private void test(Function<Consumer<ExecutionAttributes>, ExecutionInterceptor> interceptorFactory,
                      ExecutionAttribute<?>... attributesToExcludeFromTest) {
        Set<ExecutionAttribute<?>> attributesToExclude = new HashSet<>(Arrays.asList(attributesToExcludeFromTest));

        ExecutionInterceptor interceptor = interceptorFactory.apply(executionAttributes -> {
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "signing-name");
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.of("signing-region"));
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, CREDS);
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, true);
            executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH, true);
            executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, true);
            executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        });

        ClientOverrideConfiguration.Builder configBuilder =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(interceptor);

        try (MockSyncHttpClient httpClient = new MockSyncHttpClient();
             MockAsyncHttpClient asyncHttpClient = new MockAsyncHttpClient()) {
            stub200Responses(httpClient, asyncHttpClient);

            S3ClientBuilder s3Builder = createS3Builder(configBuilder, httpClient);
            S3AsyncClientBuilder s3AsyncBuilder = createS3AsyncBuilder(configBuilder, asyncHttpClient);

            CapturingAuthScheme authScheme1 = new CapturingAuthScheme();
            try (S3Client s3 = s3Builder.putAuthScheme(authScheme1)
                                        .build()) {
                callS3(s3);
                validateSignRequest(attributesToExclude, authScheme1);
            }

            CapturingAuthScheme authScheme2 = new CapturingAuthScheme();
            try (S3AsyncClient s3 = s3AsyncBuilder.putAuthScheme(authScheme2)
                                                  .build()) {
                callS3(s3);
                validateSignRequest(attributesToExclude, authScheme2);
            }
        }
    }

    private static void stub200Responses(MockSyncHttpClient httpClient, MockAsyncHttpClient asyncHttpClient) {
        HttpExecuteResponse response =
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder()
                                                        .statusCode(200)
                                                        .build())
                               .build();
        httpClient.stubResponses(response);
        asyncHttpClient.stubResponses(response);
    }

    private static S3ClientBuilder createS3Builder(ClientOverrideConfiguration.Builder configBuilder, MockSyncHttpClient httpClient) {
        return S3Client.builder()
                       .region(Region.US_WEST_2)
                       .credentialsProvider(AnonymousCredentialsProvider.create())
                       .httpClient(httpClient)
                       .overrideConfiguration(configBuilder.build());
    }

    private static S3AsyncClientBuilder createS3AsyncBuilder(ClientOverrideConfiguration.Builder configBuilder, MockAsyncHttpClient asyncHttpClient) {
        return S3AsyncClient.builder()
                            .region(Region.US_WEST_2)
                            .credentialsProvider(AnonymousCredentialsProvider.create())
                            .httpClient(asyncHttpClient)
                            .overrideConfiguration(configBuilder.build());
    }

    private static void callS3(S3Client s3) {
        s3.putObject(r -> r.bucket("foo")
                           .key("bar")
                           .checksumAlgorithm(ChecksumAlgorithm.CRC32),
                     RequestBody.fromString("text"));
    }

    private void callS3(S3AsyncClient s3) {
        s3.putObject(r -> r.bucket("foo")
                           .key("bar")
                           .checksumAlgorithm(ChecksumAlgorithm.CRC32),
                     AsyncRequestBody.fromString("text"))
          .join();
    }

    private void validateSignRequest(Set<ExecutionAttribute<?>> attributesToExclude, CapturingAuthScheme authScheme) {
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME))
                .isEqualTo("signing-name");
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNING_REGION)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4HttpSigner.REGION_NAME))
                .isEqualTo("signing-region");
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.AWS_CREDENTIALS)) {
            assertThat(authScheme.signer.signRequest.identity())
                .isEqualTo(CREDS);
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE))
                .isEqualTo(true);
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4FamilyHttpSigner.NORMALIZE_PATH))
                .isEqualTo(true);
        }
        if (!attributesToExclude.contains(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED))
                .isEqualTo(true);
        }
        if (!attributesToExclude.contains(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING)) {
            assertThat(authScheme.signer.signRequest.property(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED))
                .isEqualTo(true);
        }
    }

    private static class CapturingAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        private final CapturingHttpSigner signer = new CapturingHttpSigner();

        @Override
        public String schemeId() {
            return AwsV4AuthScheme.SCHEME_ID;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            return signer;
        }
    }

    private static class CapturingHttpSigner implements HttpSigner<AwsCredentialsIdentity> {
        private BaseSignRequest<?, ? extends AwsCredentialsIdentity> signRequest;

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            this.signRequest = request;
            return SignedRequest.builder()
                                .request(request.request())
                                .payload(request.payload().orElse(null))
                                .build();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            this.signRequest = request;
            return CompletableFuture.completedFuture(AsyncSignedRequest.builder()
                                                                       .request(request.request())
                                                                       .payload(request.payload().orElse(null))
                                                                       .build());
        }
    }
}
