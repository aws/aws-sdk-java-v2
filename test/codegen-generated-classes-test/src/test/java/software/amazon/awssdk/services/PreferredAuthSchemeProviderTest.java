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

package software.amazon.awssdk.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeParams;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeProvider;

public class PreferredAuthSchemeProviderTest {

    private static final String OPERATION_SIGV4A_ONLY = "multiAuthWithOnlySigv4a";
    private static final String OPERATION_SIGV4A_AND_SIGV4 = "multiAuthWithOnlySigv4aAndSigv4";

    private static final String SIGV4 = "aws.auth#sigv4";
    private static final String SIGV4A = "aws.auth#sigv4a";
    private static final String BEARER = "aws.auth#bearer";
    private static final String ANONYMOUS = "aws.auth#noauth";

    @ParameterizedTest(name = "{3}")
    @MethodSource("authSchemeTestCases")
    void testAuthSchemePreference(List<String> preferredAuthSchemes, String operation, String expectedFirstScheme, String testName) {
        MultiauthAuthSchemeProvider provider = MultiauthAuthSchemeProvider
            .builder()
            .withPreferredAuthSchemes(preferredAuthSchemes)
            .build();

        MultiauthAuthSchemeParams params = MultiauthAuthSchemeParams
            .builder()
            .region(Region.US_WEST_2)
            .operation(operation)
            .build();

        List<AuthSchemeOption> authSchemes = provider.resolveAuthScheme(params);

        Assertions.assertFalse(authSchemes.isEmpty());
        Assertions.assertEquals(expectedFirstScheme, authSchemes.get(0).schemeId());
    }

    static Stream<Arguments> authSchemeTestCases() {
        return Stream.of(
            Arguments.of(
                Arrays.asList(BEARER, ANONYMOUS),
                OPERATION_SIGV4A_AND_SIGV4,
                SIGV4A,
                "Unsupported auth schemes only"
            ),

            Arguments.of(
                Arrays.asList(BEARER, SIGV4, ANONYMOUS),
                OPERATION_SIGV4A_AND_SIGV4,
                SIGV4,
                "Mix of supported and unsupported schemes"
            ),

            Arguments.of(
                Arrays.asList(SIGV4, SIGV4A),
                OPERATION_SIGV4A_AND_SIGV4,
                SIGV4,
                "All supported schemes in reverse order"
            ),

            Arguments.of(
                Arrays.asList(SIGV4, SIGV4A),
                OPERATION_SIGV4A_ONLY,
                SIGV4A,
                "Operation with only one supported scheme"
            ),

            Arguments.of(
                Collections.emptyList(),
                OPERATION_SIGV4A_AND_SIGV4,
                SIGV4A,
                "Empty preference list"
            ),

            Arguments.of(
                Arrays.asList(SIGV4A, SIGV4, BEARER),
                OPERATION_SIGV4A_AND_SIGV4,
                SIGV4A,
                "First preference is supported"
            )
        );
    }
}
