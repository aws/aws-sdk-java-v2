package software.amazon.awssdk.http.apache.internal.conn;

import org.junit.jupiter.api.Test;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.OutputStream;
import software.amazon.awssdk.http.apache.internal.net.InputShutdownCheckingSslSocket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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