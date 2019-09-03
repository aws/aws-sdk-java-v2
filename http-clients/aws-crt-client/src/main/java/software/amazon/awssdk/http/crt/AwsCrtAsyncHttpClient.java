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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.HttpConnectionPoolManager;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestOptions;
import software.amazon.awssdk.crt.io.ClientBootstrap;
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
    private static final int DEFAULT_STREAM_WINDOW_SIZE = 16 * 1024 * 1024; // 16 MB

    private final Map<URI, HttpConnectionPoolManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContextOptions tlsContextOptions;
    private final TlsContext tlsContext;
    private final int windowSize;
    private final int maxConnectionsPerEndpoint;


    public AwsCrtAsyncHttpClient(DefaultBuilder builder, AttributeMap config) {
        int maxConns = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);

        Validate.isPositive(maxConns, "maxConns");
        Validate.isPositive(builder.eventLoopSize, "eventLoopSize");
        Validate.notNull(builder.cipherPreference, "cipherPreference");
        Validate.isPositive(builder.windowSize, "windowSize");

        /**
         * Must add to List in reverse order that they were created in, so that they are closed in the correct order.
         *
         * Do NOT use Dependency Injection for Native Resources. It's possible to crash the JVM Process if Native
         * Dependencies are closed in the wrong order (Eg closing the Bootstrap/Threadpool when there are still open
         * connections). By creating and owning our own Native Resources we can guarantee that things are shutdown in
         * the correct order.
         */

        bootstrap = own(new ClientBootstrap(builder.eventLoopSize));
        socketOptions = own(new SocketOptions());
        tlsContextOptions = own(new TlsContextOptions().withCipherPreference(builder.cipherPreference));
        tlsContextOptions.setVerifyPeer(builder.verifyPeer);
        tlsContext = own(new TlsContext(tlsContextOptions));

        this.windowSize = builder.windowSize;
        this.maxConnectionsPerEndpoint = maxConns;
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
        Validate.notNull(sdkRequest, "SdkHttpRequest must not be null");
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

    private HttpConnectionPoolManager createConnectionPool(URI uri) {
        Validate.notNull(uri, "URI must not be null");
        log.debug(() -> "Creating ConnectionPool for: URI:" + uri + ", MaxConns: " + maxConnectionsPerEndpoint);
        return new HttpConnectionPoolManager(bootstrap, socketOptions, tlsContext, uri, windowSize, maxConnectionsPerEndpoint);
    }

    private HttpConnectionPoolManager getOrCreateConnectionPool(URI uri) {
        Validate.notNull(uri, "URI must not be null");
        HttpConnectionPoolManager connPool = connectionPools.get(uri);

        if (connPool == null) {
            HttpConnectionPoolManager newConnPool = createConnectionPool(uri);
            HttpConnectionPoolManager alreadyExistingConnPool = connectionPools.putIfAbsent(uri, newConnPool);

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

    private HttpRequest toCrtRequest(URI uri, AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        Validate.notNull(uri, "URI must not be null");
        Validate.notNull(sdkRequest, "SdkHttpRequest must not be null");

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(sdkRequest.rawQueryParameters())
                .map(value -> "?" + value)
                .orElse("");

        HttpHeader[] crtHeaderArray = asArray(createHttpHeaderList(uri, asyncRequest));

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest asyncRequest) {
        Validate.notNull(asyncRequest, "AsyncExecuteRequest must not be null");
        Validate.notNull(asyncRequest.request(), "SdkHttpRequest must not be null");
        Validate.notNull(asyncRequest.requestContentPublisher(), "RequestContentPublisher must not be null");
        Validate.notNull(asyncRequest.responseHandler(), "ResponseHandler must not be null");

        URI uri = toUri(asyncRequest.request());
        HttpConnectionPoolManager crtConnPool = getOrCreateConnectionPool(uri);
        HttpRequest crtRequest = toCrtRequest(uri, asyncRequest);

        CompletableFuture<Void> requestFuture = new CompletableFuture<>();


        HttpRequestOptions reqOptions = new HttpRequestOptions();

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

                // Submit the Request on this Connection
                invokeSafely(() -> crtConn.makeRequest(crtRequest, reqOptions, crtToSdkAdapter));
            });

        return requestFuture;
    }

    @Override
    public void close() {
        for (HttpConnectionPoolManager connPool : connectionPools.values()) {
            connPool.close();
        }

        while (ownedSubResources.size() > 0) {
            CrtResource r = ownedSubResources.pop();
            r.close();
        }
    }

    /**
     * Builder that allows configuration of the AWS CRT HTTP implementation.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<AwsCrtAsyncHttpClient.Builder> {

        /**
         * The number of Threads to use in the EventLoop.
         * @param eventLoopSize The number of Threads to use in the EventLoop.
         * @return the builder of the method chaining.
         */
        Builder eventLoopSize(int eventLoopSize);

        /**
         * The AWS CRT TlsCipherPreference to use for this Client
         * @param tlsCipherPreference The AWS Common Runtime TlsCipherPreference
         * @return the builder of the method chaining.
         */
        Builder tlsCipherPreference(TlsCipherPreference tlsCipherPreference);

        /**
         * Whether or not to Verify the Peer's TLS Certificate Chain.
         * @param verifyPeer true if the Certificate Chain should be validated, false if validation should be skipped.
         * @return the builder of the method chaining.
         */
        Builder verifyPeer(boolean verifyPeer);

        /**
         * The AWS CRT WindowSize to use for this HttpClient. This represents the number of unread bytes that can be
         * buffered in the ResponseBodyPublisher before we stop reading from the underlying TCP socket and wait for
         * the Subscriber to read more data.
         *
         * @param windowSize The AWS Common Runtime WindowSize
         * @return the builder of the method chaining.
         */
        Builder windowSize(int windowSize);
    }

    /**
     * Factory that allows more advanced configuration of the AWS CRT HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private int eventLoopSize = Runtime.getRuntime().availableProcessors();
        private TlsCipherPreference cipherPreference = TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
        private int windowSize = DEFAULT_STREAM_WINDOW_SIZE;
        private boolean verifyPeer = true;

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
        public Builder eventLoopSize(int eventLoopSize) {
            Validate.isPositive(eventLoopSize, "eventLoopSize");
            this.eventLoopSize = eventLoopSize;
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
        public Builder verifyPeer(boolean verifyPeer) {
            this.verifyPeer = verifyPeer;
            return this;
        }

        @Override
        public Builder windowSize(int windowSize) {
            Validate.isPositive(windowSize, "windowSize");
            this.windowSize = windowSize;
            return this;
        }
    }
}
