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

package software.amazon.awssdk.http.apache.async;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.hc.core5.http2.HttpVersionPolicy.FORCE_HTTP_1;
import static org.apache.hc.core5.http2.HttpVersionPolicy.FORCE_HTTP_2;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.READ_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.config.H2Config;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLInitializationException;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses the Apache 5 asynchronous HTTP/2 client to communicate
 * with the service. This client is equivalent in power to the {@code NettyNioAsyncHttpClient}.
 */
@SdkPublicApi
public final class ApacheAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = Logger.loggerFor(ApacheAsyncHttpClient.class);

    private final ApacheAsyncRequestFactory apacheAsyncRequestFactory = new ApacheAsyncRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final ApacheAsyncRequestConfig requestConfig;
    private final Protocol protocol;
    private final Integer maxHttp2Streams;

    private ApacheAsyncHttpClient(DefaultBuilder builder, AttributeMap resolvedOptions) {
        this.httpClient = createClient(builder, resolvedOptions);
        this.requestConfig = createRequestConfig(builder, resolvedOptions);
        this.protocol = resolvedOptions.get(SdkHttpConfigurationOption.PROTOCOL);
        this.maxHttp2Streams = builder.maxHttp2Streams;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public String clientName() {
        return "ApacheAsync";
    }

    private ConnectionManagerAwareHttpClient createClient(
        ApacheAsyncHttpClient.DefaultBuilder configuration,
        AttributeMap standardOptions) {
        ApacheConnectionManagerFactory cmFactory = new ApacheConnectionManagerFactory();

        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        // from the reaper. See https://github.com/aws/aws-sdk-java/issues/722.
        // TODO: Capture performance metrics for connection establishment (cf. ClientConnectionManagerFactory)
        PoolingAsyncClientConnectionManager cm = cmFactory.create(standardOptions);

        H2Config.Builder h2config = H2Config.custom().setPushEnabled(false);
        Optional.ofNullable(maxHttp2Streams).ifPresent(h2config::setMaxConcurrentStreams);

        int soTimeout = saturatedCast(standardOptions.get(SdkHttpConfigurationOption.READ_TIMEOUT).toMillis());
        Protocol protocol = standardOptions.get(SdkHttpConfigurationOption.PROTOCOL);
        HttpAsyncClientBuilder builder = HttpAsyncClients
            .custom()
            .setConnectionManager(cm)
            .setVersionPolicy(protocol == Protocol.HTTP2 ? FORCE_HTTP_2 : FORCE_HTTP_1)
            .setH2Config(h2config.build())
            .setKeepAliveStrategy(buildKeepAliveStrategy(standardOptions))
            .disableAuthCaching()
            .disableConnectionState()
            .disableCookieManagement()
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .setIOReactorConfig(IOReactorConfig.custom()
                                               .setSoKeepAlive(false)
                                               .setSoTimeout(Timeout.ofMilliseconds(soTimeout))
                                               .setTcpNoDelay(true)
                                               .setIoThreadCount(max(2, min(Runtime.getRuntime().availableProcessors() / 2, 8)))
                                               .setSoLinger(-1, TimeUnit.MILLISECONDS)
                                               .build())
            .setUserAgent(""); // SDK will set the user agent header in the pipeline. Don't let Apache waste time

        addProxyConfig(builder, configuration);

        Duration maxIdleTime = standardOptions.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT);
        IdleConnectionReaper.getInstance().registerConnectionManager(cm, maxIdleTime.toMillis());

        CloseableHttpAsyncClient client = builder.build();
        client.start();
        return new ApacheSdkHttpClient(client, cm);
    }

    private void addProxyConfig(HttpAsyncClientBuilder builder, ApacheAsyncHttpClient.DefaultBuilder configuration) {
        ProxyConfiguration proxyConfiguration = configuration.proxyConfiguration;
        InetAddress localAddress = configuration.localAddress;

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
                                                    proxyConfiguration.nonProxyHosts(),
                                                    localAddress);
        }

        CredentialsProvider credentialsProvider = configuration.credentialsProvider;
        if (isAuthenticatedProxy(proxyConfiguration)) {
            credentialsProvider = ApacheAsyncUtils.newProxyCredentialsProvider(proxyConfiguration);
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

    private boolean isAuthenticatedProxy(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.username() != null && proxyConfiguration.password() != null;
    }

    private boolean isProxyEnabled(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.host() != null
               && proxyConfiguration.port() > 0;
    }

    @Override
    public void close() {
        PoolingAsyncClientConnectionManager cm = httpClient.getAsyncClientConnectionManager();
        IdleConnectionReaper.getInstance().deregisterConnectionManager(cm);
        cm.close();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        AsyncRequestProducer apacheRequest = apacheAsyncRequestFactory.create(request, protocol == Protocol.HTTP2);

        HttpClientContext context = ApacheAsyncUtils.newClientContext(requestConfig.proxyConfiguration());
        context.setRequestConfig(ApacheAsyncUtils.getRequestConfig(request.request(), requestConfig));

        CompletableFuture<Void> ret = new CompletableFuture<>();
        SdkAsyncHttpResponseHandler sdkAsyncHttpResponseHandler = request.responseHandler();
        AsyncResponseConsumer<Void> consumer = new ReactiveResponseConsumer(
            new SdkAsyncHttpResponseFutureCallback(sdkAsyncHttpResponseHandler));
        httpClient.execute(apacheRequest, consumer, null, context,
                           new CompletableFutureCallback<>(ret, sdkAsyncHttpResponseHandler));
        return ret;
    }

    private static final class SdkAsyncHttpResponseFutureCallback
        implements FutureCallback<Message<HttpResponse, Publisher<ByteBuffer>>> {
        private final SdkAsyncHttpResponseHandler sdkAsyncHttpResponseHandler;

        private SdkAsyncHttpResponseFutureCallback(SdkAsyncHttpResponseHandler sdkAsyncHttpResponseHandler) {
            this.sdkAsyncHttpResponseHandler = sdkAsyncHttpResponseHandler;
        }

        @Override
        public void completed(Message<HttpResponse, Publisher<ByteBuffer>> result) {
            SdkHttpResponse response = createResponse(result.getHead());
            sdkAsyncHttpResponseHandler.onHeaders(response);
            sdkAsyncHttpResponseHandler.onStream(result.getBody());
        }

        @Override
        public void failed(Exception ex) {
            sdkAsyncHttpResponseHandler.onError(ex);
        }

        @Override
        public void cancelled() {
            sdkAsyncHttpResponseHandler.onError(new SdkCancellationException(
                "Subscriber cancelled before all events were published"));
        }
    }

    private static final class CompletableFutureCallback<T> implements FutureCallback<T> {
        private final CompletableFuture<T> completableFuture;
        private final SdkAsyncHttpResponseHandler sdkAsyncHttpResponseHandler;

        private CompletableFutureCallback(CompletableFuture<T> completableFuture,
                                          SdkAsyncHttpResponseHandler sdkAsyncHttpResponseHandler) {
            this.completableFuture = completableFuture;
            this.sdkAsyncHttpResponseHandler = sdkAsyncHttpResponseHandler;
        }

        @Override
        public void completed(T result) {
            completableFuture.complete(result);
        }

        @Override
        public void failed(Exception ex) {
            sdkAsyncHttpResponseHandler.onError(ex);
            completableFuture.completeExceptionally(ex);
        }

        @Override
        public void cancelled() {
            failed(new SdkCancellationException("Subscriber cancelled before all events were published"));
        }
    }

    static SdkHttpResponse createResponse(HttpResponse apacheHttpResponse) {
        return SdkHttpResponse.builder()
                              .statusCode(apacheHttpResponse.getCode())
                              .statusText(apacheHttpResponse.getReasonPhrase())
                              .headers(transformHeaders(apacheHttpResponse))
                              .build();
    }

    private static Map<String, List<String>> transformHeaders(HttpResponse apacheHttpResponse) {
        return Stream.of(apacheHttpResponse.getHeaders())
                     .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }

    private ApacheAsyncRequestConfig createRequestConfig(DefaultBuilder builder, AttributeMap resolvedOptions) {
        return ApacheAsyncRequestConfig.builder()
                                       .connectionTimeout(resolvedOptions.get(CONNECTION_TIMEOUT))
                                       .connectionAcquireTimeout(resolvedOptions.get(CONNECTION_ACQUIRE_TIMEOUT))
                                       .proxyConfiguration(builder.proxyConfiguration)
                                       .expectContinueEnabled(Optional.ofNullable(builder.expectContinueEnabled)
                                                                      .orElse(DefaultConfiguration.EXPECT_CONTINUE_ENABLED))
                                       .build();
    }

    public interface Builder extends SdkAsyncHttpClient.Builder<ApacheAsyncHttpClient.Builder> {
        /**
         * The amount of time to wait for data to be transferred over an established, open connection before the
         * connection is timed out. A duration of 0 means infinity, and is not recommended.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out. A
         * duration of 0 means infinity, and is not recommended.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         *
         * @param connectionAcquisitionTimeout the timeout duration
         * @return this builder for method chaining
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
        @ReviewBeforeRelease("Should this be moved into ProxyConfiguration?")
        Builder localAddress(InetAddress localAddress);

        /**
         * Configure whether the client should send an HTTP expect-continue handshake before large PUT requests.
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
         * Sets the HTTP protocol to use (i.e. HTTP/1.1 or HTTP/2). Not all services support HTTP/2.
         */
        Builder protocol(Protocol protocol);

        /**
         * Sets the max number of concurrent streams for an HTTP/2 connection. This setting is only respected when the HTTP/2
         * protocol is used.
         * <p>
         * Note that this cannot exceed the value of the {@code MAX_CONCURRENT_STREAMS} setting returned by the service. If it
         * does the service setting is used instead.
         */
        Builder maxHttp2Streams(Integer maxHttp2Streams);

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
         * <p>
         * The default used by the client will be {@link SystemPropertyTlsKeyManagersProvider}. Configure an instance of
         * {@link software.amazon.awssdk.internal.http.NoneTlsKeyManagersProvider} or another implementation of
         * {@link TlsKeyManagersProvider} to override it.
         */
        Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);
    }

    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private Integer maxHttp2Streams;
        private HttpRoutePlanner httpRoutePlanner;
        private CredentialsProvider credentialsProvider;

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
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, maxIdleConnectionTimeout);
            return this;
        }

        public void setConnectionMaxIdleTime(Duration connectionMaxIdleTime) {
            connectionMaxIdleTime(connectionMaxIdleTime);
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            AttributeMap resolvedOptions = standardOptions.build().merge(serviceDefaults).merge(GLOBAL_HTTP_DEFAULTS);
            return new ApacheAsyncHttpClient(this, resolvedOptions);
        }

        @Override
        public Builder protocol(Protocol protocol) {
            standardOptions.put(SdkHttpConfigurationOption.PROTOCOL, protocol);
            return this;
        }

        public void setProtocol(Protocol protocol) {
            protocol(protocol);
        }

        @Override
        public Builder maxHttp2Streams(Integer maxHttp2Streams) {
            this.maxHttp2Streams = maxHttp2Streams;
            return this;
        }

        public void setMaxHttp2Streams(Integer maxHttp2Streams) {
            maxHttp2Streams(maxHttp2Streams);
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
        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            standardOptions.put(TLS_KEY_MANAGERS_PROVIDER, tlsKeyManagersProvider);
            return this;
        }

        public void setTlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            tlsKeyManagersProvider(tlsKeyManagersProvider);
        }
    }

    private static class ApacheConnectionManagerFactory {
        public PoolingAsyncClientConnectionManager create(AttributeMap standardOptions) {
            TlsStrategy tlsStrategy = getTlsStrategy(standardOptions);

            TimeValue connectionTtl = TimeValue.ofMilliseconds(
                standardOptions.get(CONNECTION_TIME_TO_LIVE).toMillis());

            PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder
                .create()
                .setConnectionTimeToLive(connectionTtl)
                .setTlsStrategy(tlsStrategy)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setDnsResolver(new RandomizingSystemDnsResolver())
                .build();

            cm.setDefaultMaxPerRoute(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
            cm.setMaxTotal(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));

            return cm;
        }

        private static TlsStrategy getTlsStrategy(AttributeMap standardOptions) {
            HostnameVerifier hostNameVerifier = getHostNameVerifier(standardOptions);
            SSLContext sslContext = getSslContext(standardOptions);

            return ClientTlsStrategyBuilder.create()
                                           .setSslContext(sslContext)
                                           .setHostnameVerifier(hostNameVerifier)
                                           .build();
        }

        private static HostnameVerifier getHostNameVerifier(AttributeMap standardOptions) {
            return standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)
                   ? NoopHostnameVerifier.INSTANCE
                   : HttpsSupport.getDefaultHostnameVerifier();
        }

        private static SSLContext getSslContext(AttributeMap standardOptions) {
            TrustManager[] trustManagers = null;
            if (standardOptions.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
                log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                               + "used for testing.");
                trustManagers = trustAllTrustManager();
            }

            TlsKeyManagersProvider provider = standardOptions.get(TLS_KEY_MANAGERS_PROVIDER);
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
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                        log.debug(() -> "Accepting a client certificate: " + x509Certificates[0].getSubjectDN());
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                        log.debug(() -> "Accepting a client certificate: " + x509Certificates[0].getSubjectDN());
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
        }

        private static final class RandomizingSystemDnsResolver implements DnsResolver {
            private final DnsResolver delegate = SystemDefaultDnsResolver.INSTANCE;

            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                List<InetAddress> ret = new ArrayList<>(Arrays.asList(delegate.resolve(host)));
                Collections.shuffle(ret);
                return ret.toArray(new InetAddress[] {});
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                return delegate.resolveCanonicalHostname(host);
            }
        }
    }
}
