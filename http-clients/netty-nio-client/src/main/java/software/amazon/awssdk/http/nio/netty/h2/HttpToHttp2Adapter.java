/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.http.nio.netty.h2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.EmptyHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;

/**
 * Translates HTTP/1.1 Netty objects to the corresponding HTTP/2 frame objects. Much of this was lifted from
 * {@link HttpToHttp2ConnectionHandler} but since that actually encodes to the raw bytes it doesn't play nice with
 * {@link Http2MultiplexCodec} which expects the frame objects.
 */
public class HttpToHttp2Adapter extends ChannelOutboundHandlerAdapter {

    HttpToHttp2Adapter() {
    }

    /**
     * Handles conversion of {@link HttpMessage} and {@link HttpContent} to HTTP/2 frames.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

        if (!(msg instanceof HttpMessage || msg instanceof HttpContent)) {
            ctx.write(msg, promise);
            return;
        }

        boolean release = true;
        SimpleChannelPromiseAggregator promiseAggregator =
            new SimpleChannelPromiseAggregator(promise, ctx.channel(), ctx.executor());
        try {
            boolean endStream = false;
            if (msg instanceof HttpMessage) {
                HttpMessage httpMsg = (HttpMessage) msg;

                // Convert and write the headers.
                Http2Headers http2Headers = HttpConversionUtil.toHttp2Headers(httpMsg, false);
                endStream = msg instanceof FullHttpMessage && !((FullHttpMessage) msg).content().isReadable();
                ctx.write(new DefaultHttp2HeadersFrame(http2Headers), promiseAggregator);
            }

            if (!endStream && msg instanceof HttpContent && !(msg instanceof LastHttpContent)) {
                boolean isLastContent = false;
                HttpHeaders trailers = EmptyHttpHeaders.INSTANCE;
                Http2Headers http2Trailers = EmptyHttp2Headers.INSTANCE;
                if (msg instanceof LastHttpContent) {
                    isLastContent = true;

                    // Convert any trailing headers.
                    final LastHttpContent lastContent = (LastHttpContent) msg;
                    trailers = lastContent.trailingHeaders();
                    http2Trailers = HttpConversionUtil.toHttp2Headers(trailers, false);
                }

                // Write the data
                final ByteBuf content = ((HttpContent) msg).content();
                endStream = isLastContent && trailers.isEmpty();
                release = false;
                ctx.write(new DefaultHttp2DataFrame(content, true), promiseAggregator);

                if (!trailers.isEmpty()) {
                    // Write trailing headers.
                    ctx.write(new DefaultHttp2HeadersFrame(http2Trailers, true), promiseAggregator);
                }
                ctx.flush();
            }
        } catch (Throwable t) {
            // TODO is this okay?
            // onError(ctx, t);
            promiseAggregator.setFailure(t);
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
            promiseAggregator.doneAllocatingPromises();
        }
    }

    /**
     * Provides the ability to associate the outcome of multiple {@link ChannelPromise}
     * objects into a single {@link ChannelPromise} object.
     */
    static final class SimpleChannelPromiseAggregator extends DefaultChannelPromise {
        private final ChannelPromise promise;
        private int expectedCount;
        private int doneCount;
        private Throwable lastFailure;
        private boolean doneAllocating;

        SimpleChannelPromiseAggregator(ChannelPromise promise, Channel c, EventExecutor e) {
            super(c, e);
            assert promise != null && !promise.isDone();
            this.promise = promise;
        }

        /**
         * Allocate a new promise which will be used to aggregate the overall success of this promise aggregator.
         *
         * @return A new promise which will be aggregated.
         * {@code null} if {@link #doneAllocatingPromises()} was previously called.
         */
        public ChannelPromise newPromise() {
            assert !doneAllocating : "Done allocating. No more promises can be allocated.";
            ++expectedCount;
            return this;
        }

        /**
         * Signify that no more {@link #newPromise()} allocations will be made.
         * The aggregation can not be successful until this method is called.
         *
         * @return The promise that is the aggregation of all promises allocated with {@link #newPromise()}.
         */
        public ChannelPromise doneAllocatingPromises() {
            if (!doneAllocating) {
                doneAllocating = true;
                if (doneCount == expectedCount || expectedCount == 0) {
                    return setPromise();
                }
            }
            return this;
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            if (allowFailure()) {
                ++doneCount;
                lastFailure = cause;
                if (allPromisesDone()) {
                    return tryPromise();
                }
                // TODO: We break the interface a bit here.
                // Multiple failure events can be processed without issue because this is an aggregation.
                return true;
            }
            return false;
        }

        /**
         * Fail this object if it has not already been failed.
         * <p>
         * This method will NOT throw an {@link IllegalStateException} if called multiple times
         * because that may be expected.
         */
        @Override
        public ChannelPromise setFailure(Throwable cause) {
            if (allowFailure()) {
                ++doneCount;
                lastFailure = cause;
                if (allPromisesDone()) {
                    return setPromise();
                }
            }
            return this;
        }

        @Override
        public ChannelPromise setSuccess(Void result) {
            if (awaitingPromises()) {
                ++doneCount;
                if (allPromisesDone()) {
                    setPromise();
                }
            }
            return this;
        }

        @Override
        public boolean trySuccess(Void result) {
            if (awaitingPromises()) {
                ++doneCount;
                if (allPromisesDone()) {
                    return tryPromise();
                }
                // TODO: We break the interface a bit here.
                // Multiple success events can be processed without issue because this is an aggregation.
                return true;
            }
            return false;
        }

        private boolean allowFailure() {
            return awaitingPromises() || expectedCount == 0;
        }

        private boolean awaitingPromises() {
            return doneCount < expectedCount;
        }

        private boolean allPromisesDone() {
            return doneCount == expectedCount && doneAllocating;
        }

        private ChannelPromise setPromise() {
            if (lastFailure == null) {
                promise.setSuccess();
                return super.setSuccess(null);
            } else {
                promise.setFailure(lastFailure);
                return super.setFailure(lastFailure);
            }
        }

        private boolean tryPromise() {
            if (lastFailure == null) {
                promise.trySuccess();
                return super.trySuccess(null);
            } else {
                promise.tryFailure(lastFailure);
                return super.tryFailure(lastFailure);
            }
        }
    }
}
