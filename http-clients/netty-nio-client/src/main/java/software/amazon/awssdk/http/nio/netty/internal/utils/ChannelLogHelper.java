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
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Utility class for logging the channel ID along with the message when within the context of a channel.
 */
@SdkInternalApi
public final class ChannelLogHelper {
    private ChannelLogHelper() {
    }

    public static void debug(Logger logger, Channel channel, Supplier<String> msgSupplier, Throwable t) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelIdIfAvailable(msgSupplier, channel);
        logger.debug(finalMessage.get(), t);
    }

    public static void warn(Logger logger, Channel channel, Supplier<String> msgSupplier, Throwable t) {
        if (!logger.isWarnEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelIdIfAvailable(msgSupplier, channel);
        logger.warn(finalMessage.get(), t);
    }

    public static void trace(Logger logger, Channel channel, Supplier<String> msgSupplier) {
        if (!logger.isTraceEnabled()) {
            return;
        }

        Supplier<String> finalMessage = prependChannelIdIfAvailable(msgSupplier, channel);
        logger.trace(finalMessage.get());
    }

    private static Supplier<String> prependChannelIdIfAvailable(Supplier<String> msgSupplier, Channel channel) {
        String id = channel.id().asShortText();

        return () -> String.format("[Channel %s] %s", id, msgSupplier.get());
    }
}
