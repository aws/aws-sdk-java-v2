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

package software.amazon.awssdk.services.kinesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Verifies the HTTP defaults Kinesis clients pass to an SDK-managed HTTP client: the SDK-wide read timeout, and the
 * write timeout override from {@link software.amazon.awssdk.services.kinesis.internal.KinesisHttpConfigurationOptions}.
 */
public class KinesisHttpDefaultsTest {

    @Test
    public void syncClient_usesGlobalReadTimeout_andKinesisWriteTimeout() {
        SdkHttpClient.Builder<?> httpClientBuilder = mock(SdkHttpClient.Builder.class);
        when(httpClientBuilder.buildWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));

        KinesisClient.builder()
                     .region(Region.US_EAST_1)
                     .credentialsProvider(AnonymousCredentialsProvider.create())
                     .httpClientBuilder(httpClientBuilder)
                     .build();

        ArgumentCaptor<AttributeMap> serviceDefaults = ArgumentCaptor.forClass(AttributeMap.class);
        verify(httpClientBuilder).buildWithDefaults(serviceDefaults.capture());
        AttributeMap resolved = serviceDefaults.getValue().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

        assertThat(resolved.get(SdkHttpConfigurationOption.READ_TIMEOUT)).isEqualTo(Duration.ofSeconds(30));
        assertThat(resolved.get(SdkHttpConfigurationOption.WRITE_TIMEOUT)).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void asyncClient_usesGlobalReadTimeout_andKinesisWriteTimeout_andKeepsHttp2() {
        SdkAsyncHttpClient.Builder<?> httpClientBuilder = mock(SdkAsyncHttpClient.Builder.class);
        when(httpClientBuilder.buildWithDefaults(any())).thenReturn(mock(SdkAsyncHttpClient.class));

        KinesisAsyncClient.builder()
                          .region(Region.US_EAST_1)
                          .credentialsProvider(AnonymousCredentialsProvider.create())
                          .httpClientBuilder(httpClientBuilder)
                          .build();

        ArgumentCaptor<AttributeMap> serviceDefaults = ArgumentCaptor.forClass(AttributeMap.class);
        verify(httpClientBuilder).buildWithDefaults(serviceDefaults.capture());
        AttributeMap resolved = serviceDefaults.getValue().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);

        assertThat(resolved.get(SdkHttpConfigurationOption.READ_TIMEOUT)).isEqualTo(Duration.ofSeconds(30));
        assertThat(resolved.get(SdkHttpConfigurationOption.WRITE_TIMEOUT)).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.get(SdkHttpConfigurationOption.PROTOCOL)).isEqualTo(Protocol.HTTP2);
    }
}
