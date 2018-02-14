package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.Optional;
import javax.net.ssl.SSLException;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;

public class NettyH2AsyncHttpClient implements SdkAsyncHttpClient {

    private final RequestAdapter requestAdapter = new RequestAdapter();
    // TODO hard code event loop group
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final long maxStreams;
    private H2MetricsCollector metricsCollector;

    public NettyH2AsyncHttpClient() {
        // TODO hardcoded max conns
        this(50);
    }

    public NettyH2AsyncHttpClient(int maxConns) {
        this((methodName, metricName, metric) -> {
        }, maxConns);
    }

    public NettyH2AsyncHttpClient(H2MetricsCollector metricsCollector, int maxConns) {
        this(metricsCollector, maxConns, 200);
    }

    public NettyH2AsyncHttpClient(H2MetricsCollector metricsCollector, int maxConns, long maxStreams) {
        this.metricsCollector = metricsCollector;
        this.pools = createChannelPoolMap(maxConns);
        this.maxStreams = maxStreams;
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext sdkRequestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        RequestContext context = new RequestContext(pools.get(poolKey(sdkRequest)),
                                                    sdkRequest, requestProvider,
                                                    requestAdapter.adapt(sdkRequest),
                                                    handler);
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
                                    // TODO insecure
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                                        ApplicationProtocolConfig.Protocol.ALPN,
                                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                        ApplicationProtocolNames.HTTP_2,
                                        ApplicationProtocolNames.HTTP_1_1))
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
                        // TODO resolve like in H1 impl?
                        .channel(NioSocketChannel.class)
                        // TODO connection timeout from service defaults
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .remoteAddress(key.getHost(), key.getPort());
                return new HttpOrHttp2ChannelPool(bootstrap,
                                                  new Http2MultiplexInitializer(metricsCollector, sslContext, maxStreams),
                                                  maxConnectionsPerEndpoint);
            }
        };
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        // TODO implement
        return Optional.empty();
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

}
