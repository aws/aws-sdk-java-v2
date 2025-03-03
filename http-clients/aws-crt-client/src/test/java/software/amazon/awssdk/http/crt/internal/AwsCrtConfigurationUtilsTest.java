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

package software.amazon.awssdk.http.crt.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.crt.io.TlsCipherPreference.TLS_CIPHER_PQ_DEFAULT;
import static software.amazon.awssdk.crt.io.TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;

class AwsCrtConfigurationUtilsTest {

    @AfterAll
    public static void tearDown() {
        CrtResource.waitForNoResources();
    }

    @ParameterizedTest
    @MethodSource("cipherPreferences")
    void resolveCipherPreference_pqNotSupported_shouldFallbackToSystemDefault(Boolean preferPqTls,
                                                                              TlsCipherPreference tlsCipherPreference) {
        Assumptions.assumeFalse(TLS_CIPHER_PQ_DEFAULT.isSupported());
        assertThat(AwsCrtConfigurationUtils.resolveCipherPreference(preferPqTls)).isEqualTo(tlsCipherPreference);
    }

    @Test
    void resolveCipherPreference_pqSupported_shouldHonor() {
        Assumptions.assumeTrue(TLS_CIPHER_PQ_DEFAULT.isSupported());
        assertThat(AwsCrtConfigurationUtils.resolveCipherPreference(true)).isEqualTo(TLS_CIPHER_PQ_DEFAULT);
    }

    private static Stream<Arguments> cipherPreferences() {
        return Stream.of(
            Arguments.of(null, TLS_CIPHER_SYSTEM_DEFAULT),
            Arguments.of(false, TLS_CIPHER_SYSTEM_DEFAULT),
            Arguments.of(true, TLS_CIPHER_SYSTEM_DEFAULT)
        );
    }

    @ParameterizedTest
    @MethodSource("tcpKeepAliveConfiguration")
    void tcpKeepAliveConfiguration(TcpKeepAliveConfiguration tcpKeepAliveConfiguration, Duration connectionTimeout, SocketOptions expected) {
        try (SocketOptions socketOptions = AwsCrtConfigurationUtils.buildSocketOptions(tcpKeepAliveConfiguration,
            connectionTimeout)) {
            assertThat(socketOptions)
                .satisfies(options -> {
                    assertThat(options.connectTimeoutMs).isEqualTo(expected.connectTimeoutMs);
                    assertThat(options.keepAlive).isEqualTo(expected.keepAlive);
                    assertThat(options.keepAliveIntervalSecs).isEqualTo(expected.keepAliveIntervalSecs);
                    assertThat(options.keepAliveTimeoutSecs).isEqualTo(expected.keepAliveTimeoutSecs);
                });
        }
    }

    private static Stream<Arguments> tcpKeepAliveConfiguration() {
        Duration duration1Minute = Duration.ofMinutes(1);

        SocketOptions expectedConnectTimeOutOnly = new SocketOptions();
        expectedConnectTimeOutOnly.connectTimeoutMs = (int) duration1Minute.toMillis();

        SocketOptions expectedKeepAliveOnly = new SocketOptions();
        expectedKeepAliveOnly.keepAlive = true;
        expectedKeepAliveOnly.keepAliveIntervalSecs = (int)duration1Minute.getSeconds();
        expectedKeepAliveOnly.keepAliveTimeoutSecs = (int)duration1Minute.getSeconds();

        SocketOptions expectedAll = new SocketOptions();
        expectedAll.connectTimeoutMs = (int) duration1Minute.toMillis();
        expectedAll.keepAlive = true;
        expectedAll.keepAliveIntervalSecs = (int)duration1Minute.getSeconds();
        expectedAll.keepAliveTimeoutSecs = (int)duration1Minute.getSeconds();

        return Stream.of(
            Arguments.of(null, null, new SocketOptions()),
            Arguments.of(null, duration1Minute, expectedConnectTimeOutOnly),
            Arguments.of(
                TcpKeepAliveConfiguration.builder().keepAliveInterval(duration1Minute).keepAliveTimeout(duration1Minute).build(),
                null,
                expectedKeepAliveOnly
            ),
            Arguments.of(
                TcpKeepAliveConfiguration.builder().keepAliveInterval(duration1Minute).keepAliveTimeout(duration1Minute).build(),
                duration1Minute,
                expectedAll
            )
        );
    }

}
