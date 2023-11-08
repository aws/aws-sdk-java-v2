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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.AwsCrtClientBuilderBase;
import software.amazon.awssdk.http.crt.internal.AwsCrtHttpClientBase;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestExecutor;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * An implementation of {@link SdkHttpClient} that uses the AWS Common Runtime (CRT) Http Client to communicate with
 * Http Web Services. This client has a synchronous interface, but uses non-blocking IO.
 *
 * <p>This can be created via {@link #builder()}</p>
 * {@snippet :
 * SdkHttpClient client = software.amazon.awssdk.http.crt.AwsCrtHttpClient.builder()
 * .maxConcurrency(100)
 * .connectionTimeout(Duration.ofSeconds(1))
 * .connectionMaxIdleTime(Duration.ofSeconds(5))
 * .build();
 *}
 *
 */
@SdkPublicApi
public final class AwsCrtHttpClient extends AwsCrtHttpClientBase implements SdkHttpClient {

    private AwsCrtHttpClient(DefaultBuilder builder, AttributeMap config) {
        super(builder, config);
    }

    public static AwsCrtHttpClient.Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Create a {@link AwsCrtHttpClient} client with the default configuration
     *
     * @return an {@link SdkHttpClient}
     */
    public static AwsCrtHttpClient create() {
        return new DefaultBuilder().build();
    }

    @Override
    public String clientName() {
        return SdkHttpClient.super.clientName();
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        request.metricCollector()
               .ifPresent(metricCollector -> metricCollector.reportMetric(HTTP_CLIENT_NAME, clientName()));

        /*
         * See the note on getOrCreateConnectionPool()
         *
         * In particular, this returns a ref-counted object and calling getOrCreateConnectionPool
         * increments the ref count by one.  We add a try-with-resources to release our ref
         * once we have successfully submitted a request.  In this way, we avoid a race condition
         * when close/shutdown is called from another thread while this function is executing (i.e.
         * we have a pool and no one can destroy it underneath us until we've finished submitting the
         * request)
         */
        try (HttpClientConnectionManager crtConnPool = getOrCreateConnectionPool(poolKey(request.httpRequest()))) {
            CrtRequestContext context = CrtRequestContext.builder()
                                                         .crtConnPool(crtConnPool)
                                                         .readBufferSize(this.readBufferSize)
                                                         .request(request)
                                                         .build();
            return new ExecutableHttpRequest() {
                volatile CompletableFuture<SdkHttpFullResponse> responseFuture;

                @Override
                public HttpExecuteResponse call() throws IOException {
                    HttpExecuteResponse.Builder builder = HttpExecuteResponse.builder();

                    try {
                        responseFuture = new CrtRequestExecutor().execute(context);
                        SdkHttpFullResponse response = responseFuture.get();
                        builder.response(response);
                        builder.responseBody(response.content().orElse(null));
                        return builder.build();
                    } catch (InterruptedException | ExecutionException e) {
                        if (e.getCause() instanceof IOException) {
                            throw (IOException) e.getCause();
                        }

                        if (e.getCause() instanceof HttpException) {
                            throw (HttpException) e.getCause();
                        }

                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void abort() {
                    if (responseFuture != null) {
                        responseFuture.cancel(true);
                    }
                }
            };
        }
    }

    /**
     * Builder that allows configuration of the AWS CRT HTTP implementation.
     */
    public interface Builder extends SdkHttpClient.Builder<AwsCrtHttpClient.Builder> {

        /**
         * The Maximum number of allowed concurrent requests. For HTTP/1.1 this is the same as max connections.
         * @param maxConcurrency maximum concurrency per endpoint
         * @return The builder of the method chaining.
         */
        AwsCrtHttpClient.Builder maxConcurrency(Integer maxConcurrency);

        /**
         * Configures the number of unread bytes that can be buffered in the
         * client before we stop reading from the underlying TCP socket and wait for the Subscriber
         * to read more data.
         *
         * @param readBufferSize The number of bytes that can be buffered.
         * @return The builder of the method chaining.
         */
        AwsCrtHttpClient.Builder readBufferSizeInBytes(Long readBufferSize);

        /**
         * Sets the http proxy configuration to use for this client.
         * @param proxyConfiguration The http proxy configuration to use
         * @return The builder of the method chaining.
         */
        AwsCrtHttpClient.Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Sets the http proxy configuration to use for this client.
         *
         * @param proxyConfigurationBuilderConsumer The consumer of the proxy configuration builder object.
         * @return the builder for method chaining.
         */
        AwsCrtHttpClient.Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer);

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
        AwsCrtHttpClient.Builder connectionHealthConfiguration(ConnectionHealthConfiguration healthChecksConfiguration);

        /**
         * A convenience method that creates an instance of the {@link ConnectionHealthConfiguration} builder, avoiding the
         * need to create one manually via {@link ConnectionHealthConfiguration#builder()}.
         *
         * @param healthChecksConfigurationBuilder The health checks config builder to use
         * @return The builder of the method chaining.
         * @see #connectionHealthConfiguration(ConnectionHealthConfiguration)
         */
        AwsCrtHttpClient.Builder connectionHealthConfiguration(Consumer<ConnectionHealthConfiguration.Builder>
                                                                        healthChecksConfigurationBuilder);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         * @param connectionMaxIdleTime the maximum amount of connection idle time
         * @return The builder of the method chaining.
         */
        AwsCrtHttpClient.Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         * @param connectionTimeout timeout
         * @return The builder of the method chaining.
         */
        AwsCrtHttpClient.Builder connectionTimeout(Duration connectionTimeout);

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
        AwsCrtHttpClient.Builder tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration);

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
        AwsCrtHttpClient.Builder tcpKeepAliveConfiguration(Consumer<TcpKeepAliveConfiguration.Builder>
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
        AwsCrtHttpClient.Builder postQuantumTlsEnabled(Boolean postQuantumTlsEnabled);
    }

    /**
     * Factory that allows more advanced configuration of the AWS CRT HTTP implementation.
     * Use {@link #builder()} to configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder
        extends AwsCrtClientBuilderBase<AwsCrtHttpClient.Builder> implements AwsCrtHttpClient.Builder {


        @Override
        public AwsCrtHttpClient build() {
            return new AwsCrtHttpClient(this, getAttributeMap().build()
                                                         .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }

        @Override
        public AwsCrtHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new AwsCrtHttpClient(this, getAttributeMap().build()
                                                         .merge(serviceDefaults)
                                                         .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }
    }

}
