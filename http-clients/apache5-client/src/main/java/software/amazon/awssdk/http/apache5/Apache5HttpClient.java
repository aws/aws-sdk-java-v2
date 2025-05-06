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

package software.amazon.awssdk.http.apache5;

import static software.amazon.awssdk.http.HttpMetric.AVAILABLE_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.HTTP_CLIENT_NAME;
import static software.amazon.awssdk.http.HttpMetric.LEASED_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.MAX_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.PENDING_CONCURRENCY_ACQUIRES;
import static software.amazon.awssdk.http.apache5.internal.conn.ClientConnectionRequestFactory.THREAD_LOCAL_REQUEST_METRIC_COLLECTOR;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderIterator;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.client5.http.auth.AuthSchemeProvider;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLInitializationException;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.pool.PoolStats;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.DefaultConfiguration;
import software.amazon.awssdk.http.apache5.internal.SdkProxyRoutePlanner;
import software.amazon.awssdk.http.apache5.internal.conn.ClientConnectionManagerFactory;
import software.amazon.awssdk.http.apache5.internal.conn.IdleConnectionReaper;
import software.amazon.awssdk.http.apache5.internal.conn.SdkConnectionKeepAliveStrategy;
import software.amazon.awssdk.http.apache5.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.http.apache5.internal.impl.Apache5HttpRequestFactory;
import software.amazon.awssdk.http.apache5.internal.impl.Apache5SdkHttpClient;
import software.amazon.awssdk.http.apache5.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache5.internal.utils.Apache5Utils;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

