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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;

class CredentialDaemonClientTest {
    private static final String SOCKET = "/tmp/never-connects.sock";

    private CredentialDaemonPlatform platform;
    private Function<String, Optional<String>> environment;

    @BeforeEach
    void setup() {
        assumeTrue(FakeCredentialDaemon.isSupported());
        platform = CredentialDaemonPlatform.current();
        environment = k -> "AWS_CREDENTIAL_DAEMON_SOCKET".equals(k) ? Optional.of(SOCKET) : Optional.empty();
    }

    private CredentialDaemonClient client(Duration connectTimeout, Duration callTimeout,
                                           CredentialDaemonClient.ChannelOpener channelOpener) {
        return new CredentialDaemonClient(platform, environment, connectTimeout, callTimeout, channelOpener, "Aq");
    }

    @Test
    void fetch_whenConnectStaysPending_raisesNotRunningForConnectTimeout() {
        HangingSocketChannel channel = new HangingSocketChannel();
        CredentialDaemonClient client = client(Duration.ofMillis(200), Duration.ofSeconds(5), () -> channel);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.fetch(new CredentialDaemonRequest("dev", null, null)))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("connect timeout (200 ms)").contains(SOCKET);
            });
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(200)).isLessThan(Duration.ofSeconds(4));
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void fetch_whenConnectFails_raisesNotRunningAtOnce() {
        HangingSocketChannel channel = new HangingSocketChannel();
        channel.connectFailure = new IOException("Resource temporarily unavailable");
        CredentialDaemonClient client = client(Duration.ofSeconds(5), Duration.ofSeconds(5), () -> channel);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.fetch(new CredentialDaemonRequest("dev", null, null)))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("Resource temporarily unavailable").contains(SOCKET)
                                          .doesNotContain("timeout");
            });
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
    }

    @Test
    void fetch_whenWriteFails_raisesProtocolErrorWithTheCause() {
        HangingSocketChannel channel = new HangingSocketChannel();
        channel.connectsAtOnce = true;
        CredentialDaemonClient client = client(Duration.ofSeconds(5), Duration.ofSeconds(5), () -> channel);

        assertThatThrownBy(() -> client.fetch(new CredentialDaemonRequest("dev", null, null)))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.PROTOCOL_ERROR);
                assertThat(e.getMessage()).contains("connection closed before a complete response was received");
                assertThat(e.getCause()).isInstanceOf(IOException.class).hasMessage("Broken pipe");
            });
    }

    @Test
    void fetch_whenNoLocationApplies_raisesNotRunningWithoutDialing() {
        environment = k -> Optional.empty();
        CredentialDaemonClient client = client(Duration.ofSeconds(5), Duration.ofSeconds(5), () -> {
            throw new AssertionError("The client must not dial when no location applies");
        });

        assertThatThrownBy(() -> client.fetch(new CredentialDaemonRequest("dev", null, null)))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("link its socket to $HOME/.aws/credential-daemon.sock")
                                          .contains("set AWS_CREDENTIAL_DAEMON_SOCKET");
            });
    }

    /**
     * A channel whose connect stays pending forever, fails with the given exception, or finishes at once. Its writes and
     * reads always fail.
     */
    private static final class HangingSocketChannel extends SocketChannel {
        private volatile IOException connectFailure;
        private volatile boolean connectsAtOnce;

        HangingSocketChannel() {
            super(new StubSelectorProvider());
        }

        @Override
        public boolean connect(SocketAddress remote) throws IOException {
            ensureOpen();
            if (connectFailure != null) {
                throw connectFailure;
            }
            return connectsAtOnce;
        }

        @Override
        public boolean finishConnect() throws IOException {
            ensureOpen();
            return false;
        }

        private void ensureOpen() throws IOException {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isConnectionPending() {
            return isOpen();
        }

        @Override
        public SocketChannel bind(SocketAddress local) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> SocketChannel setOption(SocketOption<T> name, T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getOption(SocketOption<T> name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return Collections.emptySet();
        }

        @Override
        public SocketChannel shutdownInput() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketChannel shutdownOutput() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket socket() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new IOException("not connected");
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            throw new IOException("not connected");
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new IOException("Broken pipe");
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new IOException("not connected");
        }

        @Override
        protected void implCloseSelectableChannel() {
        }

        @Override
        protected void implConfigureBlocking(boolean block) {
        }
    }

    /**
     * A provider whose selectors never report a channel as ready, so that a select waits for its whole timeout.
     */
    private static final class StubSelectorProvider extends SelectorProvider {
        @Override
        public AbstractSelector openSelector() {
            return new StubSelector(this);
        }

        @Override
        public DatagramChannel openDatagramChannel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DatagramChannel openDatagramChannel(ProtocolFamily family) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pipe openPipe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServerSocketChannel openServerSocketChannel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketChannel openSocketChannel() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubSelector extends AbstractSelector {
        private final Set<SelectionKey> keys = new HashSet<>();
        private final Set<SelectionKey> selectedKeys = new HashSet<>();
        private final Object lock = new Object();

        StubSelector(SelectorProvider provider) {
            super(provider);
        }

        @Override
        protected void implCloseSelector() {
            for (SelectionKey key : keys) {
                key.cancel();
                deregister((AbstractSelectionKey) key);
            }
            keys.clear();
        }

        @Override
        protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
            SelectionKey key = new StubSelectionKey(ch, this, ops);
            key.attach(att);
            keys.add(key);
            return key;
        }

        @Override
        public Set<SelectionKey> keys() {
            return keys;
        }

        @Override
        public Set<SelectionKey> selectedKeys() {
            return selectedKeys;
        }

        @Override
        public int selectNow() {
            return 0;
        }

        @Override
        public int select(long timeout) throws IOException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
            synchronized (lock) {
                long remaining;
                while ((remaining = deadline - System.nanoTime()) > 0) {
                    try {
                        TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return 0;
                    }
                }
            }
            return 0;
        }

        @Override
        public int select() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Selector wakeup() {
            return this;
        }
    }

    private static final class StubSelectionKey extends AbstractSelectionKey {
        private final SelectableChannel channel;
        private final Selector selector;
        private volatile int interestOps;

        StubSelectionKey(SelectableChannel channel, Selector selector, int interestOps) {
            this.channel = channel;
            this.selector = selector;
            this.interestOps = interestOps;
        }

        @Override
        public SelectableChannel channel() {
            return channel;
        }

        @Override
        public Selector selector() {
            return selector;
        }

        @Override
        public int interestOps() {
            return interestOps;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            this.interestOps = ops;
            return this;
        }

        @Override
        public int readyOps() {
            return 0;
        }
    }
}
