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

package software.amazon.awssdk.stability.tests;


import static org.assertj.core.api.Assertions.assertThat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.blockhound.BlockingOperationError;
import software.amazon.awssdk.utils.Logger;

public class BlockHoundIntegrationTest {
    private static final Logger log = Logger.loggerFor(BlockHoundIntegrationTest.class);

    AtomicReference<Throwable> throwableReference;
    CountDownLatch latch;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Socket clientSocket;
    Channel serverChannel;

    @Before
    public void setup() {
        throwableReference = new AtomicReference<>();
        latch = new CountDownLatch(1);
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
    }

    @After
    public void teardown() throws Exception {
        if (clientSocket != null) {
            clientSocket.close();
        }
        if (serverChannel != null) {
            serverChannel.close();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Test
    public void testBlockHoundIntegration() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                 .childHandler(new ChannelDuplexHandler() {
                     @Override
                     public void channelActive(ChannelHandlerContext ctx) {
                         log.info(() -> "Preparing to sleep on the EventLoop to test if BlockHound is installed");
                         try {
                             Thread.sleep(1000);
                             log.info(() -> "BlockHound does not appear to be successfully installed");
                         } catch (Throwable t) {
                             log.info(() -> "BlockHound is successfully installed", t);
                             throwableReference.set(t);
                         }
                         latch.countDown();
                         ctx.fireChannelActive();
                     }
                 });

        int port = getUnusedPort();
        serverChannel = bootstrap.bind(port).sync().channel();
        clientSocket = new Socket("localhost", port);

        latch.await(5, TimeUnit.SECONDS);
        assertThat(throwableReference.get())
            .withFailMessage("BlockHound does not appear to be successfully installed. "
                             + "Ensure that BlockHoundTestExecutionListener is available on "
                             + "the class path and correctly registered with JUnit: "
                             + "https://github.com/reactor/BlockHound/blob/master/docs/supported_testing_frameworks.md")
            .isInstanceOf(BlockingOperationError.class);
    }

    private static int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
