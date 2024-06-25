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

package software.amazon.awssdk.http.apache.internal.net;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLSocket;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Wrapper socket that ensures the read end of the socket is still open before performing a {@code write()}. In TLS 1.3, it is
 * permitted for the connection to be in a half-closed state, which is dangerous for the Apache client because it can get stuck in
 * a state where it continues to write to the socket and potentially end up a blocked state writing to the socket indefinitely.
 */
@SdkInternalApi
public final class InputShutdownCheckingSslSocket extends DelegateSslSocket {
    private static final Logger LOG = Logger.loggerFor(InputShutdownCheckingSslSocket.class);
    private final SSLSocket sslSocket;
    private final Socket layered;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public InputShutdownCheckingSslSocket(SSLSocket sslSocket, Socket layered) {
        super(sslSocket);
        this.sslSocket = sslSocket;
        this.layered = layered;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new InputShutdownCheckingOutputStream(sslSocket.getOutputStream());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(sslSocket.getInputStream());
    }

    // Attempt 1-byte read from the SSLSocket so that the SSLSocket reads the TLS record if there is one pending.
    private void bufferSslSocketRead() throws IOException {
        LOG.debug(() -> "Buffering SSLSocket data to consume pending TLS records that may contain a close_notify");
        synchronized (readBuffer) {
            if (!readBuffer.hasRemaining() || !dataAvailableOnLayeredSocket()) {
                return;
            }

            InputStream is = sslSocket.getInputStream();
            int originalSoTimeout = sslSocket.getSoTimeout();
            try {
                // Maximum time to block for the least amount of time possible (1ms)
                sslSocket.setSoTimeout(1);
                int read = is.read();
                if (read != -1) {
                    readBuffer.put((byte) read);
                }
            } catch (SocketTimeoutException to) {
                LOG.debug(() -> "Timeout doing single byte read on SSLSocket. Most likely due to incomplete record", to);
            } finally {
                sslSocket.setSoTimeout(originalSoTimeout);
            }
        }
    }

    private class BufferedInputStream extends FilterInputStream {
        private final byte[] oneByte = new byte[1];

        protected BufferedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int available() throws IOException {
            synchronized (readBuffer) {
                readBuffer.flip();
                int remaining = readBuffer.remaining();
                readBuffer.flip();

                if (remaining > 0) {
                    return remaining;
                }

                return in.available();
            }
        }

        @Override
        public int read() throws IOException {
            int read = read(oneByte, 0, 1);
            if (read == -1) {
                return -1;
            }
            return oneByte[0];
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }

            synchronized (readBuffer) {
                readBuffer.flip();
                if (readBuffer.hasRemaining()) {
                    int bufferedRead = Math.min(len, readBuffer.remaining());
                    readBuffer.get(b, off, bufferedRead);
                    readBuffer.flip();
                    return bufferedRead;
                } else {
                    readBuffer.flip();
                }

                return in.read(b, off, len);
            }
        }
    }

    private class InputShutdownCheckingOutputStream extends FilterOutputStream {

        InputShutdownCheckingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            checkInputShutdown();
            out.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            checkInputShutdown();
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkInputShutdown();
            out.write(b, off, len);
        }

        private void checkInputShutdown() throws IOException {
            if (sslSocket.isInputShutdown()) {
                throw new IOException("Remote end is closed.");
            }

            try {
                if (dataAvailableOnLayeredSocket()) {
                    LOG.debug(() -> String.format("%d bytes available in layered socket, calling SSLSocket's read() to try and "
                                                 + "consume the TLS record(s) if present."));
                    bufferSslSocketRead();
                }
                sslSocket.getInputStream();
            } catch (IOException inputStreamException) {
                IOException e = new IOException("Remote end is closed.");
                e.addSuppressed(inputStreamException);
                throw e;
            }
        }
    }

    private boolean dataAvailableOnLayeredSocket() throws IOException {
        return layered.getInputStream().available() > 0;
    }
}
