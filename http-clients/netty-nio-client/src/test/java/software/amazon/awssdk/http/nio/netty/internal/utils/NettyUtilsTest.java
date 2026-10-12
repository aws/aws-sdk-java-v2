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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.net.ssl.SSLEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import software.amazon.awssdk.http.nio.netty.internal.ChannelDiagnostics;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

public class NettyUtilsTest {

    private static EventLoopGroup eventLoopGroup;

    @BeforeAll
    public static void setup() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @AfterAll
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

        verifyNoMoreInteractions(delegateLogger);
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

    @Test
    public void closedChannelMessage_with_nullChannelAttribute() throws Exception {

        Channel channel = Mockito.mock(Channel.class);
        when(channel.parent()).thenReturn(null);

        assertThat(NettyUtils.closedChannelMessage(channel))
            .isEqualTo(NettyUtils.CLOSED_CHANNEL_ERROR_MESSAGE);
    }

    @Test
    public void closedChannelMessage_with_nullChannel() throws Exception {
        Channel channel = null;
        assertThat(NettyUtils.closedChannelMessage(channel))
            .isEqualTo(NettyUtils.CLOSED_CHANNEL_ERROR_MESSAGE);
    }


    @Test
    public void closedChannelMessage_with_nullParentChannel() throws Exception {

        Channel channel = mock(Channel.class);
        Attribute attribute = mock(Attribute.class);
        when(channel.parent()).thenReturn(null);
        when(channel.attr(any())).thenReturn(attribute);

        assertThat(NettyUtils.closedChannelMessage(channel))
            .isEqualTo(NettyUtils.CLOSED_CHANNEL_ERROR_MESSAGE);
    }

    @Test
    public void closedChannelMessage_with_nullParentChannelAttribute() throws Exception {

        Channel channel = mock(Channel.class);
        Attribute attribute = mock(Attribute.class);
        Channel parentChannel = mock(Channel.class);
        when(channel.parent()).thenReturn(parentChannel);
        when(channel.attr(any())).thenReturn(attribute);
        when(parentChannel.attr(any())).thenReturn(null);

        assertThat(NettyUtils.closedChannelMessage(channel))
            .isEqualTo(NettyUtils.CLOSED_CHANNEL_ERROR_MESSAGE);
    }

    private static Stream<Arguments> decorateExceptionWrappedCases() {
        return Stream.of(
            Arguments.of(new TimeoutException("...Acquire operation took longer..."),
                         Throwable.class, TimeoutException.class),
            Arguments.of(new IllegalStateException("...Too many outstanding acquire operations..."),
                         Throwable.class, IllegalStateException.class),
            Arguments.of(new ReadTimeoutException(),
                         IOException.class, ReadTimeoutException.class),
            Arguments.of(new WriteTimeoutException(),
                         IOException.class, WriteTimeoutException.class),
            Arguments.of(new ClosedChannelException(),
                         IOException.class, ClosedChannelException.class),
            Arguments.of(new IOException("...Connection reset by peer..."),
                         IOException.class, IOException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("decorateExceptionWrappedCases")
    public void decorateException_wrapsException(Throwable input,
                                                 Class<? extends Throwable> expectedType,
                                                 Class<? extends Throwable> expectedCauseType) {
        Channel channel = mock(Channel.class);
        Throwable output = NettyUtils.decorateException(channel, input);

        assertThat(output).isInstanceOf(expectedType);
        assertThat(output.getCause()).isInstanceOf(expectedCauseType);
        assertThat(output.getMessage()).isNotNull();
    }

    private static Stream<Arguments> decorateExceptionPassthroughCases() {
        return Stream.of(
            Arguments.of(new TimeoutException()),
            Arguments.of(new IllegalStateException()),
            Arguments.of(new IOException())
        );
    }

    @ParameterizedTest
    @MethodSource("decorateExceptionPassthroughCases")
    public void decorateException_noMatchingMessage_returnsOriginal(Throwable input) {
        Channel channel = mock(Channel.class);
        Throwable output = NettyUtils.decorateException(channel, input);

        assertThat(output).isSameAs(input);
    }

    private static Stream<Arguments> channelDiagnosticsExceptionCases() {
        return Stream.of(
            Arguments.of(new ReadTimeoutException(), "Read timed out"),
            Arguments.of(new WriteTimeoutException(), "Write timed out")
        );
    }

    @ParameterizedTest
    @MethodSource("channelDiagnosticsExceptionCases")
    public void decorateException_includesChannelDiagnostics(Throwable input, String expectedPrefix) {
        Channel channel = mock(Channel.class);
        Attribute attribute = mock(Attribute.class);
        ChannelDiagnostics diagnostics = new ChannelDiagnostics(channel);
        when(channel.attr(any())).thenReturn(attribute);
        when(attribute.get()).thenReturn(diagnostics);
        when(channel.parent()).thenReturn(null);

        Throwable output = NettyUtils.decorateException(channel, input);

        assertThat(output).isInstanceOf(IOException.class);
        assertThat(output.getMessage()).startsWith(expectedPrefix);
        assertThat(output.getMessage()).contains("Channel Information:");
    }

    @Test
    public void errorMessageWithChannelDiagnostics_nullChannel_returnsBaseMessage() {
        assertThat(NettyUtils.errorMessageWithChannelDiagnostics(null, "test message"))
            .isEqualTo("test message");
    }

    @Test
    public void errorMessageWithChannelDiagnostics_withDiagnostics_appendsChannelInfo() {
        Channel channel = mock(Channel.class);
        Attribute attribute = mock(Attribute.class);
        ChannelDiagnostics diagnostics = new ChannelDiagnostics(channel);
        when(channel.attr(any())).thenReturn(attribute);
        when(attribute.get()).thenReturn(diagnostics);
        when(channel.parent()).thenReturn(null);

        String result = NettyUtils.errorMessageWithChannelDiagnostics(channel, "custom error");

        assertThat(result).startsWith("custom error");
        assertThat(result).contains("Channel Information:");
    }

}
