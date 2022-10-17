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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class NettyClientLoggerTest {
    private static final String TEST_MSG = "test";
    private static final ChannelId CHANNEL_ID = DefaultChannelId.newInstance();
    private static final String CHANNEL_TO_STRING = "NettyClientLoggerTest_TestChannel";
    private static final String EXPECTED_MESSAGE_SHORT = String.format("[Channel: %s] %s",
                                                                 CHANNEL_ID.asShortText(),
                                                                 TEST_MSG);
    private static final String EXPECTED_MESSAGE_FULL = String.format("[Channel: %s] %s",
                                                                       CHANNEL_TO_STRING,
                                                                       TEST_MSG);

    @Mock
    public Logger delegateLogger;

    @Mock
    public Supplier<String> msgSupplier;

    private NettyClientLogger logger;
    private EmbeddedChannel ch;


    @BeforeClass
    public static void setup() throws InterruptedException {
    }

    @Before
    public void methodSetup() {
        when(msgSupplier.get()).thenReturn(TEST_MSG);
        logger = new NettyClientLogger(delegateLogger);
        ch = spy(new EmbeddedChannel(CHANNEL_ID));
        when(ch.toString()).thenReturn(CHANNEL_TO_STRING);
    }

    @After
    public void methodTeardown() throws InterruptedException {
        ch.close().await();
    }

    @Test
    public void debugNotEnabled_doesNotInvokeLogger() {
        when(delegateLogger.isDebugEnabled()).thenReturn(false);
        Channel channel = mock(Channel.class);

        logger.debug(channel, msgSupplier, null);

        verify(delegateLogger, never()).debug(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void debugEnabled_invokesLogger() {
        when(delegateLogger.isDebugEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.debug(ch, msgSupplier, exception);

        verify(delegateLogger).debug(EXPECTED_MESSAGE_FULL, exception);
    }

    @Test
    public void debugNotEnabled_channelNotProvided_doesNotInvokeLogger() {
        when(delegateLogger.isDebugEnabled()).thenReturn(false);

        logger.debug(null, msgSupplier, null);

        verify(delegateLogger, never()).debug(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
    }

    @Test
    public void debugEnabled_channelNotProvided_invokesLogger() {
        when(delegateLogger.isDebugEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.debug(null, msgSupplier, exception);

        verify(delegateLogger).debug(TEST_MSG, exception);
    }

    @Test
    public void warnNotEnabled_doesNotInvokeLogger() {
        when(delegateLogger.isWarnEnabled()).thenReturn(false);
        Channel channel = mock(Channel.class);

        logger.warn(channel, msgSupplier, null);

        verify(delegateLogger, never()).warn(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void warnEnabled_invokesLogger() {
        when(delegateLogger.isWarnEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.warn(ch, msgSupplier, exception);

        verify(delegateLogger).warn(EXPECTED_MESSAGE_SHORT, exception);
    }

    @Test
    public void warnEnabled_debugEnabled_invokesLogger() {
        when(delegateLogger.isWarnEnabled()).thenReturn(true);
        when(delegateLogger.isDebugEnabled()).thenReturn(true);

        RuntimeException exception = new RuntimeException("boom!");

        logger.warn(ch, msgSupplier, exception);

        verify(delegateLogger).warn(EXPECTED_MESSAGE_FULL, exception);
    }

    @Test
    public void errorNotEnabled_noChannelProvided_doesNotInvokeLogger() {
        when(delegateLogger.isErrorEnabled()).thenReturn(false);

        logger.error(null, msgSupplier, null);

        verify(delegateLogger, never()).error(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
    }

    @Test
    public void errorEnabled_noChannelProvided_invokesLogger() {
        when(delegateLogger.isErrorEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.error(null, msgSupplier, exception);

        verify(delegateLogger).error(TEST_MSG, exception);
    }

    @Test
    public void errorNotEnabled_doesNotInvokeLogger() {
        when(delegateLogger.isErrorEnabled()).thenReturn(false);
        Channel channel = mock(Channel.class);

        logger.error(channel, msgSupplier, null);

        verify(delegateLogger, never()).error(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void errorEnabled_invokesLogger() {
        when(delegateLogger.isErrorEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.error(ch, msgSupplier, exception);

        verify(delegateLogger).error(EXPECTED_MESSAGE_SHORT, exception);
    }

    @Test
    public void errorEnabled_debugEnabled_invokesLogger() {
        when(delegateLogger.isErrorEnabled()).thenReturn(true);
        when(delegateLogger.isDebugEnabled()).thenReturn(true);

        RuntimeException exception = new RuntimeException("boom!");

        logger.error(ch, msgSupplier, exception);

        verify(delegateLogger).error(EXPECTED_MESSAGE_FULL, exception);
    }

    @Test
    public void warnNotEnabled_noChannelProvided_doesNotInvokeLogger() {
        when(delegateLogger.isWarnEnabled()).thenReturn(false);

        logger.warn(null, msgSupplier, null);

        verify(delegateLogger, never()).warn(anyString(), any(Throwable.class));
        verifyNoMoreInteractions(msgSupplier);
    }

    @Test
    public void warnEnabled_noChannelProvided_invokesLogger() {
        when(delegateLogger.isWarnEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        logger.warn(null, msgSupplier, exception);

        verify(delegateLogger).warn(TEST_MSG, exception);
    }

    @Test
    public void traceNotEnabled_doesNotInvokeLogger() {
        when(delegateLogger.isTraceEnabled()).thenReturn(false);
        Channel channel = mock(Channel.class);

        logger.trace(channel, msgSupplier);

        verify(delegateLogger, never()).trace(anyString());
        verifyNoMoreInteractions(msgSupplier);
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void traceEnabled_invokesLogger() {
        when(delegateLogger.isTraceEnabled()).thenReturn(true);
        when(delegateLogger.isDebugEnabled()).thenReturn(true);

        logger.trace(ch, msgSupplier);

        verify(delegateLogger).trace(EXPECTED_MESSAGE_FULL);
    }

    @Test
    public void traceNotEnabled_noChannelProvided_doesNotInvokeLogger() {
        when(delegateLogger.isTraceEnabled()).thenReturn(false);

        logger.trace(null, msgSupplier);

        verify(delegateLogger, never()).trace(anyString());
        verifyNoMoreInteractions(msgSupplier);
    }

    @Test
    public void traceEnabled_noChannelProvided_invokesLogger() {
        when(delegateLogger.isTraceEnabled()).thenReturn(true);

        logger.trace(null, msgSupplier);

        verify(delegateLogger).trace(TEST_MSG);
    }
}
