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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.doInEventLoop;
import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.warnIfNotInEventLoop;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey;
import software.amazon.awssdk.http.nio.netty.internal.ChannelDiagnostics;
import software.amazon.awssdk.http.nio.netty.internal.UnusedChannelExceptionHandler;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;

/**
 * Contains a {@link Future} for the actual socket channel and tracks available
 * streams based on the MAX_CONCURRENT_STREAMS setting for the connection.
 */
@SdkInternalApi
public class MultiplexedChannelRecord {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(MultiplexedChannelRecord.class);

    private final Channel connection;
    private final long maxConcurrencyPerConnection;
    private final Long allowedIdleConnectionTimeMillis;
    private final Consumer<Channel> closeAndReleaseConnection;

    private final AtomicLong availableChildChannels;
    private volatile long lastReserveAttemptTimeMillis;

    // Only read or write in the connection.eventLoop()
    private final Map<ChannelId, Http2StreamChannel> childChannels = new HashMap<>();
    private ScheduledFuture<?> closeIfIdleTask;

    // Only write in the connection.eventLoop()
    private volatile RecordState state = RecordState.OPEN;

    // Stored so a failed stream acquire can report why this connection takes no new streams.
    private volatile String closedToNewReason;

    MultiplexedChannelRecord(Channel connection, long maxConcurrencyPerConnection, Duration allowedIdleConnectionTime,
                             Consumer<Channel> closeAndReleaseConnection) {
        this.connection = connection;
        this.maxConcurrencyPerConnection = maxConcurrencyPerConnection;
        this.availableChildChannels = new AtomicLong(maxConcurrencyPerConnection);
        this.allowedIdleConnectionTimeMillis = allowedIdleConnectionTime == null ? null : allowedIdleConnectionTime.toMillis();
        this.closeAndReleaseConnection = closeAndReleaseConnection;
    }

    @SdkTestInternalApi
    MultiplexedChannelRecord(Channel connection, long maxConcurrencyPerConnection, Duration allowedIdleConnectionTime) {
        this(connection, maxConcurrencyPerConnection, allowedIdleConnectionTime, Channel::close);
    }

    boolean acquireStream(Promise<Channel> promise) {
        if (claimStream()) {
            releaseClaimOnFailure(promise);
            acquireClaimedStream(promise);
            return true;
        }
        return false;
    }

    void acquireClaimedStream(Promise<Channel> promise) {
        doInEventLoop(connection.eventLoop(), () -> {
            if (state != RecordState.OPEN) {
                String message;
                if (state == RecordState.CLOSED_TO_NEW) {
                    message = String.format("Connection %s %s. Unable to open new streams on this connection.", connection,
                                            closedToNewReason);
                } else {
                    message = String.format("Connection %s was closed while acquiring new stream.", connection);
                }
                log.warn(connection, () -> message);
                promise.setFailure(new IOException(message));
                return;
            }

            Future<Http2StreamChannel> streamFuture = new Http2StreamChannelBootstrap(connection).open();
            streamFuture.addListener((GenericFutureListener<Future<Http2StreamChannel>>) future -> {
                warnIfNotInEventLoop(connection.eventLoop());

                if (!future.isSuccess()) {
                    promise.setFailure(future.cause());
                    return;
                }

                Http2StreamChannel channel = future.getNow();
                channel.pipeline().addLast(UnusedChannelExceptionHandler.getInstance());
                channel.attr(ChannelAttributeKey.HTTP2_FRAME_STREAM).set(channel.stream());
                channel.attr(ChannelAttributeKey.CHANNEL_DIAGNOSTICS).set(new ChannelDiagnostics(channel));
                childChannels.put(channel.id(), channel);
                promise.setSuccess(channel);

                if (closeIfIdleTask == null && allowedIdleConnectionTimeMillis != null) {
                    enableCloseIfIdleTask();
                }
            });
        }, promise);
    }

