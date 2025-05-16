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

package software.amazon.awssdk.http.apache5.internal.conn;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache5.internal.net.InputShutdownCheckingSslSocket;

public class InputShutdownCheckingSslSocketTest {

    @Test
    public void outputStreamChecksInputShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        when(mockSocket.isInputShutdown()).thenReturn(true);
        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();
        assertThrows(IOException.class, () -> os.write(1));
    }

    @Test
    public void outputStreamWritesNormallyWhenInputNotShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockSocket.isInputShutdown()).thenReturn(false);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();
        os.write(1);
        verify(mockOutputStream).write(1);
    }

    @Test
    public void writeByteArrayThrowsIOExceptionWhenInputIsShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        when(mockSocket.isInputShutdown()).thenReturn(true);
        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();
        assertThrows(IOException.class, () -> os.write(new byte[10]));
    }

    @Test
    public void writeByteArraySucceedsWhenInputNotShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockSocket.isInputShutdown()).thenReturn(false);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();

        byte[] data = new byte[10];
        os.write(data);
        verify(mockOutputStream).write(data);
    }

    @Test
    public void writeByteArrayWithOffsetThrowsIOExceptionWhenInputIsShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        when(mockSocket.isInputShutdown()).thenReturn(true);

        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();

        assertThrows(IOException.class, () -> os.write(new byte[10], 0, 10));
    }

    @Test
    public void writeByteArrayWithOffsetSucceedsWhenInputNotShutdown() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockSocket.isInputShutdown()).thenReturn(false);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();

        byte[] data = new byte[10];
        os.write(data, 0, 10);
        verify(mockOutputStream).write(data, 0, 10);
    }

    @Test
    public void checkInputShutdownThrowsIOExceptionWithSuppressed() throws IOException {
        SSLSocket mockSocket = mock(SSLSocket.class);
        when(mockSocket.isInputShutdown()).thenReturn(false);
        when(mockSocket.getInputStream()).thenThrow(new IOException("InputStream exception"));

        InputShutdownCheckingSslSocket socket = new InputShutdownCheckingSslSocket(mockSocket);
        OutputStream os = socket.getOutputStream();

        IOException thrown = assertThrows(IOException.class, () -> os.write(1));
        assertTrue(thrown.getMessage().contains("Remote end is closed."));
        assertTrue(thrown.getSuppressed()[0].getMessage().contains("InputStream exception"));
    }
}