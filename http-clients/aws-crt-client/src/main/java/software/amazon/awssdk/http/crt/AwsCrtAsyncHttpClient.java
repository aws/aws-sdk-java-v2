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

import static software.amazon.awssdk.http.HttpMetric.HTTP_CLIENT_NAME;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.buildProxyOptions;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.buildSocketOptions;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.resolveCipherPreference;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.resolveHttpMonitoringOptions;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestExecutor;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses the AWS Common Runtime (CRT) Http Client to communicate with
 * Http Web Services. This client is asynchronous and uses non-blocking IO.
 *
 * <p>This can be created via {@link #builder()}</p>
 * {@snippet :
    SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                .maxConcurrency(100)
                                                .connectionTimeout(Duration.ofSeconds(1))
                                                .connectionMaxIdleTime(Duration.ofSeconds(5))
                                                .build();
 * }
 *
 */
@SdkPublicApi
public final class AwsCrtAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = Logger.loggerFor(AwsCrtAsyncHttpClient.class);

    private static final String AWS_COMMON_RUNTIME = "AwsCommonRuntime";
    private static final long DEFAULT_STREAM_WINDOW_SIZE = 16L * 1024L * 1024L; // 16 MB

    private final Map<URI, HttpClientConnectionManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContext tlsContext;
    private final HttpProxyOptions proxyOptions;
    private final HttpMonitoringOptions monitoringOptions;
    private final long maxConnectionIdleInMilliseconds;
    private final long readBufferSize;
    private final int maxConnectionsPerEndpoint;
    private boolean isClosed = false;

    private AwsCrtAsyncHttpClient(DefaultBuilder builder, AttributeMap config) {
        if (config.get(PROTOCOL) == Protocol.HTTP2) {
            throw new UnsupportedOperationException("HTTP/2 is not supported in AwsCrtAsyncHttpClient yet. Use "
                                               + "NettyNioAsyncHttpClient instead.");
        }

        try (ClientBootstrap clientBootstrap = new ClientBootstrap(null, null);
             SocketOptions clientSocketOptions = buildSocketOptions(builder.tcpKeepAliveConfiguration,
                                                                    config.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
             TlsContextOptions clientTlsContextOptions =
                 TlsContextOptions.createDefaultClient()
                                  .withCipherPreference(resolveCipherPreference(builder.postQuantumTlsEnabled))
                                  .withVerifyPeer(!config.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES));
             TlsContext clientTlsContext = new TlsContext(clientTlsContextOptions)) {

            this.bootstrap = registerOwnedResource(clientBootstrap);
            this.socketOptions = registerOwnedResource(clientSocketOptions);
            this.tlsContext = registerOwnedResource(clientTlsContext);
            this.readBufferSize = builder.readBufferSize == null ? DEFAULT_STREAM_WINDOW_SIZE : builder.readBufferSize;
            this.maxConnectionsPerEndpoint = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);
            this.monitoringOptions = resolveHttpMonitoringOptions(builder.connectionHealthConfiguration);
            this.maxConnectionIdleInMilliseconds = config.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis();
            this.proxyOptions = buildProxyOptions(builder.proxyConfiguration, tlsContext);
        }
    }

    /**
     * Marks a Native CrtResource as owned by the current Java Object.
     *
     * @param subresource The Resource to own.
     * @param <T> The CrtResource Type
     * @return The CrtResource passed in
     */
    private <T extends CrtResource> T registerOwnedResource(T subresource) {
        if (subresource != null) {
            subresource.addRef();
            ownedSubResources.push(subresource);
        }
        return subresource;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Create a {@link AwsCrtAsyncHttpClient} client with the default configuration
     *
     * @return an {@link SdkAsyncHttpClient}
     */
    public static SdkAsyncHttpClient create() {
        return new DefaultBuilder().build();
    }

    @Override
    public String clientName() {
        return AWS_COMMON_RUNTIME;
    }

    private HttpClientConnectionManager createConnectionPool(URI uri) {
        log.debug(() -> "Creating ConnectionPool for: URI:" + uri + ", MaxConns: " + maxConnectionsPerEndpoint);

        HttpClientConnectionManagerOptions options = new HttpClientConnectionManagerOptions()
                .withClientBootstrap(bootstrap)
                .withSocketOptions(socketOptions)
                .withTlsContext(tlsContext)
                .withUri(uri)
                .withWindowSize(readBufferSize)
                .withMaxConnections(maxConnectionsPerEndpoint)
                .withManualWindowManagement(true)
                .withProxyOptions(proxyOptions)
                .withMonitoringOptions(monitoringOptions)
                .withMaxConnectionIdleInMilliseconds(maxConnectionIdleInMilliseconds);

        return HttpClientConnectionManager.create(options);
    }

    /*
     * Callers of this function MUST account for the addRef() on the pool before returning.
     * Every execution path consuming the return value must guarantee an associated close().
     * Currently this function is only used by execute(), which guarantees a matching close
     * via the try-with-resources block.
     *
     * This guarantees that a returned pool will not get closed (by closing the http client) during
     * the time it takes to submit a request to the pool.  Acquisition requests submitted to the pool will
     * be properly failed if the http client is closed before the acquisition completes.
     *
     * This additional complexity means we only have to keep a lock for the scope of this function, as opposed to
     * the scope of calling execute().  This function will almost always just be a hash lookup and the return of an
     * existing pool.  If we add all of execute() to the scope, we include, at minimum a JNI call to the native
     * pool implementation.
     */
    private HttpClientConnectionManager getOrCreateConnectionPool(URI uri) {
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("Client is closed. No more requests can be made with this client.");
            }

            HttpClientConnectionManager connPool = connectionPools.computeIfAbsent(uri, this::createConnectionPool);
            connPool.addRef();
            return connPool;
        }
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest asyncRequest) {

        paramNotNull(asyncRequest, "asyncRequest");
        paramNotNull(asyncRequest.request(), "SdkHttpRequest");
        paramNotNull(asyncRequest.requestContentPublisher(), "RequestContentPublisher");
        paramNotNull(asyncRequest.responseHandler(), "ResponseHandler");

        if (asyncRequest.metricCollector().isPresent()) {
            MetricCollector metricCollector = asyncRequest.metricCollector().get();

            if (metricCollector != null && !(metricCollector instanceof NoOpMetricCollector)) {
                metricCollector.reportMetric(HTTP_CLIENT_NAME, clientName());
            }
        }

        /*
         * See the note on getOrCreateConnectionPool()
         *
         * In particular, this returns a ref-counted object and calling getOrCreateConnectionPool
         * increments the ref count by one.  We add a try-with-resources to release our ref
         * once we have successfully submitted a request.  In this way, we avoid a race condition
         * when close/shutdown is called from another thread while this function is executing (ie.
         * we have a pool and no one can destroy it underneath us until we've finished submitting the
         * request)
         */
        try (HttpClientConnectionManager crtConnPool = getOrCreateConnectionPool(poolKey(asyncRequest))) {
            CrtRequestContext context = CrtRequestContext.builder()
                                                         .crtConnPool(crtConnPool)
                                                         .readBufferSize(readBufferSize)
                                                         .request(asyncRequest)
                                                         .build();

            return new CrtRequestExecutor().execute(context);
        }
    }

    private URI poolKey(AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(),
                                          sdkRequest.port(), null, null, null));
    }

    @Override
    public void close() {
        synchronized (this) {

            if (isClosed) {
                return;
            }

            connectionPools.values().forEach(pool -> IoUtils.closeQuietly(pool, log.logger()));
            ownedSubResources.forEach(r -> IoUtils.closeQuietly(r, log.logger()));
            ownedSubResources.clear();

            isClosed = true;
        }
    }

    /**
     * Builder that allows configuration of the AWS CRT HTTP implementation.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<AwsCrtAsyncHttpClient.Builder> {

        /**
         * The Maximum number of allowed concurrent requests. For HTTP/1.1 this is the same as max connections.
         * @param maxConcurrency maximum concurrency per endpoint
         * @return The builder of the method chaining.
         */
        Builder maxConcurrency(Integer maxConcurrency);

        /**
         * Configures the number of unread bytes that can be buffered in the
         * client before we stop reading from the underlying TCP socket and wait for the Subscriber
         * to read more data.
         *
         * @param readBufferSize The number of bytes that can be buffered.
         * @return The builder of the method chaining.
         */
        Builder readBufferSizeInBytes(Long readBufferSize);

        /**
         * Sets the http proxy configuration to use for this client.
         * @param proxyConfiguration The http proxy configuration to use
         * @return The builder of the method chaining.
         */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Sets the http proxy configuration to use for this client.
         *
         * @param proxyConfigurationBuilderConsumer The consumer of the proxy configuration builder object.
         * @return the builder for method chaining.
         */
        Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer);

        /**
         * Configure the health checks for all connections established by this client.
         *
         * <p>
         * You can set a throughput threshold for a connection to be considered healthy.
         * If a connection falls below this threshold ({@link ConnectionHealthConfiguration#minimumThroughputInBps()
         * }) for the configurable amount
         * of time ({@link ConnectionHealthConfiguration#minimumThroughputTimeout()}),
         * then the connection is considered unhealthy and will be shut down.
         *
         * <p>
         * By default, monitoring options are disabled. You can enable {@code healthChecks} by providing this configuration
         * and specifying the options for monitoring for the connection manager.
         * @param healthChecksConfiguration The health checks config to use
         * @return The builder of the method chaining.
         */
        Builder connectionHealthConfiguration(ConnectionHealthConfiguration healthChecksConfiguration);

        /**
         * A convenience method that creates an instance of the {@link ConnectionHealthConfiguration} builder, avoiding the
         * need to create one manually via {@link ConnectionHealthConfiguration#builder()}.
         *
         * @param healthChecksConfigurationBuilder The health checks config builder to use
         * @return The builder of the method chaining.
         * @see #connectionHealthConfiguration(ConnectionHealthConfiguration)
         */
        Builder connectionHealthConfiguration(Consumer<ConnectionHealthConfiguration.Builder>
                                                        healthChecksConfigurationBuilder);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         * @param connectionMaxIdleTime the maximum amount of connection idle time
         * @return The builder of the method chaining.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         * @param connectionTimeout timeout
         * @return The builder of the method chaining.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * Configure whether to enable {@code tcpKeepAlive} and relevant configuration for all connections established by this
         * client.
         *
         * <p>
         * By default, tcpKeepAlive is disabled. You can enable {@code tcpKeepAlive} by providing this configuration
         * and specifying periodic TCP keepalive packet intervals and timeouts. This may be required for certain connections for
         * longer durations than default socket timeouts.
         *
         * @param tcpKeepAliveConfiguration The TCP keep-alive configuration to use
         * @return The builder of the method chaining.
         */
        Builder tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration);

        /**
         * Configure whether to enable {@code tcpKeepAlive} and relevant configuration for all connections established by this
         * client.
         *
         * <p>
         * A convenience method that creates an instance of the {@link TcpKeepAliveConfiguration} builder, avoiding the
         * need to create one manually via {@link TcpKeepAliveConfiguration#builder()}.
         *
         * @param tcpKeepAliveConfigurationBuilder The TCP keep-alive configuration builder to use
         * @return The builder of the method chaining.
         * @see #tcpKeepAliveConfiguration(TcpKeepAliveConfiguration)
         */
        Builder tcpKeepAliveConfiguration(Consumer<TcpKeepAliveConfiguration.Builder>
                                              tcpKeepAliveConfigurationBuilder);

        /**
         * Configure whether to enable a hybrid post-quantum key exchange option for the Transport Layer Security (TLS) network
         * encryption protocol when communicating with services that support Post Quantum TLS. If Post Quantum cipher suites are
         * not supported on the platform, the SDK will use the default TLS cipher suites.
         *
         * <p>
         * See <a href="https://docs.aws.amazon.com/kms/latest/developerguide/pqtls.html">Using hybrid post-quantum TLS with AWS KMS</a>
         *
         * <p>
         * It's disabled by default.
         *
         * @param postQuantumTlsEnabled whether to prefer Post Quantum TLS
         * @return The builder of the method chaining.
         */
        Builder postQuantumTlsEnabled(Boolean postQuantumTlsEnabled);
    }

    /**
     * Factory that allows more advanced configuration of the AWS CRT HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private Long readBufferSize;
        private ProxyConfiguration proxyConfiguration;
        private ConnectionHealthConfiguration connectionHealthConfiguration;
        private TcpKeepAliveConfiguration tcpKeepAliveConfiguration;
        private Boolean postQuantumTlsEnabled;

        private DefaultBuilder() {
        }

        @Override
        public SdkAsyncHttpClient build() {
            return new AwsCrtAsyncHttpClient(this, standardOptions.build()
                                                                  .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new AwsCrtAsyncHttpClient(this, standardOptions.build()
                                                           .merge(serviceDefaults)
                                                           .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }

        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            Validate.isPositiveOrNull(maxConcurrency, "maxConcurrency");
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConcurrency);
            return this;
        }

        @Override
        public Builder readBufferSizeInBytes(Long readBufferSize) {
            Validate.isPositiveOrNull(readBufferSize, "readBufferSize");
            this.readBufferSize = readBufferSize;
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        @Override
        public Builder connectionHealthConfiguration(ConnectionHealthConfiguration monitoringOptions) {
            this.connectionHealthConfiguration = monitoringOptions;
            return this;
        }

        @Override
        public Builder connectionHealthConfiguration(Consumer<ConnectionHealthConfiguration.Builder>
                                                                       configurationBuilder) {
            ConnectionHealthConfiguration.Builder builder = ConnectionHealthConfiguration.builder();
            configurationBuilder.accept(builder);
            return connectionHealthConfiguration(builder.build());
        }

        @Override
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            Validate.isPositive(connectionMaxIdleTime, "connectionMaxIdleTime");
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            Validate.isPositive(connectionTimeout, "connectionTimeout");
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration) {
            this.tcpKeepAliveConfiguration = tcpKeepAliveConfiguration;
            return this;
        }

        @Override
        public Builder tcpKeepAliveConfiguration(Consumer<TcpKeepAliveConfiguration.Builder>
                                                             tcpKeepAliveConfigurationBuilder) {
            TcpKeepAliveConfiguration.Builder builder = TcpKeepAliveConfiguration.builder();
            tcpKeepAliveConfigurationBuilder.accept(builder);
            return tcpKeepAliveConfiguration(builder.build());
        }

        @Override
        public Builder postQuantumTlsEnabled(Boolean postQuantumTlsEnabled) {
            this.postQuantumTlsEnabled = postQuantumTlsEnabled;
            return this;
        }

        @Override
        public Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer) {
            ProxyConfiguration.Builder builder = ProxyConfiguration.builder();
            proxyConfigurationBuilderConsumer.accept(builder);
            return proxyConfiguration(builder.build());
        }
    }
}
