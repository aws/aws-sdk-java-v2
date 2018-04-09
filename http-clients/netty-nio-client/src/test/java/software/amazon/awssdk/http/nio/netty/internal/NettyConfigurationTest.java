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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;

import java.time.Duration;
import java.util.function.Consumer;
import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyConfigurationTest {

    @Test
    public void connectionAcquistion_ReturnsMillis() {
        NettyConfiguration configuration = createConfiguration(b -> b.connectionAcquisitionTimeout(Duration.ofSeconds(1)));
        assertThat(configuration.connectionAcquisitionTimeout()).isEqualTo(1000);
    }

    @Test
    public void connectionAcquisition_PerformsSaturatedCast() {
        NettyConfiguration configuration = createConfiguration(b -> b.connectionAcquisitionTimeout(Duration.ofMillis(Long.MAX_VALUE)));
        assertThat(configuration.connectionAcquisitionTimeout()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void connectionTimeout_OnlyPulledFromAttributeMap() {
        NettyConfiguration configuration = createConfiguration(a -> a.put(CONNECTION_TIMEOUT,
                                                                          Duration.ofSeconds(42)),
                                                               b ->
                                                                   b.connectionTimeout(Duration.ofSeconds(9001)));
        assertThat(configuration.connectionTimeout()).isEqualTo(42 * 1000);
    }

    @Test
    public void connectionTimeout_PerformsSaturatedCast() {
        NettyConfiguration configuration = createConfiguration(a -> a.put(CONNECTION_TIMEOUT,
                                                                          Duration.ofMillis(Long.MAX_VALUE)),
                                                               b -> {
                                                               });
        assertThat(configuration.connectionTimeout()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void maxConnectionsPerEndpoint_OnlyPulledFromAttributeMap() {
        NettyConfiguration configuration = createConfiguration(a -> a.put(MAX_CONNECTIONS, 10),
                                                               b -> b.maxConnectionsPerEndpoint(11));
        assertThat(configuration.maxConnectionsPerEndpoint()).isEqualTo(10);
    }

    @Test
    public void readTimeout_AppliesDefaultValueIfNotSetOnFactory() {
        NettyConfiguration configuration = createEmptyConfiguration();
        assertThat(configuration.readTimeout()).isEqualTo(60);
    }

    @Test
    public void readTimeout_HonorsFactoryOverDefault() {
        NettyConfiguration configuration = createConfiguration(b -> b.readTimeout(Duration.ofSeconds(42)));
        assertThat(configuration.readTimeout()).isEqualTo(42);
    }

    @Test
    public void writeTimeout_AppliesDefaultValueIfNotSetOnFactory() {
        NettyConfiguration configuration = createEmptyConfiguration();
        assertThat(configuration.writeTimeout()).isEqualTo(60);
    }

    @Test
    public void writeTimeout_HonorsFactoryOverDefault() {
        NettyConfiguration configuration = createConfiguration(b -> b.writeTimeout(Duration.ofSeconds(42)));
        assertThat(configuration.writeTimeout()).isEqualTo(42);
    }

    @Test
    public void trustAllCertificates_DefaultsToFalse() {
        NettyConfiguration configuration = createEmptyConfiguration();
        assertThat(configuration.trustAllCertificates()).isFalse();
    }

    @Test
    public void trustAllCertificates_HonorsFactoryOverDefault() {
        NettyConfiguration configuration = createConfiguration(b -> b.trustAllCertificates(true));
        assertThat(configuration.trustAllCertificates()).isTrue();
    }

    private NettyConfiguration createEmptyConfiguration() {
        return createConfiguration(b -> {
        });
    }

    private NettyConfiguration createConfiguration(Consumer<NettySdkHttpClientFactory.Builder> builderConsumer) {
        return createConfiguration(a -> {
        }, builderConsumer);
    }

    private NettyConfiguration createConfiguration(
        Consumer<AttributeMap.Builder> attributeMapConsumer,
        Consumer<NettySdkHttpClientFactory.Builder> builderConsumer) {
        return new NettyConfiguration(AttributeMap.builder().apply(attributeMapConsumer).build(),
                                      NettySdkHttpClientFactory.builder().apply(builderConsumer).build());
    }

}