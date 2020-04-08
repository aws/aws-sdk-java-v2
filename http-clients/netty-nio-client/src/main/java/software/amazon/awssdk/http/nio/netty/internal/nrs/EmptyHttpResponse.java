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

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
class EmptyHttpResponse extends DelegateHttpResponse implements FullHttpResponse {

    EmptyHttpResponse(HttpResponse response) {
        super(response);
    }

    @Override
    public FullHttpResponse setStatus(HttpResponseStatus status) {
        super.setStatus(status);
        return this;
    }

    @Override
    public FullHttpResponse setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }

    @Override
    public FullHttpResponse copy() {
        if (response instanceof FullHttpResponse) {
            return new EmptyHttpResponse(((FullHttpResponse) response).copy());
        } else {
            DefaultHttpResponse copy = new DefaultHttpResponse(protocolVersion(), status());
            copy.headers().set(headers());
            return new EmptyHttpResponse(copy);
        }
    }

    @Override
    public FullHttpResponse retain(int increment) {
        ReferenceCountUtil.retain(message, increment);
        return this;
    }

    @Override
    public FullHttpResponse retain() {
        ReferenceCountUtil.retain(message);
        return this;
    }

    @Override
    public FullHttpResponse touch() {
        if (response instanceof FullHttpResponse) {
            return ((FullHttpResponse) response).touch();
        } else {
            return this;
        }
    }

    @Override
    public FullHttpResponse touch(Object o) {
        if (response instanceof FullHttpResponse) {
            return ((FullHttpResponse) response).touch(o);
        } else {
            return this;
        }
    }

    @Override
    public HttpHeaders trailingHeaders() {
        return new DefaultHttpHeaders();
    }

    @Override
    public FullHttpResponse duplicate() {
        if (response instanceof FullHttpResponse) {
            return ((FullHttpResponse) response).duplicate();
        } else {
            return this;
        }
    }

    @Override
    public FullHttpResponse retainedDuplicate() {
        if (response instanceof FullHttpResponse) {
            return ((FullHttpResponse) response).retainedDuplicate();
        } else {
            return this;
        }
    }

    @Override
    public FullHttpResponse replace(ByteBuf byteBuf) {
        if (response instanceof FullHttpResponse) {
            return ((FullHttpResponse) response).replace(byteBuf);
        } else {
            return this;
        }
    }

    @Override
    public ByteBuf content() {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public int refCnt() {
        if (message instanceof ReferenceCounted) {
            return ((ReferenceCounted) message).refCnt();
        } else {
            return 1;
        }
    }

    @Override
    public boolean release() {
        return ReferenceCountUtil.release(message);
    }

    @Override
    public boolean release(int decrement) {
        return ReferenceCountUtil.release(message, decrement);
    }
}
