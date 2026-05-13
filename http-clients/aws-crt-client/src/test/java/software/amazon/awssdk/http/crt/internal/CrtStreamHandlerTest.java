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

package software.amazon.awssdk.http.crt.internal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.crt.http.HttpStreamBase;

@ExtendWith(MockitoExtension.class)
class CrtStreamHandlerTest {

    @Mock
    private HttpStreamBase stream;

    private CrtStreamHandler streamHandler;

    @BeforeEach
    void setUp() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Mockito.lenient().when(stream.isNull()).thenAnswer(invocation -> closed.get());
        Mockito.lenient().doAnswer((Answer<Void>) invocation -> {
            closed.set(true);
            return null;
        }).when(stream).close();

        streamHandler = new CrtStreamHandler();
        streamHandler.setStream(stream);
    }

    @Test
    void releaseConnection_shouldCallClose() {
        streamHandler.releaseConnection();

        verify(stream, never()).cancel();
        verify(stream).close();
    }

    @Test
    void closeConnection_shouldCallCancelAndClose() {
        streamHandler.closeConnection();

        verify(stream).cancel();
        verify(stream, Mockito.atLeastOnce()).close();
    }

    @Test
    void incrementWindow_afterReleaseConnection_shouldBeNoOp() {
        streamHandler.releaseConnection();
        streamHandler.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_afterCloseConnection_shouldBeNoOp() {
        streamHandler.closeConnection();
        streamHandler.incrementWindow(1024);

        verify(stream, never()).incrementWindow(1024);
    }

    @Test
    void incrementWindow_beforeClose_shouldWork() {
        streamHandler.incrementWindow(1024);

        verify(stream).incrementWindow(1024);
    }
}
