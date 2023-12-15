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
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Ensure that attributes set in execution interceptors are passed to custom signers. These are protected APIs, but code
 * searches show that customers are using them as if they aren't. We should push customers onto supported paths.
 */
public class InterceptorSignerAttributeTest {
    private static final AwsCredentials CREDS = AwsBasicCredentials.create("akid", "skid");

    @Test
    public void canSetSignerExecutionAttributes_beforeExecution() {
        test(attributeModifications -> new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                attributeModifications.accept(executionAttributes);
            }
        },
             AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, // Endpoint rules override signing name
             AwsSignerExecutionAttribute.SIGNING_REGION, // Endpoint rules override signing region
             AwsSignerExecutionAttribute.AWS_CREDENTIALS, // Legacy auth strategy overrides credentials
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
        },
             AwsSignerExecutionAttribute.AWS_CREDENTIALS); // Legacy auth strategy overrides credentials
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
        });

        ClientOverrideConfiguration.Builder configBuilder =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(interceptor);

        try (MockSyncHttpClient httpClient = new MockSyncHttpClient();
             MockAsyncHttpClient asyncHttpClient = new MockAsyncHttpClient()) {
            stub200Responses(httpClient, asyncHttpClient);

            S3ClientBuilder s3Builder = createS3Builder(configBuilder, httpClient);
            S3AsyncClientBuilder s3AsyncBuilder = createS3AsyncBuilder(configBuilder, asyncHttpClient);

            CapturingSigner signer1 = new CapturingSigner();
            try (S3Client s3 = s3Builder.overrideConfiguration(configBuilder.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                               signer1)
                                                                            .build())
                                        .build()) {
                callS3(s3);
                validateLegacySignRequest(attributesToExclude, signer1);
            }

            CapturingSigner signer2 = new CapturingSigner();
            try (S3AsyncClient s3 =
                     s3AsyncBuilder.overrideConfiguration(configBuilder.putAdvancedOption(SdkAdvancedClientOption.SIGNER, signer2)
                                                                       .build())
                                   .build()) {
                callS3(s3);
                validateLegacySignRequest(attributesToExclude, signer2);
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

    private void validateLegacySignRequest(Set<ExecutionAttribute<?>> attributesToExclude, CapturingSigner signer) {
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)) {
            assertThat(signer.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME))
                .isEqualTo("signing-name");
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNING_REGION)) {
            assertThat(signer.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION))
                .isEqualTo(Region.of("signing-region"));
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.AWS_CREDENTIALS)) {
            assertThat(signer.executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))
                .isEqualTo(CREDS);
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)) {
            assertThat(signer.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE))
                .isEqualTo(true);
        }
        if (!attributesToExclude.contains(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH)) {
            assertThat(signer.executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH))
                .isEqualTo(true);
        }
    }

    private static class CapturingSigner implements Signer {
        private ExecutionAttributes executionAttributes;

        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes.copy();
            return request;
        }
    }
}
