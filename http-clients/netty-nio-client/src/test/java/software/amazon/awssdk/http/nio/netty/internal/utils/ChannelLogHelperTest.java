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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.netty.channel.ChannelId;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class ChannelLogHelperTest {
    private static final String TEST_MSG = "test";
    private static final ChannelId CHANNEL_ID = DefaultChannelId.newInstance();
    private static final String EXPECTED_MESSAGE = String.format("[Channel %s] %s", CHANNEL_ID.asShortText(), TEST_MSG);

    private static EmbeddedChannel ch;

    @Mock
    public Logger mockLogger;

    @Mock
    public Supplier<String> msgSupplier;

    @BeforeClass
    public static void setup() {
        ch = new EmbeddedChannel(CHANNEL_ID);
    }

    @Before
    public void methodSetup() {
        when(msgSupplier.get()).thenReturn(TEST_MSG);
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        ch.close().await();
    }

    @Test
    public void debugNotEnabled_doesNotInvokeLogger() {
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        ChannelLogHelper.debug(mockLogger, ch, msgSupplier, null);

        verify(mockLogger, never()).debug(anyString(), any(Throwable.class));
        verifyZeroInteractions(msgSupplier);
    }

    @Test
    public void debugEnabled_invokesLogger() {
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        ChannelLogHelper.debug(mockLogger, ch, msgSupplier, exception);

        verify(mockLogger).debug(EXPECTED_MESSAGE, exception);
    }

    @Test
    public void warnNotEnabled_doesNotInvokeLogger() {
        when(mockLogger.isWarnEnabled()).thenReturn(false);

        ChannelLogHelper.warn(mockLogger, ch, msgSupplier, null);

        verify(mockLogger, never()).warn(anyString(), any(Throwable.class));
        verifyZeroInteractions(msgSupplier);
    }

    @Test
    public void warnEnabled_invokesLogger() {
        when(mockLogger.isWarnEnabled()).thenReturn(true);
        RuntimeException exception = new RuntimeException("boom!");

        ChannelLogHelper.warn(mockLogger, ch, msgSupplier, exception);

        verify(mockLogger).warn(EXPECTED_MESSAGE, exception);
    }

    @Test
    public void traceNotEnabled_doesNotInvokeLogger() {
        when(mockLogger.isTraceEnabled()).thenReturn(false);

        ChannelLogHelper.trace(mockLogger, ch, msgSupplier);

        verify(mockLogger, never()).trace(anyString());
        verifyZeroInteractions(msgSupplier);
    }

    @Test
    public void traceEnabled_invokesLogger() {
        when(mockLogger.isTraceEnabled()).thenReturn(true);

        ChannelLogHelper.trace(mockLogger, ch, msgSupplier);

        verify(mockLogger).trace(EXPECTED_MESSAGE);
    }
}
