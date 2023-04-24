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
import static software.amazon.awssdk.crt.io.TlsCipherPreference.TLS_CIPHER_PREF_PQ_TLSv1_0_2021_05;
import static software.amazon.awssdk.crt.io.TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.crt.io.TlsCipherPreference;

class AwsCrtConfigurationUtilsTest {

    @ParameterizedTest
    @MethodSource("cipherPreferences")
    void resolveCipherPreference_pqNotSupported_shouldFallbackToSystemDefault(Boolean preferPqTls,
                                                                              TlsCipherPreference tlsCipherPreference) {
        Assumptions.assumeFalse(TLS_CIPHER_PREF_PQ_TLSv1_0_2021_05.isSupported());
        assertThat(AwsCrtConfigurationUtils.resolveCipherPreference(preferPqTls)).isEqualTo(tlsCipherPreference);
    }

    @Test
    void resolveCipherPreference_pqSupported_shouldHonor() {
        Assumptions.assumeTrue(TLS_CIPHER_PREF_PQ_TLSv1_0_2021_05.isSupported());
        assertThat(AwsCrtConfigurationUtils.resolveCipherPreference(true)).isEqualTo(TLS_CIPHER_PREF_PQ_TLSv1_0_2021_05);
    }

    private static Stream<Arguments> cipherPreferences() {
        return Stream.of(
            Arguments.of(null, TLS_CIPHER_SYSTEM_DEFAULT),
            Arguments.of(false, TLS_CIPHER_SYSTEM_DEFAULT),
            Arguments.of(true, TLS_CIPHER_SYSTEM_DEFAULT)
        );
    }

}
