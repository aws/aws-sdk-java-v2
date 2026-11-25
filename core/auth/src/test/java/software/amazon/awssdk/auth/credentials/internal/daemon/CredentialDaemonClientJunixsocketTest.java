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
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatformTest.MISSING_ADDRESS_CLASS;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;
import software.amazon.awssdk.core.exception.AbortedException;

/**
 * Runs the client over junixsocket, as it does on Java 8 to 15, on any Java runtime the tests run on.
 */
class CredentialDaemonClientJunixsocketTest {
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private CredentialDaemonPlatform platform;
    private FakeCredentialDaemon daemon;

    @BeforeEach
    void setup() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported());
        platform = new CredentialDaemonPlatform(System.getProperty("os.name"), System.getProperty("java.vm.name"),
                                                System.getProperty("java.vendor"), MISSING_ADDRESS_CLASS,
                                                CredentialDaemonPlatform.JUNIXSOCKET_CLASS);
        daemon = FakeCredentialDaemon.replying(
            FakeCredentialDaemon.successReply("ASIADAEMON", Instant.now().plus(Duration.ofMinutes(15))));
        closeables.add(daemon);
    }

    @AfterEach
    void teardown() throws Exception {
        Thread.interrupted();
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
    }

    private CredentialDaemonClient client(Path socket, Duration callTimeout) {
        return new CredentialDaemonClient(platform,
                                          k -> "AWS_CREDENTIAL_DAEMON_SOCKET".equals(k) ? Optional.of(socket.toString())
                                                                                        : Optional.empty(),
                                          Duration.ofSeconds(1), callTimeout, platform::openChannel, "Aq");
    }

    private static CredentialDaemonRequest request() {
        return new CredentialDaemonRequest("dev", null, null);
    }

    @Test
    void fetch_overJunixsocket_returnsTheDaemonsCredentials() {
        CredentialDaemonResponse response = client(daemon.socketPath(), Duration.ofSeconds(5)).fetch(request());

        assertThat(response.credentials().accessKeyId()).isEqualTo("ASIADAEMON");
        assertThat(daemon.requests())
            .containsExactly("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"credential/get\",\"params\":{\"credential\":\"dev\"}}");
    }

    @Test
    void fetch_overJunixsocketWhenNothingIsAtThePath_raisesNotRunning() {
        Path missing = daemon.directory().resolve("missing.sock");

        assertThatThrownBy(() -> client(missing, Duration.ofSeconds(5)).fetch(request()))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("Could not connect to the credential daemon at " + missing);
            });
    }

    @Test
    void fetch_overJunixsocketWhenTheSocketIsStale_raisesNotRunning() throws Exception {
        Path stale = FakeCredentialDaemon.staleSocket(daemon.directory());
        closeables.add(0, () -> Files.deleteIfExists(stale));
        assertThat(Files.exists(stale, LinkOption.NOFOLLOW_LINKS)).isTrue();

        assertThatThrownBy(() -> client(stale, Duration.ofSeconds(5)).fetch(request()))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("Could not connect to the credential daemon at " + stale)
                                          .contains("Connection refused");
            });
    }

    @Test
    void fetch_overJunixsocketWhenTheDaemonNeverAnswers_raisesNotRunningForCallTimeout() {
        daemon.responder(r -> FakeCredentialDaemon.NO_REPLY);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client(daemon.socketPath(), Duration.ofMillis(300)).fetch(request()))
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                assertThat(e.getMessage()).contains("call timeout (300 ms)");
            });
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
    }

    @Test
    void fetch_overJunixsocketWhenInterruptedWhileWaitingForTheReply_raisesAbortedException() throws Exception {
        daemon.responder(r -> FakeCredentialDaemon.NO_REPLY);
        Thread caller = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                return;
            }
            caller.interrupt();
        });

        long start = System.nanoTime();
        interrupter.start();
        try {
            assertThatThrownBy(() -> client(daemon.socketPath(), Duration.ofSeconds(5)).fetch(request()))
                .isInstanceOf(AbortedException.class)
                .hasMessageContaining("interrupted")
                .hasMessageContaining(daemon.socketPath().toString());
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            interrupter.interrupt();
            interrupter.join();
        }
    }
}
