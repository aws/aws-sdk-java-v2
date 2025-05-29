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

package software.amazon.awssdk.services.multiauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.auth.AuthSchemePreferenceResolver;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.StringInputStream;

class AuthSchemePreferenceResolverTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.property());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("profileTestCases")
    void profileParsingTests(String testName, String profileContent, String profileName, List<String> expected) {
        ProfileFile profileFile = ProfileFile.builder()
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .content(new StringInputStream(profileContent))
                                             .build();

        AuthSchemePreferenceResolver.Builder resolverBuilder = AuthSchemePreferenceResolver.builder()
                                                                                           .profileFile(() -> profileFile);
        if (profileName != null) {
            resolverBuilder.profileName(profileName);
        }

        assertThat(resolverBuilder.build().resolveAuthSchemePreference()).isEqualTo(expected);
    }

    static Stream<Arguments> profileTestCases() {
        return Stream.of(
            Arguments.of(
                "Default profile parsing",
                "[default]\n" + ProfileProperty.AUTH_SCHEME_PREFERENCE + "=sigv4,bearer",
                null,
                Arrays.asList("sigv4", "bearer")
            ),
            Arguments.of(
                "Custom profile parsing",
                "[profile custom]\n" + ProfileProperty.AUTH_SCHEME_PREFERENCE + "=sigv4,bearer",
                "custom",
                Arrays.asList("sigv4", "bearer")
            ),
            Arguments.of(
                "Profile with whitespace",
                "[default]\n" + ProfileProperty.AUTH_SCHEME_PREFERENCE + "=sigv4, \tbearer \t",
                null,
                Arrays.asList("sigv4", "bearer")
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("systemSettingTestCases")
    void systemSettingParsingTests(String testName, String systemSetting, List<String> expected) {
        if (systemSetting != null) {
            System.setProperty(SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.property(), systemSetting);
        }

        AuthSchemePreferenceResolver resolver = AuthSchemePreferenceResolver.builder().build();
        assertThat(resolver.resolveAuthSchemePreference()).isEqualTo(expected);
    }

    static Stream<Arguments> systemSettingTestCases() {
        return Stream.of(
            Arguments.of("Basic system setting", "sigv4,bearer", Arrays.asList("sigv4", "bearer")),
            Arguments.of("Empty system setting", "", Collections.singletonList("")),
            Arguments.of("No system setting", null, Collections.emptyList()),

            // Whitespace/formatting cases (from schemeParsingCases)
            Arguments.of("Whitespace with tabs", "scheme1, scheme2 , \tscheme3 \t",
                         Arrays.asList("scheme1", "scheme2", "scheme3")),
            Arguments.of("Whitespace with joined schemes", "scheme1, scheme2 \t scheme3 scheme4",
                         Arrays.asList("scheme1", "scheme2scheme3scheme4")),
            Arguments.of("Whitespace in scheme names", "sigv4, sig v 4 a, bearer",
                         Arrays.asList("sigv4", "sigv4a", "bearer"))
        );
    }
}