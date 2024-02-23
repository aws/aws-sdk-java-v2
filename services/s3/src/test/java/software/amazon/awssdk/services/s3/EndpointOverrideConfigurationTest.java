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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.utils.Validate;

class EndpointOverrideConfigurationTest {

    private CapturingInterceptor interceptor;
    private CallCountS3EndpointProvider callCountS3EndpointProvider;


    @BeforeEach
    void setup() {
        this.interceptor = new CapturingInterceptor();
        this.callCountS3EndpointProvider = null;
    }

    @Test
    void testClientEndpointOverrideIsUsed() {
        S3Client syncClient = syncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)))
            .addPlugin(overrideEndpointPlugin())
            .build();
        assertThatThrownBy(() -> syncClient.getObject(r -> {
            r.key("key").bucket("bucket");
        }))
            .hasMessageContaining("boom!");

        assertThat(callCountS3EndpointProvider).isNotNull();
        assertThat(callCountS3EndpointProvider.getCallCount()).isEqualTo(2);
    }

    @Test
    void testRequestEndpointOverrideIsUsed() {
        S3Client syncClient = syncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)))
            .build();
        assertThatThrownBy(() -> syncClient.getObject(r -> {
            r.overrideConfiguration(c -> c.addPlugin(overrideEndpointPlugin()));
            r.key("key").bucket("bucket");
        }))
            .hasMessageContaining("boom!");

        assertThat(callCountS3EndpointProvider).isNotNull();
        assertThat(callCountS3EndpointProvider.getCallCount()).isEqualTo(2);
    }

    @Test
    void testAsyncClientEndpointOverrideIsUsed() {
        S3AsyncClient syncClient = asyncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)))
            .addPlugin(overrideEndpointPlugin())
            .build();
        assertThatThrownBy(() -> syncClient.getObject(r -> {
            r.key("key").bucket("bucket");
        }, AsyncResponseTransformer.toBytes()).join())
            .hasMessageContaining("boom!");

        assertThat(callCountS3EndpointProvider).isNotNull();
        assertThat(callCountS3EndpointProvider.getCallCount()).isEqualTo(2);
    }

    @Test
    void testAsyncRequestEndpointOverrideIsUsed() {
        S3AsyncClient syncClient = asyncClientBuilder()
            .addPlugin(config -> config.overrideConfiguration(c -> c.addExecutionInterceptor(interceptor)))
            .addPlugin(overrideEndpointPlugin())
            .build();
        assertThatThrownBy(() -> syncClient.getObject(r -> {
            r.overrideConfiguration(c -> c.addPlugin(overrideEndpointPlugin()));
            r.key("key").bucket("bucket");
        }, AsyncResponseTransformer.toBytes()).join())
            .hasMessageContaining("boom!");

        assertThat(callCountS3EndpointProvider).isNotNull();
        assertThat(callCountS3EndpointProvider.getCallCount()).isEqualTo(2);
    }

    private SdkPlugin overrideEndpointPlugin() {
        return config -> {
            EndpointProvider provider = config.endpointProvider();
            if (provider instanceof S3EndpointProvider) {
                S3EndpointProvider s3EndpointProvider = (S3EndpointProvider) provider;
                config.endpointProvider(endpointProvider(s3EndpointProvider));
            }
        };
    }

    private S3ClientBuilder syncClientBuilder() {
        return S3Client.builder()
                       .addPlugin(c -> {
                           S3ServiceClientConfiguration.Builder config =
                               Validate.isInstanceOf(S3ServiceClientConfiguration.Builder.class, c,
                                                     "\uD83E\uDD14");
                           config.region(Region.US_WEST_2)
                                 .credentialsProvider(
                                     StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("akid", "skid")))
                                 .overrideConfiguration(oc -> oc.addExecutionInterceptor(interceptor));
                       });

    }

    private S3AsyncClientBuilder asyncClientBuilder() {
        return S3AsyncClient.builder()
                       .addPlugin(c -> {
                           S3ServiceClientConfiguration.Builder config =
                               Validate.isInstanceOf(S3ServiceClientConfiguration.Builder.class, c,
                                                     "\uD83E\uDD14");
                           config.region(Region.US_WEST_2)
                                 .credentialsProvider(
                                     StaticCredentialsProvider.create(
                                         AwsBasicCredentials.create("akid", "skid")))
                                 .overrideConfiguration(oc -> oc.addExecutionInterceptor(interceptor));
                       });

    }

    public S3EndpointProvider endpointProvider(S3EndpointProvider delegate) {
        this.callCountS3EndpointProvider = new CallCountS3EndpointProvider(delegate);
        return this.callCountS3EndpointProvider;
    }

    static class CallCountS3EndpointProvider implements S3EndpointProvider {
        private final S3EndpointProvider delegate;
        private int callCount;

        CallCountS3EndpointProvider(S3EndpointProvider delegate) {
            this.delegate = Validate.paramNotNull(delegate, "delegate");
        }

        public int getCallCount() {
            return callCount;
        }

        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams) {
            callCount++;
            return delegate.resolveEndpoint(endpointParams);
        }
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
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

        public Context.BeforeTransmission context() {
            return context;
        }

        public class CaptureCompletedException extends RuntimeException {
            CaptureCompletedException(String message) {
                super(message);
            }
        }
    }

}
