package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.Optional;
import javax.net.ssl.SSLException;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.EventLoopGroupFactory;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.NonManagedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.http.nio.netty.internal.SharedEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyH2AsyncHttpClient implements SdkAsyncHttpClient {

    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final EventLoopGroup group;
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final NettyConfiguration configuration;
    private final long maxStreams;
    private H2MetricsCollector metricsCollector;
    private Protocol protocol;

    public NettyH2AsyncHttpClient(NettySdkHttpClientFactory factory, AttributeMap serviceDefaultsMap) {
        this.configuration = new NettyConfiguration(serviceDefaultsMap, factory);
        this.pools = createChannelPoolMap(configuration.maxConnectionsPerEndpoint());
        // TODO make configurable
        this.maxStreams = 200;
        this.protocol = serviceDefaultsMap.get(SdkHttpConfigurationOption.PROTOCOL);
        this.metricsCollector = (methodName, metricName, metric) -> {
        };
        this.group = factory.eventLoopGroupConfiguration().toEither()
                            .map(e -> e.map(NonManagedEventLoopGroup::new,
                                            EventLoopGroupFactory::create))
                            .orElseGet(SharedEventLoopGroup::get);
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext sdkRequestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        RequestContext context = new RequestContext(pools.get(poolKey(sdkRequest)),
                                                    sdkRequest, requestProvider,
                                                    requestAdapter.adapt(sdkRequest),
                                                    handler, configuration);
        return new H2RunnableRequest(context, metricsCollector);
    }

    private static URI poolKey(SdkHttpRequest sdkRequest) {
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(),
                                          sdkRequest.port(), null, null, null));
    }

    private SslContext sslContext(String protocol) {
        if (!protocol.equalsIgnoreCase("https")) {
            return null;
        }
        SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
        try {
            return SslContextBuilder.forClient()
                                    .sslProvider(provider)
                    /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                     * Please refer to the HTTP/2 specification for cipher requirements. */
                                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .trustManager(configuration.trustAllCertificates() ? InsecureTrustManagerFactory.INSTANCE : null)
                                    .build();
        } catch (SSLException e) {
            // TODO is throwing the right thing here or should we notify the handler?
            throw new RuntimeException(e);
        }
    }

    private ChannelPoolMap<URI, ChannelPool> createChannelPoolMap(int maxConnectionsPerEndpoint) {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                SslContext sslContext = sslContext(key.getScheme());
                Bootstrap bootstrap =
                    new Bootstrap()
                        .group(group)
                        .channel(resolveSocketChannelClass(group))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectionTimeout())
                        // TODO run some performance tests with and without this.
                        .option(ChannelOption.TCP_NODELAY, true)
                        .remoteAddress(key.getHost(), key.getPort());
                return new HttpOrHttp2ChannelPool(bootstrap,
                                                  new Http2MultiplexInitializer(protocol, metricsCollector, sslContext, maxStreams),
                                                  maxConnectionsPerEndpoint);
            }
        };
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return configuration.getConfigurationValue(key);
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

}
