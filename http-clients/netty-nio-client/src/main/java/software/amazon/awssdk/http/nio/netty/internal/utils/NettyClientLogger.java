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

import io.netty.channel.Channel;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * Logger facade similar to {@link software.amazon.awssdk.utils.Logger}, that also includes the Channel ID in the message when
 * provided.
 */
@SdkInternalApi
public final class NettyClientLogger {
    private final Logger delegateLogger;

    @SdkTestInternalApi
    NettyClientLogger(Logger delegateLogger) {
        this.delegateLogger = delegateLogger;
    }

    public static NettyClientLogger getLogger(Class<?> clzz) {
        Logger delegate = LoggerFactory.getLogger(clzz);
        return new NettyClientLogger(delegate);
    }

    /**
     * Log a DEBUG level message.
     *
     * @param msgSupplier Supplier for the log message
     */
    public void debug(Supplier<String> msgSupplier) {
        debug(msgSupplier, null);
    }

    /**
     * Log a DEBUG level message with the given exception.
     *
     * @param msgSupplier Supplier for the log message
     * @param t The throwable to log
     */
    public void debug(Supplier<String> msgSupplier, Throwable t) {
        if (!delegateLogger.isDebugEnabled()) {
            return;
        }

        delegateLogger.debug(msgSupplier.get(), t);
    }

    /**
     * Log a DEBUG level message including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     */
    public void debug(Channel channel, Supplier<String> msgSupplier) {
        debug(channel, msgSupplier, null);
    }

    /**
     * Log a DEBUG level message with the given exception and including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     * @param t The throwable to log
     */
    public void debug(Channel channel, Supplier<String> msgSupplier, Throwable t) {
        if (!delegateLogger.isDebugEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelInfo(true, msgSupplier, channel);
        delegateLogger.debug(finalMessage.get(), t);
    }

    /**
     * Log a WARN level message.
     *
     * @param msgSupplier Supplier for the log message
     */
    public void warn(Supplier<String> msgSupplier) {
        warn(msgSupplier, null);
    }

    /**
     * Log a WARN level message with the given exception.
     *
     * @param msgSupplier Supplier for the log message
     * @param t The throwable to log
     */
    public void warn(Supplier<String> msgSupplier, Throwable t) {
        if (!delegateLogger.isWarnEnabled()) {
            return;
        }

        delegateLogger.warn(msgSupplier.get(), t);
    }

    /**
     * Log a WARN level message and including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     */
    public void warn(Channel channel, Supplier<String> msgSupplier) {
        warn(channel, msgSupplier, null);
    }

    /**
     * Log a WARN level message with the given exception and including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     * @param t The throwable to log
     */
    public void warn(Channel channel, Supplier<String> msgSupplier, Throwable t) {
        if (!delegateLogger.isWarnEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelInfo(msgSupplier, channel);
        delegateLogger.warn(finalMessage.get(), t);
    }

    /**
     * Log a TRACE level message.
     *
     * @param msgSupplier Supplier for the log message
     */
    public void trace(Supplier<String> msgSupplier) {
        if (!delegateLogger.isTraceEnabled()) {
            return;
        }

        delegateLogger.trace(msgSupplier.get());
    }

    /**
     * Log a TRACE level message including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     */
    public void trace(Channel channel, Supplier<String> msgSupplier) {
        if (!delegateLogger.isTraceEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelInfo(true, msgSupplier, channel);
        delegateLogger.trace(finalMessage.get());
    }

    private static Supplier<String> prependChannelInfo(Supplier<String> msgSupplier, Channel channel) {
        return prependChannelInfo(false, msgSupplier, channel);
    }

    private static Supplier<String> prependChannelInfo(boolean useShortId, Supplier<String> msgSupplier, Channel channel) {
        String id;
        if (useShortId) {
            id = channel.id().asShortText();
        } else {
            id = channel.toString();
        }

        return () -> String.format("[Channel ID: %s] %s", id, msgSupplier.get());
    }
}
