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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.PROTOCOL_FUTURE;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.internal.IdleConnectionCountingChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.utils.BetterFixedChannelPool;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Channel pool that establishes an initial connection to determine protocol. Delegates
 * to appropriate channel pool implementation depending on the protocol. This assumes that
 * all connections will be negotiated with the same protocol.
 */
@SdkInternalApi
public class HttpOrHttp2ChannelPool implements SdkChannelPool {
    private final ChannelPool delegatePool;
    private final int maxConcurrency;
    private final EventLoopGroup eventLoopGroup;
    private final EventLoop eventLoop;
    private final NettyConfiguration configuration;

    private boolean protocolImplPromiseInitializationStarted = false;
    private Promise<ChannelPool> protocolImplPromise;
    private BetterFixedChannelPool protocolImpl;
    private boolean closed;

    public HttpOrHttp2ChannelPool(ChannelPool delegatePool,
                                  EventLoopGroup group,
                                  int maxConcurrency,
                                  NettyConfiguration configuration) {
        this.delegatePool = delegatePool;
        this.maxConcurrency = maxConcurrency;
        this.eventLoopGroup = group;
        this.eventLoop = group.next();
        this.configuration = configuration;
        this.protocolImplPromise = eventLoop.newPromise();
    }

    @Override
    public Future<Channel> acquire() {
        return acquire(eventLoop.newPromise());
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        doInEventLoop(eventLoop, () -> acquire0(promise), promise);
        return promise;
    }

    private void acquire0(Promise<Channel> promise) {
        if (closed) {
            promise.setFailure(new IllegalStateException("Channel pool is closed!"));
            return;
        }

        if (protocolImpl != null) {
            protocolImpl.acquire(promise);
            return;
        }
        if (!protocolImplPromiseInitializationStarted) {
            initializeProtocol();
        }
        protocolImplPromise.addListener((GenericFutureListener<Future<ChannelPool>>) future -> {
            if (future.isSuccess()) {
                future.getNow().acquire(promise);
            } else {
                // Couldn't negotiate protocol, fail this acquire.
                promise.setFailure(future.cause());
            }
        });
    }

    /**
     * Establishes a single connection to initialize the protocol and choose the appropriate {@link ChannelPool} implementation
     * for {@link #protocolImpl}.
     */
    private void initializeProtocol() {
        protocolImplPromiseInitializationStarted = true;
        delegatePool.acquire().addListener((GenericFutureListener<Future<Channel>>) future -> {
            if (future.isSuccess()) {
                Channel newChannel = future.getNow();
                newChannel.attr(PROTOCOL_FUTURE).get().whenComplete((protocol, e) -> {
                    if (e != null) {
                        failProtocolImplPromise(e);
                    } else {
                        completeProtocolConfiguration(newChannel, protocol);
                    }
                });
            } else {
                failProtocolImplPromise(future.cause());
            }
        });
    }

    /**
     * Fail the current protocolImplPromise and null it out so the next acquire will attempt to re-initialize it.
     *
     * @param e Cause of failure.
     */
    private void failProtocolImplPromise(Throwable e) {
        doInEventLoop(eventLoop, () -> {
            protocolImplPromise.setFailure(e);
            protocolImplPromise = eventLoop.newPromise();
            protocolImplPromiseInitializationStarted = false;
        });
    }

    private void completeProtocolConfiguration(Channel newChannel, Protocol protocol) {
        doInEventLoop(eventLoop, () -> {
            if (closed) {
                closeAndRelease(newChannel, new IllegalStateException("Pool closed"));
            } else {
                try {
                    protocolImplPromise.setSuccess(configureProtocol(newChannel, protocol));
                } catch (Throwable e) {
                    closeAndRelease(newChannel, e);
                }
            }
        });
    }

    private void closeAndRelease(Channel newChannel, Throwable e) {
        newChannel.close();
        delegatePool.release(newChannel);
        protocolImplPromise.setFailure(e);
    }

    private ChannelPool configureProtocol(Channel newChannel, Protocol protocol) {
        if (Protocol.HTTP1_1 == protocol) {
            // For HTTP/1.1 we use a traditional channel pool without multiplexing
            SdkChannelPool idleConnectionMetricChannelPool = new IdleConnectionCountingChannelPool(eventLoop, delegatePool);
            protocolImpl = BetterFixedChannelPool.builder()
                                                 .channelPool(idleConnectionMetricChannelPool)
                                                 .executor(eventLoop)
                                                 .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                                 .acquireTimeoutMillis(configuration.connectionAcquireTimeoutMillis())
                                                 .maxConnections(maxConcurrency)
                                                 .maxPendingAcquires(configuration.maxPendingConnectionAcquires())
                                                 .build();
        } else {
            Duration idleConnectionTimeout = configuration.reapIdleConnections()
                                             ? Duration.ofMillis(configuration.idleTimeoutMillis()) : null;
            SdkChannelPool h2Pool = new Http2MultiplexedChannelPool(delegatePool, eventLoopGroup, idleConnectionTimeout);
            protocolImpl = BetterFixedChannelPool.builder()
                                                 .channelPool(h2Pool)
                                                 .executor(eventLoop)
                                                 .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                                 .acquireTimeoutMillis(configuration.connectionAcquireTimeoutMillis())
                                                 .maxConnections(maxConcurrency)
                                                 .maxPendingAcquires(configuration.maxPendingConnectionAcquires())
                                                 .build();
        }
        // Give the channel back so it can be acquired again by protocolImpl
        delegatePool.release(newChannel);
        return protocolImpl;
    }

    @Override
    public Future<Void> release(Channel channel) {
        return release(channel, eventLoop.newPromise());
    }

    @Override
    public Future<Void> release(Channel channel, Promise<Void> promise) {
        doInEventLoop(eventLoop,
            () -> release0(channel, promise),
                      promise);
        return promise;
    }

    private void release0(Channel channel, Promise<Void> promise) {
        if (protocolImpl == null) {
            // If protocolImpl is null that means the first connection failed to establish. Release it back to the
            // underlying connection pool.
            delegatePool.release(channel, promise);
        } else {
            protocolImpl.release(channel, promise);
        }
    }

    @Override
    public void close() {
        doInEventLoop(eventLoop, this::close0);
    }

    private void close0() {
        if (closed) {
            return;
        }

        closed = true;
        if (protocolImpl != null) {
            protocolImpl.close();
        } else if (protocolImplPromiseInitializationStarted) {
            protocolImplPromise.addListener((Future<ChannelPool> f) -> {
                if (f.isSuccess()) {
                    f.getNow().close();
                } else {
                    delegatePool.close();
                }
            });
        } else {
            delegatePool.close();
        }
    }

    @Override
    public CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        protocolImplPromise.addListener(f -> {
            if (!f.isSuccess()) {
                result.completeExceptionally(f.cause());
            } else {
                protocolImpl.collectChannelPoolMetrics(metrics).whenComplete((m, t) -> {
                    if (t != null) {
                        result.completeExceptionally(t);
                    } else {
                        result.complete(m);
                    }
                });
            }
        });
        return result;
    }
}
