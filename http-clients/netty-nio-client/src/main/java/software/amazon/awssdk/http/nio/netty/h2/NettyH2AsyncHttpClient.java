package software.amazon.awssdk.http.nio.netty.h2;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;

public class NettyH2AsyncHttpClient implements SdkAsyncHttpClient {

    private final RequestAdapter requestAdapter = new RequestAdapter();
    // TODO hard code event loop group
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ChannelPoolMap<URI, ChannelPool> pools;
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
        this.metricsCollector = metricsCollector;
        this.pools = createChannelPoolMap(maxConns);
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
                return new FixedChannelPool(bootstrap,
                                            // TODO expose better options for this
                                            new Http2ClientInitializer(metricsCollector, sslContext), ChannelHealthChecker.ACTIVE,
                                            FixedChannelPool.AcquireTimeoutAction.FAIL, 10 * 1000, maxConnectionsPerEndpoint, 10_000);
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

    public static class H2ChannelPool implements ChannelPool {

        private final Bootstrap bootstrap;
        private final ChannelPoolHandler handler;
        private final AtomicInteger concurrency;
        private final EventLoop eventLoop;
        private final PriorityQueue<Channel> queue = new PriorityQueue<>(50,
                                                                         Comparator.comparing(c -> c.attr(ChannelAttributeKeys.AVAILABLE_STREAMS).get().get()));


        public H2ChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, int maxConcurrency) {
            this.bootstrap = bootstrap;
            this.handler = handler;
            this.concurrency = new AtomicInteger(maxConcurrency);
            this.eventLoop = bootstrap.config().group().next();
        }

        @Override
        public Future<Channel> acquire() {
            return acquire(new DefaultPromise<>(eventLoop));
        }

        @Override
        public Future<Channel> acquire(Promise<Channel> promise) {
            // TODO Check current concurrency, if non left then queue a pending acquire with a timeout. For now we will simply throw an exception
            if (concurrency.get() <= 0) {
                throw new IllegalStateException("Reached max concurrency for connection pool");
            }
            concurrency.decrementAndGet();
            Promise<Channel> channelPromise = new DefaultPromise<>(eventLoop);
            if (queue.peek() == null) {
                // TODO need to check if we can establish a connection
                // No connection, establish new one
                ChannelFuture connect = bootstrap.connect();
                connect.addListener(f -> {
                    Channel newChannel = connect.channel();
                    // TODO hardcoded value
                    newChannel.attr(ChannelAttributeKeys.AVAILABLE_STREAMS).set(new AtomicInteger(50));
                    int availableStreams = newChannel.attr(ChannelAttributeKeys.AVAILABLE_STREAMS).get().decrementAndGet();
                    Future<Http2StreamChannel> streamChannel = new Http2StreamChannelBootstrap(newChannel)
                        .open();

                    streamChannel
                        .addListener((GenericFutureListener<Future<Http2StreamChannel>>) future -> {
                            // TODO handle failure
                            channelPromise.setSuccess(future.getNow());
                        });
                    queue.add(newChannel);

                });
            } else {
                Channel channel = queue.poll();
                int availableStreams = channel.attr(ChannelAttributeKeys.AVAILABLE_STREAMS).get().decrementAndGet();
                new Http2StreamChannelBootstrap(channel)
                    .open().addListener((GenericFutureListener<Future<Http2StreamChannel>>) future -> {
                    // TODO handle failure
                    channelPromise.setSuccess(future.getNow());
                });
                ;
                if (availableStreams > 0) {
                    // Add back to the channel to allow it to be acquired again.
                    queue.add(channel);
                }

            }
            // Check queue and find a channel with most available streams
            // If nothing in queue or non with available streams, establish new connection if needed
            // If max connections reached, wait until a stream becomes available.
            // If no stream available within the configured timeout, throw exception
            // If new connection establish, set AVAILABLE_STREAMS to some safe default (since we haven't negioated yet)
            //     and decrement available streams by one, returning a new child channel
            return channelPromise;
        }

        @Override
        public Future<Void> release(Channel childChannel) {
            return release(childChannel, new DefaultPromise<>(eventLoop));
        }

        @Override
        public Future<Void> release(Channel childChannel, Promise<Void> promise) {
            concurrency.incrementAndGet();
            Channel parentChannel = childChannel.parent();
            int availableStreams = parentChannel.attr(ChannelAttributeKeys.AVAILABLE_STREAMS).get().incrementAndGet();
            // If this release brings available streams from 0 to 1 then add back to queue
            if (availableStreams == 1) {
                queue.add(parentChannel);
            }
            // TODO do we need to close child stream?
            childChannel.close();
            promise.setSuccess(null);
            return new SucceededFuture<>(eventLoop, null);
        }

        @Override
        public void close() {

        }
    }

}
