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

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.CredentialDaemonException;

class CredentialDaemonPlatformTest {
    private static final String ADDRESS_CLASS = "java.net.UnixDomainSocketAddress";
    private static final String DOC = "https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-credential-daemon.html";

    static Stream<Arguments> unsupportedPlatforms() {
        return Stream.of(
            Arguments.of("Windows", new CredentialDaemonPlatform("Windows 11", "OpenJDK", "Amazon.com Inc.", ADDRESS_CLASS),
                         "Windows 11"),
            Arguments.of("FreeBSD", new CredentialDaemonPlatform("FreeBSD", "OpenJDK", "Amazon.com Inc.", ADDRESS_CLASS),
                         "FreeBSD"),
            Arguments.of("Dalvik", new CredentialDaemonPlatform("Linux", "Dalvik", "The Android Project", ADDRESS_CLASS),
                         "Android"),
            Arguments.of("Java before 16", new CredentialDaemonPlatform("Linux", "OpenJDK", "Amazon.com Inc.",
                                                                         "java.net.NoSuchUnixDomainSocketAddress"),
                         "Unix domain socket support"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedPlatforms")
    void openChannel_onUnsupportedPlatform_raisesUnsupportedPlatform(String name, CredentialDaemonPlatform platform,
                                                                     String reason) {
        assertThatThrownBy(platform::openChannel)
            .isInstanceOfSatisfying(CredentialDaemonException.class, e -> {
                assertThat(e.errorCode()).isEqualTo(CredentialDaemonException.UNSUPPORTED_PLATFORM);
                assertThat(e.getMessage()).contains("Linux or macOS and Java 16 or later")
                                          .contains(reason)
                                          .contains(DOC);
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
}
