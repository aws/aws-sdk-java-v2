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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.HAS_CALLED_ON_STREAM;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.PUBLISHER_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKeys.REQUEST_CONTEXT_KEY;

import com.typesafe.netty.HandlerPublisher;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;

@Sharable
class ResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = Logger.loggerFor(ResponseHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext channelContext, HttpObject msg) throws Exception {
        RequestContext requestContext = channelContext.channel().attr(REQUEST_CONTEXT_KEY).get();
        HandlerPublisher<ByteBuffer> publisher = channelContext.channel().attr(PUBLISHER_KEY).get();

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            SdkHttpResponse sdkResponse = SdkHttpFullResponse.builder()
                                                             .headers(fromNettyHeaders(response.headers()))
                                                             .statusCode(response.status().code())
                                                             .statusText(response.status().reasonPhrase())
                                                             .build();
            requestContext.handler().headersReceived(sdkResponse);
        }

        if (msg instanceof HttpContent) {
            Boolean hasCalledOnStream = channelContext.channel().attr(HAS_CALLED_ON_STREAM).get();
            if (!Boolean.TRUE.equals(hasCalledOnStream)) {
                requestContext.handler().onStream(publisher);
                channelContext.channel().attr(HAS_CALLED_ON_STREAM).set(Boolean.TRUE);
            }
            HttpContent content = (HttpContent) msg;
            if (content.content().readableBytes() > 0) {
                channelContext.fireChannelRead(content.content().nioBuffer());
            }
            if (msg instanceof LastHttpContent) {
                channelContext.pipeline().remove(publisher);
                requestContext.channelPool().release(channelContext.channel());
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        RequestContext requestContext = ctx.channel().attr(REQUEST_CONTEXT_KEY).get();
        log.error(() -> "Exception processing request: " + requestContext.sdkRequest(), cause);
        requestContext.handler().exceptionOccurred(cause);
        requestContext.channelPool().release(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }

    private static Map<String, List<String>> fromNettyHeaders(HttpHeaders headers) {
        return headers.entries().stream()
                      .collect(groupingBy(Map.Entry::getKey,
                                          mapping(Map.Entry::getValue, Collectors.toList())));
    }
}
