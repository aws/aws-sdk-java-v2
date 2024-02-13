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

package software.amazon.awssdk.http.crt.internal;

import java.time.Duration;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.crt.ConnectionHealthConfiguration;
import software.amazon.awssdk.http.crt.ProxyConfiguration;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class AwsCrtClientBuilderBase<BuilderT> {
    private final AttributeMap.Builder standardOptions = AttributeMap.builder();
    private Long readBufferSize;
    private ProxyConfiguration proxyConfiguration;
    private ConnectionHealthConfiguration connectionHealthConfiguration;
    private TcpKeepAliveConfiguration tcpKeepAliveConfiguration;
    private Boolean postQuantumTlsEnabled;

    protected AwsCrtClientBuilderBase() {
    }

    protected AttributeMap.Builder getAttributeMap() {
        return standardOptions;
    }

    private BuilderT thisBuilder() {
        return (BuilderT) this;
    }

    public BuilderT maxConcurrency(Integer maxConcurrency) {
        Validate.isPositiveOrNull(maxConcurrency, "maxConcurrency");
        standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConcurrency);
        return thisBuilder();
    }

    public BuilderT readBufferSizeInBytes(Long readBufferSize) {
        Validate.isPositiveOrNull(readBufferSize, "readBufferSize");
        this.readBufferSize = readBufferSize;
        return thisBuilder();
    }

    public Long getReadBufferSizeInBytes() {
        return this.readBufferSize;
    }


    public BuilderT proxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
        return thisBuilder();
    }

    public ProxyConfiguration getProxyConfiguration() {
        return this.proxyConfiguration;
    }

    public BuilderT connectionHealthConfiguration(ConnectionHealthConfiguration monitoringOptions) {
        this.connectionHealthConfiguration = monitoringOptions;
        return thisBuilder();
    }

    public ConnectionHealthConfiguration getConnectionHealthConfiguration() {
        return this.connectionHealthConfiguration;
    }

    public BuilderT connectionHealthConfiguration(Consumer<ConnectionHealthConfiguration.Builder>
                                           configurationBuilder) {
        ConnectionHealthConfiguration.Builder builder = ConnectionHealthConfiguration.builder();
        configurationBuilder.accept(builder);
        connectionHealthConfiguration(builder.build());
        return thisBuilder();
    }

    public BuilderT connectionMaxIdleTime(Duration connectionMaxIdleTime) {
        Validate.isPositive(connectionMaxIdleTime, "connectionMaxIdleTime");
        standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
        return thisBuilder();
    }

    public BuilderT connectionTimeout(Duration connectionTimeout) {
        Validate.isPositive(connectionTimeout, "connectionTimeout");
        standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
        return thisBuilder();
    }

    public BuilderT tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration) {
        this.tcpKeepAliveConfiguration = tcpKeepAliveConfiguration;
        return thisBuilder();
    }

    public TcpKeepAliveConfiguration getTcpKeepAliveConfiguration() {
        return this.tcpKeepAliveConfiguration;
    }

    public BuilderT tcpKeepAliveConfiguration(Consumer<TcpKeepAliveConfiguration.Builder>
                                       tcpKeepAliveConfigurationBuilder) {
        TcpKeepAliveConfiguration.Builder builder = TcpKeepAliveConfiguration.builder();
        tcpKeepAliveConfigurationBuilder.accept(builder);
        tcpKeepAliveConfiguration(builder.build());
        return thisBuilder();
    }

    public BuilderT postQuantumTlsEnabled(Boolean postQuantumTlsEnabled) {
        this.postQuantumTlsEnabled = postQuantumTlsEnabled;
        return thisBuilder();
    }

    public Boolean getPostQuantumTlsEnabled() {
        return this.postQuantumTlsEnabled;
    }

    public BuilderT proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer) {
        ProxyConfiguration.Builder builder = ProxyConfiguration.builder();
        proxyConfigurationBuilderConsumer.accept(builder);
        proxyConfiguration(builder.build());
        return thisBuilder();
    }
}