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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class ChannelPublisherTest {

    private EventLoopGroup group;
    private Channel channel;
    private Publisher<Channel> publisher;
    private SubscriberProbe<Channel> subscriber;

    @Before
    public void start() throws Exception {
        group = new NioEventLoopGroup();
        EventLoop eventLoop = group.next();

        HandlerPublisher<Channel> handlerPublisher = new HandlerPublisher<>(eventLoop, Channel.class);
        Bootstrap bootstrap = new Bootstrap();

        bootstrap
                .channel(NioServerSocketChannel.class)
                .group(eventLoop)
                .option(ChannelOption.AUTO_READ, false)
                .handler(handlerPublisher)
                .localAddress("127.0.0.1", 0);

        channel = bootstrap.bind().await().channel();
        this.publisher = handlerPublisher;

        subscriber = new SubscriberProbe<>();
    }

    @After
    public void stop() throws Exception {
        channel.unsafe().closeForcibly();
        group.shutdownGracefully();
    }

    @Test
    public void test() throws Exception {
        publisher.subscribe(subscriber);
        Subscription sub = subscriber.takeSubscription();

        // Try one cycle
        sub.request(1);
        Socket socket1 = connect();
        receiveConnection();
        readWriteData(socket1, 1);

        // Check back pressure
        Socket socket2 = connect();
        subscriber.expectNoElements();

        // Now request the next connection
        sub.request(1);
        receiveConnection();
        readWriteData(socket2, 2);

        // Close the channel
        channel.close();
        subscriber.expectNoElements();
        subscriber.expectComplete();
    }

    private Socket connect() throws Exception {
        InetSocketAddress address = (InetSocketAddress) channel.localAddress();
        return new Socket(address.getAddress(), address.getPort());
    }

    private void readWriteData(Socket socket, int data) throws Exception {
        OutputStream os = socket.getOutputStream();
        os.write(data);
        os.flush();
        InputStream is = socket.getInputStream();
        int received = is.read();
        socket.close();
        assertEquals(received, data);
    }

    private void receiveConnection() throws Exception {
        Channel channel = subscriber.take();
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                ctx.writeAndFlush(msg);
            }
        });
        group.register(channel);
    }

    private class SubscriberProbe<T> implements Subscriber<T> {
        final BlockingQueue<Subscription> subscriptions = new LinkedBlockingQueue<>();
        final BlockingQueue<T> elements = new LinkedBlockingQueue<>();
        final Promise<Void> promise = new DefaultPromise<>(group.next());

        public void onSubscribe(Subscription s) {
            subscriptions.add(s);
        }

        public void onNext(T t) {
            elements.add(t);
        }

        public void onError(Throwable t) {
            promise.setFailure(t);
        }

        public void onComplete() {
            promise.setSuccess(null);
        }

        Subscription takeSubscription() throws Exception {
            Subscription sub = subscriptions.poll(100, TimeUnit.MILLISECONDS);
            assertNotNull(sub);
            return sub;
        }

        T take() throws Exception {
            T t = elements.poll(1000, TimeUnit.MILLISECONDS);
            assertNotNull(t);
            return t;
        }

        void expectNoElements() throws Exception {
            T t = elements.poll(100, TimeUnit.MILLISECONDS);
            assertNull(t);
        }

        void expectComplete() throws Exception {
            promise.get(100, TimeUnit.MILLISECONDS);
        }
    }
}
