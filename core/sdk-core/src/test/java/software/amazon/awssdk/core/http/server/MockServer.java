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

package software.amazon.awssdk.core.http.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * MockServer implementation with several different configurable behaviors
 */
public class MockServer {

    private final ServerBehaviorStrategy serverBehaviorStrategy;
    /**
     * The server socket which the test service will listen to.
     */
    private ServerSocket serverSocket;
    private Thread listenerThread;

    public MockServer(final ServerBehaviorStrategy serverBehaviorStrategy) {
        this.serverBehaviorStrategy = serverBehaviorStrategy;
    }

    public static MockServer createMockServer(ServerBehavior serverBehavior) {
        switch (serverBehavior) {
            case UNRESPONSIVE:
                return new MockServer(new UnresponsiveServerBehavior());
            case OVERLOADED:
                return new MockServer(new OverloadedServerBehavior());
            default:
                throw new IllegalArgumentException("Unsupported implementation for server issue: " + serverBehavior);
        }
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(0); // auto-assign a port at localhost
            System.out.println("Listening on port " + serverSocket.getLocalPort());
        } catch (IOException e) {
            throw new RuntimeException("Unable to start the server socker.", e);
        }

        listenerThread = new MockServerListenerThread(serverSocket, serverBehaviorStrategy);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stopServer() {
        listenerThread.interrupt();
        try {
            listenerThread.join(10 * 1000);
        } catch (InterruptedException e1) {
            System.err.println("The listener thread didn't terminate " + "after waiting for 10 seconds.");
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to stop the server socket.", e);
            }
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public SdkHttpFullRequest.Builder configureHttpsEndpoint(SdkHttpFullRequest.Builder request) {
        return request.uri(URI.create("https://localhost"))
                      .port(getPort());
    }

    public SdkHttpFullRequest.Builder configureHttpEndpoint(SdkHttpFullRequest.Builder request) {
        return request.uri(URI.create("http://localhost"))
                      .port(getPort());
    }

    public enum ServerBehavior {
        UNRESPONSIVE,
        OVERLOADED,
        DUMMY_RESPONSE;
    }

    public interface ServerBehaviorStrategy {
        void runServer(ServerSocket serverSocket);
    }

    private static class MockServerListenerThread extends Thread {
        /** The server socket which this thread listens and responds to. */
        private final ServerSocket serverSocket;
        private final ServerBehaviorStrategy behaviorStrategy;

        public MockServerListenerThread(ServerSocket serverSocket, ServerBehaviorStrategy behaviorStrategy) {
            super(behaviorStrategy.getClass().getName());
            this.serverSocket = serverSocket;
            this.behaviorStrategy = behaviorStrategy;
            setDaemon(true);
        }

        @Override
        public void run() {
            this.behaviorStrategy.runServer(serverSocket);
        }
    }

    /**
     * A daemon thread which runs a simple server that listens to a specific server socket. Whenever
     * a connection is created, the server simply keeps holding the connection open while
     * periodically writing data. The test client talking to this server is expected to timeout
     * appropriately, instead of hanging and waiting for the response forever.
     */
    public static class OverloadedServerBehavior implements ServerBehaviorStrategy {

        @Override
        public void runServer(ServerSocket serverSocket) {
            try {
                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeBytes("HTTP/1.1 200 OK\r\n");
                            out.writeBytes("Content-Type: text/html\r\n");
                            out.writeBytes("Content-Length: 500\r\n\r\n");
                            out.writeBytes("<html><head></head><body><h1>Hello.");
                            while (true) {
                                Thread.sleep(1 * 1000);
                                out.writeBytes("Hi.");
                            }
                        }
                    } catch (SocketException se) {
                        // Ignored or expected.
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            } catch (InterruptedException e) {
                System.err.println("Socket listener thread interrupted. Terminating the thread...");
                return;
            }
        }
    }

    /**
     * A daemon thread which runs a simple server that listens to a specific server socket. Whenever
     * a connection is created, the server simply keeps holding the connection open and no byte will
     * be written to the socket. The test client talking to this server is expected to timeout
     * appropriately, instead of hanging and waiting for the response forever.
     */
    public static class UnresponsiveServerBehavior implements ServerBehaviorStrategy {
        @Override
        public void runServer(ServerSocket serverSocket) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                System.out.println("Socket created on port " + socket.getLocalPort());
                while (true) {
                    System.out.println("I don't want to talk.");
                    Thread.sleep(10 * 1000);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            } catch (InterruptedException e) {
                System.err.println("Socket listener thread interrupted. Terminating the thread...");
                return;
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Fail to close the socket", e);
                }
            }
        }
    }

    public static class DummyResponseServerBehavior implements ServerBehaviorStrategy {

        private final HttpResponse response;
        private String content;

        public DummyResponseServerBehavior(HttpResponse response) {
            this.response = response;
            try {
                this.content = IoUtils.toUtf8String(response.getEntity().getContent());
            } catch (Exception e) {
                // Ignored or expected.
            }
        }

        public static DummyResponseServerBehavior build(int statusCode, String statusMessage, String content) {
            HttpResponse response = new BasicHttpResponse(
                    new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, statusMessage));
            setEntity(response, content);
            response.addHeader("Content-Length", String.valueOf(content.getBytes().length));
            response.addHeader("Connection", "close");
            return new DummyResponseServerBehavior(response);
        }

        private static void setEntity(HttpResponse response, String content) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new StringInputStream(content));
            response.setEntity(entity);
        }

        @Override
        public void runServer(ServerSocket serverSocket) {
            try {
                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(response.getStatusLine().toString() + "\r\n");
                            for (Header header : response.getAllHeaders()) {
                                builder.append(header.getName() + ":" + header.getValue() + "\r\n");
                            }
                            builder.append("\r\n");
                            builder.append(content);
                            System.out.println(builder.toString());
                            out.writeBytes(builder.toString());
                        }
                    } catch (SocketException se) {
                        // Ignored or expected.
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            }
        }

    }

}
