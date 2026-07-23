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
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.buildTlsConnectionOptions;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.resolveCipherPreference;
import static software.amazon.awssdk.http.crt.internal.AwsCrtConfigurationUtils.resolveMinTlsVersion;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.Http2StreamManagerOptions;
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.crt.http.HttpStreamManagerOptions;
import software.amazon.awssdk.crt.http.HttpVersion;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsConnectionOptions;
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
import software.amazon.awssdk.utils.uri.SdkUri;

/**
 * Common functionality and configuration for the CRT Http clients.
 */
@SdkProtectedApi
abstract class AwsCrtHttpClientBase implements SdkAutoCloseable {
    // TLS_NEGOTIATION_TIMEOUT diverges from the SDK global default (5s) for backwards compatibility:
    // the underlying CRT has always applied a 10s handshake timeout, so adopting the 5s global would silently tighten the
    // effective handshake timeout for existing CRT customers.
    static final AttributeMap AWS_CRT_HTTP_DEFAULTS =
        AttributeMap.builder()
                    .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, Duration.ofSeconds(10))
                    .build();

    private static final Logger log = Logger.loggerFor(AwsCrtHttpClientBase.class);

    private static final String AWS_COMMON_RUNTIME = "AwsCommonRuntime";
    private static final long DEFAULT_STREAM_WINDOW_SIZE = 16L * 1024L * 1024L; // 16 MB

    // Heuristic threshold (not an API contract) for warning about likely-accidental oversizing of the per-client
    // event-loop group. A value at or above this multiple of the available processors is almost certainly unintended.
    private static final int NUM_EVENT_LOOP_THREADS_WARN_MULTIPLIER = 4;

    protected final long readBufferSize;
    protected final Protocol protocol;
    private final Map<URI, HttpStreamManager> connectionPools = new ConcurrentHashMap<>();
    private final LinkedList<CrtResource> ownedSubResources = new LinkedList<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContext tlsContext;
    private final HttpProxyOptions proxyOptions;
    private final HttpMonitoringOptions monitoringOptions;
    private final long maxConnectionIdleInMilliseconds;
    private final int maxStreamsPerEndpoint;
    private final long connectionAcquisitionTimeout;
    private final TlsContextOptions tlsContextOptions;
    private final Duration tlsNegotiationTimeout;
    private boolean isClosed = false;

    AwsCrtHttpClientBase(AwsCrtClientBuilderBase builder, AttributeMap config) {
        // These native resources are created before being registered as owned, and each creation can throw
        // (e.g. CrtRuntimeException). Because close() is never invoked on a client that failed to construct, any
        // already-created resource must be released here if a later step throws - otherwise the private EventLoopGroup
        // (and its OS threads) and the other native handles would leak.
        EventLoopGroup eventLoopGroup = null;
        ClientBootstrap clientBootstrap = null;
        SocketOptions clientSocketOptions = null;
        TlsContextOptions clientTlsContextOptions = null;
        TlsContext clientTlsContext = null;
        try {
            Integer numEventLoopThreads = builder.getNumEventLoopThreads();
            if (numEventLoopThreads != null) {
                warnIfNumEventLoopThreadsIsExcessive(numEventLoopThreads);
                eventLoopGroup = new EventLoopGroup(numEventLoopThreads);
            }
            clientBootstrap = new ClientBootstrap(eventLoopGroup, null);
            clientSocketOptions = buildSocketOptions(builder.getTcpKeepAliveConfiguration(),
                                                     config.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
            clientTlsContextOptions =
                TlsContextOptions.createDefaultClient()
                                 .withCipherPreference(resolveCipherPreference(builder.getPostQuantumTlsEnabled()))
                                 .withMinimumTlsVersion(resolveMinTlsVersion(builder.getMinTlsVersion()))
                                 .withVerifyPeer(!config.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES));
            this.protocol = config.get(PROTOCOL);
            if (protocol == Protocol.HTTP2) {
                clientTlsContextOptions = clientTlsContextOptions.withAlpnList("h2");
            }

            this.tlsContextOptions = registerOwnedResource(clientTlsContextOptions);
            clientTlsContext = new TlsContext(clientTlsContextOptions);

            // The bootstrap holds a native reference to its event-loop group, so the group is registered before (and thus
            // closed after) the bootstrap to keep CRT teardown ordering correct. A null group leaves the shared static
            // default group untouched.
            registerOwnedResource(eventLoopGroup);
            this.bootstrap = registerOwnedResource(clientBootstrap);
            this.socketOptions = registerOwnedResource(clientSocketOptions);
            this.tlsContext = registerOwnedResource(clientTlsContext);
            this.tlsNegotiationTimeout = config.get(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT);
            this.readBufferSize = builder.getReadBufferSizeInBytes() == null ?
                                  DEFAULT_STREAM_WINDOW_SIZE : builder.getReadBufferSizeInBytes();
            this.maxStreamsPerEndpoint = config.get(SdkHttpConfigurationOption.MAX_CONNECTIONS);
            this.monitoringOptions =
                resolveHttpMonitoringOptions(builder.getConnectionHealthConfiguration())
                    .orElse(null);
            this.maxConnectionIdleInMilliseconds = config.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis();
            this.connectionAcquisitionTimeout = config.get(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT).toMillis();
            this.proxyOptions = resolveProxy(builder.getProxyConfiguration(), tlsContext).orElse(null);
        } catch (RuntimeException e) {
            // Release in reverse dependency order: the TlsContext wraps its options, and the bootstrap holds a
            // reference to the event-loop group, so those wrappers are closed before what they depend on.
            IoUtils.closeQuietly(clientTlsContext, log.logger());
            IoUtils.closeQuietly(clientTlsContextOptions, log.logger());
            IoUtils.closeQuietly(clientSocketOptions, log.logger());
            IoUtils.closeQuietly(clientBootstrap, log.logger());
            IoUtils.closeQuietly(eventLoopGroup, log.logger());
            throw e;
        }
    }

    private static void warnIfNumEventLoopThreadsIsExcessive(int numEventLoopThreads) {
        int availableProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
        if (numEventLoopThreads >= NUM_EVENT_LOOP_THREADS_WARN_MULTIPLIER * availableProcessors) {
            log.warn(() -> String.format(
                "numEventLoopThreads is set to %d, which is unusually high relative to the %d available processor(s). "
                + "Each client configured with numEventLoopThreads gets its own private event-loop group, so a high count "
                + "multiplied across multiple clients can lead to thread explosion, excessive context-switching, and increased "
                + "memory use without improving throughput. Consider benchmarking your workload to confirm this value is "
                + "necessary.",
                numEventLoopThreads, availableProcessors));
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
            ownedSubResources.push(subresource);
        }
        return subresource;
    }

    String clientName() {
        return AWS_COMMON_RUNTIME;
    }

    @SdkTestInternalApi
    Duration resolvedTlsNegotiationTimeout() {
        return tlsNegotiationTimeout;
    }

    private HttpStreamManager createConnectionPool(URI uri) {
        log.debug(() ->
                      String.format("Creating ConnectionPool for: URI:%s, MaxConns: %d, MaxStreams: %d",
                                    uri, maxStreamsPerEndpoint, maxStreamsPerEndpoint));

        boolean isHttps = "https".equalsIgnoreCase(uri.getScheme());
        TlsConnectionOptions poolTlsConnectionOptions = null;
        if (isHttps) {
            poolTlsConnectionOptions = registerOwnedResource(
                buildTlsConnectionOptions(tlsContext, tlsNegotiationTimeout, uri.getHost()));
        }

        HttpClientConnectionManagerOptions h1Options = new HttpClientConnectionManagerOptions()
                .withClientBootstrap(bootstrap)
                .withSocketOptions(socketOptions)
                .withTlsConnectionOptions(poolTlsConnectionOptions)
                .withUri(uri)
                .withWindowSize(readBufferSize)
                .withMaxConnections(maxStreamsPerEndpoint)
                .withManualWindowManagement(true)
                .withProxyOptions(proxyOptions)
                .withMonitoringOptions(monitoringOptions)
                .withMaxConnectionIdleInMilliseconds(maxConnectionIdleInMilliseconds)
                .withConnectionAcquisitionTimeoutInMilliseconds(connectionAcquisitionTimeout);

        HttpStreamManagerOptions options = new HttpStreamManagerOptions()
            .withHTTP1ConnectionManagerOptions(h1Options);

        if (protocol == Protocol.HTTP2) {
            Http2StreamManagerOptions h2Options = new Http2StreamManagerOptions()
                .withMaxConcurrentStreams(maxStreamsPerEndpoint)
                .withConnectionManagerOptions(h1Options);

            if (!isHttps) {
                h2Options.withPriorKnowledge(true);
            }

            options.withHTTP2StreamManagerOptions(h2Options);
            options.withExpectedProtocol(HttpVersion.HTTP_2);
        } else {
            options.withExpectedProtocol(HttpVersion.HTTP_1_1);
        }

        return HttpStreamManager.create(options);
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
    HttpStreamManager getOrCreateConnectionPool(URI uri) {
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("Client is closed. No more requests can be made with this client.");
            }

            HttpStreamManager connPool = connectionPools.computeIfAbsent(uri, this::createConnectionPool);
            return connPool;
        }
    }

    URI poolKey(SdkHttpRequest sdkRequest) {
        return invokeSafely(() -> SdkUri.getInstance().newUri(sdkRequest.protocol(), null, sdkRequest.host(),
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
