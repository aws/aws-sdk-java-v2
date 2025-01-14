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

import static software.amazon.awssdk.crtcore.CrtConfigurationUtils.resolveHttpMonitoringOptions;
import static software.amazon.awssdk.crtcore.CrtConfigurationUtils.resolveProxy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.buildSocketOptions;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.resolveCipherPreference;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
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
import software.amazon.awssdk.http.crt.internal.AwsCrtClientBuilderBase;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Common functionality and configuration for the CRT Http clients.
 */
@SdkProtectedApi
abstract class AwsCrtHttpClientBase implements SdkAutoCloseable {
    private static final Logger log = Logger.loggerFor(AwsCrtHttpClientBase.class);

    private static final String AWS_COMMON_RUNTIME = "AwsCommonRuntime";
    private static final long DEFAULT_STREAM_WINDOW_SIZE = 16L * 1024L * 1024L; // 16 MB

    protected final long readBufferSize;
    private final Map<URI, HttpClientConnectionManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContext tlsContext;
    private final HttpProxyOptions proxyOptions;
    private final HttpMonitoringOptions monitoringOptions;
    private final long maxConnectionIdleInMilliseconds;
    private final int maxConnectionsPerEndpoint;
    private boolean isClosed = false;

    AwsCrtHttpClientBase(AwsCrtClientBuilderBase builder, AttributeMap config) {
        if (config.get(PROTOCOL) == Protocol.HTTP2) {
            throw new UnsupportedOperationException("HTTP/2 is not supported in AwsCrtHttpClient yet. Use "
                                               + "NettyNioAsyncHttpClient instead.");
        }

        try (ClientBootstrap clientBootstrap = new ClientBootstrap(null, null);
             SocketOptions clientSocketOptions = buildSocketOptions(builder.getTcpKeepAliveConfiguration(),
                                                                    config.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
             TlsContextOptions clientTlsContextOptions =
                 TlsContextOptions.createDefaultClient()
                                  .withCipherPreference(resolveCipherPreference(builder.getPostQuantumTlsEnabled()))
                                  .withVerifyPeer(!config.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES));
             TlsContext clientTlsContext = new TlsContext(clientTlsContextOptions)) {

            this.bootstrap = registerOwnedResource(clientBootstrap);
            this.socketOptions = registerOwnedResource(clientSocketOptions);
            this.tlsContext = registerOwnedResource(clientTlsContext);
            this.readBufferSize = builder.getReadBufferSizeInBytes() == null ?
                                  DEFAULT_STREAM_WINDOW_SIZE : builder.getReadBufferSizeInBytes();
            this.maxConnectionsPerEndpoint = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);
            this.monitoringOptions = resolveHttpMonitoringOptions(builder.getConnectionHealthConfiguration()).orElse(null);
            this.maxConnectionIdleInMilliseconds = config.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis();
            this.proxyOptions = resolveProxy(builder.getProxyConfiguration(), tlsContext).orElse(null);
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

    String clientName() {
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
     * Currently, this function is only used by execute(), which guarantees a matching close
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
    HttpClientConnectionManager getOrCreateConnectionPool(URI uri) {
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("Client is closed. No more requests can be made with this client.");
            }

            HttpClientConnectionManager connPool = connectionPools.computeIfAbsent(uri, this::createConnectionPool);
            connPool.addRef();
            return connPool;
        }
    }

    URI poolKey(SdkHttpRequest sdkRequest) {
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
}
