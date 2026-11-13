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

package software.amazon.awssdk.http.nio.netty.fault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP proxy on loopback that forwards to a TLS server, or breaks each new connection's TLS handshake according to the
 * current {@link Fault}. Every fault happens after TCP connects and after the client sends its ClientHello.
 */
final class TlsFaultProxy implements AutoCloseable {
    private static final int BUFFER_SIZE = 16 * 1024;

    private final ServerSocket serverSocket;
    private final int serverPort;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "tls-fault-proxy");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<ForwardedConnection> forwardedConnections = ConcurrentHashMap.newKeySet();
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final AtomicInteger acceptedConnections = new AtomicInteger();
    private volatile Fault fault = Fault.NONE;

    enum Fault {
        /**
         * Forward the connection to the server.
         */
        NONE,

        /**
         * Read the ClientHello and never reply, so the client's TLS negotiation timeout fires.
         */
        STALL,

        /**
         * Read the ClientHello, then close the connection gracefully (FIN).
         */
        CLOSE,

        /**
         * Read the ClientHello, then reset the connection (RST).
         */
        RESET
    }

    TlsFaultProxy(int serverPort) throws IOException {
        this.serverPort = serverPort;
        this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        executor.execute(this::acceptConnections);
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    int acceptedConnections() {
        return acceptedConnections.get();
    }

    void injectFault(Fault fault) {
        this.fault = fault;
    }

    /**
     * Sends FIN to the client on every forwarded connection and waits until the client closes its side. When this returns,
     * the client has closed those connections and opens a new one for its next request.
     */
    void closeForwardedConnectionsAndAwaitClientClose(long timeout, TimeUnit unit) throws Exception {
        List<CompletableFuture<Void>> clientClosed = new ArrayList<>();
        for (ForwardedConnection connection : forwardedConnections) {
            connection.client.shutdownOutput();
            clientClosed.add(connection.clientClosed);
        }
        CompletableFuture.allOf(clientClosed.toArray(new CompletableFuture[0])).get(timeout, unit);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        sockets.forEach(TlsFaultProxy::closeQuietly);
        executor.shutdownNow();
    }

    private void acceptConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                sockets.add(client);
                acceptedConnections.incrementAndGet();
                Fault currentFault = fault;
                executor.execute(() -> handle(client, currentFault));
            } catch (IOException e) {
                // The proxy was closed.
            }
        }
    }

    private void handle(Socket client, Fault currentFault) {
        try {
            switch (currentFault) {
                case NONE:
                    forward(client);
                    break;
                case STALL:
                    readUntilClosed(client);
                    break;
                case CLOSE:
                    readClientHello(client);
                    client.close();
                    break;
                case RESET:
                    readClientHello(client);
                    client.setSoLinger(true, 0);
                    client.close();
                    break;
                default:
                    throw new IllegalStateException("Unknown fault: " + currentFault);
            }
        } catch (IOException e) {
            // The client or the proxy closed the connection.
        } finally {
            if (currentFault != Fault.NONE) {
                closeQuietly(client);
            }
        }
    }

    private void forward(Socket client) throws IOException {
        Socket upstream = new Socket(InetAddress.getLoopbackAddress(), serverPort);
        sockets.add(upstream);
        ForwardedConnection connection = new ForwardedConnection(client);
        forwardedConnections.add(connection);

        executor.execute(() -> {
            // This direction never closes the client socket, so that a FIN sent by
            // closeForwardedConnectionsAndAwaitClientClose is followed only by the client's own close.
            try {
                copy(upstream.getInputStream(), client.getOutputStream());
            } catch (IOException e) {
                // Either side closed the connection.
            }
        });

        try {
            copy(client.getInputStream(), upstream.getOutputStream());
        } catch (IOException e) {
            // Either side closed the connection.
        } finally {
            forwardedConnections.remove(connection);
            closeQuietly(upstream);
            closeQuietly(client);
            connection.clientClosed.complete(null);
        }
    }

    private static void readClientHello(Socket client) throws IOException {
        if (client.getInputStream().read(new byte[BUFFER_SIZE]) == -1) {
            throw new IOException("The client closed the connection before it sent a ClientHello");
        }
    }

    private static void readUntilClosed(Socket client) throws IOException {
        InputStream in = client.getInputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        while (in.read(buffer) != -1) {
            // Discard the ClientHello and never reply.
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            out.flush();
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignored, the socket is being discarded.
        }
    }

    private static final class ForwardedConnection {
        private final Socket client;
        private final CompletableFuture<Void> clientClosed = new CompletableFuture<>();

        private ForwardedConnection(Socket client) {
            this.client = client;
        }
    }
}
