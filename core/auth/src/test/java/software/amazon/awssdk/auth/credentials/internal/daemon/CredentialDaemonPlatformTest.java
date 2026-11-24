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
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatform.JDK_ADDRESS_CLASS;
import static software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonPlatform.JUNIXSOCKET_CLASS;

import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.newsclub.net.unix.AFSocketChannel;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;
import software.amazon.awssdk.auth.credentials.FakeCredentialDaemon;

class CredentialDaemonPlatformTest {
    static final String MISSING_ADDRESS_CLASS = "java.net.NoSuchUnixDomainSocketAddress";
    static final String MISSING_JUNIXSOCKET_CLASS = "org.newsclub.net.unix.NoSuchSocketChannel";
    private static final String DOC = "https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-credential-daemon.html";
    private static final String REQUIREMENT = "need Linux or macOS, and either Java 16 or later or the junixsocket library";

    static Stream<Arguments> unsupportedPlatforms() {
        return Stream.of(
            Arguments.of("Windows", platform("Windows 11", "OpenJDK", "Amazon.com Inc.", JDK_ADDRESS_CLASS, JUNIXSOCKET_CLASS),
                         "the operating system 'Windows 11' is not supported"),
            Arguments.of("FreeBSD", platform("FreeBSD", "OpenJDK", "Amazon.com Inc.", JDK_ADDRESS_CLASS, JUNIXSOCKET_CLASS),
                         "the operating system 'FreeBSD' is not supported"),
            Arguments.of("Dalvik", platform("Linux", "Dalvik", "The Android Project", JDK_ADDRESS_CLASS, JUNIXSOCKET_CLASS),
                         "Android is not supported"),
            Arguments.of("Java before 16 without junixsocket",
                         platform("Linux", "OpenJDK", "Amazon.com Inc.", MISSING_ADDRESS_CLASS, MISSING_JUNIXSOCKET_CLASS),
                         "this Java runtime has no Unix domain socket support, and junixsocket is not on the class path. "
                         + "To use the credential daemon on Java 8 to 15, add a dependency on "
                         + "com.kohlschutter.junixsocket:junixsocket-core (type pom)"));
    }

    private static CredentialDaemonPlatform platform(String osName, String vmName, String vendor, String addressClass,
                                                     String junixsocketClass) {
        return new CredentialDaemonPlatform(osName, vmName, vendor, addressClass, junixsocketClass);
    }

    private static CredentialDaemonPlatform thisOperatingSystem(String addressClass, String junixsocketClass) {
        return platform(System.getProperty("os.name"), System.getProperty("java.vm.name"), System.getProperty("java.vendor"),
                        addressClass, junixsocketClass);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedPlatforms")
    void openChannel_onUnsupportedPlatform_raisesUnsupportedPlatform(String name, CredentialDaemonPlatform platform,
                                                                     String reason) {
        assertThatThrownBy(platform::openChannel)
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.UNSUPPORTED_PLATFORM);
                assertThat(e.getMessage()).contains(REQUIREMENT)
                                          .contains("but " + reason + ". See " + DOC + ".");
            });
        assertThatThrownBy(() -> platform.address(Paths.get("/tmp/x.sock")))
            .isInstanceOf(CredentialDaemonException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedPlatforms")
    void fetch_onUnsupportedPlatform_raisesUnsupportedPlatform(String name, CredentialDaemonPlatform platform,
                                                               String reason) {
        CredentialDaemonClient client = new CredentialDaemonClient(platform, k -> Optional.of("/tmp/never-dialled.sock"),
                                                                     Duration.ofSeconds(1), Duration.ofSeconds(5),
                                                                     platform::openChannel, "Aq");

        assertThatThrownBy(() -> client.fetch(new CredentialDaemonRequest("dev", null, null)))
            .isInstanceOfSatisfying(CredentialDaemonException.class,
                                    e -> assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.UNSUPPORTED_PLATFORM));
    }

    @Test
    void openChannel_beforeJava16WithJunixsocket_usesJunixsocket() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported());
        CredentialDaemonPlatform platform = thisOperatingSystem(MISSING_ADDRESS_CLASS, JUNIXSOCKET_CLASS);

        platform.ensureSupported();
        try (SocketChannel channel = platform.openChannel()) {
            assertThat(channel).isInstanceOf(AFUNIXSocketChannel.class);
            assertThat(channel.isBlocking()).isTrue();
        }
        assertThat(platform.address(Paths.get("/tmp/x.sock"))).isInstanceOf(AFUNIXSocketAddress.class);
    }

    @Test
    void openChannel_onJava16WithJunixsocket_usesTheJavaRuntime() throws Exception {
        assumeTrue(FakeCredentialDaemon.isSupported() && FakeCredentialDaemon.hasJdkUnixDomainSockets());
        CredentialDaemonPlatform platform = thisOperatingSystem(JDK_ADDRESS_CLASS, JUNIXSOCKET_CLASS);

        try (SocketChannel channel = platform.openChannel()) {
            assertThat(channel).isNotInstanceOf(AFSocketChannel.class);
        }
        assertThat(platform.address(Paths.get("/tmp/x.sock")).getClass().getName()).isEqualTo(JDK_ADDRESS_CLASS);
    }
}
