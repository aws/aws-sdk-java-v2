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
 * Logger facade similar to {@link software.amazon.awssdk.utils.Logger}, that also includes channel information in the message
 * when provided. When the logger has at least DEBUG level enabled, the logger uses {@link Channel#toString()} to provide the
 * complete information about the channel. If only less verbose levels are available, then only the channel's ID is logged.
 * <p>
 * Having the channel information associated with the log message whenever available makes correlating messages that are all
 * logged within the context of that channel possible; this is impossible to do otherwise because there is a 1:M mapping from
 * event loops to channels.
 * <p>
 * <b>NOTE:</b> The absence of overrides that don't take a {@code Channel} parameter is deliberate. This is done to lessen the
 * chances that a {code Channel} is omitted from the log by accident.
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

        String finalMessage = prependChannelInfo(msgSupplier, channel);
        delegateLogger.debug(finalMessage, t);
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
     * Log a ERROR level message with the given exception and including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     * @param t The throwable to log
     */
    public void error(Channel channel, Supplier<String> msgSupplier, Throwable t) {
        if (!delegateLogger.isErrorEnabled()) {
            return;
        }

        String finalMessage = prependChannelInfo(msgSupplier, channel);
        delegateLogger.error(finalMessage, t);
    }

    /**
     * Log a ERROR level message and including the channel information.
     *
     * @param channel The channel for this message is being logged
     * @param msgSupplier Supplier for the log message
     */
    public void error(Channel channel, Supplier<String> msgSupplier) {
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

        String finalMessage = prependChannelInfo(msgSupplier, channel);
        delegateLogger.warn(finalMessage, t);
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

        String finalMessage = prependChannelInfo(msgSupplier, channel);
        delegateLogger.trace(finalMessage);
    }

    private String prependChannelInfo(Supplier<String> msgSupplier, Channel channel) {
        if (channel == null) {
            return msgSupplier.get();
        }

        String id;
        if (!delegateLogger.isDebugEnabled()) {
            id = channel.id().asShortText();
        } else {
            id = channel.toString();
        }

        return String.format("[Channel: %s] %s", id, msgSupplier.get());
    }
}
