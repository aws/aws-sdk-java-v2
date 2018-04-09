/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

public final class ChannelUtils {
    private ChannelUtils() {}

    /**
     * Removes handlers of the given class types from the pipeline.
     *
     * @param pipeline the pipeline to remove handlers from
     * @param handlers handlers to remove, identified by class
     */
    @SafeVarargs
    public static void removeIfExists(ChannelPipeline pipeline, Class<? extends ChannelHandler>... handlers) {
        for (Class<? extends ChannelHandler> handler : handlers) {
            if (pipeline.get(handler) != null) {
                pipeline.remove(handler);
            }
        }
    }
}
