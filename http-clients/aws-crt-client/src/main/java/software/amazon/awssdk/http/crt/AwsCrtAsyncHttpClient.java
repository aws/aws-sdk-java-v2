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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
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
public class AwsCrtAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = Logger.loggerFor(AwsCrtAsyncHttpClient.class);
    private static final String HOST_HEADER = "Host";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONNECTION = "Connection";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String AWS_COMMON_RUNTIME = "AwsCommonRuntime";
    private static final String NULL_REQUEST_ERROR_MESSAGE = "SdkHttpRequest must not be null";
    private static final String NULL_URI_ERROR_MESSAGE = "URI must not be null";
    private static final int DEFAULT_STREAM_WINDOW_SIZE = 16 * 1024 * 1024; // 16 MB

    private final Map<URI, HttpClientConnectionManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContextOptions tlsContextOptions;
    private final TlsContext tlsContext;
    private final int windowSize;
    private final int maxConnectionsPerEndpoint;
    private final boolean manualWindowManagement;

    public AwsCrtAsyncHttpClient(DefaultBuilder builder, AttributeMap config) {
        int maxConns = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);

        Validate.isPositive(maxConns, "maxConns");
        Validate.notNull(builder.cipherPreference, "cipherPreference");
        Validate.isPositive(builder.windowSize, "windowSize");
        Validate.notNull(builder.eventLoopGroup, "eventLoopGroup");
        Validate.notNull(builder.hostResolver, "hostResolver");

        /**
         * Must call own() in same order that CrtResources are created in, so that they will be closed in reverse order.
         *
         * Do NOT use Dependency Injection for Native CrtResources. It's possible to crash the JVM Process if Native
         * Resources are closed in the wrong order (Eg closing the Bootstrap/Threadpool when there are still open
         * connections). By creating and owning our own Native CrtResources we can guarantee that things are shutdown
         * in the correct order.
         */

        this.bootstrap = own(new ClientBootstrap(builder.eventLoopGroup, builder.hostResolver));
        this.socketOptions = own(new SocketOptions());

        /**
         * Sonar raises a false-positive that the TlsContextOptions created here will not be closed.  Using a "NOSONAR"
         * comment so that Sonar will ignore that false-positive.
         */
        this.tlsContextOptions = own(TlsContextOptions.createDefaultClient() // NOSONAR
                .withCipherPreference(builder.cipherPreference)
                .withVerifyPeer(builder.verifyPeer));

        this.tlsContext = own(new TlsContext(this.tlsContextOptions));
        this.windowSize = builder.windowSize;
        this.maxConnectionsPerEndpoint = maxConns;
        this.manualWindowManagement = builder.manualWindowManagement;
    }

    /**
     * Marks a Native CrtResource as owned by the current Java Object.
     * This will guarantee that any owned CrtResources are closed in reverse order when this Java Object is closed.
     *
     * @param subresource The Resource to own.
     * @param <T> The CrtResource Type
     * @return The CrtResource passed in
     */
    private <T extends CrtResource> T own(T subresource) {
        ownedSubResources.push(subresource);
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
        Validate.notNull(uri, NULL_URI_ERROR_MESSAGE);
        log.debug(() -> "Creating ConnectionPool for: URI:" + uri + ", MaxConns: " + maxConnectionsPerEndpoint);

        HttpClientConnectionManagerOptions options = new HttpClientConnectionManagerOptions()
                .withClientBootstrap(bootstrap)
                .withSocketOptions(socketOptions)
                .withTlsContext(tlsContext)
                .withUri(uri)
                .withWindowSize(windowSize)
                .withMaxConnections(maxConnectionsPerEndpoint)
                .withManualWindowManagement(manualWindowManagement);

        return HttpClientConnectionManager.create(options);
    }

    private HttpClientConnectionManager getOrCreateConnectionPool(URI uri) {
        Validate.notNull(uri, NULL_URI_ERROR_MESSAGE);
        HttpClientConnectionManager connPool = connectionPools.get(uri);

        if (connPool == null) {
            HttpClientConnectionManager newConnPool = createConnectionPool(uri);
            HttpClientConnectionManager alreadyExistingConnPool = connectionPools.putIfAbsent(uri, newConnPool);

            if (alreadyExistingConnPool == null) {
                connPool = newConnPool;
            } else {
                // Multiple threads trying to open connections to the same URI at once, close the newer one
                newConnPool.close();
                connPool = alreadyExistingConnPool;
            }
        }

        return connPool;
    }

    private List<HttpHeader> createHttpHeaderList(URI uri, AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.headers().size() + 2);

        // Set Host Header if needed
        if (isNullOrEmpty(sdkRequest.headers().get(HOST_HEADER))) {
            crtHeaderList.add(new HttpHeader(HOST_HEADER, uri.getHost()));
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        if (isNullOrEmpty(sdkRequest.headers().get(CONNECTION))) {
            crtHeaderList.add(new HttpHeader(CONNECTION, KEEP_ALIVE));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (isNullOrEmpty(sdkRequest.headers().get(CONTENT_LENGTH)) && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(CONTENT_LENGTH, Long.toString(contentLength.get())));
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
        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(sdkRequest.rawQueryParameters())
                .map(value -> "?" + value)
                .orElse("");

        HttpHeader[] crtHeaderArray = asArray(createHttpHeaderList(uri, asyncRequest));

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, crtToSdkAdapter);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest asyncRequest) {
        if (isClosed.get()) {
            throw new IllegalStateException("Client is closed. No more requests can be made with this client.");
        }
        Validate.notNull(asyncRequest, "AsyncExecuteRequest must not be null");
        Validate.notNull(asyncRequest.request(), NULL_REQUEST_ERROR_MESSAGE);
        Validate.notNull(asyncRequest.requestContentPublisher(), "RequestContentPublisher must not be null");
        Validate.notNull(asyncRequest.responseHandler(), "ResponseHandler must not be null");

        URI uri = toUri(asyncRequest.request());
        HttpClientConnectionManager crtConnPool = getOrCreateConnectionPool(uri);
        CompletableFuture<Void> requestFuture = new CompletableFuture<>();

        // When a Connection is ready from the Connection Pool, schedule the Request on the connection
        crtConnPool.acquireConnection()
            .whenComplete((crtConn, throwable) -> {
                // If we didn't get a connection for some reason, fail the request
                if (throwable != null) {
                    requestFuture.completeExceptionally(throwable);
                    return;
                }

                AwsCrtAsyncHttpStreamAdapter crtToSdkAdapter =
                        new AwsCrtAsyncHttpStreamAdapter(crtConn, requestFuture, asyncRequest, windowSize);
                HttpRequest crtRequest = toCrtRequest(uri, asyncRequest, crtToSdkAdapter);

                // Submit the Request on this Connection
                invokeSafely(() -> crtConn.makeRequest(crtRequest, crtToSdkAdapter).activate());
            });

        return requestFuture;
    }

    @Override
    public void close() {
        isClosed.set(true);
        for (HttpClientConnectionManager connPool : connectionPools.values()) {
            IoUtils.closeQuietly(connPool, log.logger());
        }

        while (!ownedSubResources.isEmpty()) {
            CrtResource r = ownedSubResources.pop();
            IoUtils.closeQuietly(r, log.logger());
        }
    }

    /**
     * Builder that allows configuration of the AWS CRT HTTP implementation.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<AwsCrtAsyncHttpClient.Builder> {

        /**
         * The AWS CRT TlsCipherPreference to use for this Client
         * @param tlsCipherPreference The AWS Common Runtime TlsCipherPreference
         * @return The builder of the method chaining.
         */
        Builder tlsCipherPreference(TlsCipherPreference tlsCipherPreference);

        /**
         * Whether or not to Verify the Peer's TLS Certificate Chain.
         * @param verifyPeer true if the Certificate Chain should be validated, false if validation should be skipped.
         * @return The builder of the method chaining.
         */
        Builder verifyPeer(boolean verifyPeer);

        /**
         * If set to true, then the TCP read back pressure mechanism will be enabled, and the user
         * is responsible for calling incrementWindow on the stream object.
         * @param manualWindowManagement true if the TCP back pressure mechanism should be enabled.
         * @return The builder of the method chaining.
         */
        Builder manualWindowManagement(boolean manualWindowManagement);

        /**
         * The AWS CRT WindowSize to use for this HttpClient. This represents the number of unread bytes that can be
         * buffered in the ResponseBodyPublisher before we stop reading from the underlying TCP socket and wait for
         * the Subscriber to read more data.
         *
         * @param windowSize The AWS Common Runtime WindowSize
         * @return The builder of the method chaining.
         */
        Builder windowSize(int windowSize);

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
    }

    /**
     * Factory that allows more advanced configuration of the AWS CRT HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private TlsCipherPreference cipherPreference = TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
        private int windowSize = DEFAULT_STREAM_WINDOW_SIZE;
        private boolean verifyPeer = true;
        private boolean manualWindowManagement;
        private EventLoopGroup eventLoopGroup;
        private HostResolver hostResolver;

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
        public Builder tlsCipherPreference(TlsCipherPreference tlsCipherPreference) {
            Validate.notNull(tlsCipherPreference, "cipherPreference");
            Validate.isTrue(TlsContextOptions.isCipherPreferenceSupported(tlsCipherPreference),
                            "TlsCipherPreference not supported on current Platform");
            this.cipherPreference = tlsCipherPreference;
            return this;
        }

        @Override
        public Builder verifyPeer(boolean verifyPeer) {
            this.verifyPeer = verifyPeer;
            return this;
        }

        @Override
        public Builder manualWindowManagement(boolean manualWindowManagement) {
            this.manualWindowManagement = manualWindowManagement;
            return this;
        }

        @Override
        public Builder windowSize(int windowSize) {
            Validate.isPositive(windowSize, "windowSize");
            this.windowSize = windowSize;
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
    }
}
