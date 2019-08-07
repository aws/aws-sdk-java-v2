/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.apache;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.READ_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
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
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.DefaultConfiguration;
import software.amazon.awssdk.http.apache.internal.SdkProxyRoutePlanner;
import software.amazon.awssdk.http.apache.internal.conn.ClientConnectionManagerFactory;
import software.amazon.awssdk.http.apache.internal.conn.IdleConnectionReaper;
import software.amazon.awssdk.http.apache.internal.conn.SdkConnectionKeepAliveStrategy;
import software.amazon.awssdk.http.apache.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClient;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkHttpClient} that uses Apache HTTP client to communicate with the service. This is the most
 * powerful synchronous client that adds an extra dependency and additional startup latency in exchange for more functionality,
 * like support for HTTP proxies.
 *
 * <p>See software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient for an alternative implementation.</p>
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class ApacheHttpClient implements SdkHttpClient {

    public static final String CLIENT_NAME = "Apache";

    private static final Logger log = Logger.loggerFor(ApacheHttpClient.class);

    private final ApacheHttpRequestFactory apacheHttpRequestFactory = new ApacheHttpRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final ApacheHttpRequestConfig requestConfig;
    private final AttributeMap resolvedOptions;

    @SdkTestInternalApi
    ApacheHttpClient(ConnectionManagerAwareHttpClient httpClient,
                     ApacheHttpRequestConfig requestConfig,
                     AttributeMap resolvedOptions) {
        this.httpClient = httpClient;
        this.requestConfig = requestConfig;
        this.resolvedOptions = resolvedOptions;
    }

    private ApacheHttpClient(DefaultBuilder builder, AttributeMap resolvedOptions) {
        this.httpClient = createClient(builder, resolvedOptions);
        this.requestConfig = createRequestConfig(builder, resolvedOptions);
        this.resolvedOptions = resolvedOptions;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private ConnectionManagerAwareHttpClient createClient(ApacheHttpClient.DefaultBuilder configuration,
                                                          AttributeMap standardOptions) {
        ApacheConnectionManagerFactory cmFactory = new ApacheConnectionManagerFactory();

        HttpClientBuilder builder = HttpClients.custom();
        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        // from the reaper. See https://github.com/aws/aws-sdk-java/issues/722.
        HttpClientConnectionManager cm = cmFactory.create(configuration, standardOptions);

        builder.setRequestExecutor(new HttpRequestExecutor())
               // SDK handles decompression
               .disableContentCompression()
               .setKeepAliveStrategy(buildKeepAliveStrategy(standardOptions))
               .disableRedirectHandling()
               .disableAutomaticRetries()
               .setUserAgent("") // SDK will set the user agent header in the pipeline. Don't let Apache waste time
               .setConnectionManager(ClientConnectionManagerFactory.wrap(cm));

        addProxyConfig(builder, configuration);

        if (useIdleConnectionReaper(standardOptions)) {
            IdleConnectionReaper.getInstance().registerConnectionManager(
                    cm, standardOptions.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis());
        }

        return new ApacheSdkHttpClient(builder.build(), cm);
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
            credentialsProvider = ApacheUtils.newProxyCredentialsProvider(proxyConfiguration);
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
        return Boolean.TRUE.equals(standardOptions.get(REAP_IDLE_CONNECTIONS));
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
        HttpRequestBase apacheRequest = toApacheRequest(request);
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() throws IOException {
                return execute(apacheRequest);
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

    private HttpExecuteResponse execute(HttpRequestBase apacheRequest) throws IOException {
        HttpClientContext localRequestContext = ApacheUtils.newClientContext(requestConfig.proxyConfiguration());
        HttpResponse httpResponse = httpClient.execute(apacheRequest, localRequestContext);
        return createResponse(httpResponse, apacheRequest);
    }

    private HttpRequestBase toApacheRequest(HttpExecuteRequest request) {
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
    private HttpExecuteResponse createResponse(org.apache.http.HttpResponse apacheHttpResponse,
                                               HttpRequestBase apacheRequest) throws IOException {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .statusCode(apacheHttpResponse.getStatusLine().getStatusCode())
                                                  .statusText(apacheHttpResponse.getStatusLine().getReasonPhrase())
                                                  .headers(transformHeaders(apacheHttpResponse))
                                                  .build();
        AbortableInputStream responseBody = apacheHttpResponse.getEntity() != null ?
                                   toAbortableInputStream(apacheHttpResponse, apacheRequest) : null;

        return HttpExecuteResponse.builder().response(response).responseBody(responseBody).build();

    }

    private AbortableInputStream toAbortableInputStream(HttpResponse apacheHttpResponse, HttpRequestBase apacheRequest)
            throws IOException {
        return AbortableInputStream.create(apacheHttpResponse.getEntity().getContent(), apacheRequest::abort);
    }

    private Map<String, List<String>> transformHeaders(HttpResponse apacheHttpResponse) {
        return Stream.of(apacheHttpResponse.getAllHeaders())
                     .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }

    private ApacheHttpRequestConfig createRequestConfig(DefaultBuilder builder,
                                                        AttributeMap resolvedOptions) {
        return ApacheHttpRequestConfig.builder()
                                      .socketTimeout(resolvedOptions.get(READ_TIMEOUT))
                                      .connectionTimeout(resolvedOptions.get(CONNECTION_TIMEOUT))
                                      .connectionAcquireTimeout(resolvedOptions.get(CONNECTION_ACQUIRE_TIMEOUT))
                                      .proxyConfiguration(builder.proxyConfiguration)
                                      .localAddress(Optional.ofNullable(builder.localAddress).orElse(null))
                                      .expectContinueEnabled(Optional.ofNullable(builder.expectContinueEnabled)
                                                                     .orElse(DefaultConfiguration.EXPECT_CONTINUE_ENABLED))
                                      .build();
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
     * SdkHttpClient httpClient = SdkHttpClient.builder()
     * .socketTimeout(Duration.ofSeconds(10))
     * .build();
     * </pre>
     */
    public interface Builder extends SdkHttpClient.Builder<ApacheHttpClient.Builder> {

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
         * The maximum number of connections allowed in the connection pool. Each built HTTP client has it's own private
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
         * Configure the {@link TlsKeyManagersProvider} that will provide the {@link javax.net.ssl.KeyManager}s to use
         * when constructing the SSL context.
         */
        Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private HttpRoutePlanner httpRoutePlanner;
        private CredentialsProvider credentialsProvider;
        private TlsKeyManagersProvider tlsKeyManagersProvider = SystemPropertyTlsKeyManagersProvider.create();

        private DefaultBuilder() {
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(READ_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(CONNECTION_TIMEOUT, connectionTimeout);
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
            standardOptions.put(CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            connectionAcquisitionTimeout(connectionAcquisitionTimeout);
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptions.put(MAX_CONNECTIONS, maxConnections);
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
            standardOptions.put(CONNECTION_TIME_TO_LIVE, connectionTimeToLive);
            return this;
        }

        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            connectionTimeToLive(connectionTimeToLive);
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            standardOptions.put(CONNECTION_MAX_IDLE_TIMEOUT, maxIdleConnectionTimeout);
            return this;
        }

        public void setConnectionMaxIdleTime(Duration connectionMaxIdleTime) {
            connectionMaxIdleTime(connectionMaxIdleTime);
        }

        @Override
        public Builder useIdleConnectionReaper(Boolean useIdleConnectionReaper) {
            standardOptions.put(REAP_IDLE_CONNECTIONS, useIdleConnectionReaper);
            return this;
        }

        public void setUseIdleConnectionReaper(Boolean useIdleConnectionReaper) {
            useIdleConnectionReaper(useIdleConnectionReaper);
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

        @Override
        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            this.tlsKeyManagersProvider = tlsKeyManagersProvider;
            return this;
        }

        public void setTlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            tlsKeyManagersProvider(tlsKeyManagersProvider);
        }

        public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
            credentialsProvider(credentialsProvider);
        }

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            AttributeMap resolvedOptions = standardOptions.build().merge(serviceDefaults).merge(GLOBAL_HTTP_DEFAULTS);
            return new ApacheHttpClient(this, resolvedOptions);
        }
    }

    private static class ApacheConnectionManagerFactory {

        public HttpClientConnectionManager create(ApacheHttpClient.DefaultBuilder configuration,
                                                  AttributeMap standardOptions) {
            ConnectionSocketFactory sslsf = getPreferredSocketFactory(configuration, standardOptions);

            PoolingHttpClientConnectionManager cm = new
                    PoolingHttpClientConnectionManager(
                    createSocketFactoryRegistry(sslsf),
                    null,
                    DefaultSchemePortResolver.INSTANCE,
                    null,
                    standardOptions.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE).toMillis(),
                    TimeUnit.MILLISECONDS);

            cm.setDefaultMaxPerRoute(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
            cm.setMaxTotal(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
            cm.setDefaultSocketConfig(buildSocketConfig(standardOptions));

            return cm;
        }

        private ConnectionSocketFactory getPreferredSocketFactory(ApacheHttpClient.DefaultBuilder configuration,
                                                                  AttributeMap standardOptions) {
            // TODO v2 custom socket factory
            return new SdkTlsSocketFactory(getSslContext(configuration.tlsKeyManagersProvider, standardOptions),
                                           getHostNameVerifier(standardOptions));
        }

        private HostnameVerifier getHostNameVerifier(AttributeMap standardOptions) {
            return standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)
                   ? NoopHostnameVerifier.INSTANCE
                   : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        }

        private SSLContext getSslContext(TlsKeyManagersProvider keyManagersProvider, AttributeMap standardOptions) {
            TrustManager[] trustManagers = null;
            if (standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
                log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                               + "used for testing.");
                trustManagers = trustAllTrustManager();
            }

            KeyManager[] keyManagers = null;
            if (keyManagersProvider != null) {
                keyManagers = keyManagersProvider.keyManagers();
            }

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
                               // TODO do we want to keep SO keep alive
                               .setSoKeepAlive(false)
                               .setSoTimeout(
                                       saturatedCast(standardOptions.get(SdkHttpConfigurationOption.READ_TIMEOUT)
                                                                    .toMillis()))
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
