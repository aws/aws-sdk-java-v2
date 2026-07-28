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

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.AttributeMap;


public class AwsCrtHttpClientTest extends AwsCrtHttpClientTestBase {

    @Test
    public void negativeConnectionAcquisitionTimeout_shouldFail() {
        assertThatThrownBy(() -> {
            SdkHttpClient client = AwsCrtHttpClient.builder()
                    .connectionAcquisitionTimeout(Duration.ofSeconds(-1))
                    .build();
            client.close();
        }).hasMessage("connectionAcquisitionTimeout must be positive");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTlsNegotiationTimeouts")
    void tlsNegotiationTimeout_invalidDuration_shouldThrowException(String description, Duration input,
                                                                    String expectedMessageFragment) {
        assertThatThrownBy(() -> AwsCrtHttpClient.builder().tlsNegotiationTimeout(input).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessageFragment);
    }

    @Test
    void syncBuilder_buildWithDefaults_serviceDefaultsLacksTlsNegotiationTimeout_resolvesToCrtDefault10s() {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().buildWithDefaults(AttributeMap.empty())) {
            assertThat(((AwsCrtHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(CRT_DEFAULT);
        }
    }


    @ParameterizedTest(name = "[sync] {0}")
    @MethodSource("resolutionMatrix")
    void syncBuilder_resolvedTlsNegotiationTimeout_matchesPathBPrecedence(String description, Duration customer,
                                                                          Duration serviceDefault, Duration expected) {
        AwsCrtHttpClient.Builder builder = AwsCrtHttpClient.builder();
        if (customer != null) {
            builder.tlsNegotiationTimeout(customer);
        }

        try (SdkHttpClient client = buildSync(builder, serviceDefault)) {
            assertThat(((AwsCrtHttpClient) client).resolvedTlsNegotiationTimeout()).isEqualTo(expected);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("minTlsVersionInputs")
    void syncBuilder_minTlsVersion_buildSucceeds(String description, Consumer<AwsCrtHttpClient.Builder> configure) {
        assertThatCode(() -> {
            AwsCrtHttpClient.Builder builder = AwsCrtHttpClient.builder();
            configure.accept(builder);
            builder.build().close();
        }).doesNotThrowAnyException();
    }

    @Test
    void syncBuilder_postQuantumTrueWithMinTls13_buildSucceeds() {
        assertThatCode(() -> AwsCrtHttpClient.builder()
                                             .postQuantumTlsEnabled(true)
                                             .minTlsVersion(TlsVersion.TLS_1_3)
                                             .build()
                                             .close())
            .doesNotThrowAnyException();
    }

    // CRT enforces mutual exclusivity between a non-default cipher preference and a non-default minimum TLS version
    // (aws-crt-java TlsContextOptions#getNativeHandle throws IllegalStateException). This surfaces only on platforms
    // where TLS_CIPHER_NON_PQ_DEFAULT is supported; elsewhere resolveCipherPreference(false) falls back to
    // TLS_CIPHER_SYSTEM_DEFAULT (see AwsCrtConfigurationUtils) and CRT accepts the combination.
    @Test
    void syncBuilder_postQuantumFalseWithMinTls13_failsWhenCrtEnforcesMutualExclusivity() {
        assumeTrue(TlsCipherPreference.TLS_CIPHER_NON_PQ_DEFAULT.isSupported());
        assertThatThrownBy(() -> AwsCrtHttpClient.builder()
                                                 .postQuantumTlsEnabled(false)
                                                 .minTlsVersion(TlsVersion.TLS_1_3)
                                                 .build())
            .isInstanceOf(IllegalStateException.class);
    }

    static Stream<Arguments> minTlsVersionInputs() {
        return Stream.of(
            Arguments.of("unset -> build succeeds",
                         (Consumer<AwsCrtHttpClient.Builder>) b -> { }),
            Arguments.of("SYSTEM_DEFAULT -> build succeeds",
                         (Consumer<AwsCrtHttpClient.Builder>) b -> b.minTlsVersion(TlsVersion.SYSTEM_DEFAULT)),
            Arguments.of("TLS_1_3 -> build succeeds",
                         (Consumer<AwsCrtHttpClient.Builder>) b -> b.minTlsVersion(TlsVersion.TLS_1_3)),
            Arguments.of("TLS_1_3 then null -> build succeeds",
                         (Consumer<AwsCrtHttpClient.Builder>) b -> b.minTlsVersion(TlsVersion.TLS_1_3).minTlsVersion(null))
        );
    }

    private static SdkHttpClient buildSync(AwsCrtHttpClient.Builder builder, Duration serviceDefault) {
        return serviceDefault == null
               ? builder.build()
               : builder.buildWithDefaults(serviceDefaultsMap(serviceDefault));
    }
}
