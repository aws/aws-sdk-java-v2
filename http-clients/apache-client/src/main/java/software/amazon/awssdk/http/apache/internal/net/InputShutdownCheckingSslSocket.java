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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Wrapper socket that ensures the read end of the socket is still open before performing a {@code write()}. In TLS 1.3, it is
 * permitted for the connection to be in a half-closed state, which is dangerous for the Apache client because it can get stuck in
 * a state where it continues to write to the socket and potentially end up a blocked state writing to the socket indefinitely.
 */
@SdkInternalApi
public final class InputShutdownCheckingSslSocket extends DelegateSslSocket {

    public InputShutdownCheckingSslSocket(SSLSocket sock) {
        super(sock);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new InputShutdownCheckingOutputStream(sock.getOutputStream(), sock);
    }

    private static class InputShutdownCheckingOutputStream extends FilterOutputStream {
        private final SSLSocket sock;

        InputShutdownCheckingOutputStream(OutputStream out, SSLSocket sock) {
            super(out);
            this.sock = sock;
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
            if (sock.isInputShutdown()) {
                throw new IOException("Remote end is closed.");
            }

            try {
                sock.getInputStream();
            } catch (IOException inputStreamException) {
                IOException e = new IOException("Remote end is closed.");
                e.addSuppressed(inputStreamException);
                throw e;
            }
        }
    }
}
