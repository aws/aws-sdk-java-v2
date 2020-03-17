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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class SdkSocket extends DelegateSocket {
    private static final Logger log = Logger.loggerFor(SdkSocket.class);

    public SdkSocket(Socket sock) {
        super(sock);
        log.debug(() -> "created: " + endpoint());
    }

    /**
     * Returns the endpoint in the format of "address:port"
     */
    private String endpoint() {
        return sock.getInetAddress() + ":" + sock.getPort();
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        log.trace(() -> "connecting to: " + endpoint);
        sock.connect(endpoint);
        log.debug(() -> "connected to: " + endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        log.trace(() -> "connecting to: " + endpoint);
        sock.connect(endpoint, timeout);
        log.debug(() -> "connected to: " + endpoint);
    }

    @Override
    public void close() throws IOException {
        log.debug(() -> "closing " + endpoint());
        sock.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        log.debug(() -> "shutting down input of " + endpoint());
        sock.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        log.debug(() -> "shutting down output of " + endpoint());
        sock.shutdownOutput();
    }
}
