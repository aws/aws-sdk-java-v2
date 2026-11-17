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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;

/**
 * The socket lookup cases of the credential daemon provider SEP. Each socket role ({@code env}, {@code env-file},
 * {@code link}, {@code link-stale}, {@code link-dangling}, {@code home-socket}) maps to a temporary path, with {@code HOME}
 * set to a temporary folder. The lookup is the same on Linux and macOS, so every case runs on both.
 */
class CredentialDaemonClientLocateTest {
    private static final CredentialDaemonPlatform LINUX =
        new CredentialDaemonPlatform("Linux", "OpenJDK 64-Bit Server VM", "Amazon.com Inc.", "java.net.UnixDomainSocketAddress");
    private static final CredentialDaemonPlatform MACOS =
        new CredentialDaemonPlatform("Mac OS X", "OpenJDK 64-Bit Server VM", "Amazon.com Inc.", "java.net.UnixDomainSocketAddress");
    private static final String INSTALL_HINT = "Install the credential daemon or link its socket to "
                                               + "$HOME/.aws/credential-daemon.sock, or set AWS_CREDENTIAL_DAEMON_SOCKET";

    private final Map<String, String> env = new HashMap<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private Path home;
    private Path location;

    @BeforeEach
    void setup() throws IOException {
        assumeTrue(FakeCredentialDaemon.isSupported());
        home = FakeCredentialDaemon.newSocketDirectory();
        location = home.resolve(".aws/credential-daemon.sock");
        Files.createDirectories(location.getParent());
    }

    @AfterEach
    void teardown() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
        if (home != null) {
            try (Stream<Path> files = Files.walk(home)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private CredentialDaemonClient client(CredentialDaemonPlatform platform) {
        return new CredentialDaemonClient(platform, k -> Optional.ofNullable(env.get(k)), Duration.ofSeconds(1),
                                           Duration.ofSeconds(5), platform::openChannel, "Aq");
    }

    private void assertLocates(Path expected) {
        assertThat(client(LINUX).locate()).isEqualTo(expected);
        assertThat(client(MACOS).locate()).isEqualTo(expected);
    }

    private void assertNoLocation(String reason) {
        for (CredentialDaemonPlatform platform : new CredentialDaemonPlatform[] {LINUX, MACOS}) {
            assertThatThrownBy(() -> client(platform).locate())
                .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                    assertThat(e.getMessage()).contains(reason).contains(INSTALL_HINT).contains("<CREDENTIAL_DAEMON_DOC>");
                });
        }
    }

    private Path bindSocket(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        closeables.add(FakeCredentialDaemon.listeningSocket(path));
        return path;
    }

    @Test
    void locate_env_returnsItBeforeTheLink() throws IOException {
        env.put("AWS_CREDENTIAL_DAEMON_SOCKET", "/some/where/d.sock");
        env.put("HOME", home.toString());
        Files.createSymbolicLink(location, bindSocket(home.resolve("real.sock")));

        assertLocates(Paths.get("/some/where/d.sock"));
    }

    @Test
    void locate_envFile_returnsItWithoutCheckingWhatIsThere() throws IOException {
        Path file = Files.createFile(home.resolve("env-file.sock"));
        env.put("AWS_CREDENTIAL_DAEMON_SOCKET", file.toString());

        assertLocates(file);
    }

    @Test
    void locate_withEmptyOrRelativeEnvironmentVariable_skipsIt() throws IOException {
        env.put("HOME", home.toString());
        Path target = bindSocket(home.resolve("real.sock"));
        Files.createSymbolicLink(location, target);

        env.put("AWS_CREDENTIAL_DAEMON_SOCKET", "");
        assertLocates(location);

        env.put("AWS_CREDENTIAL_DAEMON_SOCKET", "relative/d.sock");
        assertLocates(location);
    }

    @Test
    void locate_link_returnsTheLinkNotItsTarget() throws IOException {
        env.put("HOME", home.toString());
        Path target = bindSocket(home.resolve("daemon/real.sock"));
        Files.createSymbolicLink(location, target);

        assertLocates(location);
    }

    @Test
    void locate_linkIsASocket_returnsIt() throws IOException {
        env.put("HOME", home.toString());
        bindSocket(location);

        assertLocates(location);
    }

    @Test
    void locate_linkStale_returnsTheLink() throws IOException {
        env.put("HOME", home.toString());
        Path stale = FakeCredentialDaemon.staleSocket(home);
        Files.createSymbolicLink(location, stale);

        assertLocates(location);
    }

    @Test
    void locate_withRegularFileAtLocation_returnsIt() throws IOException {
        env.put("HOME", home.toString());
        Files.createFile(location);

        assertLocates(location);
    }

    @Test
    void locate_linkDangling_raisesNotRunningNamingTheLinkAndItsTarget() throws IOException {
        env.put("HOME", home.toString());
        Path missing = home.resolve("gone/missing-daemon.sock");
        Files.createSymbolicLink(location, missing);

        for (CredentialDaemonPlatform platform : new CredentialDaemonPlatform[] {LINUX, MACOS}) {
            assertThatThrownBy(() -> client(platform).locate())
                .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.NOT_RUNNING);
                    assertThat(e.getMessage()).contains(location.toString()).contains(missing.toString());
                });
        }
    }

    @Test
    void locate_noLink_raisesNotRunning() {
        env.put("HOME", home.toString());

        assertNoLocation("nothing is at " + location);
    }

    @Test
    void locate_noLinkHomeUnset_raisesNotRunning() throws IOException {
        bindSocket(location);

        assertNoLocation("HOME is not set to an absolute path");

        env.put("HOME", "");
        assertNoLocation("HOME is not set to an absolute path");
    }

    @Test
    void locate_noLinkHomeRelative_raisesNotRunning() throws IOException {
        bindSocket(location);
        env.put("HOME", "relative/home");

        assertNoLocation("HOME is not set to an absolute path");
    }
}
