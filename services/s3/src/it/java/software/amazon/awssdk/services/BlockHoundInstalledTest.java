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

package software.amazon.awssdk.services;


import static org.assertj.core.api.Assertions.assertThat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.blockhound.integration.BlockHoundIntegration;
import reactor.blockhound.junit.platform.BlockHoundTestExecutionListener;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

/**
 * This test ensures that BlockHound is correctly installed for integration tests. The test is somewhat arbitrarily placed in the
 * {@code s3} module in order to assert against the configuration of all service integration tests.
 * <p>
 * BlockHound is installed in one of two ways:
 * <ol>
 *     <li>Using BlockHound's provided {@link BlockHoundTestExecutionListener}, which will be automatically detected by the
 *     JUnit 5 platform upon initialization.</li>
 *     <li>Manually calling {@link BlockHound#install(BlockHoundIntegration...)}. This is done as part of static initialization in
 *     {@link AwsIntegrationTestBase} and {@link AwsTestBase}, which most integration/stability tests extend.
 * </ol>
 * <p>
 * This test ensures BlockHound is correctly installed by intentionally performing a blocking operation on the Netty
 * {@link EventLoop} and asserting that a {@link BlockingOperationError} is thrown to forbid it.
 */
class BlockHoundInstalledTest {
    private static final Logger log = Logger.loggerFor(BlockHoundInstalledTest.class);

    AtomicReference<Throwable> throwableReference;
    CountDownLatch latch;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Socket clientSocket;
    Channel serverChannel;

    @BeforeEach
    public void setup() {
        throwableReference = new AtomicReference<>();
        latch = new CountDownLatch(1);
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
    }

    @AfterEach
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
    void testBlockHoundInstalled() throws Exception {
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
            .withFailMessage("BlockHound does not appear to be successfully installed. Ensure that either BlockHound.install() "
                             + "is called prior to all test executions or that BlockHoundTestExecutionListener is available on "
                             + "the class path and correctly detected by JUnit: "
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