    private void enableCloseIfIdleTask() {
        warnIfNotInEventLoop(connection.eventLoop());

        // Don't poll more frequently than 1 second. Being overly-conservative is okay. Blowing up our CPU is not.
        long taskFrequencyMillis = Math.max(allowedIdleConnectionTimeMillis, 1_000);

        closeIfIdleTask = connection.eventLoop().scheduleAtFixedRate(this::closeIfIdle, taskFrequencyMillis, taskFrequencyMillis,
                                                                     TimeUnit.MILLISECONDS);
        connection.closeFuture().addListener(f -> closeIfIdleTask.cancel(false));
    }

    private void releaseClaimOnFailure(Promise<Channel> promise) {
        try {
            promise.addListener(f -> {
                if (!promise.isSuccess()) {
                    releaseClaim();
                }
            });
        } catch (Throwable e) {
            doInEventLoop(connection.eventLoop(), this::releaseClaim);
            throw e;
        }
    }

    private void releaseClaim() {
        if (availableChildChannels.incrementAndGet() > maxConcurrencyPerConnection) {
            assert false;
            log.warn(connection, () -> "Child channel count was caught attempting to be increased over max concurrency. "
                           + "Please report this issue to the AWS SDK for Java team.");
            availableChildChannels.decrementAndGet();
        }
        closeIfRetiredAndDrained();
    }

    private void closeIfRetiredAndDrained() {
        warnIfNotInEventLoop(connection.eventLoop());
        if (state != RecordState.CLOSED_TO_NEW
            || !childChannels.isEmpty()
            || availableChildChannels.get() != maxConcurrencyPerConnection) {
            return;
        }
        log.debug(connection, () -> "Closing connection " + connection + " after its last stream; it takes no new streams.");
        state = RecordState.CLOSED;
        closeAndReleaseConnection.accept(connection);
    }

    /**
     * Handle a {@link Http2GoAwayFrame} on this connection, preventing new streams from being created on it, and closing any
     * streams newer than the last-stream-id on the go-away frame. The connection closes once its last stream and pending
     * acquires end.
     */
    void handleGoAway(int lastStreamId, GoAwayException exception) {
        doInEventLoop(connection.eventLoop(), () -> {
            if (state == RecordState.CLOSED) {
                return;
            }

            // A server can send more than one GOAWAY, and each later one can lower the Last Stream ID. Setting the
            // reason on every GOAWAY keeps the current Last Stream ID in the acquire failure message.
            closedToNewReason = "received GOAWAY with Last Stream ID " + lastStreamId;

            if (state == RecordState.OPEN) {
                state = RecordState.CLOSED_TO_NEW;
            }

            // Create a copy of the children to close, because fireExceptionCaught may remove from the childChannels.
            List<Http2StreamChannel> childrenToClose = new ArrayList<>(childChannels.values());
            childrenToClose.stream()
                           .filter(cc -> cc.stream().id() > lastStreamId)
                           .forEach(cc -> cc.pipeline().fireExceptionCaught(exception));

            // Queued, so the drained check runs after Netty's GOAWAY listener returns and closes the connection if it has no
            // streams.
            if (state == RecordState.CLOSED_TO_NEW) {
                connection.eventLoop().execute(this::closeIfRetiredAndDrained);
            }
        });
    }

    /**
     * Prevent new streams from being acquired from the existing connection. The connection closes once its last stream and
     * pending acquires end.
     */
    void closeToNewStreams() {
        doInEventLoop(connection.eventLoop(), () -> {
            if (state == RecordState.OPEN) {
                state = RecordState.CLOSED_TO_NEW;
                closedToNewReason = "stopped taking new streams after an I/O error on another stream";
                // Queued, so the drained check runs after the caller's exception handler returns and closes the connection if
                // it has no streams.
                connection.eventLoop().execute(this::closeIfRetiredAndDrained);
            }
        });
    }

    /**
     * Delivers the exception to all registered child channels, and prohibits new streams being created on this connection.
     */
    void closeChildChannels(Throwable t) {
        closeAndExecuteOnChildChannels(ch -> ch.pipeline().fireExceptionCaught(decorateConnectionException(t)));
    }

    private Throwable decorateConnectionException(Throwable t) {
        String message =
            String.format("An error occurred on the connection: %s, [channel: %s]. All streams will be closed", t,
                          connection.id());
        if (t instanceof IOException) {
            return new IOException(message, t);
        }

        return new Throwable(message, t);
    }