// TODO: All the Java Doc will be updated to consider the reference of Apache4.x if required
/**
 * An implementation of {@link SdkHttpClient} that uses Apache5 HTTP client to communicate with the service. This is the most
 * powerful synchronous client that adds an extra dependency and additional startup latency in exchange for more functionality,
 * like support for HTTP proxies.
 *
 * <p>See software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient for an alternative implementation.</p>
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class Apache5HttpClient implements SdkHttpClient {

    public static final String CLIENT_NAME = "Apache5";

    private static final Logger log = Logger.loggerFor(Apache5HttpClient.class);

    private final Apache5HttpRequestFactory apacheHttpRequestFactory = new Apache5HttpRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final Apache5HttpRequestConfig requestConfig;
    private final AttributeMap resolvedOptions;

    @SdkTestInternalApi
    Apache5HttpClient(ConnectionManagerAwareHttpClient httpClient,
                     Apache5HttpRequestConfig requestConfig,
                     AttributeMap resolvedOptions) {
        this.httpClient = httpClient;
        this.requestConfig = requestConfig;
        this.resolvedOptions = resolvedOptions;
    }

    private Apache5HttpClient(DefaultBuilder builder, AttributeMap resolvedOptions) {
        this.httpClient = createClient(builder, resolvedOptions);
        this.requestConfig = createRequestConfig(builder, resolvedOptions);
        this.resolvedOptions = resolvedOptions;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Create a {@link Apache5HttpClient} with the default properties
     *
     * @return an {@link Apache5HttpClient}
     */
    public static SdkHttpClient create() {
        return new DefaultBuilder().build();
    }

    private ConnectionManagerAwareHttpClient createClient(Apache5HttpClient.DefaultBuilder configuration,
                                                          AttributeMap standardOptions) {
        ApacheConnectionManagerFactory cmFactory = new ApacheConnectionManagerFactory();

        HttpClientBuilder builder = HttpClients.custom();
        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        // from the reaper. See https://github.com/aws/aws-sdk-java/issues/722.
        HttpClientConnectionManager cm = cmFactory.create(configuration, standardOptions);

        Registry<AuthSchemeProvider> authSchemeProviderRegistry = configuration.authSchemeProviderRegistry;
        if (authSchemeProviderRegistry != null) {
            builder.setDefaultAuthSchemeRegistry(authSchemeProviderRegistry);
        }


        builder.setRequestExecutor(new HttpRequestExecutor())
               // SDK handles decompression
               .disableContentCompression()
               .setKeepAliveStrategy(buildKeepAliveStrategy(standardOptions))
               .disableRedirectHandling()
               .disableAutomaticRetries()
               .setUserAgent("") // SDK will set the user agent header in the pipeline. Don't let Apache5 waste time
               .setConnectionManager(ClientConnectionManagerFactory.wrap(cm));

        addProxyConfig(builder, configuration);

        if (useIdleConnectionReaper(standardOptions)) {
            IdleConnectionReaper.getInstance().registerConnectionManager(
                cm, standardOptions.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis());
        }

        return new Apache5SdkHttpClient(builder.build(), cm);
    }

    private void addProxyConfig(HttpClientBuilder builder,
                                DefaultBuilder configuration) {
        ProxyConfiguration proxyConfiguration = configuration.proxyConfiguration;

        Validate.isTrue(configuration.httpRoutePlanner == null || !isProxyEnabled(proxyConfiguration),
                        "The httpRoutePlanner and proxyConfiguration can't both be configured.");
        Validate.isTrue(configuration.credentialsProvider == null || !isAuthenticatedProxy(proxyConfiguration),
                        "The credentialsProvider and proxyConfiguration username/password can't both be configured.");

        HttpRoutePlanner routePlanner = configuration.httpRoutePlanner;
        if (isProxyEnabled(proxyConfiguration)) {
            log.debug(() -> "Configuring Proxy. Proxy Host: " + proxyConfiguration.host());
            routePlanner = new SdkProxyRoutePlanner(proxyConfiguration.host(),
                                                    proxyConfiguration.port(),
                                                    proxyConfiguration.scheme(),
                                                    proxyConfiguration.nonProxyHosts());
        }

        CredentialsProvider credentialsProvider = configuration.credentialsProvider;
        if (isAuthenticatedProxy(proxyConfiguration)) {
            credentialsProvider = Apache5Utils.newProxyCredentialsProvider(proxyConfiguration);
        }

        if (routePlanner != null) {
            builder.setRoutePlanner(routePlanner);
        }

        if (credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
    }

    private ConnectionKeepAliveStrategy buildKeepAliveStrategy(AttributeMap standardOptions) {
        long maxIdle = standardOptions.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis();
        return maxIdle > 0 ? new SdkConnectionKeepAliveStrategy(maxIdle) : null;
    }

    private boolean useIdleConnectionReaper(AttributeMap standardOptions) {
        return Boolean.TRUE.equals(standardOptions.get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS));
    }

    private boolean isAuthenticatedProxy(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.username() != null && proxyConfiguration.password() != null;
    }

    private boolean isProxyEnabled(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.host() != null
               && proxyConfiguration.port() > 0;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        MetricCollector metricCollector = request.metricCollector().orElseGet(NoOpMetricCollector::create);
        metricCollector.reportMetric(HTTP_CLIENT_NAME, clientName());
        HttpUriRequestBase apacheRequest = toApacheRequest(request);
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() throws IOException {
                HttpExecuteResponse executeResponse = execute(apacheRequest, metricCollector);
                collectPoolMetric(metricCollector);
                return executeResponse;
            }

            @Override
            public void abort() {
                apacheRequest.abort();
            }
        };
    }

    @Override
    public void close() {
        HttpClientConnectionManager cm = httpClient.getHttpClientConnectionManager();
        IdleConnectionReaper.getInstance().deregisterConnectionManager(cm);
        cm.shutdown();
    }

    private HttpExecuteResponse execute(HttpUriRequestBase apacheRequest, MetricCollector metricCollector) throws IOException {
        HttpClientContext localRequestContext = Apache5Utils.newClientContext(requestConfig.proxyConfiguration());
        THREAD_LOCAL_REQUEST_METRIC_COLLECTOR.set(metricCollector);
        try {
            HttpResponse httpResponse = httpClient.execute(apacheRequest, localRequestContext);
            return createResponse(httpResponse, apacheRequest);
        } finally {
            THREAD_LOCAL_REQUEST_METRIC_COLLECTOR.remove();
        }
    }

    private HttpUriRequestBase toApacheRequest(HttpExecuteRequest request) {
        return apacheHttpRequestFactory.create(request, requestConfig);
    }

    /**
     * Creates and initializes an HttpResponse object suitable to be passed to an HTTP response
     * handler object.
     *
     * @return The new, initialized HttpResponse object ready to be passed to an HTTP response handler object.
     * @throws IOException If there were any problems getting any response information from the
     *                     HttpClient method object.
     */
    private HttpExecuteResponse createResponse(HttpResponse apacheHttpResponse,
                                               HttpUriRequestBase apacheRequest) throws IOException {
        SdkHttpResponse.Builder responseBuilder =
            SdkHttpResponse.builder()
                           .statusCode(apacheHttpResponse.getCode())
                           .statusText(apacheHttpResponse.getReasonPhrase());

        HeaderIterator headerIterator = apacheHttpResponse.headerIterator();
        while (headerIterator.hasNext()) {
            Header header = headerIterator.nextHeader();
            responseBuilder.appendHeader(header.getName(), header.getValue());
        }

        AbortableInputStream responseBody = apacheHttpResponse.getEntity() != null ?
                                            toAbortableInputStream(apacheHttpResponse, apacheRequest) : null;

        return HttpExecuteResponse.builder().response(responseBuilder.build()).responseBody(responseBody).build();

    }

    private AbortableInputStream toAbortableInputStream(HttpResponse apacheHttpResponse, HttpUriRequestBase apacheRequest)
        throws IOException {
        return AbortableInputStream.create(apacheHttpResponse.getEntity().getContent(), apacheRequest::abort);
    }

    private Apache5HttpRequestConfig createRequestConfig(DefaultBuilder builder,
                                                         AttributeMap resolvedOptions) {
        return Apache5HttpRequestConfig.builder()
                                       .socketTimeout(resolvedOptions.get(SdkHttpConfigurationOption.READ_TIMEOUT))
                                       .connectionTimeout(resolvedOptions.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT))
                                       .connectionAcquireTimeout(
                                          resolvedOptions.get(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT))
                                       .proxyConfiguration(builder.proxyConfiguration)
                                       .localAddress(Optional.ofNullable(builder.localAddress).orElse(null))
                                       .expectContinueEnabled(Optional.ofNullable(builder.expectContinueEnabled)
                                                                     .orElse(DefaultConfiguration.EXPECT_CONTINUE_ENABLED))
                                       .build();
    }

    private void collectPoolMetric(MetricCollector metricCollector) {
        HttpClientConnectionManager cm = httpClient.getHttpClientConnectionManager();
        if (cm instanceof PoolingHttpClientConnectionManager && !(metricCollector instanceof NoOpMetricCollector)) {
            PoolingHttpClientConnectionManager poolingCm = (PoolingHttpClientConnectionManager) cm;
            PoolStats totalStats = poolingCm.getTotalStats();
            metricCollector.reportMetric(MAX_CONCURRENCY, totalStats.getMax());
            metricCollector.reportMetric(AVAILABLE_CONCURRENCY, totalStats.getAvailable());
            metricCollector.reportMetric(LEASED_CONCURRENCY, totalStats.getLeased());
            metricCollector.reportMetric(PENDING_CONCURRENCY_ACQUIRES, totalStats.getPending());
        }
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    /**
     * Builder for creating an instance of {@link SdkHttpClient}. The factory can be configured through the builder {@link
     * #builder()}, once built it can create a {@link SdkHttpClient} via {@link #build()} or can be passed to the SDK
     * client builders directly to have the SDK create and manage the HTTP client. See documentation on the service's respective
     * client builder for more information on configuring the HTTP layer.
     *
     * <pre class="brush: java">
     * SdkHttpClient httpClient =
     *     Apache5HttpClient.builder()
     *                     .socketTimeout(Duration.ofSeconds(10))
     *                     .build();
     * </pre>
     */
    public interface Builder extends SdkHttpClient.Builder<Apache5HttpClient.Builder> {

        /**
         * The amount of time to wait for data to be transferred over an established, open connection before the connection is
         * timed out. A duration of 0 means infinity, and is not recommended.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out. A duration of 0
         * means infinity, and is not recommended.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         * @param connectionAcquisitionTimeout the timeout duration
         * @return this builder for method chaining.
         */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /**
         * The maximum number of connections allowed in the connection pool. Each built HTTP client has its own private
         * connection pool.
         */
        Builder maxConnections(Integer maxConnections);

        /**
         * Configuration that defines how to communicate via an HTTP proxy.
         */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Configure the local address that the HTTP client should use for communication.
         */
        Builder localAddress(InetAddress localAddress);

        /**
         * Configure whether the client should send an HTTP expect-continue handshake before each request.
         */
        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        /**
         * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         */
        Builder connectionTimeToLive(Duration connectionTimeToLive);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         */
        Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout);

        /**
         * Configure whether the idle connections in the connection pool should be closed asynchronously.
         * <p>
         * When enabled, connections left idling for longer than {@link #connectionMaxIdleTime(Duration)} will be
         * closed. This will not close connections currently in use. By default, this is enabled.
         */
        Builder useIdleConnectionReaper(Boolean useConnectionReaper);

        /**
         * Configuration that defines a DNS resolver. If no matches are found, the default resolver is used.
         */
        Builder dnsResolver(DnsResolver dnsResolver);

        /**
         * Configuration that defines a custom Socket factory. If set to a null value, a default factory is used.
         * <p>
         * When set to a non-null value, the use of a custom factory implies the configuration options TRUST_ALL_CERTIFICATES,
         * TLS_TRUST_MANAGERS_PROVIDER, and TLS_KEY_MANAGERS_PROVIDER are ignored.
         */
        Builder socketFactory(ConnectionSocketFactory socketFactory);

        /**
         * Configuration that defines an HTTP route planner that computes the route an HTTP request should take.
         * May not be used in conjunction with {@link #proxyConfiguration(ProxyConfiguration)}.
         */
        Builder httpRoutePlanner(HttpRoutePlanner proxyConfiguration);

        /**
         * Configuration that defines a custom credential provider for HTTP requests.
         * May not be used in conjunction with {@link ProxyConfiguration#username()} and {@link ProxyConfiguration#password()}.
         */
        Builder credentialsProvider(CredentialsProvider credentialsProvider);

        /**
         * Configure whether to enable or disable TCP KeepAlive.
         * The configuration will be passed to the socket option {@link java.net.SocketOptions#SO_KEEPALIVE}.
         * <p>
         * By default, this is disabled.
         * <p>
         * When enabled, the actual KeepAlive mechanism is dependent on the Operating System and therefore additional TCP
         * KeepAlive values (like timeout, number of packets, etc) must be configured via the Operating System (sysctl on
         * Linux/Mac, and Registry values on Windows).
         */
        Builder tcpKeepAlive(Boolean keepConnectionAlive);

        /**
         * Configure the {@link TlsKeyManagersProvider} that will provide the {@link javax.net.ssl.KeyManager}s to use
         * when constructing the SSL context.
         * <p>
         * The default used by the client will be {@link SystemPropertyTlsKeyManagersProvider}. Configure an instance of
         * {@link software.amazon.awssdk.internal.http.NoneTlsKeyManagersProvider} or another implementation of
         * {@link TlsKeyManagersProvider} to override it.
         */
        Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);

        /**
         * Configure the {@link TlsTrustManagersProvider} that will provide the {@link javax.net.ssl.TrustManager}s to use
         * when constructing the SSL context.
         */
        Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider);

        /**
         * Configure the authentication scheme registry that can be used to obtain the corresponding authentication scheme
         * implementation for a given type of authorization challenge.
         */
        Builder authSchemeProviderRegistry(Registry<AuthSchemeProvider> authSchemeProviderRegistry);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private Registry<AuthSchemeProvider> authSchemeProviderRegistry;
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private HttpRoutePlanner httpRoutePlanner;
        private CredentialsProvider credentialsProvider;
        private DnsResolver dnsResolver;
        private ConnectionSocketFactory socketFactory;

        private DefaultBuilder() {
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.READ_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         * @param connectionAcquisitionTimeout the timeout duration
         * @return this builder for method chaining.
         */
        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            Validate.isPositive(connectionAcquisitionTimeout, "connectionAcquisitionTimeout");
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            connectionAcquisitionTimeout(connectionAcquisitionTimeout);
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        public void setMaxConnections(Integer maxConnections) {
            maxConnections(maxConnections);
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
            proxyConfiguration(proxyConfiguration);
        }

        @Override
        public Builder localAddress(InetAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public void setLocalAddress(InetAddress localAddress) {
            localAddress(localAddress);
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public void setExpectContinueEnabled(Boolean useExpectContinue) {
            this.expectContinueEnabled = useExpectContinue;
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE, connectionTimeToLive);
            return this;
        }

        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            connectionTimeToLive(connectionTimeToLive);
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, maxIdleConnectionTimeout);
            return this;
        }

        public void setConnectionMaxIdleTime(Duration connectionMaxIdleTime) {
            connectionMaxIdleTime(connectionMaxIdleTime);
        }

        @Override
        public Builder useIdleConnectionReaper(Boolean useIdleConnectionReaper) {
            standardOptions.put(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS, useIdleConnectionReaper);
            return this;
        }

        public void setUseIdleConnectionReaper(Boolean useIdleConnectionReaper) {
            useIdleConnectionReaper(useIdleConnectionReaper);
        }

        @Override
        public Builder dnsResolver(DnsResolver dnsResolver) {
            this.dnsResolver = dnsResolver;
            return this;
        }

        public void setDnsResolver(DnsResolver dnsResolver) {
            dnsResolver(dnsResolver);
        }

        @Override
        public Builder socketFactory(ConnectionSocketFactory socketFactory) {
            this.socketFactory = socketFactory;
            return this;
        }

        public void setSocketFactory(ConnectionSocketFactory socketFactory) {
            socketFactory(socketFactory);
        }

        @Override
        public Builder httpRoutePlanner(HttpRoutePlanner httpRoutePlanner) {
            this.httpRoutePlanner = httpRoutePlanner;
            return this;
        }

        public void setHttpRoutePlanner(HttpRoutePlanner httpRoutePlanner) {
            httpRoutePlanner(httpRoutePlanner);
        }

        @Override
        public Builder credentialsProvider(CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
            credentialsProvider(credentialsProvider);
        }

        @Override
        public Builder tcpKeepAlive(Boolean keepConnectionAlive) {
            standardOptions.put(SdkHttpConfigurationOption.TCP_KEEPALIVE, keepConnectionAlive);
            return this;
        }

        public void setTcpKeepAlive(Boolean keepConnectionAlive) {
            tcpKeepAlive(keepConnectionAlive);
        }

        @Override
        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            standardOptions.put(SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER, tlsKeyManagersProvider);
            return this;
        }

        public void setTlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            tlsKeyManagersProvider(tlsKeyManagersProvider);
        }

        @Override
        public Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            standardOptions.put(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER, tlsTrustManagersProvider);
            return this;
        }

        public void setTlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            tlsTrustManagersProvider(tlsTrustManagersProvider);
        }

        @Override
        public Builder authSchemeProviderRegistry(Registry<AuthSchemeProvider> authSchemeProviderRegistry) {
            this.authSchemeProviderRegistry = authSchemeProviderRegistry;
            return this;
        }

        public void setAuthSchemeProviderRegistry(Registry<AuthSchemeProvider> authSchemeProviderRegistry) {
            authSchemeProviderRegistry(authSchemeProviderRegistry);
        }

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            AttributeMap resolvedOptions = standardOptions.build().merge(serviceDefaults).merge(
                SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);
            return new Apache5HttpClient(this, resolvedOptions);
        }
    }

    private static class ApacheConnectionManagerFactory {

        public HttpClientConnectionManager create(Apache5HttpClient.DefaultBuilder configuration,
                                                  AttributeMap standardOptions) {
            ConnectionSocketFactory sslsf = getPreferredSocketFactory(configuration, standardOptions);

            PoolingHttpClientConnectionManager cm = new
                PoolingHttpClientConnectionManager(
                createSocketFactoryRegistry(sslsf),
                null,
                DefaultSchemePortResolver.INSTANCE,
                configuration.dnsResolver,
                standardOptions.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE).toMillis(),
                TimeUnit.MILLISECONDS);

            cm.setDefaultMaxPerRoute(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
            cm.setMaxTotal(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
            cm.setDefaultSocketConfig(buildSocketConfig(standardOptions));

            return cm;
        }

        private ConnectionSocketFactory getPreferredSocketFactory(Apache5HttpClient.DefaultBuilder configuration,
                                                                  AttributeMap standardOptions) {
            return Optional.ofNullable(configuration.socketFactory)
                           .orElseGet(() -> new SdkTlsSocketFactory(getSslContext(standardOptions),
                                                                    getHostNameVerifier(standardOptions)));
        }

        private HostnameVerifier getHostNameVerifier(AttributeMap standardOptions) {
            return standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)
                   ? NoopHostnameVerifier.INSTANCE
                   : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        }

        private SSLContext getSslContext(AttributeMap standardOptions) {
            Validate.isTrue(standardOptions.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) == null ||
                            !standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES),
                            "A TlsTrustManagerProvider can't be provided if TrustAllCertificates is also set");

            TrustManager[] trustManagers = null;
            if (standardOptions.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) != null) {
                trustManagers = standardOptions.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER).trustManagers();
            }

            if (standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
                log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                               + "used for testing.");
                trustManagers = trustAllTrustManager();
            }

            TlsKeyManagersProvider provider = standardOptions.get(SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER);
            KeyManager[] keyManagers = provider.keyManagers();

            try {
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                // http://download.java.net/jdk9/docs/technotes/guides/security/jsse/JSSERefGuide.html
                sslcontext.init(keyManagers, trustManagers, null);
                return sslcontext;
            } catch (final NoSuchAlgorithmException | KeyManagementException ex) {
                throw new SSLInitializationException(ex.getMessage(), ex);
            }
        }

        /**
         * Insecure trust manager to trust all certs. Should only be used for testing.
         */
        private static TrustManager[] trustAllTrustManager() {
            return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        log.debug(() -> "Accepting a client certificate: " + x509Certificates[0].getSubjectDN());
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        log.debug(() -> "Accepting a client certificate: " + x509Certificates[0].getSubjectDN());
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
        }

        private SocketConfig buildSocketConfig(AttributeMap standardOptions) {
            return SocketConfig.custom()
                               .setSoKeepAlive(standardOptions.get(SdkHttpConfigurationOption.TCP_KEEPALIVE))
                               .setSoTimeout(saturatedCast(standardOptions.get(SdkHttpConfigurationOption.READ_TIMEOUT)
                                                                          .toMillis()), TimeUnit.MILLISECONDS)
                               .setTcpNoDelay(true)
                               .build();
        }

        private Registry<ConnectionSocketFactory> createSocketFactoryRegistry(ConnectionSocketFactory sslSocketFactory) {
            return RegistryBuilder.<ConnectionSocketFactory>create()
                                  .register("http", PlainConnectionSocketFactory.getSocketFactory())
                                  .register("https", sslSocketFactory)
                                  .build();
        }
    }
}
