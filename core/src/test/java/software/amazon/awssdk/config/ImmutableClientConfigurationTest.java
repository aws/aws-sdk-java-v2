/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * Validate the functionality of {@link ImmutableClientConfiguration}.
 */
@SuppressWarnings("deprecation") // Intentional use of deprecated class
public class ImmutableClientConfigurationTest {
    private static final NoOpSignerProvider SIGNER_PROVIDER = new NoOpSignerProvider();
    private static final RequestHandler REQUEST_HANDLER = new RequestHandler() {
    };
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER = new DefaultCredentialsProvider();
    private static final URI ENDPOINT = URI.create("https://www.example.com");
    private static final RetryPolicy RETRY_POLICY = new RetryPolicy(null, null, 10, true);
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final SdkHttpClient SYNC_HTTP_CLIENT = mock(SdkHttpClient.class);
    private static final SdkAsyncHttpClient ASYNC_HTTP_CLIENT = mock(SdkAsyncHttpClient.class);

    private static final LegacyClientConfiguration EXPECTED_LEGACY_CONFIGURATION =
            new LegacyClientConfiguration()
                    .withHeader("header", "value")
                    .withClientExecutionTimeout(4_000)
                    .withGzip(true)
                    .withUserAgentPrefix("userAgentPrefix")
                    .withUserAgentSuffix("userAgentSuffix")
                    .withRetryPolicy(RETRY_POLICY)
                    .withProtocol(Protocol.HTTPS);

    private static final AwsSyncClientParams EXPECT_SYNC_CLIENT_PARAMS = new AwsSyncClientParams() {
        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return EXPECTED_LEGACY_CONFIGURATION;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return RequestMetricCollector.NONE;
        }

        @Override
        public List<RequestHandler> getRequestHandlers() {
            return Collections.singletonList(REQUEST_HANDLER);
        }

        @Override
        public SignerProvider getSignerProvider() {
            return SIGNER_PROVIDER;
        }

        @Override
        public URI getEndpoint() {
            return ENDPOINT;
        }

        @Override
        public SdkHttpClient sdkHttpClient() {
            return SYNC_HTTP_CLIENT;
        }
    };

    private static final AwsAsyncClientParams EXPECT_ASYNC_CLIENT_PARAMS = new AwsAsyncClientParams() {
        @Override
        public AwsCredentialsProvider getCredentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public LegacyClientConfiguration getClientConfiguration() {
            return EXPECTED_LEGACY_CONFIGURATION;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return RequestMetricCollector.NONE;
        }

        @Override
        public List<RequestHandler> getRequestHandlers() {
            return Collections.singletonList(REQUEST_HANDLER);
        }

        @Override
        public SignerProvider getSignerProvider() {
            return SIGNER_PROVIDER;
        }

        @Override
        public URI getEndpoint() {
            return ENDPOINT;
        }

        @Override
        public ScheduledExecutorService getExecutor() {
            return EXECUTOR_SERVICE;
        }

        @Override
        public SdkHttpClient sdkHttpClient() {
            return SYNC_HTTP_CLIENT;
        }
    };

    @Test
    public void syncParamsTranslationShouldBeCorrect() {
        ImmutableSyncClientConfiguration config = new ImmutableSyncClientConfiguration(new InitializedSyncConfiguration());
        AwsSyncClientParams legacySyncParams = config.asLegacySyncClientParams();
        assertSyncParamsMatch(EXPECT_SYNC_CLIENT_PARAMS, legacySyncParams);
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, legacySyncParams.getClientConfiguration());
    }

    @Test
    public void asyncParamsTranslationShouldBeCorrect() {
        ImmutableAsyncClientConfiguration config = new ImmutableAsyncClientConfiguration(new InitializedAsyncConfiguration());
        AwsAsyncClientParams legacyAsyncParams = config.asLegacyAsyncClientParams();
        assertAsyncParamsMatch(EXPECT_ASYNC_CLIENT_PARAMS, legacyAsyncParams);
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, legacyAsyncParams.getClientConfiguration());
    }

    private void assertAsyncParamsMatch(AwsAsyncClientParams expected, AwsAsyncClientParams given) {
        assertSyncParamsMatch(expected, given);
        assertThat(expected.getExecutor()).isEqualTo(given.getExecutor());
    }

    private void assertSyncParamsMatch(AwsSyncClientParams expected, AwsSyncClientParams given) {
        assertThat(expected.getCredentialsProvider()).isEqualTo(given.getCredentialsProvider());
        assertThat(expected.getEndpoint()).isEqualTo(given.getEndpoint());
        assertThat(expected.getRequestHandlers()).isEqualTo(given.getRequestHandlers());
        assertThat(expected.getRequestMetricCollector()).isEqualTo(given.getRequestMetricCollector());
        assertThat(expected.getSignerProvider()).isEqualTo(given.getSignerProvider());
        assertLegacyConfigurationMatches(EXPECTED_LEGACY_CONFIGURATION, given.getClientConfiguration());
    }

    private void assertLegacyConfigurationMatches(LegacyClientConfiguration expected,
                                                  LegacyClientConfiguration given) {
        assertThat(given).isEqualToIgnoringGivenFields(expected, "apacheHttpClientConfig");
    }

    private static class InitializedSyncConfiguration extends InitializedConfiguration implements SyncClientConfiguration {

        @Override
        public SdkHttpClient httpClient() {
            return SYNC_HTTP_CLIENT;
        }
    }

    private static class InitializedAsyncConfiguration extends InitializedConfiguration implements AsyncClientConfiguration {
        @Override
        public ScheduledExecutorService asyncExecutorService() {
            return EXECUTOR_SERVICE;
        }

        @Override
        public SdkAsyncHttpClient asyncHttpClient() {
            return ASYNC_HTTP_CLIENT;
        }
    }

    private static class InitializedConfiguration implements ClientConfiguration {

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return ClientOverrideConfiguration.builder()
                                              .httpRequestTimeout(Duration.ofSeconds(2))
                                              .totalExecutionTimeout(Duration.ofSeconds(4))
                                              .gzipEnabled(true)
                                              .addAdditionalHttpHeader("header", "value")
                                              .requestMetricCollector(RequestMetricCollector.NONE)
                                              .advancedOption(AdvancedClientOption.USER_AGENT_PREFIX, "userAgentPrefix")
                                              .advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX, "userAgentSuffix")
                                              .advancedOption(AdvancedClientOption.SIGNER_PROVIDER, SIGNER_PROVIDER)
                                              .retryPolicy(RETRY_POLICY)
                                              .addRequestListener(REQUEST_HANDLER)
                                              .build();
        }

        @Override
        public AwsCredentialsProvider credentialsProvider() {
            return CREDENTIALS_PROVIDER;
        }

        @Override
        public URI endpoint() {
            return ENDPOINT;
        }

    }
}