    private void closeAndExecuteOnChildChannels(Consumer<Channel> childChannelConsumer) {
        doInEventLoop(connection.eventLoop(), () -> {
            if (state == RecordState.CLOSED) {
                return;
            }
            state = RecordState.CLOSED;

            // Create a copy of the children, because they may be modified by the consumer.
            List<Http2StreamChannel> childrenToClose = new ArrayList<>(childChannels.values());
            for (Channel childChannel : childrenToClose) {
                childChannelConsumer.accept(childChannel);
            }
        });
    }

    void closeAndReleaseChild(Channel childChannel) {
        childChannel.close();
        doInEventLoop(connection.eventLoop(), () -> {
            childChannels.remove(childChannel.id());
            releaseClaim();
        });
    }

    private void closeIfIdle() {
        warnIfNotInEventLoop(connection.eventLoop());

        // Don't close if we have child channels.
        if (!childChannels.isEmpty()) {
            return;
        }

        // Check before the compareAndSet below: setting the count of a retired connection to 0 would stop it from closing.
        if (state == RecordState.CLOSED_TO_NEW) {
            closeIfRetiredAndDrained();
            return;
        }

        // If we've been closed, no need to shut down.
        if (state != RecordState.OPEN) {
            return;
        }

        // Don't close if there have been any reserves attempted since the idle connection time.
        long nonVolatileLastReserveAttemptTimeMillis = lastReserveAttemptTimeMillis;
        if (nonVolatileLastReserveAttemptTimeMillis > System.currentTimeMillis() - allowedIdleConnectionTimeMillis) {
            return;
        }

        // Cut off new streams from being acquired from this connection by setting the number of available channels to 0.
        // This write may fail if a reservation has happened since we checked the lastReserveAttemptTime.
        if (!availableChildChannels.compareAndSet(maxConcurrencyPerConnection, 0)) {
            return;
        }

        log.debug(connection, () -> "Connection " + connection + " has been idle for " +
                                    (System.currentTimeMillis() - nonVolatileLastReserveAttemptTimeMillis) +
                                    "ms and will be shut down.");

        // Mark ourselves as closed
        state = RecordState.CLOSED;

        // Start the shutdown process by closing the connection (which should be noticed by the connection pool)
        connection.close();
    }

    public Channel getConnection() {
        return connection;
    }

    private boolean claimStream() {
        lastReserveAttemptTimeMillis = System.currentTimeMillis();
        for (int attempt = 0; attempt < 5; ++attempt) {

            if (state != RecordState.OPEN) {
                return false;
            }

            long currentlyAvailable = availableChildChannels.get();

            if (currentlyAvailable <= 0) {
                return false;
            }
            if (availableChildChannels.compareAndSet(currentlyAvailable, currentlyAvailable - 1)) {
                return true;
            }
        }

        return false;
    }

    CompletableFuture<Metrics> getMetrics() {
        CompletableFuture<Metrics> result = new CompletableFuture<>();
        doInEventLoop(connection.eventLoop(), () -> {
            long availableStreams = state == RecordState.OPEN ? maxConcurrencyPerConnection - childChannels.size() : 0;
            result.complete(new Metrics().setAvailableStreams(availableStreams));
        });
        return result;
    }

    private enum RecordState {
        /**
         * The connection is open and new streams may be acquired from it, if they are available.
         */
        OPEN,

        /**
         * The connection is open, but new streams may not be acquired from it. This occurs after the connection receives a
         * GOAWAY frame or has an I/O error on one of its streams. The connection closes once its last stream and pending
         * acquires end.
         */
        CLOSED_TO_NEW,

        /**
         * The connection is closed and new streams may not be acquired from it.
         */
        CLOSED
    }

    public static class Metrics {
        private long availableStreams = 0;

        public long getAvailableStreams() {
            return availableStreams;
        }

        public Metrics setAvailableStreams(long availableStreams) {
            this.availableStreams = availableStreams;
            return this;
        }

        public void add(Metrics rhs) {
            this.availableStreams += rhs.availableStreams;
        }
    }
}
