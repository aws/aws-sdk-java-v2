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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;

public class BasicProxyAuthGeneratorTest {
    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";

    private final BasicProxyAuthGenerator authGenerator = new BasicProxyAuthGenerator(USERNAME, PASSWORD);

    @ParameterizedTest(name = "username = {0}, password = {1}, expected error = {2}")
    @MethodSource("invalidCtorParams")
    void ctor_paramsInvalid_throws(String username, String password, String errorMessage) {
        assertThatThrownBy(() -> new BasicProxyAuthGenerator(username, password))
            .hasMessageContaining(errorMessage);
    }

    @Test
    void scheme_returnsCorrectValue() {
        assertThat(authGenerator.scheme()).isEqualTo(ProxyAuthScheme.BASIC);
    }

    @Test
    void generateAuthParams_generatedCorrectly() {
        String expected = Base64.getEncoder()
                                .encodeToString(String.format("%s:%s", USERNAME, PASSWORD)
                                                      .getBytes(StandardCharsets.UTF_8));

        assertThat(authGenerator.generateAuthParams(URI.create("http://amazon.com"))).isEqualTo(expected);
    }

    private static Stream<Arguments> invalidCtorParams() {
        return Stream.of(
            Arguments.of(null, null, "username"),
            Arguments.of("", "", "username"),
            Arguments.of(null, PASSWORD, "username"),
            Arguments.of("", PASSWORD, "username"),
            Arguments.of(USERNAME, null, "password"),
            Arguments.of(USERNAME, "", "password")

        );
    }
}
