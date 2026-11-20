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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonUtils.protocolError;
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonUtils.strip;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.CredentialDaemonCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.internal.SystemSettingUtils;

/**
 * Fetches credentials from the credential daemon: one JSON-RPC line out and one line back, on a new connection for each
 * fetch. The client holds no connection or socket path between fetches.
 *
 * <p>
 * Looks up the socket on every fetch (see {@link CredentialDaemonCredentialsProvider}).
 *
 * <p>
 * Protocol errors in the response say what is wrong but never include the response content, because it can hold secrets.
 */
@SdkInternalApi
public final class CredentialDaemonClient {
    static final int MAX_LINE = 64_000;
    static final String SOCKET_ENVIRONMENT_VARIABLE = "AWS_CREDENTIAL_DAEMON_SOCKET";
    static final String LINK_LOCATION = "$HOME/.aws/credential-daemon.sock";

    private static final String SESSION_CREDENTIALS = "session_credentials";
    private static final JsonNodeParser PARSER = JsonNodeParser.builder().removeErrorLocations(true).build();

    private final CredentialDaemonPlatform platform;
    private final Function<String, Optional<String>> environment;
    private final Duration connectTimeout;
    private final Duration callTimeout;
    private final ChannelOpener channelOpener;
    private final String providerName;

    @SdkTestInternalApi
    CredentialDaemonClient(CredentialDaemonPlatform platform,
                            Function<String, Optional<String>> environment,
                            Duration connectTimeout,
                            Duration callTimeout,
                            ChannelOpener channelOpener,
                            String providerName) {
        this.platform = platform;
        this.environment = environment;
        this.connectTimeout = connectTimeout;
        this.callTimeout = callTimeout;
        this.channelOpener = channelOpener;
        this.providerName = providerName;
    }

    /**
     * Creates a client for this platform. The provider name is put on the credentials the daemon returns.
     */
    public static CredentialDaemonClient create(Duration connectTimeout, Duration callTimeout, String providerName) {
        CredentialDaemonPlatform platform = CredentialDaemonPlatform.current();
        return new CredentialDaemonClient(platform, SystemSettingUtils::resolveEnvironmentVariable,
                                           connectTimeout, callTimeout, platform::openChannel, providerName);
    }

    /**
     * Fetches the credentials named in the request.
     *
     * @throws CredentialDaemonException if the credentials could not be fetched.
     * @throws AbortedException if the thread is interrupted while connecting, writing the request or reading the response.
     * The thread's interrupt status stays set.
     */
    public CredentialDaemonResponse fetch(CredentialDaemonRequest request) {
        platform.ensureSupported();
        Path path = locate();
        byte[] line = exchange(path, request.toLine());
        return parseResponse(line, request.credentialName(), providerName);
    }

    /**
     * Returns the one socket path the client dials: {@code AWS_CREDENTIAL_DAEMON_SOCKET} when it holds an absolute path,
     * and otherwise {@code $HOME/.aws/credential-daemon.sock} when anything is there.
     *
     * @throws CredentialDaemonException with {@link CredentialDaemonException#NOT_RUNNING} when no location applies, or when
     * the link at {@code $HOME/.aws/credential-daemon.sock} points to a target that is missing.
     */
    Path locate() {
        Path fromEnvironment = absolutePath(env(SOCKET_ENVIRONMENT_VARIABLE));
        if (fromEnvironment != null) {
            return fromEnvironment;
        }

        Path home = absolutePath(env("HOME"));
        if (home == null) {
            throw noLocation("HOME is not set to an absolute path");
        }
        Path link = home.resolve(".aws").resolve("credential-daemon.sock");
        // lstat: a symbolic link counts as present even when its target is missing.
        if (Files.notExists(link, LinkOption.NOFOLLOW_LINKS)) {
            throw noLocation("nothing is at " + link);
        }
        if (Files.isSymbolicLink(link) && Files.notExists(link)) {
            throw danglingLink(link);
        }
        return link;
    }

