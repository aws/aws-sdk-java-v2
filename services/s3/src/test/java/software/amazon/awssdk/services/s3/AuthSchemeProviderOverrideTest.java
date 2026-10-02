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

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

/**
 * Verifies that request-level plugin overrides for auth scheme provider and region
 * are respected during auth scheme resolution.
 */
class AuthSchemeProviderOverrideTest {

    @Test
    void requestPluginOverridesAuthSchemeProvider_isUsed() {
        AtomicInteger defaultProviderCallCount = new AtomicInteger(0);
        AtomicInteger overrideProviderCallCount = new AtomicInteger(0);

        S3AuthSchemeProvider defaultProvider = params -> {
            defaultProviderCallCount.incrementAndGet();
            return S3AuthSchemeProvider.defaultProvider().resolveAuthScheme(params);
        };

        S3AuthSchemeProvider overrideProvider = params -> {
            overrideProviderCallCount.incrementAndGet();
            return S3AuthSchemeProvider.defaultProvider().resolveAuthScheme(params);
        };

        S3Client client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("akid", "skid")))
            .addPlugin(authSchemeProviderPlugin(defaultProvider))
            .overrideConfiguration(c -> c.addExecutionInterceptor(new AbortInterceptor()))
            .build();

        // Request with plugin that overrides auth scheme provider
        assertThatThrownBy(() -> client.getObject(r -> {
            r.overrideConfiguration(c -> c.addPlugin(authSchemeProviderPlugin(overrideProvider)));
            r.key("key").bucket("bucket");
        })).hasMessageContaining("aborted");

        // The override provider should have been called, not the default
        assertThat(overrideProviderCallCount.get()).isGreaterThan(0);
        assertThat(defaultProviderCallCount.get()).isEqualTo(0);
    }

    @Test
    void noRequestPluginOverride_usesClientProvider() {
        AtomicInteger defaultProviderCallCount = new AtomicInteger(0);

        S3AuthSchemeProvider defaultProvider = params -> {
            defaultProviderCallCount.incrementAndGet();
            return S3AuthSchemeProvider.defaultProvider().resolveAuthScheme(params);
        };

        S3Client client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("akid", "skid")))
            .addPlugin(authSchemeProviderPlugin(defaultProvider))
            .overrideConfiguration(c -> c.addExecutionInterceptor(new AbortInterceptor()))
            .build();

        // Request without override — should use client-level provider
        assertThatThrownBy(() -> client.getObject(r -> {
            r.key("key").bucket("bucket");
        })).hasMessageContaining("aborted");

        assertThat(defaultProviderCallCount.get()).isGreaterThan(0);
    }

    @Test
    void requestPluginOverridesRegion_isUsedInAuthSchemeResolution() {
        AtomicReference<Region> capturedRegion = new AtomicReference<>();

        S3AuthSchemeProvider capturingProvider = params -> {
            capturedRegion.set(params.region());
            return S3AuthSchemeProvider.defaultProvider().resolveAuthScheme(params);
        };

        S3Client client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("akid", "skid")))
            .authSchemeProvider(capturingProvider)
            .overrideConfiguration(c -> c.addExecutionInterceptor(new AbortInterceptor()))
            .build();

        // Request with plugin that overrides region to us-west-2
        assertThatThrownBy(() -> client.getObject(r -> {
            r.overrideConfiguration(c -> c.addPlugin(regionOverridePlugin(Region.US_WEST_2)));
            r.key("key").bucket("bucket");
        })).hasMessageContaining("aborted");

        // The auth scheme params should have received the overridden region
        assertThat(capturedRegion.get()).isEqualTo(Region.US_WEST_2);
    }

    private SdkPlugin regionOverridePlugin(Region region) {
        return config -> {
            S3ServiceClientConfiguration.Builder s3Config =
                (S3ServiceClientConfiguration.Builder) config;
            s3Config.region(region);
        };
    }

    private SdkPlugin authSchemeProviderPlugin(S3AuthSchemeProvider provider) {
        return config -> {
            S3ServiceClientConfiguration.Builder s3Config =
                (S3ServiceClientConfiguration.Builder) config;
            s3Config.authSchemeProvider(provider);
        };
    }

    /**
     * Interceptor that aborts the request before transmission so we don't need a real endpoint.
     */
    static class AbortInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            throw new RuntimeException("aborted");
        }
    }
}
