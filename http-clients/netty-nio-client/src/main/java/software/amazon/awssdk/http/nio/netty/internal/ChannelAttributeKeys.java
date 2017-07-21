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

import com.typesafe.netty.HandlerPublisher;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;

/**
 * Keys for attributes attached via {@link io.netty.channel.Channel#attr(AttributeKey)}.
 */
class ChannelAttributeKeys {

    /**
     * Attribute key for {@link RequestContext}.
     */
    static final AttributeKey<RequestContext> REQUEST_CONTEXT_KEY = AttributeKey.newInstance("requestContext");

    /**
     * Attribute key for {@link HandlerPublisher}.
     */
    static final AttributeKey<HandlerPublisher<ByteBuffer>> PUBLISHER_KEY = AttributeKey.newInstance("publisher");

    /**
     * Boolean to indicate whether {@link software.amazon.awssdk.http.async.SdkHttpResponseHandler#onStream(Publisher)} has
     * been called yet. We call it when we receive the first {@link io.netty.handler.codec.http.HttpContent}.
     */
    static final AttributeKey<Boolean> HAS_CALLED_ON_STREAM = AttributeKey.newInstance("hasCalledOnStream");

    private ChannelAttributeKeys() {
    }
}
