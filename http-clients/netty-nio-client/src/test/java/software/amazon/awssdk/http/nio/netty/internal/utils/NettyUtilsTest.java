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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLEngine;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

public class NettyUtilsTest {

    private static EventLoopGroup eventLoopGroup;

    @BeforeClass
    public static void setup() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        eventLoopGroup.shutdownGracefully().await();
    }

    @Test
    public void testGetOrCreateAttributeKey_calledTwiceWithSameName_returnsSameInstance() {
        String attr = "NettyUtilsTest.Foo";
        AttributeKey<String> fooAttr = NettyUtils.getOrCreateAttributeKey(attr);
        assertThat(NettyUtils.getOrCreateAttributeKey(attr)).isSameAs(fooAttr);
    }

    @Test
    public void newSslHandler_sslEngineShouldBeConfigured() throws Exception {
        SslContext sslContext = SslContextBuilder.forClient().build();
        Channel channel = null;
        try {
            channel = new MockChannel();
            SslHandler sslHandler = NettyUtils.newSslHandler(sslContext, channel.alloc(), "localhost", 80,
                                                             Duration.ofMillis(1000));

            assertThat(sslHandler.getHandshakeTimeoutMillis()).isEqualTo(1000);
            SSLEngine engine = sslHandler.engine();
            assertThat(engine.getSSLParameters().getEndpointIdentificationAlgorithm()).isEqualTo("HTTPS");
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }

    @Test
    public void doInEventLoop_inEventLoop_doesNotSubmit() {
        EventExecutor mockExecutor = mock(EventExecutor.class);
        when(mockExecutor.inEventLoop()).thenReturn(true);

        NettyUtils.doInEventLoop(mockExecutor, () -> {});
        verify(mockExecutor, never()).submit(any(Runnable.class));
    }

    @Test
    public void doInEventLoop_notInEventLoop_submits() {
        EventExecutor mockExecutor = mock(EventExecutor.class);
        when(mockExecutor.inEventLoop()).thenReturn(false);

        NettyUtils.doInEventLoop(mockExecutor, () -> {});
        verify(mockExecutor).submit(any(Runnable.class));
    }

    @Test
    public void runOrPropagate_success_runs() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();
        AtomicBoolean reference = new AtomicBoolean();

        GenericFutureListener<Future<Void>> listener =
            NettyUtils.runOrPropagate(destination, () -> reference.set(true));

        Promise<Void> source = eventLoopGroup.next().newPromise();
        source.setSuccess(null);
        listener.operationComplete(source);

        assertThat(reference.get()).isTrue();
    }

    @Test
    public void runOrPropagate_exception_propagates() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();

        GenericFutureListener<Future<Void>> listener =
            NettyUtils.runOrPropagate(destination, () -> {
            });

        Promise<Void> source = eventLoopGroup.next().newPromise();
        source.setFailure(new RuntimeException("Intentional exception for testing purposes"));
        listener.operationComplete(source);

        assertThat(destination.cause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Intentional exception for testing purposes");
    }

    @Test
    public void runOrPropagate_cancel_propagates() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();

        GenericFutureListener<Future<Void>> listener =
            NettyUtils.runOrPropagate(destination, () -> {
            });

        Promise<Void> source = eventLoopGroup.next().newPromise();
        source.cancel(false);
        listener.operationComplete(source);

        assertThat(destination.isCancelled()).isTrue();
    }

    @Test
    public void consumeOrPropagate_success_consumes() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();
        AtomicReference<String> reference = new AtomicReference<>();

        GenericFutureListener<Future<String>> listener =
            NettyUtils.consumeOrPropagate(destination, reference::set);

        Promise<String> source = eventLoopGroup.next().newPromise();
        source.setSuccess("test");
        listener.operationComplete(source);

        assertThat(reference.get()).isEqualTo("test");
    }

    @Test
    public void consumeOrPropagate_exception_propagates() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();

        GenericFutureListener<Future<String>> listener =
            NettyUtils.consumeOrPropagate(destination, s -> {
            });

        Promise<String> source = eventLoopGroup.next().newPromise();
        source.setFailure(new RuntimeException("Intentional exception for testing purposes"));
        listener.operationComplete(source);

        assertThat(destination.cause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Intentional exception for testing purposes");
    }

    @Test
    public void consumeOrPropagate_cancel_propagates() throws Exception {
        Promise<String> destination = eventLoopGroup.next().newPromise();

        GenericFutureListener<Future<String>> listener =
            NettyUtils.consumeOrPropagate(destination, s -> {
            });

        Promise<String> source = eventLoopGroup.next().newPromise();
        source.cancel(false);
        listener.operationComplete(source);

        assertThat(destination.isCancelled()).isTrue();
    }

    @Test
    public void runAndLogError_runnableDoesNotThrow_loggerNotInvoked() {
        Logger delegateLogger = mock(Logger.class);
        NettyClientLogger logger = new NettyClientLogger(delegateLogger);

        NettyUtils.runAndLogError(logger, "Something went wrong", () -> {});

        verifyZeroInteractions(delegateLogger);
    }

    @Test
    public void runAndLogError_runnableThrows_loggerInvoked() {
        Logger delegateLogger = mock(Logger.class);
        when(delegateLogger.isErrorEnabled()).thenReturn(true);

        NettyClientLogger logger = new NettyClientLogger(delegateLogger);

        String msg = "Something went wrong";
        RuntimeException error = new RuntimeException("Boom!");

        NettyUtils.runAndLogError(logger, msg, () -> {
            throw error;
        });

        verify(delegateLogger).error(msg, error);
    }
}
