/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.config.AwsAsyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsImmutableAsyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsImmutableSyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsMutableClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsSyncClientConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.SdkImmutableClientConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.core.signer.NoOpSigner;

/**
 * Validate the functionality of {@link SdkImmutableClientConfiguration}.
 */
@SuppressWarnings("deprecation") // Intentional use of deprecated class
public class AwsImmutableClientConfigurationTest {
    private static final NoOpSigner TEST_SIGNER = new NoOpSigner();
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();
    private static final URI ENDPOINT = URI.create("https://www.example.com");
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final SdkHttpClient SYNC_HTTP_CLIENT = mock(SdkHttpClient.class);
    private static final SdkAsyncHttpClient ASYNC_HTTP_CLIENT = mock(SdkAsyncHttpClient.class);
    private static final ExecutionInterceptor EXECUTION_INTERCEPTOR = new ExecutionInterceptor() {
    };

    private static final RetryPolicy RETRY_POLICY = RetryPolicy.builder()
                                                               .retryCondition(condition -> false)
                                                               .backoffStrategy(strategy -> Duration.ZERO)
                                                               .build();

    @Test
    public void immutableSyncConfigurationMatchesMutableConfiguration() {
        assertThat(new AwsImmutableSyncClientConfiguration(initializedSyncConfiguration()))
            .isEqualToComparingFieldByFieldRecursively(initializedSyncConfiguration());
    }

    @Test
    public void immutableAsyncConfigurationMatchesMutableConfiguration() {
        assertThat(new AwsImmutableAsyncClientConfiguration(initializedAsyncConfiguration()))
            .isEqualToComparingFieldByFieldRecursively(initializedAsyncConfiguration());
    }

    private AwsAsyncClientConfiguration initializedAsyncConfiguration() {
        return new AwsMutableClientConfiguration().overrideConfiguration(initializedOverrideConfiguration())
                                                  .credentialsProvider(CREDENTIALS_PROVIDER)
                                                  .endpoint(ENDPOINT)
                                                  .asyncExecutorService(EXECUTOR_SERVICE)
                                                  .asyncHttpClient(ASYNC_HTTP_CLIENT);
    }

    private AwsSyncClientConfiguration initializedSyncConfiguration() {
        return new AwsMutableClientConfiguration().overrideConfiguration(initializedOverrideConfiguration())
                                                  .credentialsProvider(CREDENTIALS_PROVIDER)
                                                  .endpoint(ENDPOINT)
                                                  .httpClient(SYNC_HTTP_CLIENT);
    }

    private ClientOverrideConfiguration initializedOverrideConfiguration() {
        return ClientOverrideConfiguration.builder()
                                          .gzipEnabled(true)
                                          .addAdditionalHttpHeader("header", "value")
                                          .advancedOption(AwsAdvancedClientOption.USER_AGENT_PREFIX, "userAgentPrefix")
                                          .advancedOption(AwsAdvancedClientOption.USER_AGENT_SUFFIX, "userAgentSuffix")
                                          .advancedOption(AwsAdvancedClientOption.SIGNER, TEST_SIGNER)
                                          .advancedOption(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION, false)
                                          .retryPolicy(RETRY_POLICY)
                                          .addExecutionInterceptor(EXECUTION_INTERCEPTOR)
                                          .build();
    }
}
