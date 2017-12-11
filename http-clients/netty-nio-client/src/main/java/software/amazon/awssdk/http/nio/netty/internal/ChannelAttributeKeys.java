/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.buffer.ByteBuf;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;

/**
 * Keys for attributes attached via {@link io.netty.channel.Channel#attr(AttributeKey)}.
 */
public class ChannelAttributeKeys {

    /**
     * Attribute key for {@link RequestContext}.
     */
    // TODO public
    public static final AttributeKey<RequestContext> REQUEST_CONTEXT_KEY = AttributeKey.newInstance("requestContext");

    static final AttributeKey<Subscriber<? super ByteBuffer>> SUBSCRIBER_KEY = AttributeKey.newInstance("subscriber");

    // TODO public
    public static final AttributeKey<Boolean> RESPONSE_COMPLETE_KEY = AttributeKey.newInstance("responseComplete");

    public static final AttributeKey<ByteBuf> CUMULATED_KEY = AttributeKey.newInstance("cumulated");

    /**
     * Future that when a protocol (http/1.1 or h2) has been selected.
     */
    public static final AttributeKey<CompletableFuture<String>> PROTOCOL_FUTURE = AttributeKey.newInstance("protocolFuture");

    private ChannelAttributeKeys() {
    }
}
