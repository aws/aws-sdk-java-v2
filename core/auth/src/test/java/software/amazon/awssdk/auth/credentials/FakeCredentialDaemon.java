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

package software.amazon.awssdk.auth.credentials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A fake credential daemon that listens on a Unix domain socket in a fresh temporary directory. Unix domain sockets are
 * reached through reflection only, because the tests are compiled for Java 8.
 */
public final class FakeCredentialDaemon implements AutoCloseable {
    /**
     * A reply that makes the daemon read the request and then never answer.
     */
    public static final byte[] NO_REPLY = new byte[0];

    /**
     * A reply that makes the daemon close the connection after reading the request, without answering.
     */
    public static final byte[] CLOSE_AT_ONCE = new byte[0];

    private final Path directory;
    private final Path socketPath;
    private final ServerSocketChannel server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<SocketChannel> heldConnections = new CopyOnWriteArrayList<>();
    private volatile Function<String, byte[]> responder;
    private volatile boolean closeBeforeReading;

    private FakeCredentialDaemon(Function<String, byte[]> responder, boolean accept, int backlog) throws IOException {
        this.directory = newSocketDirectory();
        this.socketPath = directory.resolve("d.sock");
        this.responder = responder;
        this.server = (ServerSocketChannel) invokeOpen(ServerSocketChannel.class);
        server.bind(address(socketPath), backlog);
        if (accept) {
            Thread thread = new Thread(this::acceptLoop, "fake-credential-daemon");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Returns whether this JVM and OS can run the fake daemon (Java 16 or later, not Windows).
     */
    public static boolean isSupported() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            return false;
        }
        try {
            Class.forName("java.net.UnixDomainSocketAddress");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("linux");
    }

    /**
     * Starts a daemon that answers every request with the bytes the responder returns for the request line.
     */
    public static FakeCredentialDaemon start(Function<String, byte[]> responder) throws IOException {
        return new FakeCredentialDaemon(responder, true, 50);
    }

    /**
     * Starts a daemon that answers every request with the given reply line, followed by a newline.
     */
    public static FakeCredentialDaemon replying(String replyLine) throws IOException {
        return start(r -> line(replyLine));
    }

    /**
     * Binds a socket with a backlog of 1 that never accepts, and fills the backlog, so the next blocking connect waits and the next non-blocking connect fails.
     */
    public static FakeCredentialDaemon withFullBacklog() throws IOException {
        FakeCredentialDaemon daemon = new FakeCredentialDaemon(r -> NO_REPLY, false, 1);
        daemon.fillBacklog();
        return daemon;
    }

    /**
     * Binds a socket and closes it at once. The socket file stays behind, as it does after a daemon crashes.
     */
    public static Path staleSocket(Path directory) throws IOException {
        Path path = directory.resolve("stale.sock");
        try (ServerSocketChannel channel = (ServerSocketChannel) invokeOpen(ServerSocketChannel.class)) {
            channel.bind(address(path));
        }
        return path;
    }

    /**
     * Binds a listening socket at the given path, which the caller closes.
     */
    public static AutoCloseable listeningSocket(Path path) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) invokeOpen(ServerSocketChannel.class);
        channel.bind(address(path));
        return channel;
    }

    public static Path newSocketDirectory() throws IOException {
        Path tmp = Paths.get("/tmp");
        return Files.isDirectory(tmp) ? Files.createTempDirectory(tmp, "cd") : Files.createTempDirectory("cd");
    }

    public static byte[] line(String reply) {
        return (reply + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public static String successReply(String accessKeyId, Instant expiresAt) {
        return successReply(accessKeyId, "secret", expiresAt);
    }

    public static String successReply(String accessKeyId, String secretAccessKey, Instant expiresAt) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"credential\":\"dev\",\"material\":{"
               + "\"type\":\"session_credentials\",\"access_key_id\":\"" + accessKeyId + "\","
               + "\"secret_access_key\":\"" + secretAccessKey + "\",\"session_token\":\"token\"},"
               + "\"expires_at\":\"" + expiresAt.truncatedTo(ChronoUnit.SECONDS) + "\"}}";
    }

    public static String errorReply(String code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32000,\"message\":\"" + message + "\","
               + "\"data\":{\"code\":\"" + code + "\"}}}";
    }

    public Path socketPath() {
        return socketPath;
    }

    public Path directory() {
        return directory;
    }

    public List<String> requests() {
        return Collections.unmodifiableList(new ArrayList<>(requests));
    }

    public void responder(Function<String, byte[]> responder) {
        this.responder = responder;
    }

    public void closeBeforeReading(boolean closeBeforeReading) {
        this.closeBeforeReading = closeBeforeReading;
    }

    public void reply(String replyLine) {
        responder(r -> line(replyLine));
    }

    private void fillBacklog() throws IOException {
        for (int i = 0; i < 1000; i++) {
            SocketChannel client = (SocketChannel) invokeOpen(SocketChannel.class);
            client.configureBlocking(false);
            try {
                client.connect(address(socketPath));
                heldConnections.add(client);
            } catch (IOException e) {
                client.close();
                return;
            }
        }
        throw new IllegalStateException("Could not fill the backlog of " + socketPath);
    }

    private void acceptLoop() {
        while (server.isOpen()) {
            SocketChannel channel;
            try {
                channel = server.accept();
            } catch (IOException e) {
                return;
            }
            Thread handler = new Thread(() -> handle(channel), "fake-credential-daemon-connection");
            handler.setDaemon(true);
            handler.start();
        }
    }

    private void handle(SocketChannel channel) {
        try {
            Function<String, byte[]> currentResponder = responder;
            if (closeBeforeReading) {
                channel.close();
                return;
            }
            String request = readLine(channel);
            if (request == null) {
                channel.close();
                return;
            }
            requests.add(request);
            byte[] reply = currentResponder.apply(request);
            if (reply == NO_REPLY) {
                heldConnections.add(channel);
                return;
            }
            if (reply == CLOSE_AT_ONCE) {
                channel.close();
                return;
            }
            ByteBuffer buffer = ByteBuffer.wrap(reply);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.close();
        } catch (IOException | RuntimeException e) {
            closeQuietly(channel);
        }
    }

    private static String readLine(SocketChannel channel) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        while (channel.read(buffer) > 0) {
            byte b = buffer.get(0);
            buffer.clear();
            if (b == '\n') {
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            out.write(b);
        }
        return null;
    }

    @Override
    public void close() {
        closeQuietly(server);
        heldConnections.forEach(FakeCredentialDaemon::closeQuietly);
        try {
            Files.deleteIfExists(socketPath);
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            // Best effort
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            // Ignored
        }
    }

    static SocketAddress address(Path path) {
        try {
            return (SocketAddress) Class.forName("java.net.UnixDomainSocketAddress")
                                        .getMethod("of", Path.class)
                                        .invoke(null, path);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invokeOpen(Class<?> channelClass) throws IOException {
        try {
            return channelClass.getMethod("open", ProtocolFamily.class)
                               .invoke(null, StandardProtocolFamily.valueOf("UNIX"));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
