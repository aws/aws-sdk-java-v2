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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.CHANNEL_DIAGNOSTICS;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.ChannelDiagnostics;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class NettyUtils {
    /**
     * Completed succeed future.
     */
    public static final SucceededFuture<?> SUCCEEDED_FUTURE = new SucceededFuture<>(null, null);

    public static final String CLOSED_CHANNEL_ERROR_MESSAGE = "The connection was closed during the request. The request will "
                                                              + "usually succeed on a retry, but if it does not: consider "
                                                              + "disabling any proxies you have configured, enabling debug "
                                                              + "logging, or performing a TCP dump to identify the root cause. "
                                                              + "If this is a streaming operation, validate that data is being "
                                                              + "read or written in a timely manner.";
    private static final Logger log = Logger.loggerFor(NettyUtils.class);

    private NettyUtils() {
    }

    public static Throwable decorateException(Channel channel, Throwable originalCause) {
        if (isAcquireTimeoutException(originalCause)) {
            return new Throwable(getMessageForAcquireTimeoutException(), originalCause);
        } else if (isTooManyPendingAcquiresException(originalCause)) {
            return new Throwable(getMessageForTooManyAcquireOperationsError(), originalCause);
        } else if (originalCause instanceof ReadTimeoutException) {
            return new IOException("Read timed out", originalCause);
        } else if (originalCause instanceof WriteTimeoutException) {
            return new IOException("Write timed out", originalCause);
        } else if (originalCause instanceof ClosedChannelException || isConnectionResetException(originalCause)) {
            return new IOException(NettyUtils.closedChannelMessage(channel), originalCause);
        }

        return originalCause;
    }

    private static boolean isConnectionResetException(Throwable originalCause) {
        return originalCause instanceof IOException && originalCause.getMessage().contains("Connection reset by peer");
    }

    private static boolean isAcquireTimeoutException(Throwable originalCause) {
        String message = originalCause.getMessage();
        return originalCause instanceof TimeoutException &&
               message != null &&
               message.contains("Acquire operation took longer");
    }

    private static boolean isTooManyPendingAcquiresException(Throwable originalCause) {
        String message = originalCause.getMessage();
        return originalCause instanceof IllegalStateException &&
               message != null &&
               originalCause.getMessage().contains("Too many outstanding acquire operations");
    }

    private static String getMessageForAcquireTimeoutException() {
        return "Acquire operation took longer than the configured maximum time. This indicates that a request cannot get a "
               + "connection from the pool within the specified maximum time. This can be due to high request rate.\n"

               + "Consider taking any of the following actions to mitigate the issue: increase max connections, "
               + "increase acquire timeout, or slowing the request rate.\n"

               + "Increasing the max connections can increase client throughput (unless the network interface is already "
               + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
               + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
               + "further increase your connection count, increasing the acquire timeout gives extra time for requests to "
               + "acquire a connection before timing out. If the connections doesn't free up, the subsequent requests "
               + "will still timeout.\n"

               + "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
               + "traffic bursts cannot overload the client, being more efficient with the number of times you need to "
               + "call AWS, or by increasing the number of hosts sending requests.";
    }

    private static String getMessageForTooManyAcquireOperationsError() {
        return "Maximum pending connection acquisitions exceeded. The request rate is too high for the client to keep up.\n"

               + "Consider taking any of the following actions to mitigate the issue: increase max connections, "
               + "increase max pending acquire count, decrease connection acquisition timeout, or "
               + "slow the request rate.\n"

               + "Increasing the max connections can increase client throughput (unless the network interface is already "
               + "fully utilized), but can eventually start to hit operation system limitations on the number of file "
               + "descriptors used by the process. If you already are fully utilizing your network interface or cannot "
               + "further increase your connection count, increasing the pending acquire count allows extra requests to be "
               + "buffered by the client, but can cause additional request latency and higher memory usage. If your request"
               + " latency or memory usage is already too high, decreasing the lease timeout will allow requests to fail "
               + "more quickly, reducing the number of pending connection acquisitions, but likely won't decrease the total "
               + "number of failed requests.\n"

               + "If the above mechanisms are not able to fix the issue, try smoothing out your requests so that large "
               + "traffic bursts cannot overload the client, being more efficient with the number of times you need to call "
               + "AWS, or by increasing the number of hosts sending requests.";
    }

    public static String closedChannelMessage(Channel channel) {
        ChannelDiagnostics channelDiagnostics = channel != null && channel.attr(CHANNEL_DIAGNOSTICS) != null ?
                                                channel.attr(CHANNEL_DIAGNOSTICS).get() : null;
        ChannelDiagnostics parentChannelDiagnostics = channel != null && channel.parent() != null && 
                                                      channel.parent().attr(CHANNEL_DIAGNOSTICS) != null ?
                                                      channel.parent().attr(CHANNEL_DIAGNOSTICS).get() : null;

        StringBuilder error = new StringBuilder();
        error.append(CLOSED_CHANNEL_ERROR_MESSAGE);

        if (channelDiagnostics != null) {
            error.append(" Channel Information: ").append(channelDiagnostics);

            if (parentChannelDiagnostics != null) {
                error.append(" Parent Channel Information: ").append(parentChannelDiagnostics);
            }
        }

        return error.toString();
    }

    /**
     * Creates a {@link BiConsumer} that notifies the promise of any failures either via the {@link Throwable} passed into the
     * BiConsumer of as a result of running the successFunction.
     *
     * @param successFunction Function called to process the successful result and map it into the result to notify the promise
     * with.
     * @param promise Promise to notify of success or failure.
     * @param <SuccessT> Success type.
     * @param <PromiseT> Type being fulfilled by the promise.
     * @return BiConsumer that can be used in a {@link CompletableFuture#whenComplete(BiConsumer)} method.
     */
    public static <SuccessT, PromiseT> BiConsumer<SuccessT, ? super Throwable> promiseNotifyingBiConsumer(
        Function<SuccessT, PromiseT> successFunction, Promise<PromiseT> promise) {
        return (success, fail) -> {
            if (fail != null) {
                promise.setFailure(fail);
            } else {
                try {
                    promise.setSuccess(successFunction.apply(success));
                } catch (Throwable e) {
                    promise.setFailure(e);
                }
            }
        };
    }

    /**
     * Creates a {@link BiConsumer} that notifies the promise of any failures either via the throwable passed into the BiConsumer
     * or as a result of running the successConsumer. This assumes that the successConsumer will notify the promise when it
     * completes successfully.
     *
     * @param successConsumer BiConsumer to call if the result is successful. Promise is also passed and must be notified on
     * success.
     * @param promise Promise to notify.
     * @param <SuccessT> Success type.
     * @param <PromiseT> Type being fulfilled by the Promise.
     * @return BiConsumer that can be used in a {@link CompletableFuture#whenComplete(BiConsumer)} method.
     */
    public static <SuccessT, PromiseT> BiConsumer<SuccessT, ? super Throwable> asyncPromiseNotifyingBiConsumer(
        BiConsumer<SuccessT, Promise<PromiseT>> successConsumer, Promise<PromiseT> promise) {
        return (success, fail) -> {
            if (fail != null) {
                promise.setFailure(fail);
            } else {
                try {
                    successConsumer.accept(success, promise);
                } catch (Throwable e) {
                    // If the successConsumer fails synchronously then we can notify the promise. If it fails asynchronously
                    // it's up to the successConsumer to notify.
                    promise.setFailure(e);
                }
            }
        };
    }

    /**
     * Create a {@link GenericFutureListener} that will notify the provided {@link Promise} on success and failure.
     *
     * @param channelPromise Promise to notify.
     * @return GenericFutureListener
     */
    public static <T> GenericFutureListener<Future<T>> promiseNotifyingListener(Promise<T> channelPromise) {
        return future -> {
            if (future.isSuccess()) {
                channelPromise.setSuccess(future.getNow());
            } else {
                channelPromise.setFailure(future.cause());
            }
        };
    }

    /**
     * Runs a task in the given {@link EventExecutor}. Runs immediately if the current thread is in the
     * eventExecutor.
     *
     * @param eventExecutor Executor to run task in.
     * @param runnable Task to run.
     *
     * @return The {@code Future} from from the executor.
     */
    public static Future<?> doInEventLoop(EventExecutor eventExecutor, Runnable runnable) {
        if (eventExecutor.inEventLoop()) {
            try {
                runnable.run();
                return eventExecutor.newSucceededFuture(null);
            } catch (Throwable t) {
                return eventExecutor.newFailedFuture(t);
            }
        }
        return eventExecutor.submit(runnable);
    }

    /**
     * Runs a task in the given {@link EventExecutor}. Runs immediately if the current thread is in the
     * eventExecutor. Notifies the given {@link Promise} if a failure occurs.
     *
     * @param eventExecutor Executor to run task in.
     * @param runnable Task to run.
     * @param promise Promise to notify if failure occurs.
     */
    public static void doInEventLoop(EventExecutor eventExecutor, Runnable runnable, Promise<?> promise) {
        try {
            if (eventExecutor.inEventLoop()) {
                runnable.run();
            } else {
                eventExecutor.submit(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        promise.setFailure(e);
                    }
                });
            }
        } catch (Throwable e) {
            promise.setFailure(e);
        }
    }

    public static void warnIfNotInEventLoop(EventLoop loop) {
        assert loop.inEventLoop();
        if (!loop.inEventLoop()) {
            Exception exception =
                new IllegalStateException("Execution is not in the expected event loop. Please report this issue to the "
                                          + "AWS SDK for Java team on GitHub, because it could result in race conditions.");
            log.warn(() -> "Execution is happening outside of the expected event loop.", exception);
        }
    }

    /**
     * @return an {@code AttributeKey} for {@code attr}. This returns an existing instance if it was previously created.
     */
    public static <T> AttributeKey<T> getOrCreateAttributeKey(String attr) {
        if (AttributeKey.exists(attr)) {
            return AttributeKey.valueOf(attr);
        }
        //CHECKSTYLE:OFF - This is the only place allowed to call AttributeKey.newInstance()
        return AttributeKey.newInstance(attr);
        //CHECKSTYLE:ON
    }

    /**
     * @return a new {@link SslHandler} with ssl engine configured
     */
    public static SslHandler newSslHandler(SslContext sslContext, ByteBufAllocator alloc, String peerHost, int peerPort,
                                           Duration handshakeTimeout) {
        // Need to provide host and port to enable SNI
        // https://github.com/netty/netty/issues/3801#issuecomment-104274440
        SslHandler sslHandler = sslContext.newHandler(alloc, peerHost, peerPort);
        sslHandler.setHandshakeTimeout(handshakeTimeout.toMillis(), TimeUnit.MILLISECONDS);
        configureSslEngine(sslHandler.engine());
        return sslHandler;
    }

    /**
     * Enable Hostname verification.
     *
     * See https://netty.io/4.0/api/io/netty/handler/ssl/SslContext.html#newHandler-io.netty.buffer.ByteBufAllocator-java.lang
     * .String-int-
     *
     * @param sslEngine the sslEngine to configure
     */
    private static void configureSslEngine(SSLEngine sslEngine) {
        SSLParameters sslParameters = sslEngine.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        sslEngine.setSSLParameters(sslParameters);
    }

    /**
     * Create a {@link GenericFutureListener} that will propagate any failures or cancellations to the provided {@link Promise},
     * or invoke the provided {@link Consumer} with the result of a successful operation completion. This is useful for chaining
     * together multiple futures that may depend upon each other but that may not have the same return type.
     * <p>
     * Note that if you do not need the value returned by a successful completion (or if it returns {@link Void}) you may use
     * {@link #runOrPropagate(Promise, Runnable)} instead.
     *
     * @param destination the Promise to notify upon failure or cancellation
     * @param onSuccess   the Consumer to invoke upon success
     */
    public static <T> GenericFutureListener<Future<T>> consumeOrPropagate(Promise<?> destination, Consumer<T> onSuccess) {
        return f -> {
            if (f.isSuccess()) {
                try {
                    T result = f.getNow();
                    onSuccess.accept(result);
                } catch (Throwable t) {
                    destination.tryFailure(t);
                }
            } else if (f.isCancelled()) {
                destination.cancel(false);
            } else {
                destination.tryFailure(f.cause());
            }
        };
    }

    /**
     * Create a {@link GenericFutureListener} that will propagate any failures or cancellations to the provided {@link Promise},
     * or invoke the provided {@link Runnable} upon successful operation completion. This is useful for chaining together multiple
     * futures that may depend upon each other but that may not have the same return type.
     *
     * @param destination the Promise to notify upon failure or cancellation
     * @param onSuccess   the Runnable to invoke upon success
     */
    public static <T> GenericFutureListener<Future<T>> runOrPropagate(Promise<?> destination, Runnable onSuccess) {
        return f -> {
            if (f.isSuccess()) {
                try {
                    onSuccess.run();
                } catch (Throwable t) {
                    destination.tryFailure(t);
                }
            } else if (f.isCancelled()) {
                destination.cancel(false);
            } else {
                destination.tryFailure(f.cause());
            }
        };
    }

    public static void runAndLogError(NettyClientLogger log, String errorMsg, FunctionalUtils.UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(null, () -> errorMsg, e);
        }
    }
}
