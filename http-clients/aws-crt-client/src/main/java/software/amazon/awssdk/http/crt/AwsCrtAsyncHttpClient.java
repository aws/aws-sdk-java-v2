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

import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.internal.AwsCrtAsyncHttpStreamAdapter;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An implementation of {@link SdkHttpClient} that uses the AWS Common Runtime (CRT) Http Client to communicate with
 * Http Web Services. This client is asynchronous and uses non-blocking IO.
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class AwsCrtAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = Logger.loggerFor(AwsCrtAsyncHttpClient.class);

    private static final String AWS_COMMON_RUNTIME = "AwsCommonRuntime";
    private static final String NULL_REQUEST_ERROR_MESSAGE = "SdkHttpRequest must not be null";
    private static final String NULL_URI_ERROR_MESSAGE = "URI must not be null";
    private static final int DEFAULT_STREAM_WINDOW_SIZE = 16 * 1024 * 1024; // 16 MB

    private final Map<URI, HttpClientConnectionManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContext tlsContext;
    private final HttpProxyOptions proxyOptions;
    private final HttpMonitoringOptions monitoringOptions;
    private final long maxConnectionIdleInMilliseconds;
    private final int initialWindowSize;
    private final int maxConnectionsPerEndpoint;
    private boolean isClosed = false;

    private AwsCrtAsyncHttpClient(DefaultBuilder builder, AttributeMap config) {
        int maxConns = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);

        Validate.isPositive(maxConns, "maxConns");
        Validate.notNull(builder.cipherPreference, "cipherPreference");
        Validate.isPositive(builder.initialWindowSize, "initialWindowSize");
        Validate.notNull(builder.eventLoopGroup, "eventLoopGroup");
        Validate.notNull(builder.hostResolver, "hostResolver");

        try (ClientBootstrap clientBootstrap = new ClientBootstrap(builder.eventLoopGroup, builder.hostResolver);
             SocketOptions clientSocketOptions = new SocketOptions();
             TlsContextOptions clientTlsContextOptions = TlsContextOptions.createDefaultClient() // NOSONAR
                     .withCipherPreference(builder.cipherPreference)
                     .withVerifyPeer(!config.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES));
             TlsContext clientTlsContext = new TlsContext(clientTlsContextOptions)) {

            this.bootstrap = registerOwnedResource(clientBootstrap);
            this.socketOptions = registerOwnedResource(clientSocketOptions);
            this.tlsContext = registerOwnedResource(clientTlsContext);

            this.initialWindowSize = builder.initialWindowSize;
            this.maxConnectionsPerEndpoint = maxConns;
            this.monitoringOptions = builder.monitoringOptions;
            this.maxConnectionIdleInMilliseconds = config.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis();

            this.proxyOptions = buildProxyOptions(builder.proxyConfiguration);
        }
    }

    private HttpProxyOptions buildProxyOptions(ProxyConfiguration proxyConfiguration) {
        if (proxyConfiguration != null) {
            HttpProxyOptions clientProxyOptions = new HttpProxyOptions();

            clientProxyOptions.setHost(proxyConfiguration.host());
            clientProxyOptions.setPort(proxyConfiguration.port());
            if (proxyConfiguration.scheme() != null && proxyConfiguration.scheme().equalsIgnoreCase("https")) {
                clientProxyOptions.setTlsContext(tlsContext);
            }

            if (proxyConfiguration.username() != null && proxyConfiguration.password() != null) {
                clientProxyOptions.setAuthorizationUsername(proxyConfiguration.username());
                clientProxyOptions.setAuthorizationPassword(proxyConfiguration.password());
                clientProxyOptions.setAuthorizationType(HttpProxyOptions.HttpProxyAuthorizationType.Basic);
            } else {
                clientProxyOptions.setAuthorizationType(HttpProxyOptions.HttpProxyAuthorizationType.None);
            }

            return clientProxyOptions;
        } else {
            return null;
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

    private static URI toUri(SdkHttpRequest sdkRequest) {
        Validate.notNull(sdkRequest, NULL_REQUEST_ERROR_MESSAGE);
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(), sdkRequest.port(),
                null, null, null));
    }

    public static Builder builder() {
        return new DefaultBuilder();
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
                .withWindowSize(initialWindowSize)
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
        Validate.notNull(uri, NULL_URI_ERROR_MESSAGE);
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("Client is closed. No more requests can be made with this client.");
            }

            HttpClientConnectionManager connPool = connectionPools.computeIfAbsent(uri, this::createConnectionPool);
            connPool.addRef();
            return connPool;
        }
    }

    private List<HttpHeader> createHttpHeaderList(URI uri, AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        // worst case we may add 3 more headers here
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.headers().size() + 3);

        // Set Host Header if needed
        if (isNullOrEmpty(sdkRequest.headers().get(Header.HOST))) {
            crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        if (isNullOrEmpty(sdkRequest.headers().get(Header.CONNECTION))) {
            crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (isNullOrEmpty(sdkRequest.headers().get(Header.CONTENT_LENGTH)) && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        // Add the rest of the Headers
        for (Map.Entry<String, List<String>> headerList: sdkRequest.headers().entrySet()) {
            for (String val: headerList.getValue()) {
                HttpHeader h = new HttpHeader(headerList.getKey(), val);
                crtHeaderList.add(h);
            }
        }

        return crtHeaderList;
    }

    private HttpHeader[] asArray(List<HttpHeader> crtHeaderList) {
        return crtHeaderList.toArray(new HttpHeader[crtHeaderList.size()]);
    }

    private HttpRequest toCrtRequest(URI uri, AsyncExecuteRequest asyncRequest, AwsCrtAsyncHttpStreamAdapter crtToSdkAdapter) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        Validate.notNull(uri, NULL_URI_ERROR_MESSAGE);
        Validate.notNull(sdkRequest, NULL_REQUEST_ERROR_MESSAGE);

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.length() == 0) {
            encodedPath = "/";
        }

        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(sdkRequest.rawQueryParameters())
                .map(value -> "?" + value)
                .orElse("");

        HttpHeader[] crtHeaderArray = asArray(createHttpHeaderList(uri, asyncRequest));

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, crtToSdkAdapter);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest asyncRequest) {

        Validate.notNull(asyncRequest, "AsyncExecuteRequest must not be null");
        Validate.notNull(asyncRequest.request(), NULL_REQUEST_ERROR_MESSAGE);
        Validate.notNull(asyncRequest.requestContentPublisher(), "RequestContentPublisher must not be null");
        Validate.notNull(asyncRequest.responseHandler(), "ResponseHandler must not be null");

        URI uri = toUri(asyncRequest.request());

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
        try (HttpClientConnectionManager crtConnPool = getOrCreateConnectionPool(uri)) {
            CompletableFuture<Void> requestFuture = new CompletableFuture<>();

            // When a Connection is ready from the Connection Pool, schedule the Request on the connection
            crtConnPool.acquireConnection()
                    .whenComplete((crtConn, throwable) -> {
                        // If we didn't get a connection for some reason, fail the request
                        if (throwable != null) {
                            try {
                                asyncRequest.responseHandler().onError(throwable);
                            } catch (Exception e) {
                                log.error(() -> String.format("Exception while handling error: %s", e.toString()));
                            }
                            requestFuture.completeExceptionally(new IOException(
                                    "Crt exception while acquiring connection", throwable));
                            return;
                        }

                        AwsCrtAsyncHttpStreamAdapter crtToSdkAdapter =
                                new AwsCrtAsyncHttpStreamAdapter(crtConn, requestFuture, asyncRequest, initialWindowSize);
                        HttpRequest crtRequest = toCrtRequest(uri, asyncRequest, crtToSdkAdapter);

                        // Submit the Request on this Connection
                        invokeSafely(() -> {
                            try {
                                crtConn.makeRequest(crtRequest, crtToSdkAdapter).activate();
                            } catch (IllegalStateException | CrtRuntimeException e) {
                                throw new IOException("Exception throw while submitting request to CRT http connection", e);
                            }
                        });
                    });

            return requestFuture;
        }
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
         * The maximum number of connections allowed per distinct endpoint
         * @param maxConnections maximum connections per endpoint
         * @return The builder of the method chaining.
         */
        Builder maxConnections(int maxConnections);

        /**
         * The AWS CRT TlsCipherPreference to use for this Client
         * @param tlsCipherPreference The AWS Common Runtime TlsCipherPreference
         * @return The builder of the method chaining.
         */
        Builder tlsCipherPreference(TlsCipherPreference tlsCipherPreference);

        /**
         * The AWS CRT WindowSize to use for this HttpClient.
         *
         * For an http/1.1 connection, this represents  the number of unread bytes that can be buffered in the
         * ResponseBodyPublisher before we stop reading from the underlying TCP socket and wait for the Subscriber
         * to read more data.
         *
         * @param initialWindowSize The AWS Common Runtime WindowSize
         * @return The builder of the method chaining.
         */
        Builder initialWindowSize(int initialWindowSize);

        /**
         * The AWS CRT EventLoopGroup to use for this Client.
         * @param eventLoopGroup The AWS CRT EventLoopGroup to use for this client.
         * @return The builder of the method chaining.
         */
        Builder eventLoopGroup(EventLoopGroup eventLoopGroup);

        /**
         * The AWS CRT HostResolver to use for this Client.
         * @param hostResolver The AWS CRT HostResolver to use for this client.
         * @return The builder of the method chaining.
         */
        Builder hostResolver(HostResolver hostResolver);

        /**
         * Sets the http proxy configuration to use for this client.
         * @param proxyConfiguration The http proxy configuration to use
         * @return The builder of the method chaining.
         */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Sets the http monitoring options for all connections established by this client.
         * @param monitoringOptions The http monitoring options to use
         * @return The builder of the method chaining.
         */
        Builder monitoringOptions(HttpMonitoringOptions monitoringOptions);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);
      
        /**
         * Sets the http proxy configuration to use for this client.
         *
         * @param proxyConfigurationBuilderConsumer The consumer of the proxy configuration builder object.
         * @return the builder for method chaining.
         */
        Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfigurationBuilderConsumer);
    }

    /**
     * Factory that allows more advanced configuration of the AWS CRT HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private TlsCipherPreference cipherPreference = TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
        private int initialWindowSize = DEFAULT_STREAM_WINDOW_SIZE;
        private EventLoopGroup eventLoopGroup;
        private HostResolver hostResolver;
        private ProxyConfiguration proxyConfiguration;
        private HttpMonitoringOptions monitoringOptions;

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
        public Builder maxConnections(int maxConnections) {
            Validate.isPositive(maxConnections, "maxConnections");
            standardOptions.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
            return this;
        }

        @Override
        public Builder tlsCipherPreference(TlsCipherPreference tlsCipherPreference) {
            Validate.notNull(tlsCipherPreference, "cipherPreference");
            Validate.isTrue(TlsContextOptions.isCipherPreferenceSupported(tlsCipherPreference),
                            "TlsCipherPreference not supported on current Platform");
            this.cipherPreference = tlsCipherPreference;
            return this;
        }

        @Override
        public Builder initialWindowSize(int initialWindowSize) {
            Validate.isPositive(initialWindowSize, "initialWindowSize");
            this.initialWindowSize = initialWindowSize;
            return this;
        }

        @Override
        public Builder eventLoopGroup(EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
            return this;
        }

        @Override
        public Builder hostResolver(HostResolver hostResolver) {
            this.hostResolver = hostResolver;
            return this;
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        @Override
        public Builder monitoringOptions(HttpMonitoringOptions monitoringOptions) {
            this.monitoringOptions = monitoringOptions;
            return this;
        }

        @Override
        public Builder connectionMaxIdleTime(Duration connectionMaxIdleTime) {
            standardOptions.put(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT, connectionMaxIdleTime);
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
