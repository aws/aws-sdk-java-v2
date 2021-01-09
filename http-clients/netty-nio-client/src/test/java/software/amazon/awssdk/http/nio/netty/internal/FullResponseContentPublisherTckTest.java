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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.BeforeMethod;

/**
 * TCK verification test for {@link software.amazon.awssdk.http.nio.netty.internal.ResponseHandler.FullResponseContentPublisher}.
 */
public class FullResponseContentPublisherTckTest extends PublisherVerification<ByteBuffer> {
    private static final byte[] CONTENT = new byte[16];

    private CompletableFuture<Void> executeFuture;

    private ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void methodSetup() {
        Channel chan = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(chan);
        when(chan.attr(any(AttributeKey.class))).thenReturn(mock(Attribute.class));
        executeFuture = new CompletableFuture<>();
    }

    public FullResponseContentPublisherTckTest() {
        super(new TestEnvironment());
    }

    // This is a one-shot publisher
    @Override
    public long maxElementsFromPublisher() {
        return 1;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return new ResponseHandler.FullResponseContentPublisher(mockCtx, ByteBuffer.wrap(CONTENT), executeFuture);
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }
}
