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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache5.internal.async.Apache5AsyncRequestProducer;
import software.amazon.awssdk.http.apache5.internal.async.Apache5AsyncResponseConsumer;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses Apache HttpComponents 5 NIO
 * (non-blocking I/O) to communicate with the service.
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class Apache5NioAsyncHttpClient implements SdkAsyncHttpClient {

    private static final String CLIENT_NAME = "Apache5Nio";

    private final CloseableHttpAsyncClient httpAsyncClient;

    private Apache5NioAsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaults) {
        AttributeMap resolvedOptions = serviceDefaults.merge(builder.standardOptions.build())
                                                      .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS);
        this.httpAsyncClient = createAsyncClient(builder, resolvedOptions);
        this.httpAsyncClient.start();
        // Wait for the IO reactor to become active before accepting requests
        long deadline = System.currentTimeMillis() + 5000;
        while (this.httpAsyncClient.getStatus() != IOReactorStatus.ACTIVE
               && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Apache5NioAsyncHttpClient create() {
        return (Apache5NioAsyncHttpClient) builder().build();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Apache5AsyncRequestProducer requestProducer =
            new Apache5AsyncRequestProducer(request.request(), request.requestContentPublisher());
        Apache5AsyncResponseConsumer responseConsumer =
            new Apache5AsyncResponseConsumer(request.responseHandler(), future);

        httpAsyncClient.execute(requestProducer, responseConsumer, new FutureCallback<Void>() {
            @Override
            public void completed(Void result) {
                // future already completed by responseConsumer.streamEnd()
            }

            @Override
            public void failed(Exception ex) {
                request.responseHandler().onError(ex);
                future.completeExceptionally(ex);
            }

            @Override
            public void cancelled() {
                future.cancel(false);
            }
        });

        return future;
    }

    @Override
    public String clientName() {
        return CLIENT_NAME;
    }

    @Override
    public void close() {
        httpAsyncClient.close(CloseMode.GRACEFUL);
    }

    private CloseableHttpAsyncClient createAsyncClient(DefaultBuilder builder, AttributeMap options) {
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(toTimeout(options.get(SdkHttpConfigurationOption.READ_TIMEOUT)))
            .build();

        PoolingAsyncClientConnectionManager cm = createConnectionManager(builder, options);

        return HttpAsyncClients.custom()
                               .setConnectionManager(cm)
                               .setIOReactorConfig(ioReactorConfig)
                               .disableAutomaticRetries()
                               .disableRedirectHandling()
                               .disableContentCompression()
                               .setUserAgent("")
                               .build();
    }

    private PoolingAsyncClientConnectionManager createConnectionManager(DefaultBuilder builder, AttributeMap options) {
        TlsStrategy tlsStrategy = buildTlsStrategy(builder, options);

        PoolingAsyncClientConnectionManagerBuilder cmBuilder =
            PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(options.get(SdkHttpConfigurationOption.MAX_CONNECTIONS))
                .setMaxConnPerRoute(options.get(SdkHttpConfigurationOption.MAX_CONNECTIONS))
                .setConnectionTimeToLive(toTimeValue(options.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE)));

        if (tlsStrategy != null) {
            cmBuilder.setTlsStrategy(tlsStrategy);
        }

        return cmBuilder.build();
    }

    private TlsStrategy buildTlsStrategy(DefaultBuilder builder, AttributeMap options) {
        try {
            TlsKeyManagersProvider keyManagersProvider = builder.tlsKeyManagersProvider != null
                ? builder.tlsKeyManagersProvider
                : SystemPropertyTlsKeyManagersProvider.create();

            KeyManager[] keyManagers = keyManagersProvider.keyManagers();

            TrustManager[] trustManagers = null;
            if (builder.tlsTrustManagersProvider != null) {
                trustManagers = builder.tlsTrustManagersProvider.trustManagers();
            } else if (Boolean.TRUE.equals(options.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES))) {
                trustManagers = new TrustManager[]{new TrustAllManager()};
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return ClientTlsStrategyBuilder.create()
                                           .setSslContext(sslContext)
                                           .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create TLS strategy", e);
        }
    }

    private static Timeout toTimeout(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return Timeout.DISABLED;
        }
        return Timeout.ofMilliseconds(duration.toMillis());
    }

    private static TimeValue toTimeValue(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return TimeValue.NEG_ONE_MILLISECOND;
        }
        return TimeValue.ofMilliseconds(duration.toMillis());
    }

    /**
     * Trust-all TrustManager for testing/non-prod use.
     */
    private static final class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Builder for {@link Apache5NioAsyncHttpClient}.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<Builder> {

        /**
         * Sets the read (socket) timeout.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * Sets the connection timeout.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * Sets the maximum number of connections in the pool.
         */
        Builder maxConnections(Integer maxConnections);

        /**
         * Sets the proxy configuration.
         */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Sets the proxy configuration via a consumer.
         */
        default Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer) {
            ProxyConfiguration.Builder builder = ProxyConfiguration.builder();
            proxyConfigurationBuilderConsumer.accept(builder);
            return proxyConfiguration(builder.build());
        }

        /**
         * Sets the TLS key managers provider.
         */
        Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider);

        /**
         * Sets the TLS trust managers provider.
         */
        Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider);
    }

    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private TlsKeyManagersProvider tlsKeyManagersProvider;
        private TlsTrustManagersProvider tlsTrustManagersProvider;

        private DefaultBuilder() {
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.READ_TIMEOUT, socketTimeout);
            return this;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            Validate.isPositive(maxConnections, "maxConnections");
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            // Proxy support can be wired in a follow-up phase
            return this;
        }

        @Override
        public Builder tlsKeyManagersProvider(TlsKeyManagersProvider tlsKeyManagersProvider) {
            this.tlsKeyManagersProvider = tlsKeyManagersProvider;
            return this;
        }

        @Override
        public Builder tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            this.tlsTrustManagersProvider = tlsTrustManagersProvider;
            return this;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new Apache5NioAsyncHttpClient(this, serviceDefaults);
        }
    }
}
