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

package software.amazon.awssdk.http.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * MockServer implementation with several different configurable behaviors.
 */
public class MockServer {

    private static final Logger logger = Logger.loggerFor(MockServer.class);
    private final ServerBehaviorStrategy serverBehaviorStrategy;
    private ServerSocket serverSocket;
    private Thread listenerThread;

    public MockServer(ServerBehaviorStrategy serverBehaviorStrategy) {
        this.serverBehaviorStrategy = serverBehaviorStrategy;
    }

    public static MockServer createMockServer(ServerBehavior serverBehavior) {
        switch (serverBehavior) {
            case HALF_CLOSE:
                if (!isTlsHalfCloseSupported()) {
                    throw new UnsupportedOperationException("Half close is not supported in the current Java Version "
                                                            + getJavaVersion());
                }
                return new MockServer(new HalfCloseServerBehavior());
            case FULL_CLOSE_IN_BETWEEN:
                return new MockServer(new FullCloseInBetweenServerBehavior());
            case FULL_CLOSE_AT_THE_END:
                return new MockServer(new FullCloseAtTheEndServerBehavior());
            default:
                throw new IllegalArgumentException("Unsupported implementation for server issue: " + serverBehavior);
        }
    }

    private static Map<String, String> parseHeaders(String headers) {
        Map<String, String> headerMap = new HashMap<>();
        String[] lines = headers.split("\r\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                headerMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        return headerMap;
    }

    private static String toByteToString(ByteArrayOutputStream baos) {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(baos.toByteArray())).toString();
    }

    public void startServer(TlsKeyManagersProvider keyManagersProvider) {
        try {
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(keyManagersProvider.keyManagers(), null, null);
            SSLServerSocketFactory ssf = ctx.getServerSocketFactory();
            serverSocket = ssf.createServerSocket(0);
            logger.info(() -> "Listening on port " + serverSocket.getLocalPort());
        } catch (Exception e) {
            throw new RuntimeException("Unable to start the server socket.", e);
        }
        listenerThread = new MockServerListenerThread(serverSocket, serverBehaviorStrategy);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stopServer() {
        listenerThread.interrupt();
        try {
            listenerThread.join(5 * 1000);
        } catch (InterruptedException e1) {
            logger.error(() -> "The listener thread didn't terminate after waiting for 10 seconds.");
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
        HALF_CLOSE,
        FULL_CLOSE_IN_BETWEEN,
        FULL_CLOSE_AT_THE_END
    }

    public interface ServerBehaviorStrategy {
        void runServer(ServerSocket serverSocket);
    }

    private static class MockServerListenerThread extends Thread {

        private final ServerSocket serverSocket;
        private final ServerBehaviorStrategy behaviorStrategy;

        MockServerListenerThread(ServerSocket serverSocket, ServerBehaviorStrategy behaviorStrategy) {
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

    public static class HalfCloseServerBehavior implements ServerBehaviorStrategy {

        @Override
        public void runServer(ServerSocket serverSocket) {
            Socket socket = null;
            try {
                while (true) {

                    try {
                        socket = serverSocket.accept();
                        byte[] buff = new byte[4096];
                        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
                        int read;
                        while ((read = socket.getInputStream().read(buff)) != -1) {
                            headerStream.write(buff, 0, read);
                            String headers = toByteToString(headerStream);
                            if (headers.contains("\r\n\r\n")) {
                                break;
                            }
                        }
                        Thread.sleep(100);
                        socket.shutdownOutput();
                        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeBytes("HTTP/1.1 200 OK\r\n");
                            out.writeBytes("Content-Type: text/html\r\n");
                            out.writeBytes("Content-Length: 0\r\n\r\n");
                            out.flush();
                        }

                    } catch (SocketException se) {
                        // Socket is already closed so expected
                        return;
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            } finally {
                closeQuietly(socket);
            }
        }
    }

    public static class FullCloseInBetweenServerBehavior implements ServerBehaviorStrategy {

        @Override
        public void runServer(ServerSocket serverSocket) {
            Socket socket = null;
            try {
                while (true) {

                    try {
                        socket = serverSocket.accept();
                        byte[] buff = new byte[4096];
                        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
                        int read;
                        while ((read = socket.getInputStream().read(buff)) != -1) {
                            headerStream.write(buff, 0, read);
                            String headers = toByteToString(headerStream);
                            if (headers.contains("\r\n\r\n")) {
                                break;
                            }
                        }
                        socket.close();
                        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeBytes("HTTP/1.1 200 OK\r\n");
                            out.writeBytes("Content-Type: text/html\r\n");
                            out.writeBytes("Content-Length: 0\r\n\r\n");
                            out.flush();
                        }
                    } catch (SocketException se) {
                        // Socket is already closed so expected
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            } finally {
                closeQuietly(socket);
            }
        }
    }

    public static class FullCloseAtTheEndServerBehavior implements ServerBehaviorStrategy {

        @Override
        public void runServer(ServerSocket serverSocket) {
            Socket socket = null;
            try {
                while (true) {

                    try {
                        socket = serverSocket.accept();
                        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
                        byte[] buff = new byte[4096];
                        int read;
                        while ((read = socket.getInputStream().read(buff)) != -1) {
                            headerStream.write(buff, 0, read);
                            String headers = toByteToString(headerStream);
                            if (headers.contains("\r\n\r\n")) {
                                break;
                            }
                        }
                        String headers = toByteToString(headerStream);
                        Map<String, String> headerMap = parseHeaders(headers);
                        int contentLength = Integer.parseInt(headerMap.getOrDefault("Content-Length", "0"));
                        if (headers.startsWith("PUT")) {
                            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
                            int remaining = contentLength;
                            while (remaining > 0 &&
                                   (read = socket.getInputStream().read(buff, 0, Math.min(buff.length, remaining))) != -1) {
                                bodyStream.write(buff, 0, read);
                                remaining -= read;
                            }
                        }
                        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                            out.writeBytes("HTTP/1.1 200 OK\r\n");
                            out.writeBytes("Content-Type: text/html\r\n");
                            out.writeBytes("Content-Length: 0\r\n\r\n");
                            out.flush();
                        }
                        while (true) {
                            // Stop server will interrupt to stop this thread.
                            Thread.sleep(1000);
                        }

                    } catch (SocketException se) {
                        return;
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error when waiting for new socket connection.", e);
            } finally {
                closeQuietly(socket);
            }
        }
    }


    //  TLS1.3 enbaled by default for 8u341 onwards.
    //  https://www.oracle.com/java/technologies/javase/8u341-relnotes.html
    public static boolean isTlsHalfCloseSupported() {
        String javaVersion = getJavaVersion();
        String[] versionComponents = javaVersion.split("_");
        if (versionComponents.length == 2) {
            try {
                int buildNumber = Integer.parseInt(versionComponents[1].split("-")[0]);
                if (javaVersion.startsWith("1.8.0") && buildNumber < 341) {
                    return false;
                }
            } catch (NumberFormatException e) {
                logger.error(() -> "Invalid Java version format: " + javaVersion);
                throw e;
            }
        }
        return true;
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ex) {
                logger.debug(() -> "Ignore failure in closing the socket", ex);
            }
        }
    }

    private static String getJavaVersion() {
        // CHECKSTYLE:OFF
        return System.getProperty("java.version");
        // CHECKSTYLE:ON
    }

}