    private byte[] exchange(Path path, byte[] requestLine) {
        SocketAddress address;
        SocketChannel channel;
        try {
            address = platform.address(path);
            channel = channelOpener.open();
        } catch (IOException | InvalidPathException e) {
            throw connectFailure(path, e);
        }

        AtomicBoolean callTimedOut = new AtomicBoolean();
        ScheduledFuture<?> callTimer = TimerHolder.TIMER.schedule(() -> expire(callTimedOut, channel),
                                                                  callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        try {
            connect(path, channel, address, callTimedOut);

            try {
                ByteBuffer out = ByteBuffer.wrap(requestLine);
                while (out.hasRemaining()) {
                    channel.write(out);
                }
                return readLine(channel);
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw interrupted(path, e);
                }
                if (callTimedOut.get()) {
                    throw callTimeoutFailure(path);
                }
                throw protocolError("connection closed before a complete response was received", e);
            }
        } finally {
            callTimer.cancel(false);
            IoUtils.closeQuietly(channel, null);
        }
    }

    /**
     * Connects in non-blocking mode, so that a full listen queue fails at once instead of waiting, and then switches the
     * channel back to blocking mode for the request and the response.
     */
    private void connect(Path path, SocketChannel channel, SocketAddress address, AtomicBoolean callTimedOut) {
        try {
            channel.configureBlocking(false);
            if (!channel.connect(address)) {
                awaitConnect(path, channel);
            }
            channel.configureBlocking(true);
        } catch (IOException | InvalidPathException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw interrupted(path, e);
            }
            if (callTimedOut.get()) {
                throw callTimeoutFailure(path);
            }
            throw connectFailure(path, e);
        }
    }

    /**
     * Waits on a selector for a pending connect to finish within the connect timeout. Closing the selector deregisters the
     * channel, so that the caller can switch it back to blocking mode.
     */
    private void awaitConnect(Path path, SocketChannel channel) throws IOException {
        long connectMillis = connectTimeout.toMillis();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(connectMillis);
        try (Selector selector = channel.provider().openSelector()) {
            channel.register(selector, SelectionKey.OP_CONNECT);
            while (true) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw timeoutFailure(path, "connect timeout (" + connectMillis + " ms)");
                }
                // Round up, because select(0) waits forever.
                selector.select(TimeUnit.NANOSECONDS.toMillis(remainingNanos + TimeUnit.MILLISECONDS.toNanos(1) - 1));
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("interrupted while connecting");
                }
                selector.selectedKeys().clear();
                if (channel.finishConnect()) {
                    return;
                }
            }
        }
    }

    private static byte[] readLine(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_LINE + 1);
        int scanned = 0;
        while (true) {
            if (!buffer.hasRemaining()) {
                throw protocolError("response line exceeds " + MAX_LINE + " bytes", null);
            }
            if (channel.read(buffer) < 0) {
                throw protocolError("connection closed before a complete response line, after " + buffer.position()
                                    + " bytes", null);
            }
            byte[] bytes = buffer.array();
            for (int i = scanned; i < buffer.position(); i++) {
                if (bytes[i] == '\n') {
                    return Arrays.copyOf(bytes, i);
                }
            }
            scanned = buffer.position();
        }
    }

    /**
     * Parses one response line, without its {@code \n}.
     */
    static CredentialDaemonResponse parseResponse(byte[] line, String credentialName, String providerName) {
        JsonNode response = parseObject(line);

        JsonNode version = response.field("jsonrpc").orElse(null);
        if (version == null) {
            throw protocolError("missing jsonrpc", null);
        }
        if (!version.isString() || !"2.0".equals(version.asString())) {
            throw protocolError("jsonrpc is not \"2.0\"", null);
        }

        Optional<JsonNode> result = response.field("result").filter(n -> !n.isNull());
        Optional<JsonNode> error = response.field("error").filter(n -> !n.isNull());
        if (result.isPresent() && error.isPresent()) {
            throw protocolError("the response has both result and error", null);
        }
        if (error.isPresent()) {
            throw daemonError(error.get(), credentialName);
        }
        if (!result.isPresent()) {
            throw protocolError("the response has neither result nor error", null);
        }
        return credentials(result.get(), providerName);
    }

    private static JsonNode parseObject(byte[] line) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                                         .onMalformedInput(CodingErrorAction.REPORT)
                                         .onUnmappableCharacter(CodingErrorAction.REPORT)
                                         .decode(ByteBuffer.wrap(line))
                                         .toString();
        } catch (CharacterCodingException e) {
            throw protocolError("response is not valid UTF-8", null);
        }

        JsonNode node;
        try {
            node = PARSER.parse(text);
        } catch (RuntimeException e) {
            throw protocolError("response is not valid JSON", null);
        }
        if (node == null || node.isNull()) {
            throw protocolError("response is not valid JSON", null);
        }
        if (!node.isObject()) {
            throw protocolError("response is not a JSON object", null);
        }
        return node;
    }

    private static CredentialDaemonException daemonError(JsonNode error, String credentialName) {
        if (!error.isObject()) {
            return protocolError("error is not a JSON object", null);
        }
        String message = error.field("message").filter(JsonNode::isString).map(JsonNode::asString).orElse("");
        String code = error.field("data")
                           .filter(JsonNode::isObject)
                           .flatMap(d -> d.field("code"))
                           .filter(JsonNode::isString)
                           .map(JsonNode::asString)
                           .orElse(null);
        String daemonMessage = " Daemon message: " + strip(message);

        if (CredentialDaemonException.REAUTH_REQUIRED.equals(code)) {
            return CredentialDaemonUtils.error(CredentialDaemonException.REAUTH_REQUIRED,
                                                "The credential daemon reports that credential '" + credentialName
                                                + "' requires you to sign in again." + daemonMessage, null);
        }
        if (CredentialDaemonException.CREDENTIAL_NOT_FOUND.equals(code)) {
            return CredentialDaemonUtils.error(CredentialDaemonException.CREDENTIAL_NOT_FOUND,
                                                "The credential daemon reports that this user has no credential named '"
                                                + credentialName + "'; sign in again." + daemonMessage, null);
        }
        String codeText = code == null ? "" : " with code '" + strip(code) + "'";
        return CredentialDaemonUtils.error(CredentialDaemonException.REQUEST_REJECTED,
                                            "The credential daemon rejected the request for credential '"
                                            + credentialName + "'" + codeText + "." + daemonMessage, null);
    }

    private static CredentialDaemonResponse credentials(JsonNode result, String providerName) {
        if (!result.isObject()) {
            throw protocolError("result is not a JSON object", null);
        }
        JsonNode material = result.field("material").orElse(null);
        if (material == null || !material.isObject()) {
            throw protocolError("result.material is not a JSON object", null);
        }
        JsonNode type = material.field("type").orElse(null);
        if (type == null || !type.isString() || type.asString().isEmpty()) {
            throw protocolError("result.material.type is not a non-empty string", null);
        }
        if (!SESSION_CREDENTIALS.equals(type.asString())) {
            throw CredentialDaemonUtils.error(CredentialDaemonException.UNSUPPORTED_MATERIAL,
                                               "The credential daemon returned credential material of type '"
                                               + strip(type.asString()) + "', which this version of the SDK does not "
                                               + "support. Update the AWS SDK for Java to use this credential.", null);
        }

        String accessKeyId = required(material, "access_key_id", "material.access_key_id");
        String secretAccessKey = required(material, "secret_access_key", "material.secret_access_key");
        String expiresAtText = required(result, "expires_at", "result.expires_at");
        String sessionToken = optional(material, "session_token", "material.session_token");
        String accountId = optional(material, "account_id", "material.account_id");

        Instant expiresAt;
        try {
            expiresAt = CredentialDaemonUtils.parseRfc3339(expiresAtText);
        } catch (IllegalArgumentException e) {
            throw protocolError("expires_at is not an RFC 3339 UTC timestamp", null);
        }

        AwsCredentials credentials;
        if (sessionToken != null) {
            credentials = AwsSessionCredentials.builder()
                                               .accessKeyId(accessKeyId)
                                               .secretAccessKey(secretAccessKey)
                                               .sessionToken(sessionToken)
                                               .expirationTime(expiresAt)
                                               .accountId(accountId)
                                               .providerName(providerName)
                                               .build();
        } else {
            credentials = AwsBasicCredentials.builder()
                                             .accessKeyId(accessKeyId)
                                             .secretAccessKey(secretAccessKey)
                                             .accountId(accountId)
                                             .providerName(providerName)
                                             .build();
        }
        return new CredentialDaemonResponse(credentials, expiresAt);
    }

    private static String required(JsonNode parent, String key, String fieldName) {
        JsonNode node = parent.field(key).orElse(null);
        if (node == null || node.isNull()) {
            throw protocolError("missing " + fieldName, null);
        }
        if (!node.isString()) {
            throw protocolError(fieldName + " is not a string", null);
        }
        if (node.asString().isEmpty()) {
            throw protocolError("missing " + fieldName, null);
        }
        return node.asString();
    }

    private static String optional(JsonNode parent, String key, String fieldName) {
        JsonNode node = parent.field(key).orElse(null);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isString()) {
            throw protocolError(fieldName + " is not a string", null);
        }
        return node.asString().isEmpty() ? null : node.asString();
    }

    private String env(String name) {
        return environment.apply(name).orElse(null);
    }

    private static Path absolutePath(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            Path path = Paths.get(value);
            return path.isAbsolute() ? path : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static void expire(AtomicBoolean callTimedOut, SocketChannel channel) {
        if (callTimedOut.compareAndSet(false, true)) {
            IoUtils.closeQuietly(channel, null);
        }
    }

    private static AbortedException interrupted(Path path, Exception e) {
        return AbortedException.create("The thread was interrupted during the call to the credential daemon at " + path + ".",
                                       e);
    }

    private static CredentialDaemonException connectFailure(Path path, Exception e) {
        return CredentialDaemonUtils.error(CredentialDaemonException.NOT_RUNNING,
                                            "Could not connect to the credential daemon at " + path + ": "
                                            + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                                            + ". " + CredentialDaemonUtils.START_HINT,
                                            e);
    }

    private static CredentialDaemonException noLocation(String reason) {
        return CredentialDaemonUtils.error(CredentialDaemonException.NOT_RUNNING,
                                            "No credential daemon socket was found: " + SOCKET_ENVIRONMENT_VARIABLE
                                            + " is not set to an absolute path, and " + reason + ". Install the credential "
                                            + "daemon or link its socket to " + LINK_LOCATION + ", or set "
                                            + SOCKET_ENVIRONMENT_VARIABLE + ". See " + CredentialDaemonUtils.CREDENTIAL_DAEMON_DOC
                                            + ".",
                                            null);
    }

    private static CredentialDaemonException danglingLink(Path link) {
        String target;
        try {
            target = String.valueOf(Files.readSymbolicLink(link));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            target = "an unreadable target";
        }
        return CredentialDaemonUtils.error(CredentialDaemonException.NOT_RUNNING,
                                            "The credential daemon socket link " + link + " points to " + target
                                            + ", which does not exist. " + CredentialDaemonUtils.START_HINT,
                                            null);
    }

    private CredentialDaemonException callTimeoutFailure(Path path) {
        return timeoutFailure(path, "call timeout (" + callTimeout.toMillis() + " ms)");
    }

    private static CredentialDaemonException timeoutFailure(Path path, String timeout) {
        return CredentialDaemonUtils.error(CredentialDaemonException.NOT_RUNNING,
                                            "The credential daemon at " + path + " did not answer before the " + timeout
                                            + " expired. " + CredentialDaemonUtils.START_HINT,
                                            null);
    }

    /**
     * Opens the channel that one fetch uses.
     */
    @FunctionalInterface
    interface ChannelOpener {
        SocketChannel open() throws IOException;
    }

    private static final class TimerHolder {
        private static final ScheduledThreadPoolExecutor TIMER = createTimer();

        private static ScheduledThreadPoolExecutor createTimer() {
            ScheduledThreadPoolExecutor executor =
                new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().threadNamePrefix("sdk-credential-daemon-timeout")
                                                                             .daemonThreads(true)
                                                                             .build());
            executor.setRemoveOnCancelPolicy(true);
            // The keep-alive time must be set before core threads may time out, because the default of 0 is rejected.
            executor.setKeepAliveTime(60, TimeUnit.SECONDS);
            executor.allowCoreThreadTimeOut(true);
            return executor;
        }
    }
}
