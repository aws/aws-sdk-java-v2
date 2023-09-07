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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import java.util.function.Consumer;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

class AwsCrtClientBuilderBase {
    private final AttributeMap.Builder standardOptions = AttributeMap.builder();
    private Long readBufferSize;
    private ProxyConfiguration proxyConfiguration;
    private ConnectionHealthConfiguration connectionHealthConfiguration;
    private TcpKeepAliveConfiguration tcpKeepAliveConfiguration;
    private Boolean postQuantumTlsEnabled;

    AwsCrtClientBuilderBase() {
    }

    AttributeMap.Builder getAttributeMap() {
        return standardOptions;
    }

    void maxConcurrency(Integer maxConcurrency) {
        Validate.isPositiveOrNull(maxConcurrency, "maxConcurrency");
        standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConcurrency);
    }

    void readBufferSizeInBytes(Long readBufferSize) {
        Validate.isPositiveOrNull(readBufferSize, "readBufferSize");
        this.readBufferSize = readBufferSize;
    }

    Long getReadBufferSizeInBytes() {
        return this.readBufferSize;
    }

    void proxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    ProxyConfiguration getProxyConfiguration() {
        return this.proxyConfiguration;
    }

    void connectionHealthConfiguration(ConnectionHealthConfiguration monitoringOptions) {
        this.connectionHealthConfiguration = monitoringOptions;
    }

    ConnectionHealthConfiguration getConnectionHealthConfiguration() {
        return this.connectionHealthConfiguration;
    }

    void connectionHealthConfiguration(Consumer<ConnectionHealthConfiguration.Builder>
                                           configurationBuilder) {
        ConnectionHealthConfiguration.Builder builder = ConnectionHealthConfiguration.builder();
        configurationBuilder.accept(builder);
        connectionHealthConfiguration(builder.build());
    }

    void connectionMaxIdleTime(Duration connectionMaxIdleTime) {
        Validate.isPositive(connectionMaxIdleTime, "connectionMaxIdleTime");
        standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
    }

    void connectionTimeout(Duration connectionTimeout) {
        Validate.isPositive(connectionTimeout, "connectionTimeout");
        standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
    }

    void tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration) {
        this.tcpKeepAliveConfiguration = tcpKeepAliveConfiguration;
    }

    TcpKeepAliveConfiguration getTcpKeepAliveConfiguration() {
        return this.tcpKeepAliveConfiguration;
    }

    void tcpKeepAliveConfiguration(Consumer<TcpKeepAliveConfiguration.Builder>
                                       tcpKeepAliveConfigurationBuilder) {
        TcpKeepAliveConfiguration.Builder builder = TcpKeepAliveConfiguration.builder();
        tcpKeepAliveConfigurationBuilder.accept(builder);
        tcpKeepAliveConfiguration(builder.build());
    }

    void postQuantumTlsEnabled(Boolean postQuantumTlsEnabled) {
        this.postQuantumTlsEnabled = postQuantumTlsEnabled;
    }

    Boolean getPostQuantumTlsEnabled() {
        return this.postQuantumTlsEnabled;
    }

    void proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer) {
        ProxyConfiguration.Builder builder = ProxyConfiguration.builder();
        proxyConfigurationBuilderConsumer.accept(builder);
        proxyConfiguration(builder.build());
    }
}